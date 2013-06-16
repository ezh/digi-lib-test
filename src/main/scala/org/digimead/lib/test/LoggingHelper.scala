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

import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout
import org.apache.log4j.spi.LoggingEvent
import org.apache.log4j.varia.NullAppender
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.verification.VerificationMode
import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite
import org.scalatest.mock.MockitoSugar

trait LoggingHelper extends Suite with BeforeAndAfter with BeforeAndAfterAll with MockitoSugar {
  /** Mockito log intercepter */
  val logAppenderMock = mock[org.apache.log4j.Appender]

  def isLogEnabled(config: Map[String, Any] = Map()) = config.contains("log") || System.getProperty("log") != null

  /** Setup logging before all tests. */
  protected def adjustLoggingBeforeAll(configMap: Map[String, Any]) {
    org.apache.log4j.BasicConfigurator.resetConfiguration()
    val root = org.apache.log4j.Logger.getRootLogger();
    Logger.getRootLogger().setLevel(org.apache.log4j.Level.TRACE)
    if (isLogEnabled(configMap))
      root.addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)))
    else
      root.addAppender(new NullAppender)
  }
  /** Deinitialize logging after test. */
  protected def adjustLoggingAfter {
    org.apache.log4j.Logger.getRootLogger().removeAppender(logAppenderMock)
  }
  /** Initialize logging before test. */
  protected def adjustLoggingBefore {
    org.apache.log4j.Logger.getRootLogger().addAppender(logAppenderMock)
  }

  /**
   * Mockito log captor.
   * @param a - test function with logging
   * @param b - Fx that tests log entitioes
   * @param option - mockito log options, for example
   *   'implicit val option = Mockito.atLeastOnce()' for multiple log entries
   */
  def withLogCaptor[A, B](a: => A)(b: ArgumentCaptor[org.apache.log4j.spi.LoggingEvent] => B)(implicit option: VerificationMode = Mockito.timeout(0)) = {
    Mockito.reset(logAppenderMock)
    val logCaptor = ArgumentCaptor.forClass(classOf[org.apache.log4j.spi.LoggingEvent])
    val result = a
    verify(logAppenderMock, option).doAppend(logCaptor.capture())
    b(logCaptor)
    result
  }
}
