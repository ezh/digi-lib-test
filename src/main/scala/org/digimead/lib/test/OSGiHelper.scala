/**
 * Digi-Lib-Test - various test helpers for Digi components
 * Based on few ideas from Yoann/Adele Team code under ASF 2.0 license.
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

import de.kalpatec.pojosr.framework.PojoSR
import de.kalpatec.pojosr.framework.launch.{ BundleDescriptor, ClasspathScanner, PojoServiceRegistryFactory }
import java.io.{ File, InputStream }
import java.net.URL
import java.util.ServiceLoader
import org.osgi.framework.{ BundleContext, Constants }
import scala.collection.JavaConversions.{ asScalaBuffer, seqAsJavaList }

trait OSGiHelper {
  /** Abstract value with test bundle class. */
  val testBundleClass: Class[_] // an any class from the SUIT that allow to determine proper jar

  /** The framework OSGi cache path. */
  @volatile protected var osgiCache: Option[File] = None
  /** Flag indicating whether  */
  @volatile protected var osgiCacheCleanFlag = true
  /** The framework bundle Context. */
  @volatile protected var osgiContext: Option[BundleContext] = None
  /** The registry used to register services. */
  @volatile protected var osgiRegistry: Option[PojoSR] = None
  /** PojoSR configuration */
  protected lazy val osgiConfig = {
    val config = new java.util.HashMap[String, AnyRef]()
    val allBundles = new ClasspathScanner().scanForBundles() // ArrayList[BundleDescriptor]
    allBundles.add(new BundleDescriptor(getClass.getClassLoader, getTestBundleURL(testBundleClass), getTestBundleHeaders))
    val bundles: java.util.List[BundleDescriptor] = (for (bundle ← allBundles) yield {
      val bundleURL = bundle.getUrl()
      val bundleSymbolicName = bundle.getHeaders().get(Constants.BUNDLE_SYMBOLICNAME)
      if (bundleURL != null && bundleURL.toString.matches(osgiIgnoredBundlesURLPatterns)) {
        Helper.logdebug(this, "Skip " + bundle)
        None
      } else if (bundleSymbolicName != null && bundleSymbolicName.matches(osgiIgnoredBundlesSymbolicNamePatterns)) {
        Helper.logdebug(this, "Skip " + bundle)
        None
      } else
        Some(bundle)
    }).flatten.toList
    config.put(PojoServiceRegistryFactory.BUNDLE_DESCRIPTORS, bundles)
    config
  }
  /**
   * Delay to wait between each test (can be customized by overriding the
   * setUp() method.
   */
  protected val osgiDelayBetweenTestInMs = 500
  /**
   * A pattern used to filter bundles URL (each bundle's URL is tested against
   * this pattern)
   */
  protected val osgiIgnoredBundlesURLPatterns = ".*jcommander.*|.*testng.*|.*snakeyaml.*|.*beanshell.*|.*surefire.*|.*junit.*|.*asm.*"
  /**
   * A pattern used to filter bundles symbolic name (each bundle's name is
   * tested against this pattern)
   */
  protected val osgiIgnoredBundlesSymbolicNamePatterns = ""
  /**
   * This property is used by POJOSR to know where to store the cache
   */
  protected val osgiFrameworkStorage = "org.osgi.framework.storage"

  /** Deinitialize OSGi after test. */
  protected def adjustOSGiAfter() {
    // Tear down the framework (configure Java and the OSGi environment).
    osgiContext.map(_.getBundle().stop())
    // Wait after testing (the time for bundle to stop)
    Thread.sleep(osgiDelayBetweenTestInMs)
    if (osgiCacheCleanFlag)
      osgiCache.foreach { cache ⇒
        Helper.loginfo(this, "Delete OSGi cache at " + cache.getCanonicalPath())
        Helper.deleteFile(this, cache)
      }
    osgiCache = None
  }
  /** Initialize OSGi before test. */
  protected def adjustOSGiBefore() {
    // Set up the framework (configure Java and the OSGi environment).
    // Create a random unique cache dir for each test method
    osgiCache = Some(File.createTempFile("osgi-", "-cache"))
    osgiCache.foreach { cache ⇒
      cache.delete
      cache.mkdirs
      cache.deleteOnExit
      Helper.loginfo(this, "Create OSGi cache at " + cache.getCanonicalPath())
      System.setProperty(osgiFrameworkStorage, cache.getCanonicalPath())
    }
    System.setProperty("de.kalpatec.pojosr.framework.events.sync", "true")
    // Initialize service registry
    val loader = ServiceLoader.load(classOf[PojoServiceRegistryFactory])
    // Build a new framework
    osgiRegistry = Some(new PojoSR(osgiConfig))
    osgiRegistry.foreach(_.prepare())
    osgiRegistry.map { registry ⇒
      // Keep a track of the fk bundle context
      osgiContext = Some(registry.getBundleContext())
      // Wait before testing (the time for bundle to start)
      Thread.sleep(osgiDelayBetweenTestInMs)
    }
  }
  /** Returns URL that points to the system-under-test location */
  protected def getTestBundleURL(clazz: Class[_]): URL = {
    val testBundlePackageClass = clazz.getName().replaceAll("""\.""", "/") + ".class"
    val testBundlePackageURL = try {
      getClass.getClassLoader.getResources(testBundlePackageClass).nextElement() // sure that we have one
    } catch {
      case e: NoSuchElementException ⇒
        throw new IllegalArgumentException("Unable to find bundle with class %s.".format(testBundlePackageClass), e)
    }
    val externalForm = testBundlePackageURL.toExternalForm()
    new URL(externalForm.substring(0, externalForm.length - testBundlePackageClass.length))
  }
  // Based on de.kalpatec.pojosr.framework.launch.ClasspathScanner.scanForBundles
  /** Collect system-under-test manifest headers */
  protected def getTestBundleHeaders(): java.util.HashMap[String, String] = {
    val testBundleManifest = "TEST-MANIFEST.MF"
    val testBundleManifestURL = try {
      getClass.getClassLoader.getResources(testBundleManifest).nextElement() // sure that we have one
    } catch {
      case e: NoSuchElementException ⇒
        throw new IllegalArgumentException("Unable to find 'TEST-MANIFEST.MF' for OSGi testing.", e)
    }
    val headers = new java.util.HashMap[String, String]()
    var bytes = new Array[Byte](1024 * 1024 * 2)
    var input: InputStream = null
    try {
      input = testBundleManifestURL.openStream()
      var size = 0
      var i = input.read(bytes)
      while (i != -1) {
        size += i
        if (size == bytes.length) {
          val tmp = new Array[Byte](size * 2)
          System.arraycopy(bytes, 0, tmp, 0, bytes.length)
          bytes = tmp
        }
        i = input.read(bytes, size, bytes.length - size)
      }

      // Now parse the main attributes. The idea is to do that
      // without creating new byte arrays. Therefore, we read through
      // the manifest bytes inside the bytes array and write them back into
      // the same array unless we don't need them (e.g., \r\n and \n are skipped).
      // That allows us to create the strings from the bytes array without the skipped
      // chars. We stopp as soon as we see a blankline as that denotes that the main
      //attributes part is finished.
      var key: String = null
      var last = 0
      var current = 0
      i = 0
      while (i < size) {
        bytes(i) match {
          case '\r' if (i + 1 < size) && (bytes(i + 1) == '\n') ⇒
          // skip \r and \n if it is follows by another \n
          // (we catch the blank line case in the next iteration)
          case '\n' if ((i + 1 < size) && (bytes(i + 1) == ' ')) ⇒
            i += 1 // skip space
          case ':' if key == null ⇒
            // If we don't have a key yet and see the first : we parse it as the key
            // and skip the :<blank> that follows it.
            key = new String(bytes, last, (current - last), "UTF-8")
            if ((i + 1 < size) && (bytes(i + 1) == ' ')) {
              last = current + 1
              //continue;
            } else {
              throw new Exception("Manifest error: Missing space separator - " + key)
            }
          case '\n' ⇒
            if ((last == current) && (key == null)) {
              // if we are at the end of a line
              // and it is a blank line stop parsing (main attributes are done)
              i == size
            } else {
              // Otherwise, parse the value and add it to the map (we throw an
              // exception if we don't have a key or the key already exist.
              val value = new String(bytes, last, (current - last), "UTF-8")
              if (key == null)
                throw new Exception("Manifest error: Missing attribute name - " + value)
              else if (headers.put(key, value) != null)
                throw new Exception("Manifest error: Duplicate attribute name - " + key)
              last = current
              key = null
            }
          case _ ⇒
            // write back the byte if it needs to be included in the key or the value.
            if (current != i)
              bytes(current) = bytes(i)
            current += 1
        }
        i += 1
      }
    } finally {
      if (input != null)
        input.close()
    }
    headers
  }
}
