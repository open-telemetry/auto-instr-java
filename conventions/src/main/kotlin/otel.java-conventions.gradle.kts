import io.opentelemetry.instrumentation.gradle.OtelJavaExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.time.Duration

plugins {
  `java-library`
  groovy
  checkstyle
  codenarc
  idea

  id("otel.errorprone-conventions")
  id("otel.spotless-conventions")
  id("org.owasp.dependencycheck")
}

val otelJava = extensions.create<OtelJavaExtension>("otelJava")

afterEvaluate {
  val previousBaseArchiveName = base.archivesName.get()
  if (findProperty("mavenGroupId") == "io.opentelemetry.javaagent.instrumentation") {
    base.archivesName.set("opentelemetry-javaagent-$previousBaseArchiveName")
  } else if (!previousBaseArchiveName.startsWith("opentelemetry-")) {
    base.archivesName.set("opentelemetry-$previousBaseArchiveName")
  }
}

// Version to use to compile code and run tests.
val DEFAULT_JAVA_VERSION = JavaVersion.VERSION_21

java {
  toolchain {
    languageVersion.set(
      otelJava.minJavaVersionSupported.map {
        val defaultJavaVersion = otelJava.maxJavaVersionSupported.getOrElse(DEFAULT_JAVA_VERSION).majorVersion.toInt()
        JavaLanguageVersion.of(Math.max(it.majorVersion.toInt(), defaultJavaVersion))
      }
    )
  }

  // See https://docs.gradle.org/current/userguide/upgrading_version_5.html, Automatic target JVM version
  disableAutoTargetJvm()
  withJavadocJar()
  withSourcesJar()
}

// this task is helpful for tracking down and aligning dependency versions across modules in order
// to limit versions that are be loaded by Intellij
// (the normal dependencies task selector only executes the task on a single project)
//
// ./gradlew intellijDeps
//           | grep -Po "\--- \K.*"
//           | sed "s/:[0-9].* -> \(.*\)/:\1/"
//           | sed "s/:[0-9].* -> \(.*\)/:\1/"
//           | sort -u
tasks.register<DependencyReportTask>("intellijDeps")

tasks.withType<JavaCompile>().configureEach {
  with(options) {
    release.set(otelJava.minJavaVersionSupported.map { it.majorVersion.toInt() })

    if (name != "jmhCompileGeneratedClasses") {
      compilerArgs.addAll(
        listOf(
          "-Xlint:all",
          // We suppress the "try" warning because it disallows managing an auto-closeable with
          // try-with-resources without referencing the auto-closeable within the try block.
          "-Xlint:-try",
          // We suppress the "processing" warning as suggested in
          // https://groups.google.com/forum/#!topic/bazel-discuss/_R3A9TJSoPM
          "-Xlint:-processing",
          // We suppress the "options" warning because it prevents compilation on modern JDKs
          "-Xlint:-options",
          // jdk21 generates more serial warnings than previous versions
          "-Xlint:-serial"
        )
      )
      if (System.getProperty("dev") != "true") {
        // Fail build on any warning
        compilerArgs.add("-Werror")
      }
    }

    encoding = "UTF-8"

    if (name.contains("Test")) {
      // serialVersionUID is basically guaranteed to be useless in tests
      compilerArgs.add("-Xlint:-serial")
      // when code is compiled with jdk 21 and executed with jdk 8, the -parameters flag is needed to avoid
      // java.lang.reflect.MalformedParametersException: Invalid parameter name ""
      // when junit calls java.lang.reflect.Executable.getParameters() on the constructor of a
      // non-static nested test class
      compilerArgs.add("-parameters")
    }
  }
}

// Groovy and Scala compilers don't actually understand --release option
afterEvaluate {
  tasks.withType<GroovyCompile>().configureEach {
    var javaVersion = otelJava.minJavaVersionSupported.get().majorVersion
    if (javaVersion == "8") {
      javaVersion = "1.8"
    }
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }
  tasks.withType<ScalaCompile>().configureEach {
    sourceCompatibility = otelJava.minJavaVersionSupported.get().majorVersion
    targetCompatibility = otelJava.minJavaVersionSupported.get().majorVersion
  }
  tasks.withType<Javadoc>().configureEach {
    with(options) {
      source = otelJava.minJavaVersionSupported.get().majorVersion
    }
  }
  tasks.withType<JavaCompile>().configureEach {
    if (javaCompiler.isPresent && javaCompiler.get().metadata.languageVersion.canCompileOrRun(21)) {
      // new warning in jdk21
      options.compilerArgs.add("-Xlint:-this-escape")
    }
  }
}

