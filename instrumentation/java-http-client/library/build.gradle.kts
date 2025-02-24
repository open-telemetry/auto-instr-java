plugins {
  id("otel.library-instrumentation")
  id("otel.nullaway-conventions")
  id("org.graalvm.buildtools.native")
}

otelJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_11)
}

dependencies {
  testImplementation(project(":instrumentation:java-http-client:testing"))
}

graalvmNative {

  binaries.all {
    resources.autodetect()

    // Workaround for https://github.com/junit-team/junit5/issues/3405
    buildArgs.add("--initialize-at-build-time=org.junit.platform.launcher.core.LauncherConfig")
    buildArgs.add("--initialize-at-build-time=org.junit.jupiter.engine.config.InstantiatingConfigurationParameterConverter")
  }

  // See https://github.com/graalvm/native-build-tools/issues/572
  metadataRepository {
    enabled.set(false)
  }

  toolchainDetection.set(false)
}

// To be able to execute the tests with GraalVM native
configurations.configureEach {
  exclude("org.apache.groovy", "groovy")
  exclude("org.apache.groovy", "groovy-json")
  exclude("org.spockframework", "spock-core")
}
