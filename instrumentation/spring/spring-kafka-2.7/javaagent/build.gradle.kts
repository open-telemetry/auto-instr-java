plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("org.springframework.kafka")
    module.set("spring-kafka")
    versions.set("[2.7.0,)")
    assertInverse.set(true)
  }
}

dependencies {
  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  bootstrap(project(":instrumentation:kafka:kafka-clients:kafka-clients-0.11:bootstrap"))
  bootstrap(project(":instrumentation:spring:spring-scheduling-3.1:bootstrap"))
  implementation(project(":instrumentation:kafka:kafka-clients:kafka-clients-common-0.11:library"))
  implementation(project(":instrumentation:spring:spring-kafka-2.7:library"))

  library("org.springframework.kafka:spring-kafka:2.7.0")

  testInstrumentation(project(":instrumentation:kafka:kafka-clients:kafka-clients-0.11:javaagent"))
  testInstrumentation(project(":instrumentation:spring:spring-scheduling-3.1:javaagent"))

  testImplementation(project(":instrumentation:spring:spring-kafka-2.7:testing"))

  testLibrary("org.springframework.boot:spring-boot-starter-test:2.5.3")
  testLibrary("org.springframework.boot:spring-boot-starter:2.5.3")
}

val latestDepTest = findProperty("testLatestDeps") as Boolean

testing {
  suites {
    val testNoReceiveTelemetry by registering(JvmTestSuite::class) {
      dependencies {
        implementation(project(":instrumentation:spring:spring-kafka-2.7:testing"))

        // the "library" configuration is not recognized by the test suite plugin
        if (latestDepTest) {
          implementation("org.springframework.kafka:spring-kafka:+")
          implementation("org.springframework.boot:spring-boot-starter-test:+")
          implementation("org.springframework.boot:spring-boot-starter:+")
        } else {
          implementation("org.springframework.kafka:spring-kafka:2.7.0")
          implementation("org.springframework.boot:spring-boot-starter-test:2.5.3")
          implementation("org.springframework.boot:spring-boot-starter:2.5.3")
        }
      }

      targets {
        all {
          testTask.configure {
            usesService(gradle.sharedServices.registrations["testcontainersBuildService"].service)

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

    systemProperty("testLatestDeps", findProperty("testLatestDeps") as Boolean)
    jvmArgs("-Dotel.instrumentation.kafka.experimental-span-attributes=true")
    jvmArgs("-Dotel.instrumentation.messaging.experimental.receive-telemetry.enabled=true")
  }

  check {
    dependsOn(testing.suites)
  }
}

// spring 6 (which spring-kafka 3.+ uses) requires java 17
if (latestDepTest) {
  otelJava {
    minJavaVersionSupported.set(JavaVersion.VERSION_17)
  }
}

// spring 6 uses slf4j 2.0
if (!latestDepTest) {
  configurations {
    listOf(
      testRuntimeClasspath,
      named("testNoReceiveTelemetryRuntimeClasspath"),
    )
      .forEach {
        it.configure {
          resolutionStrategy {
            // requires old logback (and therefore also old slf4j)
            force("ch.qos.logback:logback-classic:1.2.11")
            force("org.slf4j:slf4j-api:1.7.36")
          }
        }
      }
  }
}
