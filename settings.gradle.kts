pluginManagement {
  plugins {
    id("com.bmuschko.docker-remote-api") version "8.1.0"
    id("com.github.ben-manes.versions") version "0.46.0"
    id("com.github.jk1.dependency-license-report") version "2.1"
    id("com.google.cloud.tools.jib") version "3.3.1"
    id("com.gradle.plugin-publish") version "1.2.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    id("org.jetbrains.kotlin.jvm") version "1.8.21"
    id("org.xbib.gradle.plugin.jflex") version "1.7.0"
    id("org.unbroken-dome.xjc") version "2.0.0"
    id("org.graalvm.buildtools.native") version "0.9.21"
  }
}

plugins {
  id("com.gradle.enterprise") version "3.13.2"
  id("com.gradle.common-custom-user-data-gradle-plugin") version "1.10"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    mavenLocal()
  }
}

val gradleEnterpriseServer = "https://ge.opentelemetry.io"
val isCI = System.getenv("CI") != null
val geAccessKey = System.getenv("GRADLE_ENTERPRISE_ACCESS_KEY") ?: ""

// if GE access key is not given and we are in CI, then we publish to scans.gradle.com
val useScansGradleCom = isCI && geAccessKey.isEmpty()

if (useScansGradleCom) {
  gradleEnterprise {
    buildScan {
      termsOfServiceUrl = "https://gradle.com/terms-of-service"
      termsOfServiceAgree = "yes"
      isUploadInBackground = !isCI
      publishAlways()

      capture {
        isTaskInputFiles = true
      }
    }
  }
} else {
  gradleEnterprise {
    server = gradleEnterpriseServer
    buildScan {
      isUploadInBackground = !isCI

      this as com.gradle.enterprise.gradleplugin.internal.extension.BuildScanExtensionWithHiddenFeatures
      publishIfAuthenticated()
      publishAlways()

      capture {
        isTaskInputFiles = true
      }

      gradle.startParameter.projectProperties["testJavaVersion"]?.let { tag(it) }
      gradle.startParameter.projectProperties["testJavaVM"]?.let { tag(it) }
      gradle.startParameter.projectProperties["smokeTestSuite"]?.let {
        value("Smoke test suite", it)
      }
    }
  }
}

val geCacheUsername = System.getenv("GE_CACHE_USERNAME") ?: ""
val geCachePassword = System.getenv("GE_CACHE_PASSWORD") ?: ""
buildCache {
  remote<HttpBuildCache> {
    url = uri("$gradleEnterpriseServer/cache/")
    isPush = isCI && geCacheUsername.isNotEmpty()
    credentials {
      username = geCacheUsername
      password = geCachePassword
    }
  }
}

rootProject.name = "opentelemetry-java-instrumentation"

includeBuild("conventions")

include(":custom-checks")

include(":muzzle")

// agent projects
include(":opentelemetry-api-shaded-for-instrumenting")
include(":opentelemetry-ext-annotations-shaded-for-instrumenting")
include(":opentelemetry-instrumentation-annotations-shaded-for-instrumenting")
include(":opentelemetry-instrumentation-api-shaded-for-instrumenting")
include(":javaagent-bootstrap")
include(":javaagent-extension-api")
include(":javaagent-tooling")
include(":javaagent-tooling:javaagent-tooling-java9")
include(":javaagent-internal-logging-application")
include(":javaagent-internal-logging-simple")
include(":javaagent")

include(":bom")
include(":bom-alpha")
include(":instrumentation-api")
include(":instrumentation-api-semconv")
include(":instrumentation-annotations")
include(":instrumentation-annotations-support")
include(":instrumentation-annotations-support-testing")

// misc
include(":dependencyManagement")
include(":testing:agent-exporter")
include(":testing:agent-for-testing")
include(":testing:armeria-shaded-for-testing")
include(":testing-common")
include(":testing-common:integration-tests")
include(":testing-common:library-for-integration-tests")

// smoke tests
include(":smoke-tests")
include(":smoke-tests:images:fake-backend")
include(":smoke-tests:images:grpc")
include(":smoke-tests:images:play")
include(":smoke-tests:images:quarkus")
include(":smoke-tests:images:security-manager")
include(":smoke-tests:images:servlet")
hideFromDependabot(":smoke-tests:images:servlet:servlet-3.0")
hideFromDependabot(":smoke-tests:images:servlet:servlet-5.0")
hideFromDependabot(":smoke-tests:images:spring-boot")

