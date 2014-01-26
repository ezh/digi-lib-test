/**
 * Digi-Lib-Test - various test helpers for Digi components
 *
 * Copyright (c) 2012-2014 Alexey Aksenov ezh@ezh.msk.ru
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

import org.apache.log4j.{ ConsoleAppender, FileAppender, Layout, Level, Logger, PatternLayout }
import org.apache.log4j.spi.LoggingEvent
import org.apache.log4j.varia.NullAppender
import org.hamcrest.{ BaseMatcher, Description }
import org.mockito.{ ArgumentCaptor, Matchers, Mockito }
import org.mockito.Mockito.verify
import org.mockito.verification.VerificationMode
import org.scalatest.{ BeforeAndAfter, BeforeAndAfterAllConfigMap, ConfigMap, Suite }
import org.scalatest.mock.MockitoSugar

trait LoggingHelper extends Suite with BeforeAndAfter
  with BeforeAndAfterAllConfigMap with MockitoSugar {
  /** Mockito log intercepter */
  val logAppenderMock = mock[org.apache.log4j.Appender]
  /** Minimum log level if logging enabled. */
  val logLevel: Level = org.apache.log4j.Level.TRACE
  /** Log pattern. */
  lazy val logPattern: Layout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss.SSSZ} [%-60t] %-8p %-20c: %m%n")

  /** Check whether the logging is enabled. */
  def isLogEnabled(config: ConfigMap = ConfigMap.empty) = config.contains("log") || System.getProperty("log") != null
  /**
   * Log event matcher for Mockito.
   *
   * For example:
   * verify(logAppenderMock, Mockito.timeout(1000)).
   *   doAppend(logMatcher { _.getMessage().toString().startsWith("Restore stack for ") })
   */
  def logMatcher(f: LoggingEvent ⇒ Boolean) = Matchers.argThat(new BaseMatcher[LoggingEvent] {
    def describeTo(description: Description) {}
    def matches(event: AnyRef): Boolean = event match {
      case event: LoggingEvent ⇒ f(event)
    }
  })

  /** Add file appender to root logger. */
  protected def addFileAppender[T](fileName: String = s"test-${System.currentTimeMillis()}.dlog",
    f: FileAppender ⇒ T = (appender: FileAppender) ⇒ null.asInstanceOf[T]) {
    val appender = new FileAppender()
    appender.setName("FileLogger")
    appender.setFile(fileName)
    appender.setLayout(logPattern)
    appender.setThreshold(logLevel)
    appender.setAppend(true)
    f(appender)
    appender.activateOptions()
    org.apache.log4j.Logger.getRootLogger().addAppender(appender)
  }
  /** Setup logging before all tests. */
  protected def adjustLoggingBeforeAll(configMap: ConfigMap) {
    org.apache.log4j.BasicConfigurator.resetConfiguration()
    val root = org.apache.log4j.Logger.getRootLogger();
    Logger.getRootLogger().setLevel(logLevel)
    if (isLogEnabled(configMap))
      root.addAppender(new ConsoleAppender(logPattern))
    else
      root.addAppender(new NullAppender)
  }
  /** Deinitialize logging after test. */
  protected def adjustLoggingAfter() {
    org.apache.log4j.Logger.getRootLogger().removeAppender(logAppenderMock)
  }
  /** Initialize logging before test. */
  protected def adjustLoggingBefore() {
    org.apache.log4j.Logger.getRootLogger().addAppender(logAppenderMock)
  }

  /**
   * Mockito log captor.
   * @param a - test function with logging
   * @param b - Fx that tests log entitioes
   * @param option - mockito log options, for example
   *   'implicit val option = Mockito.atLeastOnce()' for multiple log entries
   */
  def withLogCaptor[A, B](a: ⇒ A)(b: ArgumentCaptor[org.apache.log4j.spi.LoggingEvent] ⇒ B)(implicit option: VerificationMode = Mockito.timeout(0)) = {
    Mockito.reset(logAppenderMock)
    val logCaptor = ArgumentCaptor.forClass(classOf[org.apache.log4j.spi.LoggingEvent])
    val result = a
    verify(logAppenderMock, option).doAppend(logCaptor.capture())
    b(logCaptor)
    result
  }
}

