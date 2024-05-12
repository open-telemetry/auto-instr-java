plugins {
  id("otel.javaagent-instrumentation")
  id("otel.scala-conventions")

  jacoco
}

muzzle {
  // Akka's fork-join was removed in 2.6, replaced with the normal java.concurrent version
  pass {
    group.set("com.typesafe.akka")
    module.set("akka-actor_2.11")
    versions.set("[2.5,)") // Scala 2.11 support was dropped after 2.5, so no 2.6 versions exist with this name
    assertInverse.set(true)
  }
  pass {
    group.set("com.typesafe.akka")
    module.set("akka-actor_2.12")
    versions.set("[2.5,2.6)")
    assertInverse.set(true)
  }
  pass {
    group.set("com.typesafe.akka")
    module.set("akka-actor_2.13")
    versions.set("[2.5.23,2.6)") // Scala 2.13 support was added in the middle of the 2.5 release
    assertInverse.set(true)
  }
}

dependencies {
  bootstrap(project(":instrumentation:executors:bootstrap"))

  library("com.typesafe.akka:akka-actor_2.11:2.5.0")

  testImplementation(project(":instrumentation:executors:testing"))
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
