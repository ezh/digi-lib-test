//
// Copyright (c) 2012-2014 Alexey Aksenov ezh@ezh.msk.ru
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// DEVELOPMENT CONFIGURATION

import sbt.osgi.manager._

OSGiManager // ++ sbt.scct.ScctPlugin.instrumentSettings - ScctPlugin is broken, have no time to fix

name := "digi-lib-test"

description := "Various test helpers for Digi components"

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

organization := "org.digimead"

organizationHomepage := Some(url("http://digimead.org"))

homepage := Some(url("https://github.com/ezh/digi-lib-test"))

version <<= (baseDirectory) { (b) => scala.io.Source.fromFile(b / "version").mkString.trim }

inConfig(OSGiConf)({
  import OSGiKey._
  Seq[Project.Setting[_]](
    osgiBndBundleSymbolicName := "org.digimead.digi.lib.test",
    osgiBndBundleCopyright := "Copyright © 2011-2014 Alexey B. Aksenov/Ezh. All rights reserved.",
    osgiBndExportPackage := List("org.digimead.*"),
    osgiBndImportPackage := List("!org.aspectj.*", "*"),
    osgiBndBundleLicense := "http://www.apache.org/licenses/LICENSE-2.0.txt;description=The Apache Software License, Version 2.0"
  )
})

crossScalaVersions := Seq("2.11.1")

scalaVersion := "2.11.1"

scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-Xcheckinit", "-feature")

// http://vanillajava.blogspot.ru/2012/02/using-java-7-to-target-much-older-jvms.html
javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation", "-source", "1.7", "-target", "1.7")

javacOptions in doc := Seq("-source", "1.7")

if (sys.env.contains("XBOOTCLASSPATH")) Seq(javacOptions += "-Xbootclasspath:" + sys.env("XBOOTCLASSPATH")) else Seq()

//
// Custom local options
//

resolvers += "digimead-maven" at "http://storage.googleapis.com/maven.repository.digimead.org/"

libraryDependencies ++= Seq(
    // https://issues.scala-lang.org/browse/SI-7751
    // .../guava-15.0.jar(com/google/common/cache/CacheBuilder.class)' is broken
    // [error] (class java.lang.RuntimeException/bad constant pool index: 0 at pos: 15214)
    "com.google.code.findbugs" % "jsr305" % "2.0.3",
    "com.google.guava" % "guava" % "17.0",
    "org.digimead" %% "pojosrx" % "0.2.3.0-SNAPSHOT",
    "org.mockito" % "mockito-core" % "1.9.5",
    "org.scalatest" %% "scalatest" % "2.2.0"
      excludeAll(ExclusionRule("org.scala-lang", "scala-reflect"), ExclusionRule("org.scala-lang", "scala-actors")),
    "org.slf4j" % "slf4j-log4j12" % "1.7.7",
    "org.digimead" %% "digi-lib" % "0.3.0.0-SNAPSHOT" % "test"
  )

//
// Testing
//

parallelExecution in Test := false

//logLevel := Level.Debug
