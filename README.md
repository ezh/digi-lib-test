digi-lib-test
=============

Various test helpers for Digi components

If you want improve it, please send request to _ezh_ at _ezh.msk.ru_. You will be given the permissions to the repository. Please, feel free to add yourself to authors.

DOCUMENTATION
-------------

### Setup

```scala
libraryDependencies += Seq(
  "org.digimead" %% "digi-lib-test" % "VERSION"
)

resolvers += "digi-lib-test" at "http://ezh.github.com/digi-lib-test/releases"
```

Download jar files directly from the [GitHub](https://github.com/ezh/digi-lib-test/tree/master/publish/releases/org/digimead)

### [API (latest version)](http://ezh.github.com/digi-lib-test/api/)

## Target platform

* Scala 2.8.2, 2.9.0, 2.9.0-1, 2.9.1, 2.9.2 (request for more if needed)
* JVM 1.5+
* The only 3rd-party library dependency is [SLF4J](http://www.slf4j.org/)

## Participate in the development ##

Branches:

* origin/master reflects a production-ready state
* origin/release-* support preparation of a new production release. Allow for last-minute dotting of i’s and crossing t’s
* origin/hotfix-* support preparation of a new unplanned production release
* origin/develop reflects a state with the latest delivered development changes for the next release (nightly builds)
* origin/feature-* new features for the upcoming or a distant future release

Structure of branches follow strategy of http://nvie.com/posts/a-successful-git-branching-model/

If you will create new origin/feature-* please open feature request for yourself.

* Anyone may comment you feature here.
* We will have a history for feature and ground for documentation
* If week passed and there wasn't any activity + all tests passed = release a new version ;-)

AUTHORS
-------

* Alexey Aksenov

LICENSE
-------

The Digi-Lib-Test Project is licensed to you under the terms of
the Apache License, version 2.0, a copy of which has been
included in the LICENSE file.
Please check the individual source files for details.

Copyright
---------

Copyright © 2012-2013 Alexey B. Aksenov/Ezh. All rights reserved.
