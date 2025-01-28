plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("org.springframework.boot")
    module.set("spring-boot-actuator-autoconfigure")
    versions.set("[2.0.0.RELEASE,)")
    extraDependency("io.micrometer:micrometer-core:1.5.0")
    assertInverse.set(true)
  }
}

dependencies {
  library("org.springframework.boot:spring-boot-actuator-autoconfigure:2.0.0.RELEASE")
  library("io.micrometer:micrometer-core:1.5.0")

  implementation(project(":instrumentation:micrometer:micrometer-1.5:javaagent"))

  // dependency management pins logback-classic to 1.3 which is the last release that supports java 8
  latestDepTestLibrary("ch.qos.logback:logback-classic:+")
}

testing {
  suites {
    val testPrometheus by registering(JvmTestSuite::class) {
      dependencies {
        runtimeOnly("io.micrometer:micrometer-registry-prometheus:1.14.3")
      }
    }
  }
}

tasks.withType<Test>().configureEach {
  // required on jdk17
  jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
  jvmArgs("-XX:+IgnoreUnrecognizedVMOptions")

  jvmArgs("-Dotel.instrumentation.spring-boot-actuator-autoconfigure.enabled=true")
}

val latestDepTest = findProperty("testLatestDeps") as Boolean

// spring 6 (spring boot 3) requires java 17
if (latestDepTest) {
  otelJava {
    minJavaVersionSupported.set(JavaVersion.VERSION_17)
  }
}

// spring 6 (spring boot 3) uses slf4j 2.0
if (!latestDepTest) {
  configurations.testRuntimeClasspath {
    resolutionStrategy {
      // requires old logback (and therefore also old slf4j)
      force("ch.qos.logback:logback-classic:1.2.11")
      force("org.slf4j:slf4j-api:1.7.36")
    }
  }
}

tasks {
  check {
    dependsOn(testing.suites)
  }
}
