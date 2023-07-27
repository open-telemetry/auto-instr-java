import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("otel.library-instrumentation")
  id("otel.java-conventions")
  id("otel.jmh-conventions")
}

dependencies {
  implementation(project(mapOf("path" to ":instrumentation:opentelemetry-api:opentelemetry-api-1.27:javaagent")))
    library("io.projectreactor:reactor-core:3.1.0.RELEASE")
  jmh("io.projectreactor:reactor-core:3.1.0.RELEASE")
  implementation(project(":instrumentation-annotations-support"))
  testLibrary("io.projectreactor:reactor-test:3.1.0.RELEASE")

  testImplementation(project(":instrumentation:reactor:reactor-3.1:testing"))

  latestDepTestLibrary("io.projectreactor:reactor-core:3.4.+")
  latestDepTestLibrary("io.projectreactor:reactor-test:3.4.+")
}

tasks {
    // TODO this should live in jmh-conventions
    named<JavaCompile>("jmhCompileGeneratedClasses") {
        options.errorprone {
            isEnabled.set(false)
        }
    }
}


