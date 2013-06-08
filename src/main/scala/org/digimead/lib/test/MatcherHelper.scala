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

import org.scalatest.exceptions.TestFailedException

trait MatcherHelper {
  def expectDefined(obj: Any)(test: PartialFunction[Any, Unit]) {
    if (!test.isDefinedAt(obj)) {
      val t = new Throwable
      throw new TestFailedException("Unexpected argument " + obj + "\n" + t.getStackTraceString, 1)
    }
  }
}
