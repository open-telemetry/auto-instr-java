plugins {
  id("otel.javaagent-instrumentation")
  id("otel.scala-conventions")

  jacoco
}

muzzle {
  pass {
    group.set("com.typesafe.akka")
    module.set("akka-actor_2.11")
    versions.set("[2.3,)")
    assertInverse.set(true)
  }
  pass {
    group.set("com.typesafe.akka")
    module.set("akka-actor_2.12")
    versions.set("[2.3,)")
    assertInverse.set(true)
  }
  pass {
    group.set("com.typesafe.akka")
    module.set("akka-actor_2.13")
    versions.set("[2.3,)")
    assertInverse.set(true)
  }
}

dependencies {
  bootstrap(project(":instrumentation:executors:bootstrap"))

  compileOnly("com.typesafe.akka:akka-actor_2.11:2.3.2") // first version in maven central
  testImplementation("com.typesafe.akka:akka-actor_2.11:2.3.2") // first version in maven central

  latestDepTestLibrary("com.typesafe.akka:akka-actor_2.13:+")
}

if (findProperty("testLatestDeps") as Boolean) {
  configurations {
    // akka artifact name is different for regular and latest tests
    testImplementation {
      exclude("com.typesafe.akka", "akka-actor_2.11")
    }
  }
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
