plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("co.elastic.clients")
    module.set("elasticsearch-java")
    versions.set("[7.16,7.17.20)") // 7.17.20+ has native, on-by-default opentelemetry instrumentation
  }
  pass {
    group.set("co.elastic.clients")
    module.set("elasticsearch-java")
    versions.set("[8.0.0,8.10)") // 8.10+ has native, on-by-default opentelemetry instrumentation
  }
  fail {
    group.set("co.elastic.clients")
    module.set("elasticsearch-java")
    versions.set("(,7.16)")
  }
  fail {
    group.set("co.elastic.clients")
    module.set("elasticsearch-java")
    versions.set("[7.17.20,8.0.0)")
  }
  fail {
    group.set("co.elastic.clients")
    module.set("elasticsearch-java")
    versions.set("[8.10,)")
  }
}

dependencies {
  library("co.elastic.clients:elasticsearch-java:7.16.0")

  implementation(project(":instrumentation:elasticsearch:elasticsearch-rest-common-5.0:javaagent"))

  testInstrumentation(project(":instrumentation:elasticsearch:elasticsearch-rest-7.0:javaagent"))
  testInstrumentation(project(":instrumentation:apache-httpclient:apache-httpclient-4.0:javaagent"))
  testInstrumentation(project(":instrumentation:apache-httpasyncclient-4.1:javaagent"))

  testImplementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
  testImplementation("org.testcontainers:elasticsearch")

  latestDepTestLibrary("co.elastic.clients:elasticsearch-java:7.17.19") // native on-by-default instrumentation after this version
}

val latestDepTest = findProperty("testLatestDeps") as Boolean
testing {
  suites {
    val version8Test by registering(JvmTestSuite::class) {
      dependencies {
        sources {
          java {
            setSrcDirs(listOf("src/test/java"))
          }
          resources {
            setSrcDirs(listOf("src/test/resources"))
          }
        }

        implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
        implementation("org.testcontainers:elasticsearch")

        if (latestDepTest) {
          // 8.10+ has native, on-by-default opentelemetry instrumentation
          implementation("co.elastic.clients:elasticsearch-java:8.9.+")
        } else {
          implementation("co.elastic.clients:elasticsearch-java:8.0.0")
        }
      }
    }
  }
}

tasks {
  withType<Test>().configureEach {
    jvmArgs("-Dotel.instrumentation.common.experimental.controller-telemetry.enabled=true")
    usesService(gradle.sharedServices.registrations["testcontainersBuildService"].service)
  }

  val testStableSemconv by registering(Test::class) {
    jvmArgs("-Dotel.semconv-stability.opt-in=database")
  }

  check {
    dependsOn(testing.suites)
    dependsOn(testStableSemconv)
  }
}
