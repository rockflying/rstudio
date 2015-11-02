/*
 * FileLock.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <sys/types.h>
#include <unistd.h>

#include <set>
#include <vector>

#define RSTUDIO_DEBUG_LABEL "FileLock"
#define RSTUDIO_ENABLE_DEBUG_MACROS
#include <core/Macros.hpp>

#include <core/Algorithm.hpp>
#include <core/FileLock.hpp>

#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>

// A file locking mechanism using directories on the filesystem to indicate
// who and what processes hold locks.

namespace rstudio {
namespace core {

namespace {

const std::string getProcessIdStringImpl()
{
   std::stringstream ss;
   ss << (long) ::getpid();
   return ss.str();
}

const std::string& getProcessIdString()
{
   static const std::string instance = getProcessIdStringImpl();
   return instance;
}

namespace detail {
FilePath lockPathRoot()
{
   FilePath path("/tmp/rstudio-locks");
   path.ensureDirectory();
   return path.canonicalPath();
}
} // namespace detail

FilePath& lockPathRoot()
{
   static FilePath instance = detail::lockPathRoot();
   return instance;
}

FilePath& isLockingPath()
{
   static FilePath instance = lockPathRoot().complete("lock");
   return instance;
}

Error beginLocking()
{
   if (isLockingPath().exists())
      return fileExistsError(ERROR_LOCATION);
   
   return isLockingPath().ensureDirectory();
}

bool endLocking()
{
   return isLockingPath().removeIfExists();
}

class EndLockingScope : boost::noncopyable
{
public:
   ~EndLockingScope()
   {
      endLocking();
   }
};

Error getRunningProcessIds(std::set<std::string>* pContainer)
{
   // enumerate children in the /proc directory
   std::vector<FilePath> children;
   Error error = FilePath("/proc").children(&children);
   if (error)
      return error;
   
   // loop through each directory, and figure out if it's
   // a running rsession
   BOOST_FOREACH(const FilePath& filePath, children)
   {
      if (!filePath.isDirectory())
         continue;
      
      FilePath exePath = filePath.complete("exe");
      if (!exePath.exists())
         continue;
      
      FilePath normalizedPath = exePath.canonicalPath();
      std::string name = normalizedPath.filename();
      
      if (name == "rsession")
         pContainer->insert(filePath.filename());
   }
   
   return Success();
}

bool isRStudioProcess(const std::string& pid)
{
#ifdef __APPLE__
   return true;
#else
   std::set<std::string> processes;
   Error error = getRunningProcessIds(&processes);
   if (error)
      LOG_ERROR(error);
   return processes.count(pid);
#endif
}

} // anonymous namespace

const FilePath& phantomFileSystemPath()
{
   static FilePath instance = lockPathRoot().complete("pfs");
   return instance;
}

bool FileLock::isLocked(const FilePath& filePath)
{
   FilePath lockFilePath = phantomFileSystemPath().complete(filePath.absolutePathNative());
   return lockFilePath.exists() && !lockFilePath.empty();
}

Error FileLock::acquire(const FilePath& filePath)
{
   DEBUG("> Attempting to lock '" << filePath.absolutePath() << "'");
   
   // attempt to acquire the lock. for safety, only one process can
   // lock a file at a time.
   Error error = beginLocking();
   if (error)
      return error;
   
   // allow other processes to lock files when we're done
   EndLockingScope scope;
   
   // get the absolute path to the file (without the leading slash)
   std::string absPath = filePath.absolutePath();
   if (absPath.size() > 0 && absPath[0] == '/')
      absPath = absPath.substr(1);
   
   // lock the file by generating a lock directory in our phantom file system
   FilePath lockFilePath = phantomFileSystemPath().complete(absPath);
   
   DEBUG("? PFS: '" << phantomFileSystemPath().absolutePath());
   DEBUG("? Abs: '" << absPath << "'");
   DEBUG("? Lock file path: '" << lockFilePath.absolutePath() << "'");
   
   // if no directory exists, create it
   if (!lockFilePath.exists())
   {
      Error error = lockFilePath.ensureDirectory();
      if (error)
         return error;
   }
   
   DEBUG("> Created lock file path.");
   
   // if a directory exists, attempt to grab a lock
   std::vector<FilePath> childPaths;
   error = lockFilePath.children(&childPaths);
   if (error)
      return error;

   // if there are children, try to reap if appropriate -- find 'stale'
   // PIDs
   for (std::vector<FilePath>::iterator it = childPaths.begin();
        it != childPaths.end();
        ++it)
   {
      const FilePath& pidPath = *it;
      std::string pid = pidPath.filename();

      // reap if it's not attached to a running RStudio process
      if (!isRStudioProcess(pid))
      {
         DEBUG("Reaping lock from process '" << pid << "'");
         Error error = pidPath.remove();
         if (error)
            LOG_ERROR(error);
      }
   }
   
   // enumerate children once more -- if there are no children, take
   // ownership
   childPaths.clear();
   error = lockFilePath.children(&childPaths);
   if (error)
      return error;
   
   DEBUG("? There are " << childPaths.size() << " lock(s) active.");

   if (!childPaths.empty())
      return core::fileExistsError(ERROR_LOCATION);
   
   DEBUG("? File is not locked; locking...");

   FilePath lockFile = lockFilePath.complete(getProcessIdString());
   DEBUG("? Lock file path on PFS: '" << lockFile.absolutePath() << "'");
   error = lockFile.ensureDirectory();
   if (error)
      return error;
   
   DEBUG("> Successfully locked file: '" << filePath.absolutePath() << "'");
   DEBUG("> Lockfile at: " << lockFile.absolutePath() << "'");
   
   filePath_ = lockFile;
   return Success();
}

Error FileLock::release()
{
   DEBUG("Releasing '" << filePath_.absolutePath() << "'");
   
   // if the file doesn't exist, something weird happened?
   if (!filePath_.exists())
      return Success();
   
   Error error = filePath_.remove();
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   // clear all directories up to pfs
   FilePath parent = filePath_;
   while (parent != phantomFileSystemPath())
   {
      if (parent.empty())
         parent.remove();
      parent = parent.parent();
   }
   
   return Success();
}

} // namespace core
} // namespace rstudio