evaluationDependsOn(":dependencyManagement")
val dependencyManagementConf = configurations.create("dependencyManagement") {
  isCanBeConsumed = false
  isCanBeResolved = false
  isVisible = false
}
afterEvaluate {
  configurations.configureEach {
    if (isCanBeResolved && !isCanBeConsumed) {
      extendsFrom(dependencyManagementConf)
    }
  }
}

// Force 4.0, or 4.1 to the highest version of that branch. Since 4.0 and 4.1 often have
// compatibility issues we can't just force to the highest version using normal BOM dependencies.
abstract class NettyAlignmentRule : ComponentMetadataRule {
  override fun execute(ctx: ComponentMetadataContext) {
    with(ctx.details) {
      if (id.group == "io.netty" && id.name != "netty") {
        if (id.version.startsWith("4.1.")) {
          belongsTo("io.netty:netty-bom:4.1.118.Final", false)
        } else if (id.version.startsWith("4.0.")) {
          belongsTo("io.netty:netty-bom:4.0.56.Final", false)
        }
      }
    }
  }
}

dependencies {
  add(dependencyManagementConf.name, platform(project(":dependencyManagement")))

  components.all<NettyAlignmentRule>()

  compileOnly("com.google.code.findbugs:jsr305")
  compileOnly("com.google.errorprone:error_prone_annotations")

  codenarc("org.codenarc:CodeNarc:3.6.0")
  codenarc(platform("org.codehaus.groovy:groovy-bom:3.0.23"))

  modules {
    // checkstyle uses the very old google-collections which causes Java 9 module conflict with
    // guava which is also on the classpath
    module("com.google.collections:google-collections") {
      replacedBy("com.google.guava:guava", "google-collections is now part of Guava")
    }
  }
}

testing {
  suites.withType(JvmTestSuite::class).configureEach {
    dependencies {
      implementation("org.junit.jupiter:junit-jupiter-api")
      implementation("org.junit.jupiter:junit-jupiter-params")
      runtimeOnly("org.junit.jupiter:junit-jupiter-engine")
      runtimeOnly("org.junit.vintage:junit-vintage-engine")
      implementation("org.junit-pioneer:junit-pioneer")

      implementation("org.assertj:assertj-core")
      implementation("org.awaitility:awaitility")
      implementation("org.mockito:mockito-core")
      implementation("org.mockito:mockito-inline")
      implementation("org.mockito:mockito-junit-jupiter")

      implementation("org.objenesis:objenesis")
      implementation("org.spockframework:spock-core") {
        with(this as ExternalDependency) {
          // exclude optional dependencies
          exclude(group = "cglib", module = "cglib-nodep")
          exclude(group = "net.bytebuddy", module = "byte-buddy")
          exclude(group = "org.junit.platform", module = "junit-platform-testkit")
          exclude(group = "org.jetbrains", module = "annotations")
          exclude(group = "org.objenesis", module = "objenesis")
          exclude(group = "org.ow2.asm", module = "asm")
        }
      }
      implementation("org.spockframework:spock-junit4") {
        with(this as ExternalDependency) {
          // spock-core is already added as dependency
          // exclude it here to avoid pulling in optional dependencies
          exclude(group = "org.spockframework", module = "spock-core")
        }
      }
      implementation("ch.qos.logback:logback-classic")
      implementation("org.slf4j:log4j-over-slf4j")
      implementation("org.slf4j:jcl-over-slf4j")
      implementation("org.slf4j:jul-to-slf4j")
      implementation("com.github.stefanbirkner:system-rules")
    }
  }
}

var path = project.path
if (path.startsWith(":instrumentation:")) {
  // remove segments that are a prefix of the next segment
  // for example :instrumentation:log4j:log4j-context-data:log4j-context-data-2.17 is transformed to log4j-context-data-2.17
  var tmpPath = path
  val suffix = tmpPath.substringAfterLast(':')
  var prefix = ":instrumentation:"
  if (suffix == "library") {
    // strip ":library" suffix
    tmpPath = tmpPath.substringBeforeLast(':')
  } else if (suffix == "library-autoconfigure") {
    // replace ":library-autoconfigure" with "-autoconfigure"
    tmpPath = tmpPath.substringBeforeLast(':') + "-autoconfigure"
  } else if (suffix == "javaagent") {
    // strip ":javaagent" suffix and add it to prefix
    prefix += "javaagent:"
    tmpPath = tmpPath.substringBeforeLast(':')
  }
  val segments = tmpPath.substring(":instrumentation:".length).split(':')
  var newPath = ""
  var done = false
  for (s in segments) {
    if (!done && (newPath.isEmpty() || s.startsWith(newPath))) {
      newPath = s
    } else {
      newPath += ":$s"
      done = true
    }
  }
  if (newPath.isNotEmpty()) {
    path = prefix + newPath
  }
}
var javaModuleName = "io.opentelemetry" + path.replace(".", "_").replace("-", "_").replace(":", ".")

