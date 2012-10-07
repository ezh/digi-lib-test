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

import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec

import org.digimead.digi.lib.log.Logging
import org.digimead.digi.lib.log.Record
import org.digimead.digi.lib.log.appender.Appender
import org.digimead.digi.lib.log.appender.Console
import org.digimead.digi.lib.log.appender.NullAppender
import org.digimead.digi.lib.util.SyncVar

trait TestHelperLogging {
  val logHistory = new AtomicReference[Seq[Record]](Seq())
  val logSearchFunction = new AtomicReference[(String, (String, String) => Boolean)](null)
  val logSearchResult = new SyncVar[Record]()
  val logSubscriber = new LogSubscriber

  def withLogging[T](map: Map[String, Any], appenders: Seq[Appender] = Seq(Console))(f: => T) {
    val logAppenders: Seq[Appender] = if (map.contains("log") || System.getProperty("log") != null)
      appenders
    else
      Seq(NullAppender)
    try {
      Logging.addAppender(logAppenders)
      Logging.Event.subscribe(logSubscriber)
      f // do testing
      Logging.Event.removeSubscription(logSubscriber)
    } finally {
      Logging.delAppender(logAppenders)
    }
  }
  def assertLog(s: String, f: (String, String) => Boolean)(implicit timeout: Long): Record = {
    logSubscriber.synchronized {
      searchLogHistory(s, f, logHistory.getAndSet(Seq())) match {
        case Some((record, otherRecords)) =>
          logHistory.set(otherRecords)
          logSearchFunction.set(null)
          logSearchResult.unset()
          return record
        case None =>
          logSearchFunction.set(s, f)
          logSearchResult.unset()
      }
    }
    val result = logSearchResult.get(timeout)
    assert(result != None, "log record \"" + s + "\" not found")
    result.get
  }
  @tailrec
  final def searchLogHistory(s: String, f: (String, String) => Boolean, history: Seq[Record]): Option[(Record, Seq[Record])] = {
    history match {
      case x :: xs =>
        if (f(x.message.trim, s.trim))
          return Some(x, xs)
        else
          searchLogHistory(s, f, xs)
      case Nil =>
        None
    }
  }
  class LogSubscriber extends Logging.Event.Sub {
    def notify(pub: Logging.Event.Pub, event: Logging.Event) = synchronized {
      event match {
        case event: Logging.Event.Outgoing =>
          logSearchFunction.get match {
            case null =>
              logHistory.set(logHistory.get :+ event.record)
            case (message, f) if event.record.message == null =>
            // skip, offline logHistory is useless, we are online
            case (message, f) if f != null && message != null && event.record.message != null =>
              if (f(event.record.message.trim, message.trim)) {
                logHistory.set(Seq())
                logSearchFunction.set(null)
                logSearchResult.put(event.record, 60000)
              }
          }
        case _ =>
      }
    }
  }
}
