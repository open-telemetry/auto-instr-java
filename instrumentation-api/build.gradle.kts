import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("otel.java-conventions")
  id("otel.animalsniffer-conventions")
  id("otel.jacoco-conventions")
  id("otel.japicmp-conventions")
  id("otel.publish-conventions")
  id("otel.jmh-conventions")
}

group = "io.opentelemetry.instrumentation"

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry:opentelemetry-extension-incubator")
  implementation("io.opentelemetry:opentelemetry-semconv")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("org.junit-pioneer:junit-pioneer")

  jmhImplementation(project(":instrumentation-api-semconv"))
}

tasks {
  named<Checkstyle>("checkstyleMain") {
    exclude("**/concurrentlinkedhashmap/**")
  }

  // TODO this should live in jmh-conventions
  named<JavaCompile>("jmhCompileGeneratedClasses") {
    options.errorprone {
      isEnabled.set(false)
    }
  }

  withType<Test>().configureEach {
    // required on jdk17
    jvmArgs("--add-opens=java.base/java.util=ALL-UNNAMED")
    jvmArgs("-XX:+IgnoreUnrecognizedVMOptions")
  }
}