tasks {
  named<Jar>("jar") {
    // By default Gradle Jar task can put multiple files with the same name
    // into a Jar. This may lead to confusion. For example if auto-service
    // annotation processing creates files with same name in `scala` and
    // `java` directory this would result in Jar having two files with the
    // same name in it. Which in turn would result in only one of those
    // files being actually considered when that Jar is used leading to very
    // confusing failures. Instead we should 'fail early' and avoid building such Jars.
    duplicatesStrategy = DuplicatesStrategy.FAIL

    manifest {
      attributes(
        "Implementation-Title" to project.name,
        "Implementation-Version" to project.version,
        "Implementation-Vendor" to "OpenTelemetry",
        "Implementation-URL" to "https://github.com/open-telemetry/opentelemetry-java-instrumentation",
        "Automatic-Module-Name" to javaModuleName
      )
    }
  }

  named<Javadoc>("javadoc") {
    with(options as StandardJavadocDocletOptions) {
      source = "8"
      encoding = "UTF-8"
      docEncoding = "UTF-8"
      charSet = "UTF-8"
      breakIterator(true)

      addStringOption("Xdoclint:none", "-quiet")
      // non-standard option to fail on warnings, see https://bugs.openjdk.java.net/browse/JDK-8200363
      addStringOption("Xwerror", "-quiet")
    }
  }

  withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    dirPermissions {
      unix("755")
    }
    filePermissions {
      unix("644")
    }
  }

  // Convenient when updating errorprone
  register("compileAllJava") {
    dependsOn(withType<JavaCompile>())
  }
}

normalization {
  runtimeClasspath {
    metaInf {
      ignoreAttribute("Implementation-Version")
    }
  }
}

fun isJavaVersionAllowed(version: JavaVersion): Boolean {
  if (otelJava.minJavaVersionSupported.get().compareTo(version) > 0) {
    return false
  }
  if (otelJava.maxJavaVersionForTests.isPresent && otelJava.maxJavaVersionForTests.get().compareTo(version) < 0) {
    return false
  }
  return true
}

abstract class TestcontainersBuildService : BuildService<BuildServiceParameters.None>

// To limit number of concurrently running resource intensive tests add
// tasks {
//   test {
//     usesService(gradle.sharedServices.registrations["testcontainersBuildService"].service)
//   }
// }
gradle.sharedServices.registerIfAbsent("testcontainersBuildService", TestcontainersBuildService::class.java) {
  maxParallelUsages.convention(2)
}

val resourceNames = listOf("Host", "Os", "Process", "ProcessRuntime")
val resourceClassesCsv = resourceNames.joinToString(",") { "io.opentelemetry.sdk.extension.resources.${it}ResourceProvider" }

tasks.withType<Test>().configureEach {
  useJUnitPlatform()

  // There's no real harm in setting this for all tests even if any happen to not be using context
  // propagation.
  jvmArgs("-Dio.opentelemetry.context.enableStrictContext=${rootProject.findProperty("enableStrictContext") ?: true}")
  // TODO: Have agent map unshaded to shaded.
  if (project.findProperty("disableShadowRelocate") != "true") {
    jvmArgs("-Dio.opentelemetry.javaagent.shaded.io.opentelemetry.context.enableStrictContext=${rootProject.findProperty("enableStrictContext") ?: true}")
  } else {
    jvmArgs("-Dotel.instrumentation.opentelemetry-api.enabled=false")
    jvmArgs("-Dotel.instrumentation.opentelemetry-instrumentation-api.enabled=false")
  }

  // Disable default resource providers since they cause lots of output we don't need.
  jvmArgs("-Dotel.java.disabled.resource.providers=$resourceClassesCsv")

  val trustStore = project(":testing-common").file("src/misc/testing-keystore.p12")
  // Work around payara not working when this is set for some reason.
  // Don't set for:
  // - aws-sdk as we have tests that interact with AWS and need normal trustStore
  // - camel as we have tests that interact with AWS and need normal trustStore
  // - vaadin as tests need to be able to download nodejs when not cached in ~/.vaadin/
  if (project.name != "jaxrs-2.0-payara-testing" && !project.path.contains("vaadin") && project.description != "camel-2-20" && !project.path.contains("aws-sdk")) {
    jvmArgumentProviders.add(KeystoreArgumentsProvider(trustStore))
  }

  // All tests must complete within 15 minutes.
  // This value is quite big because with lower values (3 mins) we were experiencing large number of false positives
  timeout.set(Duration.ofMinutes(15))

  val defaultMaxRetries = if (System.getenv().containsKey("CI")) 5 else 0
  val maxTestRetries = gradle.startParameter.projectProperties["maxTestRetries"]?.toInt() ?: defaultMaxRetries

  develocity.testRetry {
    // You can see tests that were retried by this mechanism in the collected test reports and build scans.
    maxRetries.set(maxTestRetries);
  }

  reports {
    junitXml.isOutputPerTestCase = true
  }

  testLogging {
    exceptionFormat = TestExceptionFormat.FULL
    showStandardStreams = true
  }
}

