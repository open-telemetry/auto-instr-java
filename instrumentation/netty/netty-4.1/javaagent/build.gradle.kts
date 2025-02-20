plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("io.netty")
    module.set("netty-codec-http")
    versions.set("[4.1.0.Final,5.0.0)")
    assertInverse.set(true)
  }
  pass {
    group.set("io.netty")
    module.set("netty-all")
    versions.set("[4.1.0.Final,5.0.0)")
    excludeDependency("io.netty:netty-tcnative")
    assertInverse.set(true)
  }
  fail {
    group.set("io.netty")
    module.set("netty")
    versions.set("[,]")
  }
}

dependencies {
  library("io.netty:netty-codec-http:4.1.0.Final")
  implementation(project(":instrumentation:netty:netty-4.1:library"))
  implementation(project(":instrumentation:netty:netty-common-4.0:javaagent"))
  implementation(project(":instrumentation:netty:netty-common-4.0:library"))
  implementation(project(":instrumentation:netty:netty-common:library"))

  testImplementation(project(":instrumentation:netty:netty-4.1:testing"))
  testInstrumentation(project(":instrumentation:netty:netty-3.8:javaagent"))
  testInstrumentation(project(":instrumentation:netty:netty-4.0:javaagent"))

  // Contains logging handler
  testLibrary("io.netty:netty-handler:4.1.0.Final")
  testLibrary("io.netty:netty-transport-native-epoll:4.1.0.Final:linux-x86_64")

  // first version with kqueue, add it only as a compile time dependency
  testCompileOnly("io.netty:netty-transport-native-kqueue:4.1.11.Final:osx-x86_64")
}

tasks {
  val testConnectionSpan by registering(Test::class) {
    filter {
      includeTestsMatching("Netty41ConnectionSpanTest")
      includeTestsMatching("Netty41ClientSslTest")
    }
    include("**/Netty41ConnectionSpanTest.*", "**/Netty41ClientSslTest.*")

    jvmArgs("-Dotel.instrumentation.netty.connection-telemetry.enabled=true")
    jvmArgs("-Dotel.instrumentation.netty.ssl-telemetry.enabled=true")
  }

  test {
    systemProperty("testLatestDeps", findProperty("testLatestDeps") as Boolean)

    filter {
      excludeTestsMatching("Netty41ConnectionSpanTest")
      excludeTestsMatching("Netty41ClientSslTest")
    }
  }

  check {
    dependsOn(testConnectionSpan)
  }
}

if (!(findProperty("testLatestDeps") as Boolean)) {
  // No BOM for 4.1.0 so we can't use enforcedPlatform to override our transitive version
  // management, so hook into the resolutionStrategy.
  configurations.configureEach {
    if (!name.contains("muzzle")) {
      resolutionStrategy.eachDependency {
        if (requested.group == "io.netty" &&
          requested.name != "netty-bom" &&
          !requested.name.startsWith("netty-transport-native") &&
          !requested.name.startsWith("netty-transport-classes")) {
          useVersion("4.1.0.Final")
        }
      }
    }
  }
}
