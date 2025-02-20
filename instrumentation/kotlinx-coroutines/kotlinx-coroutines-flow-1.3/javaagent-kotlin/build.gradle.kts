import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// We are using a separate module for kotlin source instead of placing them in
// instrumentation/kotlinx-coroutines/kotlinx-coroutines-flow-1.3/javaagent because muzzle
// generation plugin currently doesn't handle kotlin sources correctly.
plugins {
  id("org.jetbrains.kotlin.jvm")
  id("otel.java-conventions")
}

dependencies {
  compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  compileOnly(project(":instrumentation-api"))
  compileOnly(project(":instrumentation-annotations-support"))
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_1_8)
  }
}
