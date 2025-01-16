plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("com.aerospike")
    module.set("aerospike-client")
    versions.set("[7.1.0,)")
    assertInverse.set(true)
  }
}

val latestDepTest = findProperty("testLatestDeps") as Boolean

dependencies {
  library("com.aerospike:aerospike-client:7.1.0")
  implementation("io.opentelemetry:opentelemetry-api-incubator")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")
}

if (latestDepTest) {
  otelJava {
    minJavaVersionSupported.set(JavaVersion.VERSION_21)
  }
}

tasks {
  test {
    jvmArgs("-Djava.net.preferIPv4Stack=true")
    jvmArgs("-Dotel.instrumentation.aerospike.experimental-span-attributes=true")
    jvmArgs("-Dotel.instrumentation.aerospike.experimental-metrics=true")
    usesService(gradle.sharedServices.registrations["testcontainersBuildService"].service)
  }
}
