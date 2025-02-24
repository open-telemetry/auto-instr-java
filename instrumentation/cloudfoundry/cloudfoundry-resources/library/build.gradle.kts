plugins {
  id("otel.sdk-extension")
}

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")
  testCompileOnly("com.google.auto.service:auto-service-annotations")

  implementation("org.snakeyaml:snakeyaml-engine")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
}
