plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("io.vertx")
    module.set("vertx-rx-java2")
    versions.set("[3.5.0,)")
  }
}

// The first Vert.x version that uses rx-java 2
val vertxVersion = "3.5.0"

dependencies {
  compileOnly("io.vertx:vertx-web:$vertxVersion")
  compileOnly("io.vertx:vertx-rx-java2:$vertxVersion")

  testInstrumentation(project(":instrumentation:jdbc:javaagent"))
  testInstrumentation(project(":instrumentation:netty:netty-4.1:javaagent"))
  testInstrumentation(project(":instrumentation:rxjava:rxjava-2.0:javaagent"))
  testInstrumentation(project(":instrumentation:vertx:vertx-http-client:vertx-http-client-3.0:javaagent"))
  testInstrumentation(project(":instrumentation:vertx:vertx-http-client:vertx-http-client-4.0:javaagent"))
  testInstrumentation(project(":instrumentation:vertx:vertx-web-3.0:javaagent"))
}

testing {
  suites {
    val version35Test by registering(JvmTestSuite::class) {
      dependencies {
        // this only exists to make Intellij happy since it doesn't (currently at least) understand our
        // inclusion of this artifact inside :testing-common
        compileOnly(project.dependencies.project(":testing:armeria-shaded-for-testing", configuration = "shadow"))

        implementation("org.hsqldb:hsqldb:2.3.4")
        compileOnly("io.vertx:vertx-codegen:$vertxVersion")
        implementation("io.vertx:vertx-web:$vertxVersion")
        implementation("io.vertx:vertx-rx-java2:$vertxVersion")
        implementation("io.vertx:vertx-web-client:$vertxVersion")
        implementation("io.vertx:vertx-jdbc-client:$vertxVersion")
        implementation("io.vertx:vertx-circuit-breaker:$vertxVersion")
      }
    }

    val latestDepTest by registering(JvmTestSuite::class) {
      dependencies {
        // this only exists to make Intellij happy since it doesn't (currently at least) understand our
        // inclusion of this artifact inside :testing-common
        compileOnly(project.dependencies.project(":testing:armeria-shaded-for-testing", configuration = "shadow"))

        implementation("org.hsqldb:hsqldb:2.3.4")
        implementation("io.vertx:vertx-web:latest.release")
        implementation("io.vertx:vertx-rx-java2:latest.release")
        implementation("io.vertx:vertx-web-client:latest.release")
        implementation("io.vertx:vertx-jdbc-client:latest.release")
        implementation("io.vertx:vertx-circuit-breaker:latest.release")
      }
    }
  }
}

val testLatestDeps = findProperty("testLatestDeps") as Boolean

tasks {
  if (testLatestDeps) {
    // disable regular test running and compiling tasks when latest dep test task is run
    named("test") {
      enabled = false
    }
    named("compileTestGroovy") {
      enabled = false
    }
  }

  named("latestDepTest") {
    enabled = testLatestDeps
  }

  check {
    dependsOn(testing.suites)
  }
}
