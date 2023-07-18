plugins {
  id("otel.java-conventions")
  id("org.springframework.boot") version "3.1.0"
  id("org.graalvm.buildtools.native")
}

description = "smoke-tests-otel-starter"

otelJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_17)
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
  implementation("com.h2database:h2")
  implementation("org.apache.commons:commons-dbcp2")
  implementation(project(":instrumentation:jdbc:library"))
  implementation(project(":instrumentation:spring:starters:spring-boot-starter"))
  implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation(project(":testing-common"))
}

tasks {
  compileAotTestJava {
    with(options) {
      compilerArgs.add("-Xlint:-deprecation,-unchecked,none")
      // To disable warnings/failure coming from the Java compiler during the Spring AOT processing
      // -deprecation,-unchecked and none are required (none is not enough)
    }
  }
}

// To be able to execute the tests as GraalVM native executables
configurations.configureEach {
  exclude("org.apache.groovy", "groovy")
  exclude("org.apache.groovy", "groovy-json")
  exclude("org.spockframework", "spock-core")
}
