plugins {
  id("otel.library-instrumentation")
}

dependencies {
  implementation("org.junit.jupiter:junit-jupiter-api")
  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-exporter-otlp")
}