hideFromDependabot("instrumentation:akka:akka-actor-2.3:javaagent")
hideFromDependabot(":instrumentation:akka:akka-actor-fork-join-2.5:javaagent")
hideFromDependabot(":instrumentation:akka:akka-http-10.0:javaagent")
hideFromDependabot(":instrumentation:apache-dbcp-2.0:javaagent")
hideFromDependabot(":instrumentation:apache-dbcp-2.0:library")
hideFromDependabot(":instrumentation:apache-dbcp-2.0:testing")
hideFromDependabot(":instrumentation:apache-dubbo-2.7:javaagent")
hideFromDependabot(":instrumentation:apache-dubbo-2.7:library-autoconfigure")
hideFromDependabot(":instrumentation:apache-dubbo-2.7:testing")
hideFromDependabot(":instrumentation:apache-httpasyncclient-4.1:javaagent")
hideFromDependabot(":instrumentation:apache-httpclient:apache-httpclient-2.0:javaagent")
hideFromDependabot(":instrumentation:apache-httpclient:apache-httpclient-4.0:javaagent")
hideFromDependabot(":instrumentation:apache-httpclient:apache-httpclient-4.3:library")
hideFromDependabot(":instrumentation:apache-httpclient:apache-httpclient-4.3:testing")
hideFromDependabot(":instrumentation:apache-httpclient:apache-httpclient-5.0:javaagent")
hideFromDependabot(":instrumentation:armeria-1.3:javaagent")
hideFromDependabot(":instrumentation:armeria-1.3:library")
hideFromDependabot(":instrumentation:armeria-1.3:testing")
hideFromDependabot(":instrumentation:async-http-client:async-http-client-1.9:javaagent")
hideFromDependabot(":instrumentation:async-http-client:async-http-client-2.0:javaagent")
hideFromDependabot(":instrumentation:aws-lambda:aws-lambda-core-1.0:javaagent")
hideFromDependabot(":instrumentation:aws-lambda:aws-lambda-core-1.0:library")
hideFromDependabot(":instrumentation:aws-lambda:aws-lambda-core-1.0:testing")
hideFromDependabot(":instrumentation:aws-lambda:aws-lambda-events-2.2:javaagent")
hideFromDependabot(":instrumentation:aws-lambda:aws-lambda-events-2.2:library")
hideFromDependabot(":instrumentation:aws-lambda:aws-lambda-events-2.2:testing")
hideFromDependabot(":instrumentation:aws-sdk:aws-sdk-1.11:javaagent")
hideFromDependabot(":instrumentation:aws-sdk:aws-sdk-1.11:library")
hideFromDependabot(":instrumentation:aws-sdk:aws-sdk-1.11:library-autoconfigure")
hideFromDependabot(":instrumentation:aws-sdk:aws-sdk-1.11:testing")
hideFromDependabot(":instrumentation:aws-sdk:aws-sdk-2.2:javaagent")
hideFromDependabot(":instrumentation:aws-sdk:aws-sdk-2.2:library")
hideFromDependabot(":instrumentation:aws-sdk:aws-sdk-2.2:library-autoconfigure")
hideFromDependabot(":instrumentation:aws-sdk:aws-sdk-2.2:testing")
hideFromDependabot(":instrumentation:azure-core:azure-core-1.14:javaagent")
hideFromDependabot(":instrumentation:azure-core:azure-core-1.14:library-instrumentation-shaded")
hideFromDependabot(":instrumentation:azure-core:azure-core-1.19:javaagent")
hideFromDependabot(":instrumentation:azure-core:azure-core-1.19:library-instrumentation-shaded")
hideFromDependabot(":instrumentation:azure-core:azure-core-1.36:javaagent")
hideFromDependabot(":instrumentation:azure-core:azure-core-1.36:library-instrumentation-shaded")
hideFromDependabot(":instrumentation:camel-2.20:javaagent")
hideFromDependabot(":instrumentation:camel-2.20:javaagent-unit-tests")
hideFromDependabot(":instrumentation:cassandra:cassandra-3.0:javaagent")
hideFromDependabot(":instrumentation:cassandra:cassandra-4.0:javaagent")
hideFromDependabot(":instrumentation:cassandra:cassandra-4.4:javaagent")
hideFromDependabot(":instrumentation:cassandra:cassandra-4.4:library")
hideFromDependabot(":instrumentation:cassandra:cassandra-4.4:testing")
hideFromDependabot(":instrumentation:cassandra:cassandra-4-common:testing")
hideFromDependabot(":instrumentation:cdi-testing")
hideFromDependabot(":instrumentation:graphql-java-12.0:javaagent")
hideFromDependabot(":instrumentation:graphql-java-12.0:library")
hideFromDependabot(":instrumentation:graphql-java-12.0:testing")
hideFromDependabot(":instrumentation:internal:internal-application-logger:bootstrap")
hideFromDependabot(":instrumentation:internal:internal-application-logger:javaagent")
hideFromDependabot(":instrumentation:internal:internal-class-loader:javaagent")
hideFromDependabot(":instrumentation:internal:internal-class-loader:javaagent-integration-tests")
hideFromDependabot(":instrumentation:internal:internal-eclipse-osgi-3.6:javaagent")
hideFromDependabot(":instrumentation:internal:internal-lambda:javaagent")
hideFromDependabot(":instrumentation:internal:internal-lambda-java9:javaagent")
hideFromDependabot(":instrumentation:internal:internal-reflection:javaagent")
hideFromDependabot(":instrumentation:internal:internal-reflection:javaagent-integration-tests")
hideFromDependabot(":instrumentation:internal:internal-url-class-loader:javaagent")
hideFromDependabot(":instrumentation:internal:internal-url-class-loader:javaagent-integration-tests")
hideFromDependabot(":instrumentation:c3p0-0.9:javaagent")
hideFromDependabot(":instrumentation:c3p0-0.9:library")
hideFromDependabot(":instrumentation:c3p0-0.9:testing")
hideFromDependabot(":instrumentation:couchbase:couchbase-2.0:javaagent")
hideFromDependabot(":instrumentation:couchbase:couchbase-2.6:javaagent")
hideFromDependabot(":instrumentation:couchbase:couchbase-2-common:javaagent")
hideFromDependabot(":instrumentation:couchbase:couchbase-2-common:javaagent-unit-tests")
hideFromDependabot(":instrumentation:couchbase:couchbase-3.1:javaagent")
hideFromDependabot(":instrumentation:couchbase:couchbase-3.1:tracing-opentelemetry-shaded")
hideFromDependabot(":instrumentation:couchbase:couchbase-3.1.6:javaagent")
hideFromDependabot(":instrumentation:couchbase:couchbase-3.1.6:tracing-opentelemetry-shaded")
hideFromDependabot(":instrumentation:couchbase:couchbase-3.2:javaagent")
hideFromDependabot(":instrumentation:couchbase:couchbase-3.2:tracing-opentelemetry-shaded")
hideFromDependabot(":instrumentation:couchbase:couchbase-common:testing")
hideFromDependabot(":instrumentation:dropwizard:dropwizard-metrics-4.0:javaagent")
hideFromDependabot(":instrumentation:dropwizard:dropwizard-views-0.7:javaagent")
hideFromDependabot(":instrumentation:dropwizard:dropwizard-testing")
hideFromDependabot(":instrumentation:elasticsearch:elasticsearch-rest-common:javaagent")
hideFromDependabot(":instrumentation:opensearch:opensearch-rest-common:javaagent")
hideFromDependabot(":instrumentation:elasticsearch:elasticsearch-rest-5.0:javaagent")
hideFromDependabot(":instrumentation:elasticsearch:elasticsearch-rest-6.4:javaagent")
hideFromDependabot(":instrumentation:elasticsearch:elasticsearch-rest-7.0:javaagent")
hideFromDependabot(":instrumentation:opensearch:opensearch-rest-1.0:javaagent")
hideFromDependabot(":instrumentation:elasticsearch:elasticsearch-transport-common:javaagent")
hideFromDependabot(":instrumentation:elasticsearch:elasticsearch-transport-common:testing")
hideFromDependabot(":instrumentation:elasticsearch:elasticsearch-transport-5.0:javaagent")
hideFromDependabot(":instrumentation:elasticsearch:elasticsearch-transport-5.3:javaagent")
hideFromDependabot(":instrumentation:elasticsearch:elasticsearch-transport-6.0:javaagent")
hideFromDependabot(":instrumentation:executors:bootstrap")
hideFromDependabot(":instrumentation:executors:javaagent")
hideFromDependabot(":instrumentation:executors:testing")
hideFromDependabot(":instrumentation:external-annotations:javaagent")
hideFromDependabot(":instrumentation:external-annotations:javaagent-unit-tests")
hideFromDependabot(":instrumentation:finatra-2.9:javaagent")
hideFromDependabot(":instrumentation:geode-1.4:javaagent")
hideFromDependabot(":instrumentation:google-http-client-1.19:javaagent")
hideFromDependabot(":instrumentation:grails-3.0:javaagent")
hideFromDependabot(":instrumentation:grizzly-2.0:javaagent")
hideFromDependabot(":instrumentation:grpc-1.6:javaagent")
hideFromDependabot(":instrumentation:grpc-1.6:library")
hideFromDependabot(":instrumentation:grpc-1.6:testing")
hideFromDependabot(":instrumentation:guava-10.0:javaagent")
hideFromDependabot(":instrumentation:guava-10.0:library")
hideFromDependabot(":instrumentation:gwt-2.0:javaagent")
hideFromDependabot(":instrumentation:hibernate:hibernate-3.3:javaagent")
hideFromDependabot(":instrumentation:hibernate:hibernate-4.0:javaagent")
hideFromDependabot(":instrumentation:hibernate:hibernate-6.0:javaagent")
hideFromDependabot(":instrumentation:hibernate:hibernate-6.0:spring-testing")
hideFromDependabot(":instrumentation:hibernate:hibernate-common:javaagent")
hideFromDependabot(":instrumentation:hibernate:hibernate-procedure-call-4.3:javaagent")
hideFromDependabot(":instrumentation:hikaricp-3.0:javaagent")
hideFromDependabot(":instrumentation:hikaricp-3.0:library")
hideFromDependabot(":instrumentation:hikaricp-3.0:testing")
hideFromDependabot(":instrumentation:http-url-connection:javaagent")
hideFromDependabot(":instrumentation:hystrix-1.4:javaagent")
hideFromDependabot(":instrumentation:java-http-client:javaagent")
hideFromDependabot(":instrumentation:java-http-client:library")
hideFromDependabot(":instrumentation:java-http-client:testing")
hideFromDependabot(":instrumentation:java-util-logging:javaagent")
hideFromDependabot(":instrumentation:java-util-logging:shaded-stub-for-instrumenting")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-common:bootstrap")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-common:javaagent")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-common:testing")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-1.0:javaagent")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-annotations:javaagent")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-arquillian-testing")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-common:javaagent")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-cxf-3.2:javaagent")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-jersey-2.0:javaagent")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-payara-testing")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-resteasy-3.0:javaagent")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-resteasy-3.1:javaagent")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-resteasy-common:javaagent")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-common:testing")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-tomee-testing")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-wildfly-testing")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-3.0:jaxrs-3.0-annotations:javaagent")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-3.0:jaxrs-3.0-common:javaagent")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-3.0:jaxrs-3.0-common:testing")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-3.0:jaxrs-3.0-jersey-3.0:javaagent")
hideFromDependabot(":instrumentation:jaxrs:jaxrs-3.0:jaxrs-3.0-resteasy-6.0:javaagent")
hideFromDependabot(":instrumentation:jaxrs-client:jaxrs-client-1.1:javaagent")
hideFromDependabot(":instrumentation:jaxrs-client:jaxrs-client-2.0-testing")
hideFromDependabot(":instrumentation:jaxws:jaxws-2.0:javaagent")
hideFromDependabot(":instrumentation:jaxws:jaxws-2.0-arquillian-testing")
hideFromDependabot(":instrumentation:jaxws:jaxws-2.0-axis2-1.6:javaagent")
hideFromDependabot(":instrumentation:jaxws:jaxws-2.0-cxf-3.0:javaagent")
hideFromDependabot(":instrumentation:jaxws:jaxws-2.0-cxf-3.0:javaagent-unit-tests")
hideFromDependabot(":instrumentation:jaxws:jaxws-2.0-metro-2.2:javaagent")
hideFromDependabot(":instrumentation:jaxws:jaxws-2.0-common-testing")
hideFromDependabot(":instrumentation:jaxws:jaxws-2.0-tomee-testing")
hideFromDependabot(":instrumentation:jaxws:jaxws-2.0-wildfly-testing")
hideFromDependabot(":instrumentation:jaxws:jaxws-common:javaagent")
hideFromDependabot(":instrumentation:jaxws:jaxws-jws-api-1.1:javaagent")
hideFromDependabot(":instrumentation:jboss-logmanager:jboss-logmanager-appender-1.1:javaagent")
hideFromDependabot(":instrumentation:jboss-logmanager:jboss-logmanager-mdc-1.1:javaagent")
hideFromDependabot(":instrumentation:jdbc:bootstrap")
hideFromDependabot(":instrumentation:jdbc:javaagent")
hideFromDependabot(":instrumentation:jdbc:library")
hideFromDependabot(":instrumentation:jdbc:testing")
hideFromDependabot(":instrumentation:jedis:jedis-1.4:javaagent")
hideFromDependabot(":instrumentation:jedis:jedis-3.0:javaagent")
hideFromDependabot(":instrumentation:jedis:jedis-4.0:javaagent")
hideFromDependabot(":instrumentation:jedis:jedis-common:javaagent")
hideFromDependabot(":instrumentation:jetty:jetty-8.0:javaagent")
hideFromDependabot(":instrumentation:jetty:jetty-11.0:javaagent")
hideFromDependabot(":instrumentation:jetty:jetty-common:javaagent")
hideFromDependabot(":instrumentation:jetty-httpclient:jetty-httpclient-9.2:javaagent")
hideFromDependabot(":instrumentation:jetty-httpclient:jetty-httpclient-9.2:library")
hideFromDependabot(":instrumentation:jetty-httpclient:jetty-httpclient-9.2:testing")
hideFromDependabot(":instrumentation:jms:jms-1.1:javaagent")
hideFromDependabot(":instrumentation:jms:jms-3.0:javaagent")
hideFromDependabot(":instrumentation:jms:jms-common:javaagent")
hideFromDependabot(":instrumentation:jms:jms-common:javaagent-unit-tests")
hideFromDependabot(":instrumentation:jmx-metrics:javaagent")
hideFromDependabot(":instrumentation:jmx-metrics:library")
hideFromDependabot(":instrumentation:jodd-http-4.2:javaagent")
hideFromDependabot(":instrumentation:jsf:jsf-javax-common:javaagent")
hideFromDependabot(":instrumentation:jsf:jsf-javax-common:testing")
hideFromDependabot(":instrumentation:jsf:jsf-jakarta-common:javaagent")
hideFromDependabot(":instrumentation:jsf:jsf-jakarta-common:testing")
hideFromDependabot(":instrumentation:jsf:jsf-mojarra-1.2:javaagent")
hideFromDependabot(":instrumentation:jsf:jsf-mojarra-3.0:javaagent")
hideFromDependabot(":instrumentation:jsf:jsf-myfaces-1.2:javaagent")
hideFromDependabot(":instrumentation:jsf:jsf-myfaces-3.0:javaagent")
hideFromDependabot(":instrumentation:jsp-2.3:javaagent")
hideFromDependabot(":instrumentation:kafka:kafka-clients:kafka-clients-0.11:bootstrap")
hideFromDependabot(":instrumentation:kafka:kafka-clients:kafka-clients-0.11:javaagent")
hideFromDependabot(":instrumentation:kafka:kafka-clients:kafka-clients-0.11:testing")
hideFromDependabot(":instrumentation:kafka:kafka-clients:kafka-clients-2.6:library")
hideFromDependabot(":instrumentation:kafka:kafka-clients:kafka-clients-common:library")
hideFromDependabot(":instrumentation:kafka:kafka-streams-0.11:javaagent")
hideFromDependabot(":instrumentation:kotlinx-coroutines:javaagent")
hideFromDependabot(":instrumentation:ktor:ktor-1.0:library")
hideFromDependabot(":instrumentation:ktor:ktor-2.0:library")
hideFromDependabot(":instrumentation:ktor:ktor-common:library")
hideFromDependabot(":instrumentation:kubernetes-client-7.0:javaagent")
hideFromDependabot(":instrumentation:kubernetes-client-7.0:javaagent-unit-tests")
hideFromDependabot(":instrumentation:lettuce:lettuce-common:library")
hideFromDependabot(":instrumentation:lettuce:lettuce-4.0:javaagent")
hideFromDependabot(":instrumentation:lettuce:lettuce-5.0:javaagent")
hideFromDependabot(":instrumentation:lettuce:lettuce-5.1:javaagent")
hideFromDependabot(":instrumentation:lettuce:lettuce-5.1:library")
hideFromDependabot(":instrumentation:lettuce:lettuce-5.1:testing")
hideFromDependabot(":instrumentation:liberty:compile-stub")
hideFromDependabot(":instrumentation:liberty:liberty-20.0:javaagent")
hideFromDependabot(":instrumentation:liberty:liberty-dispatcher-20.0:javaagent")
hideFromDependabot(":instrumentation:log4j:log4j-appender-1.2:javaagent")
hideFromDependabot(":instrumentation:log4j:log4j-mdc-1.2:javaagent")
hideFromDependabot(":instrumentation:log4j:log4j-context-data:log4j-context-data-2.7:javaagent")
hideFromDependabot(":instrumentation:log4j:log4j-context-data:log4j-context-data-2.17:javaagent")
hideFromDependabot(":instrumentation:log4j:log4j-context-data:log4j-context-data-2.17:library-autoconfigure")
hideFromDependabot(":instrumentation:log4j:log4j-context-data:log4j-context-data-common:testing")
hideFromDependabot(":instrumentation:log4j:log4j-appender-2.17:javaagent")
hideFromDependabot(":instrumentation:log4j:log4j-appender-2.17:library")
hideFromDependabot(":instrumentation:logback:logback-appender-1.0:javaagent")
hideFromDependabot(":instrumentation:logback:logback-appender-1.0:library")
hideFromDependabot(":instrumentation:logback:logback-mdc-1.0:javaagent")
hideFromDependabot(":instrumentation:logback:logback-mdc-1.0:library")
hideFromDependabot(":instrumentation:logback:logback-mdc-1.0:testing")
hideFromDependabot(":instrumentation:methods:javaagent")
hideFromDependabot(":instrumentation:micrometer:micrometer-1.5:javaagent")
hideFromDependabot(":instrumentation:micrometer:micrometer-1.5:library")
hideFromDependabot(":instrumentation:micrometer:micrometer-1.5:testing")
hideFromDependabot(":instrumentation:mongo:mongo-3.1:javaagent")
hideFromDependabot(":instrumentation:mongo:mongo-3.1:library")
hideFromDependabot(":instrumentation:mongo:mongo-3.1:testing")
hideFromDependabot(":instrumentation:mongo:mongo-3.7:javaagent")
hideFromDependabot(":instrumentation:mongo:mongo-4.0:javaagent")
hideFromDependabot(":instrumentation:mongo:mongo-async-3.3:javaagent")
hideFromDependabot(":instrumentation:mongo:mongo-common:testing")
hideFromDependabot(":instrumentation:netty:netty-3.8:javaagent")
hideFromDependabot(":instrumentation:netty:netty-4.0:javaagent")
hideFromDependabot(":instrumentation:netty:netty-4.1:javaagent")
hideFromDependabot(":instrumentation:netty:netty-4.1:library")
hideFromDependabot(":instrumentation:netty:netty-4.1:testing")
hideFromDependabot(":instrumentation:netty:netty-4-common:javaagent")
hideFromDependabot(":instrumentation:netty:netty-4-common:library")
hideFromDependabot(":instrumentation:netty:netty-common:library")
hideFromDependabot(":instrumentation:okhttp:okhttp-2.2:javaagent")
hideFromDependabot(":instrumentation:okhttp:okhttp-3.0:javaagent")
hideFromDependabot(":instrumentation:okhttp:okhttp-3.0:library")
hideFromDependabot(":instrumentation:okhttp:okhttp-3.0:testing")
hideFromDependabot(":instrumentation:opencensus-shim:testing")
hideFromDependabot(":instrumentation:opentelemetry-api:opentelemetry-api-1.0:javaagent")
hideFromDependabot(":instrumentation:opentelemetry-api:opentelemetry-api-1.4:javaagent")
hideFromDependabot(":instrumentation:opentelemetry-api:opentelemetry-api-1.10:javaagent")
hideFromDependabot(":instrumentation:opentelemetry-api:opentelemetry-api-1.15:javaagent")
hideFromDependabot(":instrumentation:opentelemetry-api:opentelemetry-api-logs-1.23:javaagent")
hideFromDependabot(":instrumentation:opentelemetry-extension-annotations-1.0:javaagent")
hideFromDependabot(":instrumentation:opentelemetry-extension-kotlin-1.0:javaagent")
hideFromDependabot(":instrumentation:opentelemetry-instrumentation-annotations-1.16:javaagent")
hideFromDependabot(":instrumentation:opentelemetry-instrumentation-api:javaagent")
hideFromDependabot(":instrumentation:opentelemetry-instrumentation-api:testing")
hideFromDependabot(":instrumentation:oracle-ucp-11.2:javaagent")
hideFromDependabot(":instrumentation:oracle-ucp-11.2:library")
hideFromDependabot(":instrumentation:oracle-ucp-11.2:testing")
hideFromDependabot(":instrumentation:oshi:javaagent")
hideFromDependabot(":instrumentation:oshi:library")
hideFromDependabot(":instrumentation:oshi:testing")
hideFromDependabot(":instrumentation:play:play-mvc:play-mvc-2.4:javaagent")
hideFromDependabot(":instrumentation:play:play-mvc:play-mvc-2.6:javaagent")
hideFromDependabot(":instrumentation:play:play-ws:play-ws-1.0:javaagent")
hideFromDependabot(":instrumentation:play:play-ws:play-ws-2.0:javaagent")
hideFromDependabot(":instrumentation:play:play-ws:play-ws-2.1:javaagent")
hideFromDependabot(":instrumentation:play:play-ws:play-ws-common:javaagent")
hideFromDependabot(":instrumentation:play:play-ws:play-ws-common:testing")
hideFromDependabot(":instrumentation:pulsar:pulsar-2.8:javaagent")
hideFromDependabot(":instrumentation:pulsar:pulsar-2.8:javaagent-unit-tests")
hideFromDependabot(":instrumentation:quartz-2.0:javaagent")
hideFromDependabot(":instrumentation:quartz-2.0:library")
hideFromDependabot(":instrumentation:quartz-2.0:testing")
hideFromDependabot(":instrumentation:r2dbc-1.0:javaagent")
hideFromDependabot(":instrumentation:r2dbc-1.0:library")
hideFromDependabot(":instrumentation:r2dbc-1.0:library-instrumentation-shaded")
hideFromDependabot(":instrumentation:r2dbc-1.0:testing")
hideFromDependabot(":instrumentation:rabbitmq-2.7:javaagent")
hideFromDependabot(":instrumentation:ratpack:ratpack-1.4:javaagent")
hideFromDependabot(":instrumentation:ratpack:ratpack-1.4:testing")
hideFromDependabot(":instrumentation:ratpack:ratpack-1.7:library")
hideFromDependabot(":instrumentation:reactor:reactor-3.1:javaagent")
hideFromDependabot(":instrumentation:reactor:reactor-3.1:library")
hideFromDependabot(":instrumentation:reactor:reactor-3.1:testing")
hideFromDependabot(":instrumentation:reactor:reactor-netty:reactor-netty-0.9:javaagent")
hideFromDependabot(":instrumentation:reactor:reactor-netty:reactor-netty-1.0:javaagent")
hideFromDependabot(":instrumentation:reactor:reactor-netty:reactor-netty-1.0:javaagent-unit-tests")
hideFromDependabot(":instrumentation:rediscala-1.8:javaagent")
hideFromDependabot(":instrumentation:redisson:redisson-3.0:javaagent")
hideFromDependabot(":instrumentation:redisson:redisson-3.17:javaagent")
hideFromDependabot(":instrumentation:redisson:redisson-common:javaagent")
hideFromDependabot(":instrumentation:redisson:redisson-common:testing")
hideFromDependabot(":instrumentation:resources:library")
hideFromDependabot(":instrumentation:restlet:restlet-1.1:javaagent")
hideFromDependabot(":instrumentation:restlet:restlet-1.1:library")
hideFromDependabot(":instrumentation:restlet:restlet-1.1:testing")
hideFromDependabot(":instrumentation:restlet:restlet-2.0:javaagent")
hideFromDependabot(":instrumentation:restlet:restlet-2.0:library")
hideFromDependabot(":instrumentation:restlet:restlet-2.0:testing")
hideFromDependabot(":instrumentation:rmi:bootstrap")
hideFromDependabot(":instrumentation:rmi:javaagent")
hideFromDependabot(":instrumentation:rocketmq:rocketmq-client:rocketmq-client-4.8:javaagent")
hideFromDependabot(":instrumentation:rocketmq:rocketmq-client:rocketmq-client-4.8:library")
hideFromDependabot(":instrumentation:rocketmq:rocketmq-client:rocketmq-client-4.8:testing")
hideFromDependabot(":instrumentation:rocketmq:rocketmq-client:rocketmq-client-5.0:javaagent")
hideFromDependabot(":instrumentation:rocketmq:rocketmq-client:rocketmq-client-5.0:testing")
hideFromDependabot(":instrumentation:runtime-metrics:javaagent")
hideFromDependabot(":instrumentation:runtime-metrics:library")
hideFromDependabot(":instrumentation:runtime-telemetry-jfr:javaagent")
hideFromDependabot(":instrumentation:runtime-telemetry-jfr:library")
hideFromDependabot(":instrumentation:rxjava:rxjava-1.0:library")
hideFromDependabot(":instrumentation:rxjava:rxjava-2.0:library")
hideFromDependabot(":instrumentation:rxjava:rxjava-2.0:testing")
hideFromDependabot(":instrumentation:rxjava:rxjava-2.0:javaagent")
hideFromDependabot(":instrumentation:rxjava:rxjava-3.0:library")
hideFromDependabot(":instrumentation:rxjava:rxjava-3.0:javaagent")
hideFromDependabot(":instrumentation:rxjava:rxjava-3.1.1:library")
hideFromDependabot(":instrumentation:rxjava:rxjava-3.1.1:javaagent")
hideFromDependabot(":instrumentation:rxjava:rxjava-3-common:library")
hideFromDependabot(":instrumentation:rxjava:rxjava-3-common:testing")
hideFromDependabot(":instrumentation:scala-fork-join-2.8:javaagent")
hideFromDependabot(":instrumentation:servlet:servlet-common:bootstrap")
hideFromDependabot(":instrumentation:servlet:servlet-common:javaagent")
hideFromDependabot(":instrumentation:servlet:servlet-javax-common:javaagent")
hideFromDependabot(":instrumentation:servlet:servlet-2.2:javaagent")
hideFromDependabot(":instrumentation:servlet:servlet-3.0:javaagent")
hideFromDependabot(":instrumentation:servlet:servlet-3.0:javaagent-unit-tests")
hideFromDependabot(":instrumentation:servlet:servlet-5.0:javaagent")
hideFromDependabot(":instrumentation:spark-2.3:javaagent")
hideFromDependabot(":instrumentation:spring:spring-batch-3.0:javaagent")
hideFromDependabot(":instrumentation:spring:spring-boot-actuator-autoconfigure-2.0:javaagent")
hideFromDependabot(":instrumentation:spring:spring-boot-resources:library")
hideFromDependabot(":instrumentation:spring:spring-boot-resources:testing")
hideFromDependabot(":instrumentation:spring:spring-core-2.0:javaagent")
hideFromDependabot(":instrumentation:spring:spring-data:spring-data-1.8:javaagent")
hideFromDependabot(":instrumentation:spring:spring-data:spring-data-3.0:testing")
hideFromDependabot(":instrumentation:spring:spring-data:spring-data-common:testing")
hideFromDependabot(":instrumentation:spring:spring-integration-4.1:javaagent")
hideFromDependabot(":instrumentation:spring:spring-integration-4.1:library")
hideFromDependabot(":instrumentation:spring:spring-integration-4.1:testing")
hideFromDependabot(":instrumentation:spring:spring-jms:spring-jms-2.0:javaagent")
hideFromDependabot(":instrumentation:spring:spring-jms:spring-jms-6.0:javaagent")
hideFromDependabot(":instrumentation:spring:spring-kafka-2.7:javaagent")
hideFromDependabot(":instrumentation:spring:spring-kafka-2.7:library")
hideFromDependabot(":instrumentation:spring:spring-kafka-2.7:testing")
hideFromDependabot(":instrumentation:spring:spring-rabbit-1.0:javaagent")
hideFromDependabot(":instrumentation:spring:spring-rmi-4.0:javaagent")
hideFromDependabot(":instrumentation:spring:spring-scheduling-3.1:bootstrap")
hideFromDependabot(":instrumentation:spring:spring-scheduling-3.1:javaagent")
hideFromDependabot(":instrumentation:spring:spring-web:spring-web-3.1:javaagent")
hideFromDependabot(":instrumentation:spring:spring-web:spring-web-3.1:library")
hideFromDependabot(":instrumentation:spring:spring-web:spring-web-3.1:testing")
hideFromDependabot(":instrumentation:spring:spring-web:spring-web-6.0:javaagent")
hideFromDependabot(":instrumentation:spring:spring-webmvc:spring-webmvc-3.1:javaagent")
hideFromDependabot(":instrumentation:spring:spring-webmvc:spring-webmvc-3.1:wildfly-testing")
hideFromDependabot(":instrumentation:spring:spring-webmvc:spring-webmvc-5.3:library")
hideFromDependabot(":instrumentation:spring:spring-webmvc:spring-webmvc-6.0:javaagent")
hideFromDependabot(":instrumentation:spring:spring-webmvc:spring-webmvc-6.0:library")
hideFromDependabot(":instrumentation:spring:spring-webmvc:spring-webmvc-common:javaagent")
hideFromDependabot(":instrumentation:spring:spring-webmvc:spring-webmvc-common:testing")
hideFromDependabot(":instrumentation:spring:spring-webflux:spring-webflux-5.0:javaagent")
hideFromDependabot(":instrumentation:spring:spring-webflux:spring-webflux-5.3:testing")
hideFromDependabot(":instrumentation:spring:spring-webflux:spring-webflux-5.3:library")
hideFromDependabot(":instrumentation:spring:spring-ws-2.0:javaagent")
hideFromDependabot(":instrumentation:spring:spring-boot-autoconfigure")
hideFromDependabot(":instrumentation:spring:starters:spring-boot-starter")
hideFromDependabot(":instrumentation:spring:starters:jaeger-spring-boot-starter")
hideFromDependabot(":instrumentation:spring:starters:zipkin-spring-boot-starter")
hideFromDependabot(":instrumentation:spymemcached-2.12:javaagent")
hideFromDependabot(":instrumentation:struts-2.3:javaagent")
hideFromDependabot(":instrumentation:tapestry-5.4:javaagent")
hideFromDependabot(":instrumentation:thrift-0.14.1:javaagent")
hideFromDependabot(":instrumentation:thrift-0.14.1:testing")
hideFromDependabot(":instrumentation:tomcat:tomcat-7.0:javaagent")
hideFromDependabot(":instrumentation:tomcat:tomcat-10.0:javaagent")
hideFromDependabot(":instrumentation:tomcat:tomcat-common:javaagent")
hideFromDependabot(":instrumentation:tomcat:tomcat-jdbc")
hideFromDependabot(":instrumentation:twilio-6.6:javaagent")
hideFromDependabot(":instrumentation:undertow-1.4:bootstrap")
hideFromDependabot(":instrumentation:undertow-1.4:javaagent")
hideFromDependabot(":instrumentation:vaadin-14.2:javaagent")
hideFromDependabot(":instrumentation:vaadin-14.2:testing")
hideFromDependabot(":instrumentation:vertx:vertx-http-client:vertx-http-client-3.0:javaagent")
hideFromDependabot(":instrumentation:vertx:vertx-http-client:vertx-http-client-4.0:javaagent")
hideFromDependabot(":instrumentation:vertx:vertx-http-client:vertx-http-client-common:javaagent")
hideFromDependabot(":instrumentation:vertx:vertx-kafka-client-3.6:javaagent")
hideFromDependabot(":instrumentation:vertx:vertx-kafka-client-3.6:testing")
hideFromDependabot(":instrumentation:vertx:vertx-rx-java-3.5:javaagent")
hideFromDependabot(":instrumentation:vertx:vertx-sql-client-4.0:javaagent")
hideFromDependabot(":instrumentation:vertx:vertx-web-3.0:javaagent")
hideFromDependabot(":instrumentation:vertx:vertx-web-3.0:testing")
hideFromDependabot(":instrumentation:vibur-dbcp-11.0:javaagent")
hideFromDependabot(":instrumentation:vibur-dbcp-11.0:library")
hideFromDependabot(":instrumentation:vibur-dbcp-11.0:testing")
hideFromDependabot(":instrumentation:wicket-8.0:javaagent")
hideFromDependabot(":instrumentation:zio:zio-2.0:javaagent")

// benchmark
include(":benchmark-overhead-jmh")
include(":benchmark-jfr-analyzer")

// this effectively hides the submodule from dependabot because dependabot only regex parses gradle
// files looking for certain patterns
fun hideFromDependabot(projectPath: String) {
  include(projectPath)
}
