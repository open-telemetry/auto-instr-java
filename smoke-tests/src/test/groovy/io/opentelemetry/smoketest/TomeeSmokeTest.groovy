/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.smoketest

import java.time.Duration

abstract class TomeeSmokeTest extends AppServerTest {

  protected String getTargetImagePrefix() {
    "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet-tomee"
  }

  @Override
  protected TargetWaitStrategy getWaitStrategy() {
    return new TargetWaitStrategy.Log(Duration.ofMinutes(3), ".*Server startup in.*")
  }

  @Override
  protected String getSpanName(String path) {
    switch (path) {
      case "/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless":
        return "GET /*"
    }
    return super.getSpanName(path)
  }
}

@AppServer(version = "7.0.9", jdk = "8")
class Tomee70Jdk8 extends TomeeSmokeTest {
}

@AppServer(version = "7.0.9", jdk = "8-openj9")
class Tomee70Jdk8Openj9 extends TomeeSmokeTest {
}

@AppServer(version = "7.1.4", jdk = "8")
class Tomee71Jdk8 extends TomeeSmokeTest {
}

@AppServer(version = "7.1.4", jdk = "8-openj9")
class Tomee71Jdk8Openj9 extends TomeeSmokeTest {
}

@AppServer(version = "8.0.14", jdk = "8")
class Tomee8Jdk8 extends TomeeSmokeTest {
}

@AppServer(version = "8.0.14", jdk = "11")
class Tomee8Jdk11 extends TomeeSmokeTest {
}

@AppServer(version = "8.0.14", jdk = "17")
class Tomee8Jdk17 extends TomeeSmokeTest {
}

@AppServer(version = "8.0.14", jdk = "20")
class Tomee8Jdk20 extends TomeeSmokeTest {
}

@AppServer(version = "8.0.14", jdk = "21")
class Tomee8Jdk21 extends TomeeSmokeTest {
}

@AppServer(version = "8.0.14", jdk = "8-openj9")
class Tomee8Jdk8Openj9 extends TomeeSmokeTest {
}

@AppServer(version = "8.0.14", jdk = "11-openj9")
class Tomee8Jdk11Openj9 extends TomeeSmokeTest {
}

@AppServer(version = "8.0.14", jdk = "17-openj9")
class Tomee8Jdk17Openj9 extends TomeeSmokeTest {
}

@AppServer(version = "8.0.14", jdk = "18-openj9")
class Tomee8Jdk18Openj9 extends TomeeSmokeTest {
}

@AppServer(version = "9.0.0", jdk = "8")
class Tomee9Jdk8 extends TomeeSmokeTest {
}

@AppServer(version = "9.0.0", jdk = "11")
class Tomee9Jdk11 extends TomeeSmokeTest {
}

@AppServer(version = "9.0.0", jdk = "17")
class Tomee9Jdk17 extends TomeeSmokeTest {
}

@AppServer(version = "9.0.0", jdk = "20")
class Tomee9Jdk20 extends TomeeSmokeTest {
}

@AppServer(version = "9.0.0", jdk = "21")
class Tomee9Jdk21 extends TomeeSmokeTest {
}

@AppServer(version = "9.0.0", jdk = "8-openj9")
class Tomee9Jdk8Openj9 extends TomeeSmokeTest {
}

@AppServer(version = "9.0.0", jdk = "11-openj9")
class Tomee9Jdk11Openj9 extends TomeeSmokeTest {
}

@AppServer(version = "9.0.0", jdk = "17-openj9")
class Tomee9Jdk17Openj9 extends TomeeSmokeTest {
}

@AppServer(version = "9.0.0", jdk = "18-openj9")
class Tomee9Jdk18Openj9 extends TomeeSmokeTest {
}
