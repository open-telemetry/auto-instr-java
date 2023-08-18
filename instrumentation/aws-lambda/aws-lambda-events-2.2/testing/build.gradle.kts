plugins {
  id("otel.java-conventions")
}

dependencies {
  api(project(":testing-common"))

  api("com.amazonaws:aws-lambda-java-core:1.0.0")
  api("com.amazonaws:aws-lambda-java-events:3.9.0")

  api("org.junit-pioneer:junit-pioneer")
  api("org.mockito:mockito-junit-jupiter")

  implementation("com.google.guava:guava")

  implementation("org.apache.groovy:groovy")
  implementation("io.opentelemetry:opentelemetry-api")
  implementation("org.spockframework:spock-core")
  implementation("com.github.stefanbirkner:system-lambda")
}
