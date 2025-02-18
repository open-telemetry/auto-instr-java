plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("org.springframework.pulsar")
    module.set("spring-pulsar")
    versions.set("[1.2.0,]")
  }
}

dependencies {
  library("org.springframework.pulsar:spring-pulsar:1.2.0")

  testInstrumentation(project(":instrumentation:pulsar:pulsar-2.8:javaagent"))
  testImplementation("org.testcontainers:pulsar")

  testLibrary("org.springframework.pulsar:spring-pulsar:1.2.0")
  testLibrary("org.springframework.boot:spring-boot-starter-test:3.2.4")
  testLibrary("org.springframework.boot:spring-boot-starter:3.2.4")
}

val latestDepTest = findProperty("testLatestDeps") as Boolean

// spring 6 (spring boot 3) requires java 17
if (latestDepTest) {
  otelJava {
    minJavaVersionSupported.set(JavaVersion.VERSION_17)
  }
}
