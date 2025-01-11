plugins {
  id("otel.library-instrumentation")
}

dependencies {
  library("com.rabbitmq:amqp-client:5.5.3")

  testImplementation("org.testcontainers:testcontainers")
}

tasks.withType<Test>().configureEach {
  // TODO run tests both with and without experimental span attributes
  jvmArgs("-Dotel.instrumentation.rabbitmq.experimental-span-attributes=true")
  jvmArgs("-Dotel.instrumentation.messaging.experimental.receive-telemetry.enabled=true")

  usesService(gradle.sharedServices.registrations["testcontainersBuildService"].service)
}
