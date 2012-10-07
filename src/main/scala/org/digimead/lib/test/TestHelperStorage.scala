/**
 * Digi-Lib-Test - various test helpers for Digi components
 *
 * Copyright (c) 2012 Alexey Aksenov ezh@ezh.msk.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.digimead.lib.test

import java.io.File

trait TestHelperStorage {
  // recursively delete a folder. should be built in. bad java.
  private def deleteFolder(folder: File): Unit = {
    for (f <- folder.listFiles) {
      if (f.isDirectory) {
        deleteFolder(f)
      } else {
        f.delete
      }
    }
    folder.delete
  }

  def withTempFolder[T](f: (File) => T): Unit = {
    val tempFolder = System.getProperty("java.io.tmpdir")
    var folder: File = null
    do {
      folder = new File(tempFolder, "scala-test-" + System.currentTimeMillis)
    } while (!folder.mkdir)
    try {
      f(folder)
    } finally {
      deleteFolder(folder)
    }
  }
}
