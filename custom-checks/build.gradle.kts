plugins {
  id("otel.java-conventions")

  jacoco
}

dependencies {
  implementation("com.google.errorprone:error_prone_core")

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")

  testImplementation("com.google.errorprone:error_prone_test_helpers")
}

otelJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_11)
}

// We cannot use "--release" javac option here because that will forbid exporting com.sun.tools package.
// We also can't seem to use the toolchain without the "--release" option. So disable everything.

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
  toolchain {
    languageVersion.set(null as JavaLanguageVersion?)
  }
}

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(null as Int?)

      compilerArgs.addAll(
        listOf(
          "--add-exports",
          "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
          "--add-exports",
          "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
          "--add-exports",
          "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
          "--add-exports",
          "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
          "--add-exports",
          "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        ),
      )
    }
  }
}

tasks.withType<Test>().configureEach {
  // required on jdk17
  jvmArgs("--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED")
  jvmArgs("--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED")
  jvmArgs("--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED")
  jvmArgs("--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED")
  jvmArgs("--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED")
  jvmArgs("--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED")
  jvmArgs("--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED")
  jvmArgs("--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED")
  jvmArgs("-XX:+IgnoreUnrecognizedVMOptions")
}

tasks.withType<Javadoc>().configureEach {
  // using com.sun.tools.javac.api.JavacTrees breaks javadoc generation
  enabled = false
}

// Our conventions apply this project as a dependency in the errorprone configuration, which would cause
// a circular dependency if trying to compile this project with that still there. So we filter this
// project out.
configurations {
  named("errorprone") {
    dependencies.removeIf {
      it is ProjectDependency && it.dependencyProject == project
    }
  }
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
