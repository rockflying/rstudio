/*
 * FileLock.hpp
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

#ifndef CORE_FILE_LOCK_HPP
#define CORE_FILE_LOCK_HPP

#include <core/FilePath.hpp>

#include <boost/noncopyable.hpp>

namespace rstudio {
namespace core {

class Error;
class FilePath;

class FileLock : boost::noncopyable
{
public:
   static bool isLocked(const FilePath& filePath);
   Error acquire(const FilePath& filePath);
   Error release();
   FilePath filePath() const { return filePath_; }
private:
   FilePath filePath_;
};

const FilePath& phantomFileSystemPath();

} // namespace core
} // namespace rstudio


#endif // CORE_FILE_LOCK_HPP
