plugins {
  `java-platform`
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val dependencyVersions = hashMapOf<String, String>()
rootProject.extra["versions"] = dependencyVersions

// this line is managed by .github/scripts/update-sdk-version.sh
val otelSdkVersion = "1.46.0"
val otelContribVersion = "1.43.0-alpha"
val otelSdkAlphaVersion = otelSdkVersion.replaceFirst("(-SNAPSHOT)?$".toRegex(), "-alpha$1")

// Need both BOM and groovy jars
val groovyVersion = "4.0.25"

// We don't force libraries we instrument to new versions since we compile and test against specific
// old baseline versions but we do try to force those libraries' transitive dependencies to new
// versions where possible so that we don't end up with explosion of dependency versions in
// Intellij, which causes Intellij to spend lots of time indexing all of those different dependency
// versions, and makes debugging painful because Intellij has no idea which dependency version's
// source to use when stepping through code.
//
// Sometimes libraries we instrument do require a specific version of a transitive dependency and
// that can be applied in the specific instrumentation gradle file, e.g.
// configurations.testRuntimeClasspath.resolutionStrategy.force "com.google.guava:guava:19.0"

val DEPENDENCY_BOMS = listOf(
  "com.fasterxml.jackson:jackson-bom:2.18.2",
  "com.squareup.okio:okio-bom:3.10.2", // see https://github.com/open-telemetry/opentelemetry-java/issues/5637
  "com.google.guava:guava-bom:33.4.0-jre",
  "org.apache.groovy:groovy-bom:${groovyVersion}",
  "io.opentelemetry:opentelemetry-bom:${otelSdkVersion}",
  "io.opentelemetry:opentelemetry-bom-alpha:${otelSdkAlphaVersion}",
  "org.junit:junit-bom:5.11.4",
  "org.testcontainers:testcontainers-bom:1.20.4",
  "org.spockframework:spock-bom:2.4-M5-groovy-4.0"
)

val autoServiceVersion = "1.1.1"
val autoValueVersion = "1.11.0"
val errorProneVersion = "2.36.0"
val byteBuddyVersion = "1.16.1"
val asmVersion = "9.7.1"
val jmhVersion = "1.37"
val mockitoVersion = "4.11.0"
val slf4jVersion = "2.0.16"
val semConvVersion = "1.30.0-rc.1"

val CORE_DEPENDENCIES = listOf(
  "io.opentelemetry.semconv:opentelemetry-semconv:${semConvVersion}",
  "io.opentelemetry.semconv:opentelemetry-semconv-incubating:${semConvVersion}",
  "com.google.auto.service:auto-service:${autoServiceVersion}",
  "com.google.auto.service:auto-service-annotations:${autoServiceVersion}",
  "com.google.auto.value:auto-value:${autoValueVersion}",
  "com.google.auto.value:auto-value-annotations:${autoValueVersion}",
  "com.google.errorprone:error_prone_annotations:${errorProneVersion}",
  "com.google.errorprone:error_prone_core:${errorProneVersion}",
  "com.google.errorprone:error_prone_test_helpers:${errorProneVersion}",
  // When updating, also update conventions/build.gradle.kts
  "net.bytebuddy:byte-buddy:${byteBuddyVersion}",
  "net.bytebuddy:byte-buddy-dep:${byteBuddyVersion}",
  "net.bytebuddy:byte-buddy-agent:${byteBuddyVersion}",
  "net.bytebuddy:byte-buddy-gradle-plugin:${byteBuddyVersion}",
  "org.ow2.asm:asm:${asmVersion}",
  "org.ow2.asm:asm-tree:${asmVersion}",
  "org.ow2.asm:asm-util:${asmVersion}",
  "org.openjdk.jmh:jmh-core:${jmhVersion}",
  "org.openjdk.jmh:jmh-generator-bytecode:${jmhVersion}",
  "org.mockito:mockito-core:${mockitoVersion}",
  "org.mockito:mockito-junit-jupiter:${mockitoVersion}",
  "org.mockito:mockito-inline:${mockitoVersion}",
  "org.slf4j:slf4j-api:${slf4jVersion}",
  "org.slf4j:slf4j-simple:${slf4jVersion}",
  "org.slf4j:log4j-over-slf4j:${slf4jVersion}",
  "org.slf4j:jcl-over-slf4j:${slf4jVersion}",
  "org.slf4j:jul-to-slf4j:${slf4jVersion}"
)

// See the comment above about why we keep this rather large list.
// There are dependencies included here that appear to have no usages, but are maintained at
// this top level to help consistently satisfy large numbers of transitive dependencies.
val DEPENDENCIES = listOf(
  "io.r2dbc:r2dbc-proxy:1.1.5.RELEASE",
  "ch.qos.logback:logback-classic:1.3.15", // 1.4+ requires Java 11+
  "com.github.stefanbirkner:system-lambda:1.2.1",
  "com.github.stefanbirkner:system-rules:1.19.0",
  "uk.org.webcompere:system-stubs-jupiter:2.0.3",
  "com.uber.nullaway:nullaway:0.12.3",
  "commons-beanutils:commons-beanutils:1.10.0",
  "commons-cli:commons-cli:1.9.0",
  "commons-codec:commons-codec:1.18.0",
  "commons-collections:commons-collections:3.2.2",
  "commons-digester:commons-digester:2.1",
  "commons-fileupload:commons-fileupload:1.5",
  "commons-io:commons-io:2.18.0",
  "commons-lang:commons-lang:2.6",
  "commons-logging:commons-logging:1.3.4",
  "commons-validator:commons-validator:1.9.0",
  "io.netty:netty:3.10.6.Final",
  "io.opentelemetry.contrib:opentelemetry-aws-resources:${otelContribVersion}",
  "io.opentelemetry.contrib:opentelemetry-aws-xray-propagator:${otelContribVersion}",
  "io.opentelemetry.contrib:opentelemetry-gcp-resources:${otelContribVersion}",
  "io.opentelemetry.contrib:opentelemetry-baggage-processor:${otelContribVersion}",
  "io.opentelemetry.proto:opentelemetry-proto:1.5.0-alpha",
  "io.opentelemetry:opentelemetry-extension-annotations:1.18.0", // deprecated, no longer part of bom
  "org.assertj:assertj-core:3.27.3",
  "org.awaitility:awaitility:4.2.2",
  "com.google.code.findbugs:annotations:3.0.1u2",
  "com.google.code.findbugs:jsr305:3.0.2",
  "org.apache.groovy:groovy:${groovyVersion}",
  "org.apache.groovy:groovy-json:${groovyVersion}",
  "org.codehaus.mojo:animal-sniffer-annotations:1.24",
  "org.junit-pioneer:junit-pioneer:1.9.1",
  "org.objenesis:objenesis:3.4",
  "javax.validation:validation-api:2.0.1.Final",
  "org.snakeyaml:snakeyaml-engine:2.9",
  "org.elasticmq:elasticmq-rest-sqs_2.13:1.6.11"
)

javaPlatform {
  allowDependencies()
}

dependencies {
  for (bom in DEPENDENCY_BOMS) {
    api(enforcedPlatform(bom))
    val split = bom.split(':')
    dependencyVersions[split[0]] = split[2]
  }
  constraints {
    for (dependency in CORE_DEPENDENCIES) {
      api(dependency)
      val split = dependency.split(':')
      dependencyVersions[split[0]] = split[2]
    }
    for (dependency in DEPENDENCIES) {
      api(dependency)
      val split = dependency.split(':')
      dependencyVersions[split[0]] = split[2]
    }
  }
}
