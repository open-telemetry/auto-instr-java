plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("com.ctrip.framework.apollo")
    module.set("apollo-client")
    versions.set("[1.0.0,)")
    assertInverse.set(true)
  }
}

dependencies {
  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  library("com.ctrip.framework.apollo:apollo-client:1.0.0")

  testImplementation("com.ctrip.framework.apollo:apollo-client:1.1.0")
  testImplementation(project(":testing-common"))
}

tasks.withType<Test>().configureEach {
  jvmArgs("-Dotel.instrumentation.apolloconfig-apolloclient.enabled=true")
}
