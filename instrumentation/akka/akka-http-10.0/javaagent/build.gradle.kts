plugins {
  id("otel.javaagent-instrumentation")
  id("otel.scala-conventions")

  jacoco
}

muzzle {
  pass {
    group.set("com.typesafe.akka")
    module.set("akka-http_2.11")
    versions.set("[10,)")
    assertInverse.set(true)
    // later versions of akka-http expect streams to be provided
    extraDependency("com.typesafe.akka:akka-stream_2.11:2.5.32")
  }
  pass {
    group.set("com.typesafe.akka")
    module.set("akka-http_2.12")
    versions.set("[10,)")
    assertInverse.set(true)
    // later versions of akka-http expect streams to be provided
    extraDependency("com.typesafe.akka:akka-stream_2.12:2.5.32")
  }
  pass {
    group.set("com.typesafe.akka")
    module.set("akka-http_2.13")
    versions.set("[10,)")
    assertInverse.set(true)
    // later versions of akka-http expect streams to be provided
    extraDependency("com.typesafe.akka:akka-stream_2.13:2.5.32")
  }
}

dependencies {
  library("com.typesafe.akka:akka-http_2.11:10.0.0")
  library("com.typesafe.akka:akka-stream_2.11:2.4.14")

  testInstrumentation(project(":instrumentation:akka:akka-actor-2.3:javaagent"))
  testInstrumentation(project(":instrumentation:akka:akka-actor-fork-join-2.5:javaagent"))
  testInstrumentation(project(":instrumentation:scala-fork-join-2.8:javaagent"))

  latestDepTestLibrary("com.typesafe.akka:akka-http_2.13:+")
  latestDepTestLibrary("com.typesafe.akka:akka-stream_2.13:+")
}

tasks {
  withType<Test>().configureEach {
    // required on jdk17
    jvmArgs("--add-exports=java.base/sun.security.util=ALL-UNNAMED")
    jvmArgs("-XX:+IgnoreUnrecognizedVMOptions")

    jvmArgs("-Dio.opentelemetry.javaagent.shaded.io.opentelemetry.context.enableStrictContext=false")

    systemProperty("testLatestDeps", findProperty("testLatestDeps") as Boolean)
  }
}

if (findProperty("testLatestDeps") as Boolean) {
  configurations {
    // akka artifact name is different for regular and latest tests
    testImplementation {
      exclude("com.typesafe.akka", "akka-http_2.11")
      exclude("com.typesafe.akka", "akka-stream_2.11")
    }
  }
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
