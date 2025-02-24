plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("io.vertx")
    module.set("vertx-sql-client")
    versions.set("[4.0.0,)")
    assertInverse.set(true)
  }
}

dependencies {
  library("io.vertx:vertx-sql-client:4.0.0")
  compileOnly("io.vertx:vertx-codegen:4.0.0")

  testInstrumentation(project(":instrumentation:netty:netty-4.1:javaagent"))

  testLibrary("io.vertx:vertx-pg-client:4.0.0")
  testLibrary("io.vertx:vertx-codegen:4.0.0")
}

tasks {
  withType<Test>().configureEach {
    usesService(gradle.sharedServices.registrations["testcontainersBuildService"].service)
  }

  val testStableSemconv by registering(Test::class) {
    jvmArgs("-Dotel.semconv-stability.opt-in=database")
  }

  check {
    dependsOn(testStableSemconv)
  }
}

val latestDepTest = findProperty("testLatestDeps") as Boolean
if (!latestDepTest) {
  // https://bugs.openjdk.org/browse/JDK-8320431
  otelJava {
    maxJavaVersionForTests.set(JavaVersion.VERSION_21)
  }
}