class KeystoreArgumentsProvider(
  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  val trustStore: File
) : CommandLineArgumentProvider {
  override fun asArguments(): Iterable<String> = listOf(
    "-Djavax.net.ssl.trustStore=${trustStore.absolutePath}",
    "-Djavax.net.ssl.trustStorePassword=testing"
  )
}

afterEvaluate {
  val testJavaVersion = gradle.startParameter.projectProperties["testJavaVersion"]?.let(JavaVersion::toVersion)
  val useJ9 = gradle.startParameter.projectProperties["testJavaVM"]?.run { this == "openj9" }
    ?: false
  tasks.withType<Test>().configureEach {
    if (testJavaVersion != null) {
      javaLauncher.set(
        javaToolchains.launcherFor {
          languageVersion.set(JavaLanguageVersion.of(testJavaVersion.majorVersion))
          implementation.set(if (useJ9) JvmImplementation.J9 else JvmImplementation.VENDOR_SPECIFIC)
        }
      )
      isEnabled = isEnabled && isJavaVersionAllowed(testJavaVersion)
    } else {
      // We default to testing with Java 11 for most tests, but some tests don't support it, where we change
      // the default test task's version so commands like `./gradlew check` can test all projects regardless
      // of Java version.
      if (!isJavaVersionAllowed(DEFAULT_JAVA_VERSION) && otelJava.maxJavaVersionForTests.isPresent) {
        javaLauncher.set(
          javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(otelJava.maxJavaVersionForTests.get().majorVersion))
          }
        )
      }
    }
  }
}

codenarc {
  configFile = rootProject.file("buildscripts/codenarc.groovy")
}

checkstyle {
  configFile = rootProject.file("buildscripts/checkstyle.xml")
  // this version should match the version of google_checks.xml used as basis for above configuration
  toolVersion = "10.21.2"
  maxWarnings = 0
}

dependencyCheck {
  skipConfigurations = listOf("errorprone", "checkstyle", "annotationProcessor")
  suppressionFile = "buildscripts/dependency-check-suppressions.xml"
  failBuildOnCVSS = 7.0f // fail on high or critical CVE
  nvd.apiKey = System.getenv("NVD_API_KEY")
  nvd.delay = 3500 // until next dependency check release (https://github.com/jeremylong/DependencyCheck/pull/6333)
}

idea {
  module {
    isDownloadJavadoc = false
    isDownloadSources = false
  }
}

when (projectDir.name) {
  "bootstrap", "javaagent", "library", "library-autoconfigure", "testing" -> {
    // We don't use this group anywhere in our config, but we need to make sure it is unique per
    // instrumentation so Gradle doesn't merge projects with same name due to a bug in Gradle.
    // https://github.com/gradle/gradle/issues/847
    // In otel.publish-conventions, we set the maven group, which is what matters, to the correct
    // value.
    group = "io.opentelemetry.dummy.${projectDir.parentFile.name}"
  }
}

configurations.configureEach {
  resolutionStrategy {
    // While you might think preferProjectModules would do this, it doesn't. If this gets hard to
    // manage, we could consider having the io.opentelemetry.instrumentation add information about
    // what modules they add to reference generically.
    dependencySubstitution {
      substitute(module("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")).using(project(":instrumentation-api"))
      substitute(module("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")).using(project(":instrumentation-api-incubator"))
      substitute(module("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations")).using(project(":instrumentation-annotations"))
      substitute(module("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support")).using(
        project(":instrumentation-annotations-support")
      )
      substitute(module("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")).using(project(":javaagent-bootstrap"))
      substitute(module("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")).using(project(":javaagent-extension-api"))
      substitute(module("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")).using(project(":javaagent-tooling"))
      substitute(module("io.opentelemetry.javaagent:opentelemetry-agent-for-testing")).using(project(":testing:agent-for-testing"))
      substitute(module("io.opentelemetry.javaagent:opentelemetry-testing-common")).using(project(":testing-common"))
      substitute(module("io.opentelemetry.javaagent:opentelemetry-muzzle")).using(project(":muzzle"))
      substitute(module("io.opentelemetry.javaagent:opentelemetry-javaagent")).using(project(":javaagent"))
    }

    // The above substitutions ensure dependencies managed by this BOM for external projects refer to this repo's projects here.
    // Excluding the bom as well helps ensure if we miss a substitution, we get a resolution failure instead of using the
    // wrong version.
    exclude("io.opentelemetry.instrumentation", "opentelemetry-instrumentation-bom-alpha")
  }
}
