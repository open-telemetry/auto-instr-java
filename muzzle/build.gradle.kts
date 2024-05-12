plugins {
  id("otel.java-conventions")
  id("otel.japicmp-conventions")
  id("otel.publish-conventions")

  jacoco
}

group = "io.opentelemetry.javaagent"

dependencies {
  // Only used during compilation by bytebuddy plugin
  compileOnly("com.google.guava:guava")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  api("net.bytebuddy:byte-buddy-dep")
  implementation("org.ow2.asm:asm-tree")

  implementation(project(":javaagent-bootstrap"))
  implementation(project(":instrumentation-api"))
  implementation(project(":javaagent-extension-api"))

  // Used by byte-buddy but not brought in as a transitive dependency.
  compileOnly("com.google.code.findbugs:annotations")

  testImplementation(project(":testing-common"))
  testImplementation("com.google.guava:guava")
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
