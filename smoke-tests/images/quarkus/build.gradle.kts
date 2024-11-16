import com.google.cloud.tools.jib.gradle.JibTask
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

buildscript {
  dependencies {
    classpath("com.google.cloud.tools:jib-quarkus-extension-gradle:0.1.2")
  }
}

plugins {
  id("otel.java-conventions")

  id("com.google.cloud.tools.jib")
  id("io.quarkus") version "3.16.3"
}

dependencies {
  implementation(enforcedPlatform("io.quarkus:quarkus-bom:3.16.3"))
  implementation("io.quarkus:quarkus-rest")
}

// Quarkus 3.7+ requires Java 17+
val targetJDK = project.findProperty("targetJDK") ?: "17"

val tag = findProperty("tag")
  ?: DateTimeFormatter.ofPattern("yyyyMMdd.HHmmSS").format(LocalDateTime.now())

java {
  // this is needed to avoid jib failing with
  // "Your project is using Java 21 but the base image is for Java 17"
  // (it seems the jib plugins does not understand toolchains yet)
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

jib {
  from.image = "eclipse-temurin:$targetJDK"
  to.image = "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-quarkus:jdk$targetJDK-$tag"
  container {
    mainClass = "bogus" // to suppress Jib warning about missing main class
  }
  pluginExtensions {
    pluginExtension {
      implementation = "com.google.cloud.tools.jib.gradle.extension.quarkus.JibQuarkusExtension"
      properties = mapOf("packageType" to "fast-jar")
    }
  }
}

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      // Quarkus 3.7+ requires Java 17+
      release.set(17)
    }
  }

  withType<JibTask>().configureEach {
    dependsOn(quarkusBuild)
  }

  sourcesJar {
    dependsOn(quarkusGenerateCode)
  }
}
