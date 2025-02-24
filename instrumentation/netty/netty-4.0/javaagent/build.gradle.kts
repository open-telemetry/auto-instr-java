plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("io.netty")
    module.set("netty-codec-http")
    versions.set("[4.0.0.Final,4.1.0.Final)")
    assertInverse.set(true)
  }
  pass {
    group.set("io.netty")
    module.set("netty-all")
    versions.set("[4.0.0.Final,4.1.0.Final)")
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
  library("io.netty:netty-codec-http:4.0.0.Final")
  implementation(project(":instrumentation:netty:netty-common-4.0:javaagent"))
  implementation(project(":instrumentation:netty:netty-common-4.0:library"))
  implementation(project(":instrumentation:netty:netty-common:library"))

  testInstrumentation(project(":instrumentation:netty:netty-3.8:javaagent"))
  testInstrumentation(project(":instrumentation:netty:netty-4.1:javaagent"))

  latestDepTestLibrary("io.netty:netty-codec-http:4.0.+") // see netty-4.1 module
}

tasks {
  val testConnectionSpan by registering(Test::class) {
    filter {
      includeTestsMatching("Netty40ConnectionSpanTest")
      includeTestsMatching("Netty40ClientSslTest")
    }
    include("**/Netty40ConnectionSpanTest.*", "**/Netty40ClientSslTest.*")

    jvmArgs("-Dotel.instrumentation.netty.connection-telemetry.enabled=true")
    jvmArgs("-Dotel.instrumentation.netty.ssl-telemetry.enabled=true")
  }

  test {
    filter {
      excludeTestsMatching("Netty40ConnectionSpanTest")
      excludeTestsMatching("Netty40ClientSslTest")
    }
  }

  check {
    dependsOn(testConnectionSpan)
  }
}

// We need to force the dependency to the earliest supported version because other libraries declare newer versions.
if (!(findProperty("testLatestDeps") as Boolean)) {
  configurations.configureEach {
    if (!name.contains("muzzle")) {
      resolutionStrategy {
        eachDependency {
          // specifying a fixed version for all libraries with io.netty' group
          if (requested.group == "io.netty" && requested.name != "netty-bom") {
            useVersion("4.0.0.Final")
          }
        }
      }
    }
  }
}
