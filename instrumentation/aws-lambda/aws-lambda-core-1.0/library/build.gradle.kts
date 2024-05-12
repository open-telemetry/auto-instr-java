plugins {
  id("otel.library-instrumentation")

  jacoco
}

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  library("com.amazonaws:aws-lambda-java-core:1.0.0")

  // We do lightweight parsing of JSON to extract HTTP headers from requests for propagation.
  // This will be commonly needed even for users that don't use events, but luckily it's not too big.
  // Note that Lambda itself uses Jackson, but does not expose it to the function so we need to include
  // it here.
  implementation("com.fasterxml.jackson.core:jackson-core")

  // allows to get the function ARN
  testLibrary("com.amazonaws:aws-lambda-java-core:1.2.1")

  testImplementation("io.opentelemetry.contrib:opentelemetry-aws-xray-propagator")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-extension-trace-propagators")
  testImplementation("com.google.guava:guava")

  testImplementation(project(":instrumentation:aws-lambda:aws-lambda-core-1.0:testing"))
  testImplementation("uk.org.webcompere:system-stubs-jupiter")
}

tasks.withType<Test>().configureEach {
  // required on jdk17
  jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
  jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")
  jvmArgs("-XX:+IgnoreUnrecognizedVMOptions")
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
