/**
 * Digi-Lib-Test - various test helpers for Digi components
 *
 * Copyright (c) 2012-2013 Alexey Aksenov ezh@ezh.msk.ru
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

import java.io.{ BufferedInputStream, ByteArrayInputStream, File, FileInputStream, FileOutputStream, FileWriter, InputStream, OutputStream }
import java.math.BigInteger
import java.nio.channels.FileChannel
import java.security.{ DigestInputStream, MessageDigest }
import scala.annotation.tailrec

/**
 * Add a file routines to testing infrastructure
 */
trait StorageHelper {
  /** Recursively copy a folder or a file */
  def copy(from: File, to: File): Unit =
    if (from.isDirectory())
      Option(from.listFiles()) match {
        case Some(files) ⇒
          to.mkdirs()
          files.foreach(file ⇒ copy(file, new File(to, file.getName())))
        case None ⇒
      }
    else
      copyFile(from, to)
  /** Copy a file */
  def copyFile(sourceFile: File, destFile: File): Boolean = {
    if (!destFile.exists())
      destFile.createNewFile()
    var source: FileChannel = null
    var destination: FileChannel = null
    try {
      source = new FileInputStream(sourceFile).getChannel()
      destination = new FileOutputStream(destFile).getChannel()
      destination.transferFrom(source, 0, source.size())
    } finally {
      if (source != null) {
        source.close()
      }
      if (destination != null) {
        destination.close()
      }
    }
    sourceFile.length == destFile.length
  }
  /** Recursively delete a folder or delete a file */
  def deleteFolder(folder: File): Unit = {
    assert(folder != null, "folder must be non-null")
    for (f ← Option(folder.listFiles) getOrElse { Helper.logwarn(getClass, "Folder %s does not exist or not a file".format(folder)); Array[File]() }) {
      if (f.isDirectory) {
        deleteFolder(f)
      } else {
        f.delete
      }
    }
    folder.delete
  }
  /** Calculate digest for a byte array. */
  def digest(data: Array[Byte]): Option[String] = digest(data, "SHA-1")
  /** Calculate digest for a byte array. */
  def digest(data: Array[Byte], algorithm: String): Option[String] =
    digest(new ByteArrayInputStream(data), algorithm)
  /** Calculate digest for a file. */
  def digest(file: File): Option[String] = digest(file, "SHA-1")
  /** Calculate digest for a file. */
  def digest(file: File, algorithm: String): Option[String] =
    digest(new BufferedInputStream(new FileInputStream(file)), algorithm)
  /** Calculate digest for a stream and close it. */
  def digest(stream: InputStream, algorithm: String = "SHA-1"): Option[String] = {
    val md = MessageDigest.getInstance(algorithm)
    var is: InputStream = stream
    try {
      is = new DigestInputStream(is, md)
      val buffer = new Array[Byte](1024)
      var read = is.read(buffer)
      while (read != -1)
        read = is.read(buffer)
    } catch {
      case e: Throwable ⇒
        Helper.logwarn(getClass, "Unable to calculate digest: " + e.getMessage())
        return None
    } finally {
      is.close()
    }
    val bigInt = new BigInteger(1, md.digest())
    Some(String.format("%32s", bigInt.toString(16)).replace(' ', '0'))
  }
  /** Write to a file */
  def writeToFile(file: File, text: String) {
    val fw = new FileWriter(file)
    try { fw.write(text) }
    finally { fw.close }
  }
  /** Write to a stream */
  def writeToStream(in: InputStream, out: OutputStream, bufferSize: Int = 8192) {
    val buffer = new Array[Byte](bufferSize)
    @tailrec
    def next(exit: Boolean = false) {
      if (exit) {
        in.close()
        out.close()
        return
      }
      val read = in.read(buffer)
      if (read > 0)
        out.write(buffer, 0, read)
      next(read == -1)
    }
    next()
  }
  /** Allocate temporary folder for code block */
  def withTempFolder[T](f: (File) ⇒ T): Unit = {
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
  /** Iterate over directory recursively */
  def visitPath[T](path: File, visitor: File ⇒ T) {
    val list = path.listFiles()
    if (list == null) return
    for (f ← list) {
      if (f.isDirectory())
        visitPath(f, visitor)
      visitor(f)
    }
  }
}
