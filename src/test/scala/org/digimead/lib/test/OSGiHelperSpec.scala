/**
 * Digi-Lib-Test - various test helpers for Digi components
 *
 * Copyright (c) 2013 Alexey Aksenov ezh@ezh.msk.ru
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

import org.digimead.digi.lib.DependencyInjection
import org.scalatest.ConfigMap
import org.scalatest.Matchers
import org.scalatest.WordSpec
import org.scalatest.mock.MockitoSugar
import org.slf4j.LoggerFactory

class OSGiHelperSpec extends WordSpec with OSGiHelper with LoggingHelper with Matchers with MockitoSugar {
  val log = LoggerFactory.getLogger(getClass)
  val testBundleClass = classOf[OSGiHelper]

  after {
    adjustOSGiAfter
  }
  before {
    DependencyInjection(org.digimead.digi.lib.default, false)
    adjustOSGiBefore
    osgiRegistry.foreach(_.start())
  }

  "OSGi framework" must {
    "running" in {
      val result = for {
        context ← osgiContext
        registry ← osgiRegistry
      } yield {
        val framework = context.getBundle()
        val bundles = context.getBundles()
        bundles should contain(framework)
        for (bundle ← bundles) {
          log.info("Bundle id: %s, symbolic name: %s, location: %s".
            format(bundle.getBundleId(), bundle.getSymbolicName(), bundle.getLocation()))
          val refs = bundle.getRegisteredServices()
          if (refs != null) {
            log.info("There are " + refs.length + " provided services")
            for (serviceReference ← refs) {
              for (key ← serviceReference.getPropertyKeys()) {
                log.info("\t" + key + " = " + (serviceReference.getProperty(key) match {
                  case arr: Array[_] ⇒ arr.mkString(",")
                  case n ⇒ n.toString
                }))
              }
              log.info("-----")
            }
          }
        }
      }
      assert(result.nonEmpty, "Unable to initialize OSGi framework.")
    }
  }

  override def beforeAll(configMap: ConfigMap) { adjustLoggingBeforeAll(configMap) }
}
