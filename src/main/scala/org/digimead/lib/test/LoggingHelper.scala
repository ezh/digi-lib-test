/**
 * Digi-Lib-Test - various test helpers for Digi components
 *
 * Copyright (c) 2012-2015 Alexey Aksenov ezh@ezh.msk.ru
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

import java.util.concurrent.{ ConcurrentHashMap, Exchanger, TimeUnit }
import org.apache.log4j.{ ConsoleAppender, EnhancedThrowableRenderer, FileAppender, Layout, Level, PatternLayout }
import org.apache.log4j.spi.{ LoggingEvent, ThrowableRenderer, ThrowableRendererSupport }
import org.apache.log4j.varia.NullAppender
import org.hamcrest.{ BaseMatcher, Description }
import org.mockito.{ ArgumentCaptor, Matchers, Mockito }
import org.mockito.Mockito.{ spy, verify }
import org.mockito.verification.VerificationMode
import org.scalatest.{ BeforeAndAfter, BeforeAndAfterAllConfigMap, ConfigMap, Finders, Suite }
import org.scalatest.mock.MockitoSugar
import scala.collection.JavaConverters.{ asScalaBufferConverter, mapAsScalaMapConverter }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait LoggingHelper extends Suite with BeforeAndAfter
    with BeforeAndAfterAllConfigMap with MockitoSugar {
  /** Mockito log intercepter */
  val testLogAppender = spy(new LoggingHelper.TestAppender)
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
      case event: LoggingEvent if event != null && event.getMessage() != null ⇒ f(event)
      case event ⇒ false
    }
  })
  /** Capture and test against log messages. */
  def logVerify[T](f: ⇒ T)(cb: LoggingEvent ⇒ Boolean)(implicit timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Boolean = {
    val exchanger = new Exchanger[Null]()
    val callback = new LoggingHelper.CaptureCallback(cb, exchanger)
    val limit = System.currentTimeMillis() + unit.toMillis(timeout)
    val hook = new LoggingHelper.TestHook(callback, limit)
    LoggingHelper.hooks += hook -> limit
    try { f; try { exchanger.exchange(null, timeout, unit); true } catch { case e: Throwable ⇒ false } }
    finally LoggingHelper.hooks -= hook
  }

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
    root.setLevel(logLevel)
    root.addAppender(testLogAppender)
    if (isLogEnabled(configMap))
      root.addAppender(new ConsoleAppender(logPattern))
    else
      root.addAppender(new NullAppender)
  }
  /** Enable detailed throwable output for log4j. */
  protected def enableDetailedThrowable() =
    org.apache.log4j.Logger.getRootLogger.getLoggerRepository match {
      case trs: ThrowableRendererSupport ⇒
        trs.setThrowableRenderer(new LoggingHelper.DetailedThrowableRenderer)
      case other ⇒
        throw new IllegalStateException("Log4j LoggerRepository isn't ThrowableRendererSupport")
    }

  /**
   * Mockito log captor.
   * @param a - test function with logging
   * @param b - Fx that tests log entities
   * @param option - mockito log options, for example
   *   'implicit val option = Mockito.atLeastOnce()' for multiple log entries
   */
  def withMockitoLogCaptor[A, B](a: ⇒ A)(b: ArgumentCaptor[org.apache.log4j.spi.LoggingEvent] ⇒ B)(implicit option: VerificationMode = Mockito.timeout(0)): A = {
    Mockito.reset(testLogAppender)
    val logCaptor = ArgumentCaptor.forClass(classOf[org.apache.log4j.spi.LoggingEvent])
    val result = a
    verify(testLogAppender, option).doAppend(logCaptor.capture())
    b(logCaptor)
    result
  }
  /**
   * Mockito log captor.
   * @param a - test function with logging
   * @param b - Fx that tests log entities (log level, log message, throwable)
   * @param option - mockito log options, for example
   *   'implicit val option = Mockito.atLeastOnce()' for multiple log entries
   */
  def withMockitoLogMatcher[A](a: ⇒ A)(b: PartialFunction[(Level, String, Option[String]), Boolean])(implicit option: VerificationMode = Mockito.timeout(0)): A = {
    val logTest: ArgumentCaptor[org.apache.log4j.spi.LoggingEvent] ⇒ Unit = {
      logCaptor ⇒
        val default: PartialFunction[(Level, String, Option[String]), Boolean] =
          { case (a, message, throwable) ⇒ false }
        val unexpectedEvents = logCaptor.getAllValues().asScala.filter { e ⇒
          !b.applyOrElse((e.getLevel, e.getMessage.toString(), Option(e.getThrowableStrRep).map(_.mkString("\n"))), default)
        }
        if (unexpectedEvents.nonEmpty) {
          val messages = for { e ← unexpectedEvents }
            yield s"${e.getLevel()}: ${e.getMessage()}" +
            (Option(e.getThrowableStrRep).map(_.mkString("\n")) match {
              case Some(throwable) ⇒ "\n" + throwable
              case None ⇒ ""
            })
          fail(s"Unexpected log message detected.\n" + messages.mkString("\n"))
        }
    }
    withMockitoLogCaptor(a)(logTest)(option)
  }
}

object LoggingHelper {
  val hooks = new ConcurrentHashMap[TestHook, Long].asScala

  /** Log appender that retransmit messages to test hooks. */
  class TestAppender extends NullAppender {
    override def doAppend(event: LoggingEvent) {
      if (event.getMessage() != null) {
        val keys = hooks.synchronized { hooks.keys.toSeq }
        keys.foreach(hook ⇒ Future { hook(event) })
      }
    }
  }
  /** Test hook that validates log event against the callback. */
  class TestHook(callback: CaptureCallback, limit: Long) {
    def apply(event: LoggingEvent) {
      val now = System.currentTimeMillis()
      if (limit < now)
        hooks -= this
      else {
        val eventMatches = event.getMessage() != null && {
          try callback(event)
          catch {
            case e: Throwable ⇒
              println("LoggingHelper.TestHook error: " + e.getMessage())
              e.printStackTrace()
              false
          }
        }
        if (eventMatches) {
          try callback.exchanger.exchange(null, 100, TimeUnit.MILLISECONDS)
          catch { case e: Throwable ⇒ }
          hooks -= this
        }
      }
    }
  }
  /** Log callback for LoggingHelper.logVerify. */
  class CaptureCallback(val f: LoggingEvent ⇒ Boolean, val exchanger: Exchanger[Null]) {
    def apply(event: LoggingEvent): Boolean = f(event)
  }
  /** Implementation of ThrowableRenderer with detailed stacktrace. */
  class DetailedThrowableRenderer extends ThrowableRenderer {
    /** Base renderer. */
    val renderer = new EnhancedThrowableRenderer()

    /**
     * Render Throwable.
     * @param t throwable, may not be null.
     * @return String representation.
     */
    def doRender(throwable: Throwable): Array[String] = {
      val header = renderer.doRender(throwable)
      Option(throwable.getCause) match {
        case Some(cause) ⇒
          val footer = doRender(cause)
          footer(0) = "Caused by: " + footer(0)
          header ++ footer
        case None ⇒
          header
      }
    }
  }
}
