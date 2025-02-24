plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("io.vertx")
    module.set("vertx-kafka-client")
    versions.set("[3.5.1,)")
    assertInverse.set(true)
  }
}

dependencies {
  bootstrap(project(":instrumentation:kafka:kafka-clients:kafka-clients-0.11:bootstrap"))
  implementation(project(":instrumentation:kafka:kafka-clients:kafka-clients-common-0.11:library"))

  library("io.vertx:vertx-kafka-client:3.6.0")
  // vertx-codegen is needed for Xlint's annotation checking
  library("io.vertx:vertx-codegen:3.6.0")

  testImplementation(project(":instrumentation:vertx:vertx-kafka-client-3.6:testing"))

  testInstrumentation(project(":instrumentation:kafka:kafka-clients:kafka-clients-0.11:javaagent"))
}

val latestDepTest = findProperty("testLatestDeps") as Boolean

testing {
  suites {
    val testNoReceiveTelemetry by registering(JvmTestSuite::class) {
      dependencies {
        implementation(project(":instrumentation:vertx:vertx-kafka-client-3.6:testing"))

        // the "library" configuration is not recognized by the test suite plugin
        if (latestDepTest) {
          implementation("io.vertx:vertx-kafka-client:latest.release")
          implementation("io.vertx:vertx-codegen:latest.release")
        } else {
          implementation("io.vertx:vertx-kafka-client:3.6.0")
          implementation("io.vertx:vertx-codegen:3.6.0")
        }
      }

      targets {
        all {
          testTask.configure {
            usesService(gradle.sharedServices.registrations["testcontainersBuildService"].service)

            systemProperty("testLatestDeps", findProperty("testLatestDeps") as Boolean)

            jvmArgs("-Dotel.instrumentation.kafka.experimental-span-attributes=false")
            jvmArgs("-Dotel.instrumentation.messaging.experimental.receive-telemetry.enabled=false")
          }
        }
      }
    }
  }
}

tasks {
  test {
    usesService(gradle.sharedServices.registrations["testcontainersBuildService"].service)

    systemProperty("testLatestDeps", latestDepTest)

    jvmArgs("-Dotel.instrumentation.kafka.experimental-span-attributes=true")
    jvmArgs("-Dotel.instrumentation.messaging.experimental.receive-telemetry.enabled=true")
  }

  check {
    dependsOn(testing.suites)
  }
}
