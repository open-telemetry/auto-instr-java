plugins {
  id("otel.library-instrumentation")
  id("otel.japicmp-conventions")
}

base.archivesName.set("opentelemetry-spring-boot-autoconfigure")
group = "io.opentelemetry.instrumentation"

val springBootVersion = "2.7.18" // AutoConfiguration is added in 2.7.0, but can be used with older versions

// r2dbc-proxy is shadowed to prevent org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration
// from being loaded by Spring Boot (by the presence of META-INF/services/io.r2dbc.spi.ConnectionFactoryProvider) - even if the user doesn't want to use R2DBC.
sourceSets {
  main {
    val shadedDep = project(":instrumentation:r2dbc-1.0:library-instrumentation-shaded")
    output.dir(
      shadedDep.file("build/extracted/shadow-spring"),
      "builtBy" to ":instrumentation:r2dbc-1.0:library-instrumentation-shaded:extractShadowJarSpring",
    )
  }
  create("javaSpring3") {
    java {
      setSrcDirs(listOf("src/main/javaSpring3"))
    }
  }
}

configurations {
  named("javaSpring3CompileOnly") {
    extendsFrom(configurations["compileOnly"])
  }
}

dependencies {
  compileOnly("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
  annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor:$springBootVersion")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
  implementation("javax.validation:validation-api")

  implementation(project(":instrumentation-annotations-support"))
  implementation(project(":instrumentation:kafka:kafka-clients:kafka-clients-2.6:library"))
  implementation(project(":instrumentation:mongo:mongo-3.1:library"))
  compileOnly(project(path = ":instrumentation:r2dbc-1.0:library-instrumentation-shaded", configuration = "shadow"))
  implementation(project(":instrumentation:spring:spring-kafka-2.7:library"))
  implementation(project(":instrumentation:spring:spring-web:spring-web-3.1:library"))
  implementation(project(":instrumentation:spring:spring-webmvc:spring-webmvc-5.3:library"))
  compileOnly("javax.servlet:javax.servlet-api:3.1.0")
  implementation(project(":instrumentation:spring:spring-webflux:spring-webflux-5.3:library"))
  implementation(project(":instrumentation:micrometer:micrometer-1.5:library"))
  implementation(project(":instrumentation:log4j:log4j-appender-2.17:library"))
  compileOnly("org.apache.logging.log4j:log4j-core:2.17.0")
  implementation(project(":instrumentation:logback:logback-appender-1.0:library"))
  implementation(project(":instrumentation:logback:logback-mdc-1.0:library"))
  compileOnly("ch.qos.logback:logback-classic:1.0.0")
  implementation(project(":instrumentation:jdbc:library"))
  implementation(project(":instrumentation:runtime-telemetry:runtime-telemetry-java8:library"))
  implementation(project(":instrumentation:runtime-telemetry:runtime-telemetry-java17:library"))

  library("org.springframework.kafka:spring-kafka:2.9.0")
  library("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
  library("org.springframework.boot:spring-boot-starter-aop:$springBootVersion")
  library("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
  library("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
  library("org.springframework.boot:spring-boot-starter-data-mongodb:$springBootVersion")
  library("org.springframework.boot:spring-boot-starter-data-r2dbc:$springBootVersion")

  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  implementation(project(":sdk-autoconfigure-support"))
  compileOnly("io.opentelemetry:opentelemetry-extension-trace-propagators")
  compileOnly("io.opentelemetry.contrib:opentelemetry-aws-xray-propagator")
  compileOnly("io.opentelemetry:opentelemetry-exporter-logging")
  compileOnly("io.opentelemetry:opentelemetry-exporter-otlp")
  compileOnly("io.opentelemetry:opentelemetry-exporter-zipkin")
  compileOnly(project(":instrumentation-annotations"))

  compileOnly(project(":instrumentation:resources:library"))
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")

  testLibrary("org.springframework.boot:spring-boot-starter-test:$springBootVersion") {
    exclude("org.junit.vintage", "junit-vintage-engine")
  }
  testImplementation("javax.servlet:javax.servlet-api:3.1.0")
  testImplementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
  testRuntimeOnly("com.h2database:h2:1.4.197")
  testRuntimeOnly("io.r2dbc:r2dbc-h2:1.0.0.RELEASE")

  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation(project(":instrumentation:resources:library"))
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry:opentelemetry-extension-trace-propagators")
  testImplementation("io.opentelemetry.contrib:opentelemetry-aws-xray-propagator")
  testImplementation("io.opentelemetry:opentelemetry-exporter-logging")
  testImplementation("io.opentelemetry:opentelemetry-exporter-otlp")
  testImplementation("io.opentelemetry:opentelemetry-exporter-zipkin")
  testImplementation(project(":instrumentation-annotations"))

  // needed for the Spring Boot 3 support
  implementation(project(":instrumentation:spring:spring-webmvc:spring-webmvc-6.0:library"))

  // give access to common classes, e.g. InstrumentationConfigUtil
  add("javaSpring3CompileOnly", files(sourceSets.main.get().output.classesDirs))
  add("javaSpring3CompileOnly", "org.springframework.boot:spring-boot-starter-web:3.2.4")
  add("javaSpring3CompileOnly", "io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  add("javaSpring3CompileOnly", project(":instrumentation:spring:spring-web:spring-web-3.1:library"))
  add("javaSpring3CompileOnly", project(":instrumentation:spring:spring-webmvc:spring-webmvc-6.0:library"))
}

val latestDepTest = findProperty("testLatestDeps") as Boolean

// spring 6 (spring boot 3) requires java 17
if (latestDepTest) {
  otelJava {
    minJavaVersionSupported.set(JavaVersion.VERSION_17)
  }
}

val testJavaVersion = gradle.startParameter.projectProperties["testJavaVersion"]?.let(JavaVersion::toVersion)
val testSpring3 = (testJavaVersion == null || testJavaVersion.compareTo(JavaVersion.VERSION_17) >= 0)

testing {
  suites {
    val testLogbackAppender by registering(JvmTestSuite::class) {
      dependencies {
        implementation(project())
        implementation(project(":testing-common"))
        implementation("io.opentelemetry:opentelemetry-sdk")
        implementation("io.opentelemetry:opentelemetry-sdk-testing")
        implementation("org.mockito:mockito-inline")
        implementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")

        implementation(project(":instrumentation:logback:logback-appender-1.0:library"))
        // using the same versions as in the spring-boot-autoconfigure
        implementation("ch.qos.logback:logback-classic") {
          version {
            strictly("1.2.11")
          }
        }
        implementation("org.slf4j:slf4j-api") {
          version {
            strictly("1.7.32")
          }
        }
      }
    }

    val testLogbackMissing by registering(JvmTestSuite::class) {
      dependencies {
        implementation(project())
        implementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")

        implementation("org.slf4j:slf4j-api") {
          version {
            strictly("1.7.32")
          }
        }
      }
    }

    val testSpring3 by registering(JvmTestSuite::class) {
      dependencies {
        implementation(project())
        implementation("org.springframework.boot:spring-boot-starter-web:3.2.4")
        implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
        implementation(project(":instrumentation:spring:spring-web:spring-web-3.1:library"))
        implementation(project(":instrumentation:spring:spring-webmvc:spring-webmvc-6.0:library"))
        implementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
        implementation("org.springframework.boot:spring-boot-starter-test:3.2.4") {
          exclude("org.junit.vintage", "junit-vintage-engine")
        }
      }
    }
  }
}

configurations.configureEach {
  if (name.contains("testLogbackMissing")) {
    exclude("ch.qos.logback", "logback-classic")
  }
}

tasks {
  compileTestJava {
    options.compilerArgs.add("-parameters")
  }

  withType<Test>().configureEach {
    systemProperty("testLatestDeps", latestDepTest)

    // required on jdk17
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    jvmArgs("-XX:+IgnoreUnrecognizedVMOptions")
  }

  named<JavaCompile>("compileJavaSpring3Java") {
    sourceCompatibility = "17"
    targetCompatibility = "17"
    options.release.set(17)
  }

  named<JavaCompile>("compileTestSpring3Java") {
    sourceCompatibility = "17"
    targetCompatibility = "17"
    options.release.set(17)
  }

  named<Test>("testSpring3") {
    isEnabled = testSpring3
  }

  named<Jar>("jar") {
    from(sourceSets["javaSpring3"].output)
  }

  named<Jar>("sourcesJar") {
    from(sourceSets["javaSpring3"].java)
  }

  val testStableSemconv by registering(Test::class) {
    jvmArgs("-Dotel.semconv-stability.opt-in=database")
  }

  check {
    dependsOn(testing.suites)
    dependsOn(testStableSemconv)
  }
}
