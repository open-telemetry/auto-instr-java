import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("org.jetbrains.kotlinx")
    module.set("kotlinx-coroutines-core")
    versions.set("[1.0.0,1.3.8)")
    extraDependency(project(":instrumentation-annotations"))
    extraDependency("io.opentelemetry:opentelemetry-api:1.27.0")
  }
  // 1.3.9 (and beyond?) have changed how artifact names are resolved due to multiplatform variants
  pass {
    group.set("org.jetbrains.kotlinx")
    module.set("kotlinx-coroutines-core-jvm")
    versions.set("[1.3.9,)")
    extraDependency(project(":instrumentation-annotations"))
    extraDependency("io.opentelemetry:opentelemetry-api:1.27.0")
  }
}

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-extension-kotlin")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  compileOnly(project(":opentelemetry-instrumentation-annotations-shaded-for-instrumenting", configuration = "shadow"))

  implementation("org.ow2.asm:asm-tree")
  implementation("org.ow2.asm:asm-util")
  implementation(project(":instrumentation:opentelemetry-instrumentation-annotations:opentelemetry-instrumentation-annotations-common:javaagent"))

  testInstrumentation(project(":instrumentation:opentelemetry-extension-kotlin-1.0:javaagent"))
  testInstrumentation(project(":instrumentation:reactor:reactor-3.1:javaagent"))

  testImplementation("io.opentelemetry:opentelemetry-extension-kotlin")
  testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  testImplementation(project(":instrumentation:reactor:reactor-3.1:library"))
  testImplementation(project(":instrumentation-annotations"))

  testLibrary("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.0")
  testLibrary("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.0.0")
  testLibrary("io.vertx:vertx-lang-kotlin-coroutines:3.6.0")
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_1_8)
    // generate metadata for Java 1.8 reflection on method parameters, used in @WithSpan tests
    javaParameters = true
  }
}
