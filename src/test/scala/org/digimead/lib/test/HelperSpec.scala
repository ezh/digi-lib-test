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
import org.digimead.digi.lib.log.api.Loggable
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpec
import org.scalatest.Matchers
import org.slf4j.LoggerFactory

class HelperSpec extends WordSpec with BeforeAndAfterAll with Matchers {
  DependencyInjection(org.digimead.digi.lib.default, false)

  "Helper" should {
    "have proper detection of RichLogger classes via reflection" in {
      val stub = new TestLogger
      assert(Helper.containsMethod(stub.getClass(), "debugWhere", classOf[String], java.lang.Integer.TYPE))
      assert(Helper.containsMethod(stub.getClass(), "debugWhere", classOf[String]))
      assert(!Helper.containsMethod(stub.getClass(), "debugWhere1", classOf[String]))
    }
    "log messages" in {
      val a = new Test1
      Helper.getLogger(a) should not be ('empty)
      val b = new Test2
      Helper.getLogger(b) should not be ('empty)
      val c = new Test3
      Helper.getLogger(c) should be('empty)
    }
  }
  class TestLogger {
    /** Log a debug message with the caller location. */
    def debugWhere(msg: String) {}
    /** Log a debug message with the specific caller location. */
    def debugWhere(msg: String, stackLine: Int) {}
  }
  class Test1 {
    val log = LoggerFactory.getLogger(getClass)
  }
  class Test2 extends Loggable {
  }
  class Test3 {
  }
}
