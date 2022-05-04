pluginManagement {
  plugins {
    id("com.bmuschko.docker-remote-api") version "7.3.0"
    id("com.github.ben-manes.versions") version "0.42.0"
    id("com.github.jk1.dependency-license-report") version "2.1"
    id("com.google.cloud.tools.jib") version "3.2.1"
    id("com.gradle.plugin-publish") version "1.0.0-rc-1"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("org.jetbrains.kotlin.jvm") version "1.6.20"
    id("org.unbroken-dome.test-sets") version "4.0.0"
    id("org.xbib.gradle.plugin.jflex") version "1.6.0"
  }
}

plugins {
  id("com.gradle.enterprise") version "3.9"
  id("com.github.burrunan.s3-build-cache") version "1.3"
  id("com.gradle.common-custom-user-data-gradle-plugin") version "1.6.5"
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
include(":opentelemetry-instrumentation-api-shaded-for-instrumenting")
include(":javaagent-bootstrap")
include(":javaagent-extension-api")
include(":javaagent-tooling")
include(":javaagent-tooling:javaagent-tooling-java9")
include(":javaagent")

include(":bom-alpha")
include(":instrumentation-api")
include(":instrumentation-api-semconv")
include(":instrumentation-appender-api-internal")
include(":instrumentation-appender-sdk-internal")
include(":instrumentation-api-annotation-support")

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
include(":smoke-tests:images:servlet")
include(":smoke-tests:images:servlet:servlet-3.0")
include(":smoke-tests:images:servlet:servlet-5.0")
include(":smoke-tests:images:spring-boot")

include(":instrumentation:akka:akka-actor-2.5:javaagent")
include(":instrumentation:akka:akka-actor-fork-join-2.5:javaagent")
include(":instrumentation:akka:akka-http-10.0:javaagent")
include(":instrumentation:apache-camel-2.20:javaagent")
include(":instrumentation:apache-camel-2.20:javaagent-unit-tests")
include(":instrumentation:apache-dubbo-2.7:javaagent")
include(":instrumentation:apache-dubbo-2.7:library-autoconfigure")
include(":instrumentation:apache-dubbo-2.7:testing")
include(":instrumentation:apache-httpasyncclient-4.1:javaagent")
include(":instrumentation:apache-httpclient:apache-httpclient-2.0:javaagent")
include(":instrumentation:apache-httpclient:apache-httpclient-4.0:javaagent")
include(":instrumentation:apache-httpclient:apache-httpclient-4.3:library")
include(":instrumentation:apache-httpclient:apache-httpclient-4.3:testing")
include(":instrumentation:apache-httpclient:apache-httpclient-5.0:javaagent")
include(":instrumentation:armeria-1.3:javaagent")
include(":instrumentation:armeria-1.3:library")
include(":instrumentation:armeria-1.3:testing")
include(":instrumentation:async-http-client:async-http-client-1.9:javaagent")
include(":instrumentation:async-http-client:async-http-client-2.0:javaagent")
include(":instrumentation:aws-lambda:aws-lambda-core-1.0:javaagent")
include(":instrumentation:aws-lambda:aws-lambda-core-1.0:library")
include(":instrumentation:aws-lambda:aws-lambda-core-1.0:testing")
include(":instrumentation:aws-lambda:aws-lambda-events-2.2:javaagent")
include(":instrumentation:aws-lambda:aws-lambda-events-2.2:library")
include(":instrumentation:aws-lambda:aws-lambda-events-2.2:testing")
include(":instrumentation:aws-sdk:aws-sdk-1.11:javaagent")
include(":instrumentation:aws-sdk:aws-sdk-1.11:library")
include(":instrumentation:aws-sdk:aws-sdk-1.11:library-autoconfigure")
include(":instrumentation:aws-sdk:aws-sdk-1.11:testing")
include(":instrumentation:aws-sdk:aws-sdk-2.2:javaagent")
include(":instrumentation:aws-sdk:aws-sdk-2.2:library")
include(":instrumentation:aws-sdk:aws-sdk-2.2:library-autoconfigure")
include(":instrumentation:aws-sdk:aws-sdk-2.2:testing")
include(":instrumentation:azure-core:azure-core-1.14:javaagent")
include(":instrumentation:azure-core:azure-core-1.14:library-instrumentation-shaded")
include(":instrumentation:azure-core:azure-core-1.19:javaagent")
include(":instrumentation:azure-core:azure-core-1.19:library-instrumentation-shaded")
include(":instrumentation:cassandra:cassandra-3.0:javaagent")
include(":instrumentation:cassandra:cassandra-4.0:javaagent")
include(":instrumentation:cdi-testing")
include(":instrumentation:graphql-java-12.0:javaagent")
include(":instrumentation:graphql-java-12.0:library")
include(":instrumentation:graphql-java-12.0:testing")
include(":instrumentation:internal:internal-class-loader:javaagent")
include(":instrumentation:internal:internal-class-loader:javaagent-integration-tests")
include(":instrumentation:internal:internal-eclipse-osgi-3.6:javaagent")
include(":instrumentation:internal:internal-lambda:javaagent")
include(":instrumentation:internal:internal-lambda-java9:javaagent")
include(":instrumentation:internal:internal-reflection:javaagent")
include(":instrumentation:internal:internal-reflection:javaagent-integration-tests")
include(":instrumentation:internal:internal-url-class-loader:javaagent")
include(":instrumentation:internal:internal-url-class-loader:javaagent-integration-tests")
include(":instrumentation:couchbase:couchbase-2.0:javaagent")
include(":instrumentation:couchbase:couchbase-2.6:javaagent")
include(":instrumentation:couchbase:couchbase-2-common:javaagent")
include(":instrumentation:couchbase:couchbase-2-common:javaagent-unit-tests")
include(":instrumentation:couchbase:couchbase-3.1:javaagent")
include(":instrumentation:couchbase:couchbase-3.1:tracing-opentelemetry-shaded")
include(":instrumentation:couchbase:couchbase-3.1.6:javaagent")
include(":instrumentation:couchbase:couchbase-3.1.6:tracing-opentelemetry-shaded")
include(":instrumentation:couchbase:couchbase-3.2:javaagent")
include(":instrumentation:couchbase:couchbase-3.2:tracing-opentelemetry-shaded")
include(":instrumentation:couchbase:couchbase-common:testing")
include(":instrumentation:dropwizard:dropwizard-views-0.7:javaagent")
include(":instrumentation:dropwizard:dropwizard-testing")
include(":instrumentation:elasticsearch:elasticsearch-rest-common:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-rest-5.0:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-rest-6.4:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-rest-7.0:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-transport-common:library")
include(":instrumentation:elasticsearch:elasticsearch-transport-common:testing")
include(":instrumentation:elasticsearch:elasticsearch-transport-5.0:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-transport-5.3:javaagent")
include(":instrumentation:elasticsearch:elasticsearch-transport-6.0:javaagent")
include(":instrumentation:executors:bootstrap")
include(":instrumentation:executors:javaagent")
include(":instrumentation:executors:testing")
include(":instrumentation:external-annotations:javaagent")
include(":instrumentation:external-annotations:javaagent-unit-tests")
include(":instrumentation:finatra-2.9:javaagent")
include(":instrumentation:geode-1.4:javaagent")
include(":instrumentation:google-http-client-1.19:javaagent")
include(":instrumentation:grails-3.0:javaagent")
include(":instrumentation:grizzly-2.0:javaagent")
include(":instrumentation:grpc-1.6:javaagent")
include(":instrumentation:grpc-1.6:library")
include(":instrumentation:grpc-1.6:testing")
include(":instrumentation:guava-10.0:javaagent")
include(":instrumentation:guava-10.0:library")
include(":instrumentation:gwt-2.0:javaagent")
include(":instrumentation:hibernate:hibernate-3.3:javaagent")
include(":instrumentation:hibernate:hibernate-4.0:javaagent")
include(":instrumentation:hibernate:hibernate-common:javaagent")
include(":instrumentation:hibernate:hibernate-procedure-call-4.3:javaagent")
include(":instrumentation:http-url-connection:javaagent")
include(":instrumentation:hystrix-1.4:javaagent")
include(":instrumentation:java-http-client:javaagent")
include(":instrumentation:java-util-logging:javaagent")
include(":instrumentation:java-util-logging:shaded-stub-for-instrumenting")
include(":instrumentation:jaxrs:jaxrs-common:bootstrap")
include(":instrumentation:jaxrs:jaxrs-1.0:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-annotations:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-arquillian-testing")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-common:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-cxf-3.2:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-jersey-2.0:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-payara-testing")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-resteasy-3.0:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-resteasy-3.1:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-resteasy-common:javaagent")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-common:testing")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-tomee-testing")
include(":instrumentation:jaxrs:jaxrs-2.0:jaxrs-2.0-wildfly-testing")
include(":instrumentation:jaxrs-client:jaxrs-client-1.1:javaagent")
include(":instrumentation:jaxrs-client:jaxrs-client-2.0-testing")
include(":instrumentation:jaxws:jaxws-2.0:javaagent")
include(":instrumentation:jaxws:jaxws-2.0-arquillian-testing")
include(":instrumentation:jaxws:jaxws-2.0-axis2-1.6:javaagent")
include(":instrumentation:jaxws:jaxws-2.0-cxf-3.0:javaagent")
include(":instrumentation:jaxws:jaxws-2.0-cxf-3.0:javaagent-unit-tests")
include(":instrumentation:jaxws:jaxws-2.0-metro-2.2:javaagent")
include(":instrumentation:jaxws:jaxws-2.0-common-testing")
include(":instrumentation:jaxws:jaxws-2.0-tomee-testing")
include(":instrumentation:jaxws:jaxws-2.0-wildfly-testing")
include(":instrumentation:jaxws:jaxws-common:library")
include(":instrumentation:jaxws:jaxws-jws-api-1.1:javaagent")
include(":instrumentation:jboss-logmanager-1.1:javaagent")
include(":instrumentation:jboss-logmanager-mdc-1.1:javaagent")
include(":instrumentation:jdbc:javaagent")
include(":instrumentation:jdbc:library")
include(":instrumentation:jdbc:testing")
include(":instrumentation:jedis:jedis-1.4:javaagent")
include(":instrumentation:jedis:jedis-3.0:javaagent")
include(":instrumentation:jedis:jedis-4.0:javaagent")
include(":instrumentation:jedis:jedis-common:javaagent")
include(":instrumentation:jetty:jetty-8.0:javaagent")
include(":instrumentation:jetty:jetty-11.0:javaagent")
include(":instrumentation:jetty:jetty-common:javaagent")
include(":instrumentation:jetty-httpclient:jetty-httpclient-9.2:javaagent")
include(":instrumentation:jetty-httpclient:jetty-httpclient-9.2:library")
include(":instrumentation:jetty-httpclient:jetty-httpclient-9.2:testing")
include(":instrumentation:jms-1.1:javaagent")
include(":instrumentation:jms-1.1:javaagent-unit-tests")
include(":instrumentation:jsf:jsf-common:javaagent")
include(":instrumentation:jsf:jsf-common:testing")
include(":instrumentation:jsf:jsf-mojarra-1.2:javaagent")
include(":instrumentation:jsf:jsf-myfaces-1.2:javaagent")
include(":instrumentation:jsp-2.3:javaagent")
include(":instrumentation:kafka:kafka-clients:kafka-clients-0.11:bootstrap")
include(":instrumentation:kafka:kafka-clients:kafka-clients-0.11:javaagent")
include(":instrumentation:kafka:kafka-clients:kafka-clients-0.11:testing")
include(":instrumentation:kafka:kafka-clients:kafka-clients-2.6:library")
include(":instrumentation:kafka:kafka-clients:kafka-clients-common:library")
include(":instrumentation:kafka:kafka-streams-0.11:javaagent")
include(":instrumentation:kotlinx-coroutines:javaagent")
include(":instrumentation:ktor:ktor-1.0:library")
include(":instrumentation:ktor:ktor-2.0:library")
include(":instrumentation:ktor:ktor-common:library")
include(":instrumentation:kubernetes-client-7.0:javaagent")
include(":instrumentation:kubernetes-client-7.0:javaagent-unit-tests")
include(":instrumentation:lettuce:lettuce-common:library")
include(":instrumentation:lettuce:lettuce-4.0:javaagent")
include(":instrumentation:lettuce:lettuce-5.0:javaagent")
include(":instrumentation:lettuce:lettuce-5.1:javaagent")
include(":instrumentation:lettuce:lettuce-5.1:library")
include(":instrumentation:lettuce:lettuce-5.1:testing")
include(":instrumentation:liberty:compile-stub")
include(":instrumentation:liberty:liberty:javaagent")
include(":instrumentation:liberty:liberty-dispatcher:javaagent")
include(":instrumentation:log4j:log4j-appender-1.2:javaagent")
include(":instrumentation:log4j:log4j-mdc-1.2:javaagent")
include(":instrumentation:log4j:log4j-context-data:log4j-context-data-2.7:javaagent")
include(":instrumentation:log4j:log4j-context-data:log4j-context-data-2.17:javaagent")
include(":instrumentation:log4j:log4j-context-data:log4j-context-data-2.17:library-autoconfigure")
include(":instrumentation:log4j:log4j-context-data:log4j-context-data-2-common:testing")
include(":instrumentation:log4j:log4j-appender-2.17:javaagent")
include(":instrumentation:log4j:log4j-appender-2.17:library")
include(":instrumentation:logback:logback-appender-1.0:javaagent")
include(":instrumentation:logback:logback-appender-1.0:library")
include(":instrumentation:logback:logback-mdc-1.0:javaagent")
include(":instrumentation:logback:logback-mdc-1.0:library")
include(":instrumentation:logback:logback-mdc-1.0:testing")
include(":instrumentation:methods:javaagent")
include(":instrumentation:micrometer:micrometer-1.5:javaagent")
include(":instrumentation:mongo:mongo-3.1:javaagent")
include(":instrumentation:mongo:mongo-3.1:library")
include(":instrumentation:mongo:mongo-3.1:testing")
include(":instrumentation:mongo:mongo-3.7:javaagent")
include(":instrumentation:mongo:mongo-4.0:javaagent")
include(":instrumentation:mongo:mongo-async-3.3:javaagent")
include(":instrumentation:mongo:mongo-common:testing")
include(":instrumentation:netty:netty-3.8:javaagent")
include(":instrumentation:netty:netty-4.0:javaagent")
include(":instrumentation:netty:netty-4.1:javaagent")
include(":instrumentation:netty:netty-4.1-common:javaagent")
include(":instrumentation:netty:netty-4-common:javaagent")
include(":instrumentation:netty:netty-common:javaagent")
include(":instrumentation:okhttp:okhttp-2.2:javaagent")
include(":instrumentation:okhttp:okhttp-3.0:javaagent")
include(":instrumentation:okhttp:okhttp-3.0:library")
include(":instrumentation:okhttp:okhttp-3.0:testing")
include(":instrumentation:opentelemetry-annotations-1.0:javaagent")
include(":instrumentation:opentelemetry-annotations-1.0:testing")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.0:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.4:javaagent")
include(":instrumentation:opentelemetry-api:opentelemetry-api-1.10:javaagent")
include(":instrumentation:opentelemetry-instrumentation-api:javaagent")
include(":instrumentation:opentelemetry-instrumentation-api:testing")
include(":instrumentation:oshi:javaagent")
include(":instrumentation:oshi:library")
include(":instrumentation:oshi:testing")
include(":instrumentation:play:play-2.4:javaagent")
include(":instrumentation:play:play-2.6:javaagent")
include(":instrumentation:play-ws:play-ws-1.0:javaagent")
include(":instrumentation:play-ws:play-ws-2.0:javaagent")
include(":instrumentation:play-ws:play-ws-2.1:javaagent")
include(":instrumentation:play-ws:play-ws-common:javaagent")
include(":instrumentation:play-ws:play-ws-common:testing")
include(":instrumentation:quartz-2.0:javaagent")
include(":instrumentation:quartz-2.0:library")
include(":instrumentation:quartz-2.0:testing")
include(":instrumentation:rabbitmq-2.7:javaagent")
include(":instrumentation:ratpack:ratpack-1.4:javaagent")
include(":instrumentation:ratpack:ratpack-1.4:testing")
include(":instrumentation:ratpack:ratpack-1.7:library")
include(":instrumentation:reactor:reactor-3.1:javaagent")
include(":instrumentation:reactor:reactor-3.1:library")
include(":instrumentation:reactor:reactor-3.1:testing")
include(":instrumentation:reactor:reactor-netty:reactor-netty-0.9:javaagent")
include(":instrumentation:reactor:reactor-netty:reactor-netty-1.0:javaagent")
include(":instrumentation:rediscala-1.8:javaagent")
include(":instrumentation:redisson-3.0:javaagent")
include(":instrumentation:restlet:restlet-1.0:javaagent")
include(":instrumentation:restlet:restlet-1.0:library")
include(":instrumentation:restlet:restlet-1.0:testing")
include(":instrumentation:restlet:restlet-2.0:javaagent")
include(":instrumentation:restlet:restlet-2.0:library")
include(":instrumentation:restlet:restlet-2.0:testing")
include(":instrumentation:rmi:bootstrap")
include(":instrumentation:rmi:javaagent")
include(":instrumentation:rocketmq-client-4.8:javaagent")
include(":instrumentation:rocketmq-client-4.8:library")
include(":instrumentation:rocketmq-client-4.8:testing")
include(":instrumentation:runtime-metrics:javaagent")
include(":instrumentation:runtime-metrics:library")
include(":instrumentation:rxjava:rxjava-1.0:library")
include(":instrumentation:rxjava:rxjava-2.0:library")
include(":instrumentation:rxjava:rxjava-2.0:testing")
include(":instrumentation:rxjava:rxjava-2.0:javaagent")
include(":instrumentation:rxjava:rxjava-3.0:library")
include(":instrumentation:rxjava:rxjava-3.0:javaagent")
include(":instrumentation:rxjava:rxjava-3.1.1:library")
include(":instrumentation:rxjava:rxjava-3.1.1:javaagent")
include(":instrumentation:rxjava:rxjava-3-common:library")
include(":instrumentation:rxjava:rxjava-3-common:testing")
include(":instrumentation:scala-fork-join-2.8:javaagent")
include(":instrumentation:servlet:servlet-common:bootstrap")
include(":instrumentation:servlet:servlet-common:javaagent")
include(":instrumentation:servlet:servlet-javax-common:javaagent")
include(":instrumentation:servlet:servlet-2.2:javaagent")
include(":instrumentation:servlet:servlet-3.0:javaagent")
include(":instrumentation:servlet:servlet-5.0:javaagent")
include(":instrumentation:spark-2.3:javaagent")
include(":instrumentation:spring:spring-batch-3.0:javaagent")
include(":instrumentation:spring:spring-boot-actuator-autoconfigure-2.0:javaagent")
include(":instrumentation:spring:spring-core-2.0:javaagent")
include(":instrumentation:spring:spring-data-1.8:javaagent")
include(":instrumentation:spring:spring-integration-4.1:javaagent")
include(":instrumentation:spring:spring-integration-4.1:library")
include(":instrumentation:spring:spring-integration-4.1:testing")
include(":instrumentation:spring:spring-kafka-2.7:javaagent")
include(":instrumentation:spring:spring-kafka-2.7:testing")
include(":instrumentation:spring:spring-rabbit-1.0:javaagent")
include(":instrumentation:spring:spring-rmi-4.0:javaagent")
include(":instrumentation:spring:spring-scheduling-3.1:javaagent")
include(":instrumentation:spring:spring-web-3.1:javaagent")
include(":instrumentation:spring:spring-web-3.1:library")
include(":instrumentation:spring:spring-web-3.1:testing")
include(":instrumentation:spring:spring-webmvc-3.1:javaagent")
include(":instrumentation:spring:spring-webmvc-3.1:library")
include(":instrumentation:spring:spring-webmvc-3.1:wildfly-testing")
include(":instrumentation:spring:spring-webflux-5.0:javaagent")
include(":instrumentation:spring:spring-webflux-5.0:library")
include(":instrumentation:spring:spring-ws-2.0:javaagent")
include(":instrumentation:spring:spring-boot-autoconfigure")
include(":instrumentation:spring:starters:spring-starter")
include(":instrumentation:spring:starters:jaeger-exporter-starter")
include(":instrumentation:spring:starters:otlp-exporter-starter")
include(":instrumentation:spring:starters:zipkin-exporter-starter")
include(":instrumentation:spymemcached-2.12:javaagent")
include(":instrumentation:struts-2.3:javaagent")
include(":instrumentation:tapestry-5.4:javaagent")
include(":instrumentation:tomcat:tomcat-7.0:javaagent")
include(":instrumentation:tomcat:tomcat-10.0:javaagent")
include(":instrumentation:tomcat:tomcat-common:javaagent")
include(":instrumentation:twilio-6.6:javaagent")
include(":instrumentation:undertow-1.4:bootstrap")
include(":instrumentation:undertow-1.4:javaagent")
include(":instrumentation:vaadin-14.2:javaagent")
include(":instrumentation:vaadin-14.2:testing")
include(":instrumentation:vertx:vertx-http-client:vertx-http-client-3.0:javaagent")
include(":instrumentation:vertx:vertx-http-client:vertx-http-client-4.0:javaagent")
include(":instrumentation:vertx:vertx-http-client:vertx-http-client-common:javaagent")
include(":instrumentation:vertx:vertx-kafka-client-3.5:javaagent")
include(":instrumentation:vertx:vertx-kafka-client-3.5:testing")
include(":instrumentation:vertx:vertx-rx-java-3.5:javaagent")
include(":instrumentation:vertx:vertx-web-3.0:javaagent")
include(":instrumentation:vertx:vertx-web-3.0:testing")
include(":instrumentation:wicket-8.0:javaagent")

// benchmark
include(":benchmark-overhead-jmh")
include(":benchmark-jfr-analyzer")
