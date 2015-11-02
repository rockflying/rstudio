/*
 * FileLocktests.cpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

#include <core/FileLock.hpp>

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace tests {

const char* const s_unitTests = "unit_tests";

TEST_CASE("File Locking")
{
   SECTION("A lock can only be acquired once")
   {
      // ensure the phantom file system is cleared
      phantomFileSystemPath().removeIfExists();
      
      Error error;
      
      FileLock lock1;
      FileLock lock2;
      
      error = lock1.acquire(FilePath("/tmp"));
      if (error)
         LOG_ERROR(error);
      
      CHECK((error == Success()));
      
      error = lock2.acquire(FilePath("/tmp"));
      CHECK_FALSE((error == Success()));
      
      // release and re-acquire
      error = lock1.release();
      CHECK((error == Success()));
      
      error = lock2.acquire(FilePath("/tmp"));
      CHECK((error == Success()));
   }
}

} // end namespace tests
} // end namespace core
} // end namespace rstudio
