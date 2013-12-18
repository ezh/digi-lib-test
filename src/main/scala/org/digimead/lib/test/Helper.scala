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

import language.reflectiveCalls

object Helper {
  /** Check if method exists. */
  def containsMethod(clazz: Class[_], name: String, params: java.lang.Class[_]*) = try {
    clazz.getMethod(name, params: _*)
    true
  } catch {
    case _: Throwable ⇒
      false
  }
  /** Log trace message if possible */
  def logtrace(obj: AnyRef, message: String) = try {
    getLogger(obj) match {
      case Some(logger) ⇒ logger.trace(message)
      case None ⇒ System.err.println("TRACE: " + message)
    }
  } catch {
    case e: NoSuchMethodException ⇒
      System.err.println("TRACE: " + message)
  }
  /** Log debug message if possible */
  def logdebug(obj: AnyRef, message: String) = try {
    getLogger(obj) match {
      case Some(logger) ⇒ logger.debug(message)
      case None ⇒ System.err.println("DEBUG: " + message)
    }
  } catch {
    case e: NoSuchMethodException ⇒
      System.err.println("DEBUG: " + message)
  }
  /** Log info message if possible */
  def loginfo(obj: AnyRef, message: String) = try {
    getLogger(obj) match {
      case Some(logger) ⇒ logger.info(message)
      case None ⇒ System.err.println("INFO: " + message)
    }
  } catch {
    case e: NoSuchMethodException ⇒
      System.err.println("INFO: " + message)
  }
  /** Log warn message if possible */
  def logwarn(obj: AnyRef, message: String) = try {
    getLogger(obj) match {
      case Some(logger) ⇒ logger.warn(message)
      case None ⇒ System.err.println("WARN: " + message)
    }
  } catch {
    case e: NoSuchMethodException ⇒
      System.err.println("WARN: " + message)
  }
  /** Log error message if possible */
  def logerror(obj: AnyRef, message: String) = try {
    getLogger(obj) match {
      case Some(logger) ⇒ logger.error(message)
      case None ⇒ System.err.println("ERROR: " + message)
    }
  } catch {
    case e: NoSuchMethodException ⇒
      System.err.println("ERROR: " + message)
  }
  /** Get logger if possible */
  def getLogger(obj: AnyRef): Option[org.slf4j.Logger] = try {
    obj.getClass.getDeclaredMethods().find { m ⇒ classOf[org.slf4j.Logger].isAssignableFrom(m.getReturnType()) }.
      map { method ⇒
        method.setAccessible(true)
        method.invoke(obj).asInstanceOf[org.slf4j.Logger]
      }
  } catch {
    case e: Throwable ⇒
      None
  }
}
