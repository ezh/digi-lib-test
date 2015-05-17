/**
 * Digi-Lib-Test - various test helpers for Digi components
 *
 * Copyright (c) 2013-2015 Alexey Aksenov ezh@ezh.msk.ru
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

import scala.collection.JavaConversions._

import org.scalatest.ConfigMap
import org.scalatest.WordSpec
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import org.slf4j.LoggerFactory

class LoggingHelperSpec extends WordSpec with LoggingHelper with Matchers with MockitoSugar {
  val log = LoggerFactory.getLogger(getClass)

  "LoggingHelper" should {
    "interaction with mockito" in {
      withMockitoLogCaptor { log.debug("mockito test interception") } { logCaptor ⇒
        val enter = logCaptor.getAllValues().head
        enter.getLevel() should be(org.apache.log4j.Level.DEBUG)
        enter.getMessage.toString should be("mockito test interception")
      }
    }
    "provides detailed description of throwable" in {
      val cause1 = new RuntimeException("cause1")
      val cause2 = new RuntimeException("cause2", cause1)
      val renderer = new LoggingHelper.DetailedThrowableRenderer
      val result = renderer.doRender(new RuntimeException("123", cause2))
      assert(result.filter(_.startsWith("Caused by: ")).size === 2)
    }
  }

  override def beforeAll(configMap: ConfigMap) { adjustLoggingBeforeAll(configMap) }
}
