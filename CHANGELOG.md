# Changelog

## Unreleased

### Migration notes

- The `java.net.http.HttpClient` instrumentation package
  `io.opentelemetry.instrumentation.httpclient` was deprecated in favor of the new package name
  `io.opentelemetry.instrumentation.javahttpclient`
- The class `io.opentelemetry.instrumentation.netty.v4.common.HttpRequestAndChannel` was
  renamed to `io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel`

## Version 2.13.1 (2025-02-18)

### 🛠️ Bug fixes

- Backport: Fix double instrumentation of Java runtime metrics
  ([#13339](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13339))

## Version 2.13.0 (2025-02-17)

### Migration notes

- `io.opentelemetry.instrumentation.api.incubator.semconv.util.SpanNames` has been deprecated,
  replaced by the stable `io.opentelemetry.instrumentation.api.semconv.util.SpanNames`
- In preparation for stabilizing HTTP library instrumentation, the classes and methods
  that were deprecated in the prior two releases have now been removed
  ([#13135](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13135),
   [#13150](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13150))
- Deprecated Dubbo instrumentation method was removed
  ([#13076](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13076))

### 🌟 New javaagent instrumentation

- `jdk.httpserver` instrumentation
  ([#13243](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13243))

### 🌟 New library instrumentation

- `jdk.httpserver` instrumentation
  ([#13243](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13243))

### 📈 Enhancements

- Add database client metrics to Lettuce instrumentation
  ([#13032](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13032))
- Stabilize io.opentelemetry.instrumentation.api.semconv.util.SpanNames
  ([#12487](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12487))
- Implement ExtendedTextMapGetter in http server instrumentations
  ([#13053](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13053))
- Implement ExtendedTextMapGetter in kafka-clients instrumentation
  ([#13068](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13068))
- Scrub system property secrets from process resource attribute values
  ([#13225](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13225))
- Add database client metrics to AWS SDK 2.x DynamoDB instrumentation
  ([#13283](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13283))
- Add runtime metrics to Spring boot starter
  ([#13173](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13173))

### 🛠️ Bug fixes

- Fix akka shutdown hanging
  ([#13073](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13073))
- Fix MalformedInputException on z/OS
  ([#13042](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13042))
- Fix scope leak in aws sdk instrumentation
  ([#13129](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13129))
- Fix MapConverter does not get initialized when `OTEL_SDK_DISABLED` is set to true
  ([#13224](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13224))
- Fix logback appender on android
  ([#13234](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13234))
- Fix Ktor 3 CallLogging and StatusPages don't have Trace IDs
  ([#13239](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13239))
- Fix Micrometer-bridge breaking Spring Actuator metrics
  ([#13083](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13083))

## Version 2.12.0 (2025-01-17)

### Migration notes

- Some Java agent instrumentation suppression keys have been renamed to match their module names:
  - `elasticsearch-rest-6.0` --> `elasticsearch-rest-6.4`
  - `internal-application-logging` --> `internal-application-logger`
  - `javalin-5` -> `javalin-5.0`
  - `pulsar-2.8.0` -> `pulsar-2.8`
- In preparation for stabilizing HTTP library instrumentation soon:
  - `setCaptured*Headers(List)` methods in `*TelemetryBuilder` classes were changed to
    `setCaptured*Headers(Collection)`
    ([#12901](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12901))
  - `setKnownMethods(Set)` methods in `*TelemetryBuilder` classes were changed to
    `setKnownMethods(Collection)`
    ([#12902](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12902))

### 📈 Enhancements

- Support `ExtendedTextMapGetter` in gRPC instrumentation
  ([#13011](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13011))
- Add database client metrics in DynamoDB instrumentation
  ([#13033](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13033))
- Propagate context into async http client CompletableFuture callbacks
  ([#13041](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13041))
- Exclude spring routing data source from Spring Starter instrumentation
  ([#13054](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13054))
- Instrument jdbc batch queries
  ([#12797](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12797))

### 🛠️ Bug fixes

- Fix incorrect dubbo trace caused by using rpcContext.isProviderSide()
  ([#12930](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12930))
- Fix ClickHouse query failing with syntax error
  ([#13020](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13020))
- Fix instrumentation module not loading silently when duplicate helper classnames are detected
  ([#13005](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13005))
- Fix compatibility problem due to DubboHeadersGetter#keys in Dubbo 2.7.6 and 2.7.7
  ([#12982](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12982))
- Fix appender install for async Logback appenders
  ([#13047](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/13047))

## Version 2.11.0 (2024-12-23)

### Migration notes

In preparation for stabilizing HTTP library instrumentation soon:

- `addAttributeExtractor` methods in a few `*TelemetryBuilder` classes have been deprecated
  and renamed to `addAttributesExtractor` (which is how most of them were named already)
  ([#12860](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12860))
- `setEmitExperimental*` methods in `*TelemetryBuilder` classes have been deprecated and moved
  to internal/experimental classes, see Javadoc `@deprecated` for exact relocation
  ([#12847](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12847))
- `ApacheHttpClient5*` classes have been deprecated and renamed to `ApacheHttpClient*`
  ([#12854](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12854))
- `RatpackTelemetry*` classes have been deprecated and split into `RatpackClientTelemetry*`
  and `RatpackServerTelemetry*`
  ([#12853](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12853))
- `SpringWebfluxTelemetry*` classes have been deprecated and split into
  `SpringWebfluxClientTelemetry*` and `SpringWebfluxServerTelemetry*`
  ([#12852](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12852))
- `ArmeriaTelemetry*` classes have been deprecated and split into `ArmeriaClientTelemetry*`
  and `ArmeriaServerTelemetry*`
  ([#12851](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12851))
- `*KtorClientTracing*` and `*KtorServerTracing*` have been deprecated and renamed to
  `*KtorClientTelemetry*` and `*KtorServerTelemetry*`
  ([#12855](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12855))
- Experimental opt-in attribute `spring-webflux.handler.type` was removed in favor of
  standard `code.*` attributes
  ([#12887](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12887))

### 📈 Enhancements

- Map lettuce 5.1 `db.namespace` to `db.name` (unless using experimental database semconv stability opt-in)
  ([#12609](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12609))
- Log4j2: add option to fill code attributes
  ([#12592](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12592))
- Fill jvm.thread.state attribute for jvm.thread.count metric on jdk8
  ([#12724](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12724))
- Update Spring Scheduling `code.*` attribute extraction for latest release of Spring Scheduling
  ([#12739](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12739))
- Add jctools classes to `reflect-config.json` for better native image support
  ([#12736](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12736))
- Support Pulsar Client send message with transaction
  ([#12731](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12731))
- Implement reading of simple key-value Logstash JSON Marker attributes
  ([#12513](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12513))
- Add agent instrumentation for Ratpack 1.7+
  ([#12572](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12572))
- Added `spring-scheduling.enabled` property to spring-configuration-metadata.json
  ([#12791](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12791))
- Remove class files from spring-boot-autoconfigure source jar
  ([#12798](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12798))
- Updated Camel rules adding route.started, route.added, and thread pools' pool.core_size
  ([#12763](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12763))
- Add database client metrics (when using experimental database semconv stability opt-in)
  ([#12806](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12806),
   [#12818](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12818))
- Add dynamodb instrumenter for aws v1_11 sdk
  ([#12756](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12756))
- Remove public suffixes list from the agent
  ([#10763](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10763))
- Add an option to disable automatic kafka interceptor configuration in spring starter
  ([#12833](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12833))
- Add code attributes to spring webmvc controller spans
  ([#12839](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12839))
- Hibernate 6: don't record error on NoResultException
  ([#12879](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12879))
- Add support for missing spring list properties
  ([#12819](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12819))
- Ktor: support setting custom `spanNameExtractor` (#12842)
  ([#12850](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12850))
- Rename "db.client.connections.usage" to "db.client.connection.count"
  (when using experimental database semconv stability opt-in)
  ([#12886](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12886))
- Support Struts 7.0
  ([#12935](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12935))
- Support latest Ktor release
  ([#12937](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12937))

### 🛠️ Bug fixes

- Logback: don't make MDCPropertyMap of logging event immutable
  ([#12718](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12718))
- Avoid exception when redisson address is null
  ([#12883](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12883))
- Add close to fix CWE-404
  ([#12908](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12908))

## Version 2.10.0 (2024-11-13)

### 🌟 New javaagent instrumentation

- Ktor 3 instrumentation
  ([#12562](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12562))

### 🌟 New library instrumentation

- Ktor 3 instrumentation
  ([#12562](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12562))

### Migration notes

- Spring Boot Starter Scheduling instrumentation scope name changed from
  `io.opentelemetry.spring-scheduling-3.1` to `io.opentelemetry.spring-boot-autoconfigure`
  to reflect the module's name.
- Default flush timeout for aws lambda javaagent instrumentation changed from 1 second
  to 10 seconds to match the flush timeout used in the aws lambda library instrumentation.
  ([#12576](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12576))

### 📈 Enhancements

- Delegate loading of java package to platform loader
  ([#12505](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12505))
- Set up virtual field transforms before otel sdk is initialized
  ([#12444](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12444))
- Update azure-core-tracing-opentelemetry version and improve HTTP suppression to back off
  when Azure SDK tracing is disabled.
  ([#12489](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12489))
- Ktor2 http client uses low level instrumentation
  ([#12530](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12530))
- Add logback mdc instrumentation to spring boot starter
  ([#12515](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12515))
- Run class load listener only once
  ([#12565](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12565))
- Remove duplicate byte buddy classes to reduce agent jar file size
  ([#12571](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12571))
- Support additional JVM arg syntax in service name resource detector
  ([#12544](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12544))

### 🛠️ Bug fixes

- Fix derby directory connection string parser
  ([#12479](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12479))
- Improve whitespace handling in oracle jdbc url parser
  ([#12512](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12512))
- Fix SpanKey bridging for unbridgeable span
  ([#12511](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12511))
- Fix lettuce instrumentation and tests to pass against latest version
  ([#12552](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12552))
- Fix Kafka initialization occasionally failed due to concurrent injection of
  OpenTelemetryMetricsReporter
  ([#12583](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12583))

## Version 2.9.0 (2024-10-17)

### 📈 Enhancements

- Allow JMX Insight reuse for remote connections
  ([#12178](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12178))
- Add opentelemetry-semconv-incubating to bom-alpha
  ([#12266](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12266))
- Bridge more incubating api
  ([#12230](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12230))
- Jetty HttpClient 12: propagate context to all response listeners
  ([#12326](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12326))
- Add Pekko Scheduler context propagation
  ([#12359](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12359))
- Add Akka Scheduler context propagation
  ([#12373](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12373))
- Add instrumentation for spring-cloud-aws SqsListener annotation
  ([#12314](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12314))
- Align SpringConfigProperties with DefaultConfigProperties
  ([#12398](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12398))
- Clear context propagation virtual field
  ([#12397](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12397))
- The opt-in experimental attribute `aws.requestId` was renamed to `aws.request_id`
  (to match the semantic conventions) and it is now emitted by default.
  ([#12352](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12352))
- Ability to set Logback argument capture with a property in Spring Boot Starter
  ([#12442](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12442))
- Support experimental declarative configuration
  ([#12265](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12265))
- Spring Boot Starter: Add auto configuration for spring scheduling instrumentation
  ([#12438](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12438))
- Extract `APIGatewayProxyRequestEvent` headers for context propagation in AWS Lambda
  instrumentation
  ([#12440](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12440))
- Support JMX state metrics
  ([#12369](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12369))
- Allow method instrumentation module to trace methods in boot loader
  ([#12454](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12454))

### 🛠️ Bug fixes

- Fix gc duration metric in runtime-telemetry-java17
  ([#12256](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12256))
- Fix vert.x route containing duplicate segments when RoutingContext.next is used
  ([#12260](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12260))
- Fixes for latest mongo version
  ([#12331](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12331))
- Fix context propagation for ratpack request body stream
  ([#12330](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12330))
- Fix lambda instrumentation to forceFlush logs also
  ([#12341](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12341))
- Can't add custom AttributeExtractor to Apache HttpClient 5 library instrumentation
  ([#12394](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12394))
- Disable logback capture arguments by default
  ([#12445](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12445))
- Add support for missing list properties in spring starter
  ([#12434](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12434))

## Version 2.8.0 (2024-09-13)

### Migration notes

- The unit on the opt-in Java 17 JFR-based metrics was updated from milliseconds to seconds
  to conform with the semantic conventions.
  If you are using the Java agent, this only affects you if you are opting in via
  `otel.instrumentation.runtime-telemetry-java17.enable-all=true`.
  ([#12084](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12084),
  [#12244](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12244))

### 📈 Enhancements

- Update Pulsar instrumentation to work with next Pulsar release
  ([#11648](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11648))
- Capture `network.peer.address` in OkHttp 3.0 instrumentation
  ([#12012](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12012))
- Add support for CXF 4.0 JAX-WS
  ([#12077](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12077))
- Add rules for capturing Apache Camel metrics exposed by JMX MBeans
  ([#11901](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11901))
- Make RocketMQ span status extractor delegate to the default extractor
  ([#12183](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12183))
- Bridge log body any value
  ([#12204](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12204))
- Add declarative config support for resource providers
  ([#12144](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12144))

### 🛠️ Bug fixes

- Fix Javaagent doesn't work with `java.net.spi.InetAddressResolverProvider`
  ([#11987](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11987))
- Fix Oracle UCP 11 metrics not emitted
  ([#12052](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12052))
- Fix wrong database info captured while using Apache ShardingSphere
  ([#12066](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12066))
- Fix RabbitMQ NullPointerException
  ([#12109](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12109))
- Fix possible `NullPointerException` in Play instrumentation
  ([#12121](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12121))
- Fix error span status for successful requests in Ktor
  ([#12161](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12161))
- Make OpenTelemetryHandlerMappingFilter handle exceptions from
  `ServletRequestPathUtils.parseAndCache()`
  ([#12221](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12221))
- Fix tracing CoroutineCrudRepository.findById
  ([#12131](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12131))
- Fix capturing context in log4j library instrumentation with async logger
  ([#12176](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12176))

## Version 1.33.6 (2024-08-26)

### 📈 Enhancements

- Backport: Update the OpenTelemetry SDK version to 1.41.0
  ([#12071](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12071))

## Version 2.7.0 (2024-08-16)

### 📈 Enhancements

- Add span baggage processor
  ([#11697](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11697))
- Improve tomcat version detection
  ([#11936](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11936))
- Improve akka route handling with java dsl
  ([#11926](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11926))
- Ignore Alibaba fastjson ASMClassLoader
  ([#11954](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11954))
- Use `aws-lambda-java-serialization` library, which is available by default, while deserializing
  input and serializing output
  ([#11868](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11868))
- Logback appender: map timestamp in nanoseconds if possible
  ([#11974](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11974))
- Save ILoggingEvent.getArgumentArray() arguments from Logback
  ([#11865](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11865))
- Update Java 17-based metrics to stable semconv
  ([#11914](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11914))
- Add Pulsar Consumer metrics
  ([#11891](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11891))

### 🛠️ Bug fixes

- Fix missing throw statement in RestClientWrapper
  ([#11893](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11893))
- Fix ClickHouse tracing when database name not included in connection string
  ([#11852](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11852))
- Fix class cast exception, noop meter does not implement incubating API
  ([#11934](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11934))
- Closing a kafka producer/consumer should not disable metrics from other consumers/producers
  ([#11975](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11975))
- Fix ending span in Ktor plugin
  ([#11726](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11726))
- Fix netty memory leak
  ([#12003](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/12003))

## Version 1.33.5 (2024-07-25)

### 📈 Enhancements

- Backport: Update the OpenTelemetry SDK version to 1.40.0
  ([#11879](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11879))

## Version 2.6.0 (2024-07-17)

The Spring Boot Starter (`opentelemetry-spring-boot-starter`) is now stable.

### Migration notes

- The `opentelemetry-spring-boot` and `opentelemetry-spring-boot-3` artifacts have been merged
  into a single artifact named `opentelemetry-spring-boot-autoconfigure`
  which supports both Spring Boot 2 and Spring Boot 3
- Two experimental HTTP metrics have been renamed:
  - `http.server.request.size` &rarr; `http.server.request.body.size`,
  - `http.server.response.size` &rarr; `http.server.response.body.size`

### 🌟 New javaagent instrumentation

- Javalin
  ([#11587](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11587))
- ClickHouse
  ([#11660](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11660))

### 📈 Enhancements

- Support HTTP client instrumentation configuration in Spring starter
  ([#11620](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11620))
- Influxdb client: don't fill `db.statement` for create/drop database and write operations
  ([#11557](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11557))
- Support `otel.instrumentation.common.default-enabled` in the Spring starter
  ([#11746](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11746))
- Support Jetty HTTP client 12
  ([#11519](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11519))
- Add Pulsar `messaging.producer.duration` metric
  ([#11591](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11591))
- Improve instrumentation suppression behavior
  ([#11640](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11640))
- Propagate OpenTelemetry context through custom AWS client context for Lambda direct calls
  ([#11675](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11675))
- Spring Native support for `@WithSpan`
  ([#11757](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11757))
- Support HTTP server instrumentation config properties in the Spring starter
  ([#11667](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11667))

### 🛠️ Bug fixes

- Fix `http.server.active_requests` metric with async requests
  ([#11638](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11638))

## Version 1.33.4 (2024-06-19)

### 📈 Enhancements

- Backport: Undertow, run response customizer on all ServerConnection implementations
  ([#11548](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11548))
- Backport: Improve security manager support
  ([#11606](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11606))
- Backport: Update the OpenTelemetry SDK version to 1.39.0
  ([#11603](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11603))

### 🛠️ Bug fixes

- Backport: Avoid NullPointerException when JMS destination is not available
  ([#11577](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11577))
- Backport: Fix Spring Kafka instrumentation closing the trace too early
  ([#11592](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11592))
- Backport: Fix gRPC instrumentation adding duplicates to metadata instead of overwriting
  ([#11604](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11604))
- Backport: Fix request header capture corrupting tomcat request
  ([#11607](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11607))

## Version 2.5.0 (2024-06-17)

### 📈 Enhancements

- Add support for Informix connection string parsing in JDBC instrumentation
  ([#11542](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11542))
- Generate an SBOM for the javaagent artifact
  ([#11075](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11075))
- Extract sql operation even when the sanitizer is disabled
  ([#11472](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11472))
- Improve security manager support
  ([#11466](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11466))
- Generate Log4j2Plugin.dat for OpenTelemetryAppender
  ([#11503](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11503))
- Stop kotlin coroutine dispatcher from propagating context
  ([#11500](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11500))
- Handle Vert.x sub routes
  ([#11535](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11535))
- Undertow: run response customizer on all ServerConnection implementations
  ([#11539](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11539))
- Allow configuring MDC key names for trace_id, span_id, trace_flags
  ([#11329](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11329))
- Apply async end strategy to all kotlin coroutine flows
  ([#11583](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11583))

### 🛠️ Bug fixes

- Fix container.id issue in some crio scenarios
  ([#11382](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11382))
- Fix Finagle http client context propagation
  ([#11400](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11400))
- Fix sporadically failing finagle test
  ([#11441](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11441))
- Fix request header capture corrupting tomcat request
  ([#11469](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11469))
- Fix Ktor server instrumentation when Ktor client library is not present
  ([#11454](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11454))
- Fix gRPC instrumentation adding duplicates to metadata instead of overwriting
  ([#11308](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11308))
- Avoid NullPointerException when JMS destination is not available
  ([#11570](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11570))
- Fix Spring Kafka instrumentation closing the trace too early
  ([#11471](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11471))

## Version 1.33.3 (2024-05-21)

### 📈 Enhancements

- Backport: Fix the logic to get container.id resource attribute
  ([#11333](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11333))
- Backport: Update the OpenTelemetry SDK version to 1.38.0
  ([#11386](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11386))
- Backport: Fix a bug in undertow instrumentation related to HTTP/2
  ([#11387](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11387))
- Backport: Fix container.id issue in some crio scenarios
  ([#11405](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11405))

## Version 2.4.0 (2024-05-18)

### 🌟 New javaagent instrumentation

- InfluxDB
  ([#10850](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10850))
- Armeria gRPC
  ([#11351](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11351))
- Apache ShenYu
  ([#11260](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11260))

### 📈 Enhancements

- Instrument ConnectionSource in Akka/Pekko HTTP Servers
  ([#11103](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11103))
- Use constant span name when using Spring AMQP AnonymousQueues
  ([#11141](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11141))
- Add support for `RestClient` in Spring starter
  ([#11038](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11038))
- Add support for WebFlux server in Spring starter
  ([#11185](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11185))
- Add async operation end strategy for Kotlin coroutines flow
  ([#11168](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11168))
- Add automatic JDBC instrumentation to the Spring starter
  ([#11258](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11258))
- Add `StructuredTaskScope` instrumentation
  ([#11202](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11202))
- Allow reading OTel context from reactor ContextView
  ([#11235](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11235))
- Add spring starter r2dbc support
  ([#11221](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11221))
- Enable instrumentation of Spring EJB clients
  ([#11104](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11104))
- Support `otel.instrumentation.kafka.experimental-span-attributes` in Spring starter
  ([#11263](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11263))
- Remove incubating semconv dependency from library instrumentation
  ([#11324](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11324))
- Add extension functions for Ktor plugins
  ([#10963](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10963))
- Add dedicated flag for R2DBC statement sanitizer
  ([#11384](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11384))
- Allow library instrumentations to override span name
  ([#11355](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11355))
- Don't sanitize PostgreSQL parameter markers
  ([#11388](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11388))
- Make statement sanitizer configurable for Spring Boot
  ([#11350](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11350))

### 🛠️ Bug fixes

- Fix GraphQL instrumentation to work with latest version
  ([#11142](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11142))
- Fix jmx-metrics on WildFly
  ([#11151](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11151))
- End gRPC server span in onComplete instead of close
  ([#11170](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11170))
- Fix a bug in undertow instrumentation related to HTTP/2
  ([#11361](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11361))
- Armeria http client reports wrong protocol version
  ([#11334](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11334))
- Use daemon thread for scheduling in jmx-metrics BeanFinder
  ([#11337](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11337))

## Version 1.33.2 (2024-04-20)

### 📈 Enhancements

- Backport: elasticsearch-java 7.17.20 has native instrumentation
  ([#11098](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11098))
- Update the OpenTelemetry SDK version to 1.37.0
  ([#11118](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11118))
- Backport: graphql-java-22.0 support
  ([#11171](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11171))

## Version 2.3.0 (2024-04-12)

### 📈 Enhancements

- Handle async requests in spring mvc library instrumentation
  ([#10868](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10868))
- Support statement sanitizer enabled flag in lettuce 5.1 instrumentation
  ([#10922](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10922))
- Remove AWS Active Tracing span linking
  ([#10930](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10930))
- Make spring boot honor the standard environment variables for maps
  ([#11000](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11000))
- Pulsar: use span links when receive telemetry is enabled
  ([#10650](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10650))
- Rename `messaging.kafka.destination.partition` to `messaging.destination.partition.id`
  ([#11086](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11086))
- Support `service.instance.id` in spring starter
  ([#11071](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11071))
- Add library instrumentation for RestTemplateBuilder
  ([#11054](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11054))
- Add cloud resource providers in spring starter
  ([#11014](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11014))

### 🛠️ Bug fixes

- Fix disabling virtual thread context propagation
  ([#10881](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10881))
- Fix virtual thread instrumentation for jdk 21 ea versions
  ([#10887](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10887))
- Fix spring kafka interceptor wrappers not delegating some methods
  ([#10935](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10935))
- AWS Lambda Runtime legacy internal handlers need to be ignored from being instrumented and so
  traced …
  ([#10942](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10942))
- Metro: ignore UnsupportedOperationException when updating span name
  ([#10996](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10996))
- Fix jedis plugin for 2.7.2
  ([#10982](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10982))
- Fix idle in druid instrumentation
  ([#11079](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/11079))

## Version 1.33.1 (2024-03-20)

### 📈 Enhancements

- Backport: Capture `http.route` for akka-http
  ([#10777](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10777))
- Update the OpenTelemetry SDK version to 1.36.0
  ([#10866](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10866))

## Version 2.2.0 (2024-03-14)

### Migration notes

- Remove deprecated spring properties
  ([#10454](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10454))

### 🌟 New javaagent instrumentation

- Add cloud resource detectors in javaagent, but keep them disabled by default
  ([#10754](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10754))
- Add support for XXL-JOB
  ([#10421](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10421))

### 📈 Enhancements

- Don't fill network peer for cassandra SniEndPoint
  ([#10573](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10573))
- Spring boot starter: add service.version detection, improve service.name detection
  ([#10457](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10457))
- Always create a JMS consumer span
  ([#10604](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10604))
- Ability to disable the automatic Logback appender addition
  ([#10629](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10629))
- Allow excluding all methods of a class
  ([#10753](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10753))
- Preserve attribute type for logback key value pairs
  ([#10781](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10781))
- Add instrumentation for graphql 20 that does not use deprecated methods
  ([#10779](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10779))
- Capture http.route for pekko-http
  ([#10799](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10799))
- Normalize SQL IN(?, ?, ...) statements to "in(?)" to reduce cardinality of db.statement attribute
  ([#10564](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10564))
- Capture `db.operation` for CREATE/DROP/ALTER SQL statement
  ([#10020](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10020))
- Ignore AWS Lambda Runtime internal handlers
  ([#10736](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10736))
- Spring use SDK autoconfig
  ([#10453](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10453))
- Add manifest resource detector
  ([#10621](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10621))
- Add instrumentation for jetty 12
  ([#10575](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10575))
- add host.id resource provider
  ([#10627](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10627))
- Spring starter includes annotation dependency
  ([#10613](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10613))

### 🛠️ Bug fixes

- Don't fail spring application startup if sdk is disabled
  ([#10602](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10602))
- Fix shading aws propagator
  ([#10669](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10669))
- Disable http and rpc metrics when advice can not be applied
  ([#10671](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10671))
- Fix native tests
  ([#10685](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10685))
- Fix tomcat instrumentation when user includes wrong servlet api
  ([#10757](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10757))
- Override xray trace header instead of appending
  ([#10766](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10766))
- Fix spring boot starter failing without logback
  ([#10802](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10802))
- Fix spring kafka context leak when batch listener is retried
  ([#10741](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10741))
- Fix the logic to get container.id resource attribute
  ([#10737](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10737))
- Configure kafka metrics reporter as class
  ([#10855](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10855))
- Disable context propagation when virtual thread is switched to the carrier thread
  ([#10854](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10854))

## Version 1.33.0 (2024-02-28)

### Migration notes

- The deprecated Jaeger exporter has been removed
  ([#10524](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10524))

### 📈 Enhancements

- Backport: Set route only on the SERVER span
  ([#10580](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10580))
- Update the OpenTelemetry SDK version to 1.35.0
  ([#10524](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10524))

## Version 2.1.0 (2024-02-16)

### Migration notes

- Deprecated config properties have been removed in favor of the new names:
  - `otel.instrumentation.kafka.client-propagation.enabled` ->
    `otel.instrumentation.kafka.producer-propagation.enabled`
  - `otel.instrumentation.netty.always-create-connect-span` ->
    `otel.instrumentation.netty.connection-telemetry.enabled`
  - `otel.instrumentation.http.capture-headers.client.request` ->
    `otel.instrumentation.http.client.capture-request-headers`
  - `otel.instrumentation.http.capture-headers.client.response` ->
    `otel.instrumentation.http.client.capture-response-headers`
  - `otel.instrumentation.http.capture-headers.server.request` ->
    `otel.instrumentation.http.server.capture-request-headers`
  - `otel.instrumentation.http.capture-headers.server.response` ->
    `otel.instrumentation.http.server.capture-response-headers`
  - `otel.instrumentation.http.client.emit-experimental-metrics` ->
    `otel.instrumentation.http.client.emit-experimental-telemetry`
  - `otel.instrumentation.http.server.emit-experimental-metrics` ->
    `otel.instrumentation.http.server.emit-experimental-telemetry`
    ([#10349](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10349))
- The deprecated Jaeger exporter has been removed
  ([#10241](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10241))
- Actuator instrumentation has been disabled by default.
  You can enable using `OTEL_INSTRUMENTATION_SPRING_BOOT_ACTUATOR_AUTOCONFIGURE_ENABLED=true`
  or `-Dotel.instrumentation.spring-boot-actuator-autoconfigure.enabled=true`.
  ([#10394](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10394))
- Spring starter: removed support for the deprecated
  @io.opentelemetry.extension.annotations.WithSpan
  annotation. Use @io.opentelemetry.instrumentation.annotations.WithSpan annotation instead.
  ([#10530](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10530))

### 🌟 New javaagent instrumentation

- MyBatis framework instrumentation
  ([#10258](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10258))
- Finagle instrumentation
  ([#10141](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10141))

### 🌟 New library instrumentation

- Apache HttpClient 5 instrumentation
  ([#10100](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10100))

### 📈 Enhancements

- Spring starter: add distro version resource attribute
  ([#10276](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10276))
- Add context propagation for rector schedulers
  ([#10311](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10311))
- Spring starter: automatic addition of the OTel Logback appender
  ([#10306](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10306))
- Spring starter: add resource detectors
  ([#10277](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10277))
- Allow closing the observables for System and Process metrics gathered by OSHI
  ([#10364](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10364))
- Spring starter: Allow to configure the OTel Logback appender from system properties
  ([#10355](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10355))
- Spring starter: re-use sdk logic for configuring otlp exporters
  ([#10292](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10292))
  - All available spring starter properties (including the new properties) can be found
    [here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/v2.1.0/instrumentation/spring/spring-boot-autoconfigure/src/main/resources/META-INF/additional-spring-configuration-metadata.json)
  - You can also use auto-completion in your IDE to see the available properties in
    `application.properties` or `application.yml`
- Spring starter: add SystemOutLogRecordExporter
  ([#10420](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10420))
- Spring starter: use duration parser of config properties
  ([#10512](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10512))
- Spring starter: support `otel.propagators`
  ([#10408](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10408))
- Set route only on the SERVER span
  ([#10290](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10290))
- Convert Apache HttpClient 4.3 library instrumentation to "low-level" HTTP instrumentation
  ([#10253](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10253))

### 🛠️ Bug fixes

- Fix log replay of the Log4j 2 appender
  ([#10243](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10243))
- Fix Netty addListener instrumentation
  ([#10254](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10254))
- Fix Calling shutdown() multiple times warning in spring starter
  ([#10222](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10222))
- Correctly fix NPE in servlet AsyncListener
  ([#10250](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10250))
- add @ConditionalOnMissingBean to LoggingMetricExporter
  ([#10283](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10283))
- Make Netty Instrumentation HttpServerRequestTracingHandler propagate "Channel Inactive" event
  to downstream according to parent contract
  ([#10303](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10303))
- Improve rediscala instrumentation to address sporadic test failure
  ([#10301](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10301))
- Undertow: restore attached context only when it is for different trace
  ([#10336](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10336))
- Reactor kafka wrapper delegates to wrong method
  ([#10333](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10333))
- Spring starter: add missing LoggingMetricExporterAutoConfiguration to spring factories
  ([#10282](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10282))
- Spring starter: Fix MapConverter does not get initialized if some exporters are turned off
  ([#10346](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10346))
- Update azure-core-tracing-opentelemetry version and fix double-collection for synchronous
  HTTP requests
  ([#10350](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10350))
- Allow OSGI dynamic import for `io.opentelemetry` package when matching
  ([#10385](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10385))
- Use direct peer address in `client.address` when X-Forwarded-For is not present
  ([#10370](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10370))
- Netty: don't expose tracing handler in handlers map
  ([#10410](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10410))
- Wrap request to avoid modifying attributes of the original request
  ([#10389](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10389))
- Fix JarAnalyzer warnings on Payara
  ([#10458](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10458))
- Return wrapped connection from `Statement.getConnection()`
  ([#10554](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10554))
- Spring starter: Fix `otel.propagators`
  ([#10559](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10559))
- Populate `server.address` and `server.port` in Cassandra instrumentation
  ([#10357](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10357))

### 🧰 Tooling

- Allow multiple invokedynamic InstrumentationModules to share classloaders
  ([#10015](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10015))

## Version 1.32.1 (2024-02-02)

### 📈 Enhancements

- Backport: update jackson packages to v2.16.1
  ([#10198](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10198),
  [#10199](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10199))
- Backport: implement forEach support for aws sqs tracing list
  ([#10195](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10195))
- Backport: Bridge metric advice in OpenTelemetry API 1.32
  ([#10026](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10026))
- Update the OpenTelemetry SDK version to 1.34.1
  ([#10320](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10320))

### 🛠️ Bug fixes

- Backport: Handle authority from request when HttpHost is null
  ([#10204](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10204))
- Backport: Null check for nullable response object in aws sdk 1.1 instrumentation
  ([#10029](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10029))
- Backport: Make Netty Instrumentation HttpServerRequestTracingHandler propagate "Channel Inactive"
  event to downstream according to parent contract
  ([#10303](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10303))
- Backport: Fix Netty addListener instrumentation
  ([#10254](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10254))
- Backport: Update azure-core-tracing-opentelemetry version and fix sync suppression
  ([#10350](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10350))

## Version 2.0.0 (2024-01-12)

The 2.0.0 release contains significant breaking changes that will most likely affect all users,
please be sure to read the breaking changes below carefully.

Note: 1.32.x will be security patched for at least 6 months in case some of the changes below are
too disruptive to adopt right away.

### ⚠️⚠️ Breaking changes ⚠️⚠️

- The default OTLP protocol has been changed from `grpc` to `http/protobuf` in order to align with
  the [specification](https://github.com/open-telemetry/opentelemetry-specification/blob/v1.28.0/specification/protocol/exporter.md#specify-protocol).
  You can switch to the `grpc` protocol using `OTEL_EXPORTER_OTLP_PROTOCOL=grpc`
  or `-Dotel.exporter.otlp.protocol=grpc`.
- Micrometer metric bridge has been disabled by default. You can enable it using
  `OTEL_INSTRUMENTATION_MICROMETER_ENABLED=true`
  or `-Dotel.instrumentation.micrometer.enabled=true`.
- The OTLP logs exporter is now enabled by default. You can disable it using
  `OTEL_LOGS_EXPORTER=none` or `-Dotel.logs.exporter=none`.
- Controller spans are now disabled by default. You can enable them using
  `OTEL_INSTRUMENTATION_COMMON_EXPERIMENTAL_CONTROLLER_TELEMETRY_ENABLED=true`
  or `-Dotel.instrumentation.common.experimental.controller-telemetry.enabled=true`.
- View spans are now disabled by default. You can enable them using
  `OTEL_INSTRUMENTATION_COMMON_EXPERIMENTAL_VIEW_TELEMETRY_ENABLED=true`
  or `-Dotel.instrumentation.common.experimental.view-telemetry.enabled=true`.
- ⚠️⚠️ Stable HTTP semantic conventions are now emitted ⚠️⚠️
  - TOO MANY CHANGES TO LIST HERE, be sure to review the full
    [list of changes](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/non-normative/http-migration.md#summary-of-changes).
- Stable JVM semantic conventions are now emitted.
  - Memory metrics
    - `process.runtime.jvm.memory.usage` renamed to `jvm.memory.used`
    - `process.runtime.jvm.memory.committed` renamed to `jvm.memory.committed`
    - `process.runtime.jvm.memory.limit` renamed to `jvm.memory.limit`
    - `process.runtime.jvm.memory.usage_after_last_gc` renamed to `jvm.memory.used_after_last_gc`
    - `process.runtime.jvm.memory.init` renamed to `jvm.memory.init` (still experimental)
    - Metric attributes
      - `type` renamed to `jvm.memory.type`
      - `pool` renamed to `jvm.memory.pool.name`
  - Garbage collection metrics
    - `process.runtime.jvm.gc.duration` renamed to `jvm.gc.duration`
    - Metric attributes
      - `name` renamed to `jvm.gc.name`
      - `action` renamed to `jvm.gc.action`
  - Thread metrics
    - `process.runtime.jvm.threads.count` renamed to `jvm.threads.count`
    - Metric attributes
      - `daemon` renamed to `jvm.thread.daemon`
  - Classes metrics
    - `process.runtime.jvm.classes.loaded` renamed to `jvm.classes.loaded`
    - `process.runtime.jvm.classes.unloaded` renamed to `jvm.classes.unloaded`
    - `process.runtime.jvm.classes.current_loaded` renamed to `jvm.classes.count`
  - CPU metrics
    - `process.runtime.jvm.cpu.utilization` renamed to `jvm.cpu.recent_utilization`
    - `process.runtime.jvm.system.cpu.load_1m` renamed to `jvm.system.cpu.load_1m` (still
      experimental)
    - `process.runtime.jvm.system.cpu.utilization` renamed to `jvm.system.cpu.utilization` (still
      experimental)
  - Buffer metrics
    - `process.runtime.jvm.buffer.limit` renamed to `jvm.buffer.memory.limit` (still experimental)
    - `process.runtime.jvm.buffer.count` renamed to `jvm.buffer.count` (still experimental)
    - `process.runtime.jvm.buffer.usage` renamed to `jvm.buffer.memory.usage` (still experimental)
    - Metric attributes
      - `pool` renamed to `jvm.buffer.pool.name`

### More migration notes

- Lettuce CONNECT spans are now disabled by default. You can enable them using
  `OTEL_INSTRUMENTATION_LETTUCE_CONNECTION_TELEMETRY_ENABLED=true`
  or `-Dotel.instrumentation.lettuce.connection-telemetry.enabled=true`.
- The configuration property
  `otel.instrumentation.log4j-appender.experimental.capture-context-data-attributes` has been
  renamed to `otel.instrumentation.log4j-appender.experimental.capture-mdc-attributes`.
- MDC attribute prefixes (`log4j.mdc.` and `logback.mdc.*`) have been removed.
- The artifact `instrumentation-api-semconv` has been renamed to `instrumentation-api-incubator`.
- HTTP classes have been moved from `instrumentation-api-incubator` to `instrumentation-api`
  and as a result are now stable.

### 🌟 New javaagent instrumentation

- Vert.x redis client
  ([#9838](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9838))

### 📈 Enhancements

- Reduce reactor stack trace depth
  ([#9923](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9923))
- Implement `error.type` in `spring-webflux` and `reactor-netty` instrumentations
  ([#9967](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9967))
- Bridge metric advice in OpenTelemetry API 1.32
  ([#10026](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10026))
- Capture http.route for akka-http
  ([#10039](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10039))
- Rename `telemetry.auto.version` to `telemetry.distro.version` and add `telemetry.distro.name`
  ([#9065](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9065))
- Implement forEach support for aws sqs tracing list
  ([#10062](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10062))
- Add http client response attributes to aws sqs process spans
  ([#10074](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10074))
- Add support for `OTEL_RESOURCE_ATTRIBUTES`, `OTEL_SERVICE_NAME`, `OTEL_EXPORTER_OTLP_HEADERS`,
  and `OTEL_EXPORTER_OTLP_PROTOCOL` for spring boot starter
  ([#9950](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9950))
- Add elasticsearch-api-client as instrumentation name to elasticsearch-api-client-7.16
  ([#10102](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10102))
- Add instrumentation for druid connection pool
  ([#9935](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9935))
- Remove deprecated rocketmq setting
  ([#10125](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10125))
- JMX metrics for Tomcat with 'Tomcat' JMX domain
  ([#10115](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10115))
- Capture the SNS topic ARN under the 'messaging.destination.name' span attribute.
  ([#10096](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10096))
- Add network attributes to rabbitmq process spans
  ([#10210](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10210))
- Add UserExcludedClassloadersConfigurer
  ([#10134](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10134))
- Apply both server attributes & network attributes to Lettuce 5.1
  ([#10197](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10197))

### 🛠️ Bug fixes

- Fix aws propagator presence check in spring boot starter
  ([#9924](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9924))
- Capture authority from apache httpclient request when HttpHost is null
  ([#9990](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9990))
- Fix NoSuchBeanDefinitionException with the JDBC driver configuration in spring boot starter
  ([#9978](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9978))
- Null check for nullable response object in aws sdk 1.1 instrumentation
  ([#10029](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10029))
- Fix using opentelemetry-spring-boot with Java 8 and Gradle
  ([#10066](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10066))
- Fix transforming Java record types
  ([#10052](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10052))
- Fix warnings from the spring boot starter
  ([#10086](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10086))
- Resolve `ParameterNameDiscoverer` Bean Conflict in `spring-boot-autoconfigure`
  ([#10105](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/10105))

## Version 1.32.0 (2023-11-18)

### Migration notes

- Old server/client socket getter methods deprecated
  ([#9716](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9716))

### 📈 Enhancements

- Allow enabling receive telemetry in kafka library instrumentation
  ([#9693](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9693))
- Add JdbcTelemetry and JdbcTelemetryBuilder
  ([#9685](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9685))
- Rename http.resend_count to http.request.resend_count
  ([#9700](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9700))
- Define `url.scheme` in terms of logical operation in HTTP server semconv
  (when opting in to new semconv)
  ([#9698](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9698))
- Generate only consumer span for sqs receive message
  ([#9652](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9652))
- Replace `(client|server).socket.(address|port)` attributes with
  `network.(peer|local).(address|port)`
  (when opting in to new semconv)
  ([#9676](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9676))
- Add capability for invokedynamic InstrumentationModules to inject proxies
  ([#9565](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9565))
- Make `network.transport` and `network.type` opt-in (when opting in to new semconv)
  ([#9719](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9719))
- Factor in `X-Forwarded-Host`/`Forwarded` when capturing `server.address` and `server.port`
  (when opting in to new semconv)
  ([#9721](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9721))
- Move class that should've been internal to internal package
  ([#9725](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9725))
- Only set `server.port` when `server.address` is set (when opting in to new semconv)
  ([#9737](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9737))
- Add messaging conventions to sqs spans
  ([#9712](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9712))
- Make the JDBC driver config work with the OTel starter
  ([#9625](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9625))
- Don't normalize the '-' character in HTTP header names when opting in to new semconv
  ([#9735](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9735))
- Add instrumentation for jaxws metro 3.0+
  ([#9705](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9705))
- Change `user_agent.original` from recommended to opt-in on HTTP client spans
  ([#9776](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9776))
- Change the precedence between `:authority` and `Host` headers
  ([#9774](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9774))
- Move capturing enduser.id attribute behind a flag
  ([#9751](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9751),
  [#9788](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9788))
- Remove conditional requirement on `network.peer.address` and `network.peer.port`
  (when opting in to new semconv)
  ([#9775](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9775))
- Change `client.port` from recommended to opt-in on HTTP server spans
  (when opting in to new semconv)
  ([#9786](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9786))
- Make `url.scheme` opt in for HTTP client metrics and make `server.port` required
  (when opting in to new semconv)
  ([#9784](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9784))
- Change `http.request.body.size` and `http.response.body.size` attributes from recommended to
  opt-in
  (when opting in to new semconv)
  ([#9799](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9799))
- Capture `http.route` in spring-cloud-gateway
  ([#9597](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9597))
- Always set messaging operation
  ([#9791](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9791))
- Change `network.protocol.name` from opt-in to conditionally required
  (when opting in to new semconv)
  ([#9797](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9797))
- Support specifying `spring.application.name` in the `bootstrap.properties`, `bootstrap.yml`
  and `bootstrap.yaml`
  ([#9801](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9801))
- Add process spans to aws-1 sqs instrumentation
  ([#9796](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9796))
- Implement capturing message headers for aws1 sqs spans
  ([#9824](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9824))
- Add process spans to aws2 sqs instrumentation
  ([#9778](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9778))
- Add `service.name` to MDC
  ([#9647](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9647))
- Capture enduser attributes in Spring Security
  ([#9777](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9777))
- Capture message id in aws1 sqs instrumentation
  ([#9841](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9841))
- Implement capturing message headers for aws2 sqs spans
  ([#9842](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9842))
- Move kafka metrics to separate instrumentation module
  ([#9862](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9862))
- Capture logback logger context properties
  ([#9553](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9553))
- Stable JVM semconv implementation: classes
  ([#9821](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9821))
- Stable JVM semconv implementation: threads
  ([#9839](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9839))
- Stable JVM semconv implementation: GC
  ([#9890](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9890))
- Bridge incubator metrics apis
  ([#9884](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9884))
- Ability to instrument logs before OTel injection into OTel appenders
  ([#9798](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9798))
- Suppress instrumentation based on suppress Context key
  ([#9739](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9739))
- Stable JVM semconv implementation: the rest
  ([#9896](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9896))

### 🛠️ Bug fixes

- Fix armeria server instrumentation for http2
  ([#9723](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9723))
- Guard against null list from aws SQS lib
  ([#9710](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9710))
- Fix parsing port from mariadb jdbc url
  ([#9863](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9863))

### 🧰 Tooling

- Improve disableShadowRelocate
  ([#9715](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9715))
- Allow injection of helper bytecode as resources
  ([#9752](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9752))
- Rewrite @Advice.Enter for indy advice
  ([#9887](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9887))

## Version 1.31.0 (2023-10-12)

### 🌟 New javaagent instrumentation

- Apache Pekko
  ([#9527](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9527))

### 📈 Enhancements

- Add instrumentation for vert.x sql client withTransaction method
  ([#9462](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9462))
- Improve hibernate reactive instrumentation
  ([#9486](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9486))
- Support application.yaml files in SpringBootServiceNameDetector
  ([#9515](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9515))
- Add Spring Boot service version finder / ResourceProvider
  ([#9480](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9480))
- Split hibernate reactive instrumentation
  ([#9531](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9531))
- Rework reactor netty context tracking
  ([#9286](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9286))
- Improve spring data reactive instrumentation
  ([#9561](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9561))
- Support akka http latest version
  ([#9573](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9573))
- Enhance AWS SDK Instrumentation with Detailed HTTP Error Information
  ([#9448](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9448))
- Update HTTP metrics' descriptions
  ([#9635](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9635))
- Remove server.socket.address from HTTP/RPC metrics
  ([#9633](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9633))
- Remove 0 bucket from stable HTTP metrics
  ([#9631](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9631))
- Suppress nested http client spans in aws2 instrumentation
  ([#9634](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9634))
- Implement spec changes for grpc server span error status
  ([#9641](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9641))
- Improve vertx-sql client context propagation
  ([#9640](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9640))
- Add url.scheme to HTTP client metrics
  ([#9642](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9642))
- Add support for newVirtualThreadPerTaskExecutor
  ([#9616](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9616))
- Implement error.type attribute in HTTP semconv
  ([#9466](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9466))
- Emit package events
  ([#9301](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9301))
- Added Automatic-Module-Name header to MANIFEST.MF in instrumentation libraries
  ([#9140](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9140))
- Support paths in `peer.service` mappings
  ([#9061](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9061))
- Compile ktor library instrumentation for earlier kotlin version
  ([#9661](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9661))

### 🛠️ Bug fixes

- Fix NPE happening when .headersWhen() is used (reactor-netty)
  ([#9511](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9511))
- Spring webflux: add user spans as children of the controller span
  ([#9572](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9572))
- Make netty ChannelPipeline removeLast return user handler
  ([#9584](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9584))

## Version 1.30.0 (2023-09-14)

### Migration notes

- Experimental HTTP server metrics have been split out from `HttpServerMetrics` into a separate
  class `HttpServerExperimentalMetrics`
  ([#9259](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9259))
- `HttpClientResend` has been renamed to `HttpClientResendCount`, `HttpRouteHolder`
  has been renamed to `HttpServerRoute`
  ([#9280](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9280))
- The previously deprecated `otel.javaagent.experimental.extensions` configuration was removed
  (it is replaced by `otel.javaagent.extensions`)
  ([#9378](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9378))

### 🌟 New javaagent instrumentation

- Add instrumentation for hibernate reactive
  ([#9304](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9304))

### 📈 Enhancements

- Don't log stack trace for expected exceptions in AWS SDK instrumentation
  ([#9279](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9279))
- Add support for the AWS Secrets Manager JDBC URLs
  ([#9335](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9335))
- More support for upcoming semantic convention changes
  ([#9346](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9346)
  [#9345](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9345),
  [#9320](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9320),
  [#9355](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9355),
  [#9381](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9381),
  [#9441](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9441))
- Unwrap Runnable in ThreadPoolExecutor before/after methods
  ([#9326](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9326))
- Add javaagent to instrumentation bom
  ([#9026](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9026))
- Add support for multiple headers in AlternateUrlSchemeProvider
  ([#9389](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9389))
- Skip PreparedStatement wrappers
  ([#9399](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9399))
- Disable Elasticsearch instrumentation for ES clients 8.10+
  ([#9337](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9337))
- Disable elasticsearch-rest-7.0 instrumentation when elasticsearch-java 8.10+ is present
  ([#9450](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9450))
- Use attributes advice for HTTP & RPC metrics
  ([#9440](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9440))
- Update Messaging semantic conventions to 1.21.0
  ([#9408](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9408))
- Add semconv to alpha bom
  ([#9425](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9425))

### 🛠️ Bug fixes

- Ensure .class files aren't present in the resources library MR jar
  ([#9245](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9245))
- Fixed getDefinedPackage lookup for OpenJ9 (8)
  ([#9272](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9272))
- Ignore aws sdk v2 presign requests
  ([#9275](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9275))
- Add logging timestamp for log4j1 appender instrument
  ([#9309](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9309))
- Fix VerifyError with kotlin @WithSpan instrumentation
  ([#9313](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9313))
- Fix serializing key to string in Lettuce instrumentation
  ([#9347](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9347))
- Auto-instrumentation with JMX not working without a trigger
  ([#9362](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9362))
- Return default DbInfo when connection is null
  ([#9413](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9413))
- Fix instrumentation for reactor kafka 1.3.21
  ([#9445](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9445))

## Version 1.29.0 (2023-08-17)

### Migration notes

- `NetClientAttributesExtractor` and `NetServerAttributesExtractor`
  have been deprecated
  ([#9165](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9165),
  [#9156](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9156))
- `HttpClientAttributesGetter` now extends `NetClientAttributesGetter`
  and `HttpServerAttributesGetter` extends `NetServerAttributesGetter`
  ([#9015](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9015),
  [#9088](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9088))
- A couple of Spring starter configuration options have been renamed to match Java agent options:
  - `otel.springboot.httpclients.enabled` -> `otel.instrumentation.spring-webmvc.enabled`
    or `otel.instrumentation.spring-webmvc.enabled` depending on the underlying http client
  - `otel.springboot.aspects.enabled` -> `otel.instrumentation.annotations.enabled`
    ([#8950](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8950))
- Previously deprecated suppression key `executor` was removed from executors module,
  the new suppression key is `executors`
  ([#9064](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9064))

### 🌟 New javaagent instrumentation

- Ktor
  ([#9149](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9149))

### 🌟 New library instrumentation

- Elasticsearch rest client
  ([#8911](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8911))

### 📈 Enhancements

- Include OpenTelemetry logging appenders in the Spring Starter
  ([#8945](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8945))
- Support RPC metrics under "stable" http semconv opt-in
  ([#8948](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8948))
- Support the `http.request.method_original` attribute under "stable" semconv opt-in
  ([#8779](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8779))
- Make `server.socket.*` attributes on the HTTP server side opt-in
  ([#8747](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8747))
- Fill additional db.* attributes on DataSource#getConnection()
  ([#8966](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8966))
- Lettuce instrumentation - optimization to avoid extra toString()
  ([#8984](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8984))
- Allow overriding span name in spring web library instrumentation
  ([#8933](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8933))
- Implement HTTP resend spec for Reactor Netty (excl CONNECT spans)
  ([#8111](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8111))
- Set `process.runtime.jvm.system.cpu.load_1m` metric unit to `{run_queue_item}`
  ([#8777](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8777))
- Update elasticsearch instrumentation to work with latest version
  ([#9066](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9066))
- Reactor Netty: emit actual HTTP client spans on connection errors
  ([#9063](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9063))
- Rename `http.*.duration` to `http.*.request.duration` under "stable" semconv opt-in
  ([#9089](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9089))
- Snippet inject support for non-empty head tags
  ([#8736](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8736))
- Update network.protocol.version `2.0` -> `2` and `3.0` -> `3`
  ([#9145](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9145))
- @WithSpan support for kotlin coroutines
  ([#8870](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8870))

### 🛠️ Bug fixes

- Fix incompatibility between quarkus resteasy reactive and vertx-web instrumentations
  ([#8998](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8998))
- Fix `IllegalArgumentException` in `MetroServerSpanNaming`
  ([#9075](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9075))
- Fix rector netty instrumentation under concurrency
  ([#9081](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9081))
- Improve grpc cancellation propagation
  ([#8957](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8957))
- Add missing timestamp for jboss logmanager instrumentation
  ([#9159](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9159))
- Handle kafka `METRIC_REPORTER_CLASSES_CONFIG` being set to a List
  ([#9155](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9155))
- Fix NullPointerException with Pulsar and SSL
  ([#9166](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9166))
- Netty 4.1: handle closing connection before the request completes
  ([#9157](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9157))
- Micrometer bridge: use app's thread context class loader for callbacks into app
  ([#9000](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9000))
- Fix context propagation in Executor#execute() for non-capturing lambdas
  ([#9179](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9179))
- Ensure reactor netty spans are ended in the order they were started
  ([#9203](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/9203))

## Version 1.28.0 (2023-07-12)

### Migration notes

- Rename HTTP configuration settings
  ([#8758](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8758))
  - `otel.instrumentation.http.capture-headers.client.request`
    → `otel.instrumentation.http.client.capture-request-headers`
  - `otel.instrumentation.http.capture-headers.client.response`
    → `otel.instrumentation.http.client.capture-response-headers`
  - `otel.instrumentation.http.capture-headers.server.request`
    → `otel.instrumentation.http.server.capture-request-headers`
  - `otel.instrumentation.http.capture-headers.server.response`
    → `otel.instrumentation.http.server.capture-response-headers`

### 📈 Enhancements

- Support latest armeria release
  ([#8745](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8745))
- Support latest mongo release
  ([#8785](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8785))
- Remove `server.{address,port}` from HTTP server metrics
  ([#8771](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8771))
- aws-sdk-2.2.: Support injection into SQS.SendMessageBatch message attributes
  ([#8798](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8798))
- Log4j and Logback appenders opt-in to using GlobalOpenTelemetry
  ([#8791](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8791))
- aws-sdk-2.2: SNS.Publish support with experimental messaging propagator flag
  ([#8830](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8830))
- support for adding baggage to log4j 2 ContextData
  ([#8810](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8810))
- Micrometer bridge: interpret no SLO config as no buckets advice
  ([#8856](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8856))
- Instrumentation for Elasticsearch 8+
  ([#8799](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8799))
- Add support for schemaUrls auto-computed from `AttributesExtrator`s
  ([#8864](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8864))
- Initialize appenders in the spring boot starter
  ([#8888](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8888))
- Support reactor-netty 1.0.34+
  ([#8922](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8922))
- Rename messaging operation "send" to "publish" per spec
  ([#8929](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8929))
- Extract query arguments without regex on lettuce 6
  ([#8932](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8932))

### 🛠️ Bug fixes

- Fix logging timestamp
  ([#8761](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8761))
- Minor fixes to the `server.*` attributes extrator
  ([#8772](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8772))
- Fix context leak on call to AmazonS3.generatePresignedUrl
  ([#8815](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8815))
- Fix exception when pulsar has multiple service addresses
  ([#8816](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8816))
- Fix NPE in aws instrumentation on duplicate TracingExecutionInterceptor
  ([#8896](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8896))
- (micrometer) don't add . to empty unit with prometheus naming conventions
  ([#8872](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8872))
- Set server span name for aborted requests in quarkus resteasy native
  ([#8891](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8891))
- Fix instrumentation of Azure SDK EventHubs library
  ([#8916](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8916))
- Fix http attributes of AWS SDK V2 instrumentation
  ([#8931](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8931))

## Version 1.27.0 (2023-06-14)

### Migration notes

- Jersey 3.0 instrumentation suppression keys have changed from `jaxrs-2.0` to `jaxrs-3.0`,
  and from `jersey-2.0` to `jersey-3.0`
  ([#8486](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8486))
- The `opentelemetry-runtime-metrics` artifact has been renamed and split into
  `opentelemetry-runtime-telemetry-java8` and `opentelemetry-runtime-telemetry-java17`
  ([#8165](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8165),
  [#8715](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8715))
- `InetSocketAddressNetServerAttributesGetter` and `InetSocketAddressNetClientAttributesGetter`
  have been deprecated
  ([#8341](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8341),
  [#8591](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8591))
- The new HTTP and network semantic conventions can be opted into using either the system
  property `otel.semconv-stability.opt-in` or the environment variable
  `OTEL_SEMCONV_STABILITY_OPT_IN`, which support two values:
  - `http` - emit the new, stable HTTP and networking attributes, and stop emitting the old
    experimental HTTP and networking attributes that the instrumentation emitted previously.
  - `http/dup` - emit both the old and the stable HTTP and networking attributes, allowing
    for a more seamless transition.
  - The default behavior (in the absence of one of these values) is to continue emitting
    the same HTTP and network semantic conventions that were emitted in `1.26.0`.
  - Note: this option will be removed later this year after the new HTTP and network
    semantic conventions are marked stable, at which time the Java instrumentation version
    will be bumped from 1.x to 2.0, and the old HTTP and network semantic conventions will
    no longer be supported.

### 🌟 New javaagent instrumentation

- Quarkus RESTEasy Reactive
  ([#8487](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8487))
- Reactor Kafka
  ([#8439](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8439),
  [#8529](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8529))

### 📈 Enhancements

- Use histogram advice in the Micrometer bridge
  ([#8334](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8334))
- Disable by default gauge-based histograms in the Micrometer bridge
  ([#8360](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8360))
- Specify ...jvm.gc.duration buckets using advice API
  ([#8436](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8436))
- Make spanKindExtractor configurable in Ktor instrumentations
  ([#8255](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8255))
- aws-sdk-2.2: Support non-X-Ray propagation for SQS
  ([#8405](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8405))
- Implement HTTP resend spec for OkHttp 3
  ([#7652](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7652))
- Use jakarta.servlet.error.exception request attribute on jetty11
  ([#8503](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8503))
- Provide spring-boot-autoconfigure configuration metadata
  ([#8516](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8516))
- Read AWS lambda tracing info from `com.amazonaws.xray.traceHeader` system property
  ([#8368](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8368))
- Skip not decorator check for classes in boot loader
  ([#8594](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8594))
- Change context propagation debug failures from error to warn
  ([#8601](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8601))
- JavaScript Snippet Injection Support Servlet 5
  ([#8569](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8569))
- Faster type matching
  ([#8525](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8525))
- Explicitly disable the logging exporter by default to override the upcoming SDK autoconfigure
  module change in 1.27.0 and preserve existing behavior
  ([#8647](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8647))
- Support otel.sdk.disabled in the spring boot starter
  ([#8602](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8602))
- Add OTLP log exporter to the OpenTelemetry Spring Starter
  ([#8493](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8493))
- Suppress Spring Actuator instrumentation when micrometer instrumentation is suppressed
  ([#8677](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8677))
- Replace "1" with the appropriate units and don't fall back to "1" if the unit is unknown
  ([#8668](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8668))
- Added setOpenTelemetry method to log4j appender
  ([#8231](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8231))

### 🛠️ Bug fixes

- Fix using logback mdc instrumentation along with SocketAppender
  ([#8498](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8498))
- Fix failure when kafka metrics reporter is set to an empty string
  ([#8523](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8523))
- Remove extra space from kubernetes client span name
  ([#8540](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8540))
- Fix jetty context leak
  ([#8552](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8552))
- Filter out scalar Mono/Flux instances
  ([#8571](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8571))
- End netty client span when channel is closed
  ([#8605](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8605))
- Unregistering jmx gc metrics on close
  ([#8650](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8650))
- Fix snippet injection ClassCastException
  ([#8701](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8701))
- Only instrument the actual Spring `TaskScheduler` implementations
  ([#8676](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8676))

## Version 1.26.0 (2023-05-11)

### Migration notes

- `java.util.concurrent` executors instrumentation suppression key has changed from `executor` to
  `executors`
  ([#8451](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8451))

### 🌟 New javaagent instrumentation

- Add instrumentation for vertx-sql-client
  ([#8311](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8311))

### 🌟 New library instrumentation

### 📈 Enhancements

- Make net.transport an optional attribute
  ([#8279](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8279))
- Introduce `@AddingSpanAttributes` annotation
  ([#7787](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7787))
- JavaScript snippet injection
  ([#7650](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7650))
- Map `peer.service` also from `net.sock.peer.name`
  ([#7888](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7888))
- Set up EarlyInitAgentConfig even earlier
  ([#8413](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8413))

### 🛠️ Bug fixes

- Fix nested http.route
  ([#8282](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8282))
- Fix undertow instrumentation with http pipelining
  ([#8400](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8400))
- Fix http pipelining on Grizzly
  ([#8411](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8411))
- Fix http pipelining on Netty 4.1 server
  ([#8412](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8412))
- Allow transforming classes with missing field types
  ([#8393](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8393))
- Fix reactor flux retry context leak
  ([#8456](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8456))

### 🧰 Tooling

- Introduce new incubating `InstrumenterBuilder` methods
  ([#8392](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8392))

## Version 1.25.1 (2023-04-27)

- Fix apache pulsar instrumentation missing from the Java agent
  ([#8378](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8378))

## Version 1.25.0 (2023-04-13)

### Migration notes

- Apache Pulsar instrumentation name is changed from `io.opentelemetry.apache-pulsar-2.8` to
  `io.opentelemetry.pulsar-2.8` and Apache Camel instrumentation name is changed from
  `io.opentelemetry.apache-camel-2.20` to `io.opentelemetry.camel-2.20`
  ([#8195](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8195))
- Mojarra instrumentation suppression key has changed from `mojarra` to `jsf-mojarra`
  and MyFaces instrumentation suppression key has changed from `myfaces` to `jsf-myfaces`
  ([#7811](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7811))

### 🌟 New javaagent instrumentation

- R2DBC
  ([#7977](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7977))
- Add JFR streaming metrics gatherer
  ([#7886](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7886))
- ZIO 2.0 instrumentation
  ([#7980](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7980))

### 🌟 New library instrumentation

- R2DBC
  ([#7977](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7977))
- Add JFR streaming metrics gatherer
  ([#7886](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7886))
- Add library instrumentation for java http client
  ([#8138](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8138))

### 📈 Enhancements

- Move X-Ray Env Variable propagation to span link instead of parent
  ([#7970](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7970))
- Reduce memory usage for ClassLoaderHasClassesNamedMatcher
  ([#7866](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7866))
- Remove password from db.user parsed from JDBC url
  ([#8106](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8106))
- Ignore appd agent classes
  ([#8065](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8065))
- Add http client metrics to apache http library instrumentation
  ([#8128](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8128))
- Instrument additional pulsar receive methods
  ([#8171](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8171))
- Support latest Hibernate 6 version
  ([#8189](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8189))
- Support spring boot service name detector when spring boot application is packaged in one jar
  ([#8101](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8101))
- Support parsing Spring boot service name when more than one yaml is defined
  ([#8006](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8006))
- Add option to capture logback key value pairs
  ([#8074](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8074))
- Bridge agent logs into application's slf4j logger
  ([#7339](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7339),
  [#8228](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8228))
- Better container ID parsing
  ([#8206](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8206))
- Pulsar batch receive instrumentation
  ([#8173](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8173))
- Improve compatibility with SecurityManager
  ([#7983](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7983))
- Instrument akka-http bindAndHandle
  ([#8174](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8174))
- Spring scheduling: run error handler with the same context as task
  ([#8220](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8220))
- Switch from http.flavor to net.protocol.\*
  ([#8131](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8131),
  [#8244](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8244))
- Support latest Armeria release
  ([#8247](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8247))
- Populate `process.command_args` for Java 9+ and improve `process.command_line` for Java 8
  ([#8130](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8130))

### 🛠️ Bug fixes

- Fix TracingCqlSession ClassCastException
  ([#8041](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8041))
- Kafka: avoid registering duplicate metrics reporter
  ([#8099](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8099))
- Prefix baggage key not value when adding it to logback mdc
  ([#8066](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8066))
- Fix error when using shardingsphere
  ([#8110](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8110))
- Fix order of cxf handlers to enable symmetric tracing around jaxws handler chain
  ([#8160](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8160))
- Fix javaagent armeria server instrumentation
  ([#8281](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8281))

### 🧰 Tooling

- Add HttpServerResponseCustomizer support for various servers
  ([#8094](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8094),
  [#8095](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8095),
  [#8265](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8265),
  [#8264](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8264),
  [#8273](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8273),
  [#8263](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8263),
  [#8274](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8274),
  [#8272](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8272))
- Add `removeJarVersionNumbers` build setting
  ([#8116](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8116))
- Add `disableShadowRelocate` build setting
  ([#8117](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8117))

## Version 1.24.0 (2023-03-15)

### Migration notes

### 🌟 New javaagent instrumentation

- Add Apache Pulsar client instrumentation
  ([#5926](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5926),
  [#7999](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7999),
  [#7943](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7943),
  [#8007](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8007))
- Add Jodd-Http instrumentation
  ([#7868](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7868))

### 🌟 New library instrumentation

- Add Ktor client instrumentation
  ([#7982](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7982),
  [#7997](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7997))
- Add Spring Webflux server instrumentation
  ([#7899](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7899))

### 📈 Enhancements

- Implement `messaging.kafka.*` attributes spec
  ([#7824](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7824),
  [#7860](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7860))
- Make RxJava2 instrumentation Android-friendly
  ([#7895](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7895))
- Support more semantic convention for RocketMQ trace
  ([#7871](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7871))
- Instrumenting cassandra executeReactive method
  ([#6441](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6441))
- Make the OpenTelemetry Logback appender work with GraalVM native images
  ([#7989](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7989))
- Add baggage to Logback MDC; controlled by a configuration flag
  ([#7892](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7892))
- Make the Spring Boot autoconfigure module work with Spring Boot 3
  ([#8028](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8028),
  [#8051](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8051))

### 🛠️ Bug fixes

- Handle JMX MBean attributes with embedded dots
  ([#7841](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7841))
- Fix `ClassCastException` when using `-Dotel.jmx.target.system=tomcat`
  ([#7884](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7884))
- Fix NPE in the AWS SDK 2 instrumentation when request instrumentation is suppressed
  ([#7953](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7953))
- Fix Kotlin coroutine context propagation
  ([#7879](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7879))
- Fix the JAX-RS annotation instrumentation on Open Liberty
  ([#7890](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7890))
- Fix an `AbstractMethodError` in the Logback instrumentation
  ([#7967](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7967))
- Fix NPE in the RabbitMQ instrumentation
  ([#8021](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8021))
- Fix JMX metrics usage examples
  ([#7877](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7877))

### 🧰 Tooling

- Remove deprecated `instrumentation-api-semconv` code
  ([#7838](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7838))
- Look up helper class bytes when they are needed
  ([#7839](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7839))
- Run smoke tests using Open Liberty 21.0.0.12 and 22.0.0.12
  ([#7878](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7878),
  [#7857](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7857))
- Add additional groovy script classloaders to ignore list.
  ([#7460](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7460))
- Make AggregationTemporality configurable for `OtlpInMemoryMetricExporter` in
  the `agent-for-testing` module
  ([#7904](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7904))
- Upgrade to gradle 8.0.2
  ([#7910](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7910),
  [ 7978](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7978))
- Replace the test-sets plugin with Gradle test suites
  ([#7930](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7930),
  [#7933](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7933),
  [#7932](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7932),
  [#7931](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7931),
  [#7929](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7929),
  [#7946](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7946),
  [#7945](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7945),
  [#7944](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7944),
  [#7943](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7943),
  [#7942](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7942),
  [#7928](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7928),
  [#7951](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7951))
- Add a utility for tracking HTTP resends
  ([#7986](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7986))
- Remove deprecated `messaging.url` attribute from messaging getter
  ([#8008](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8008))
- Add protocol name&version to net attribute getters
  ([#7994](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7994))
- Test http client captured headers
  ([#7993](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7993))
- Improve compatibility with other agents
  ([#7916](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7916))
- Share timer class used by messaging instrumentations
  ([#8009](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8009))
- Convert groovy tests to java
  ([#7976](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7976),
  [#7975](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7975),
  [#7912](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7912))
- Check that extracting extension jar doesn't escape designated directory
  ([#7908](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7908))
- Assert user agent when it is present in HTTP client tests
  ([#7918](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7918))
- Update the OpenTelemetry SDK version to 1.24.0
  ([#8027](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/8027))
- Provide ability to add HTTP server response headers, with Tomcat implementation
  ([#7990](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7990))

## Version 1.23.0 (2023-02-16)

### Migration notes

- HTTP span names are now `"{http.method} {http.route}"` instead of just `"{http.route}"`,
  reflecting the recent change in the HTTP semantic conventions
  ([#7730](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7730))
- Added the base version into library package names to make room for other base versions
  without breaking backwards compatibility in the future once these artifacts are declared stable
  ([#7608](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7608),
  [#7752](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7752),
  [#7764](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7764))
  - `io.opentelemetry.instrumentation.spring.web.SpringWebTelemetry`
    → `io.opentelemetry.instrumentation.spring.web.v3_1.SpringWebTelemetry`
  - `io.opentelemetry.instrumentation.spring.webflux.SpringWebfluxTelemetry`
    → `io.opentelemetry.instrumentation.spring.webflux.v5_0.SpringWebfluxTelemetry`
  - `io.opentelemetry.instrumentation.spring.integration.SpringIntegrationTelemetry`
    → `io.opentelemetry.instrumentation.spring.integration.v4_1.SpringIntegrationTelemetry`
  - `io.opentelemetry.instrumentation.logback.v1_0.OpenTelemetryAppender`
    → `io.opentelemetry.instrumentation.logback.mdc.v1_0.OpenTelemetryAppender`
  - `io.opentelemetry.instrumentation.apachedbcp.ApacheDbcpTelemetry`
    → `io.opentelemetry.instrumentation.apachedbcp.v2_0.ApacheDbcpTelemetry`
  - `io.opentelemetry.instrumentation.c3p0.C3p0Telemetry`
    → `io.opentelemetry.instrumentation.c3p0.v0_9.C3p0Telemetry`
  - `io.opentelemetry.instrumentation.graphql.GraphQLTelemetry`
    → `io.opentelemetry.instrumentation.graphql.v12_0.GraphQLTelemetry`
  - `io.opentelemetry.instrumentation.guava.GuavaAsyncOperationEndStrategy`
    → `io.opentelemetry.instrumentation.guava.v10_0.GuavaAsyncOperationEndStrategy`
  - `io.opentelemetry.instrumentation.hikaricp.HikariTelemetry`
    → `io.opentelemetry.instrumentation.hikaricp.v3_0.HikariTelemetry`
  - `io.opentelemetry.instrumentation.kafkaclients.KafkaTelemetry`
    → `io.opentelemetry.instrumentation.kafkaclients.v2_6.KafkaTelemetry`
  - `io.opentelemetry.instrumentation.oracleucp.OracleUcpTelemetry`
    → `io.opentelemetry.instrumentation.oracleucp.v11_2.OracleUcpTelemetry`
  - `io.opentelemetry.instrumentation.ratpack.RatpackTelemetry`
    → `io.opentelemetry.instrumentation.ratpack.v1_7.RatpackTelemetry`
  - `io.opentelemetry.instrumentation.reactor.ContextPropagationOperator`
    →` io.opentelemetry.instrumentation.reactor.v3_1.ContextPropagationOperator`
  - `io.opentelemetry.instrumentation.viburdbcp.ViburTelemetry`
    → `io.opentelemetry.instrumentation.viburdbcp.v11_0.ViburTelemetry`
- Several instrumentation scope names have been fixed
  ([#7632](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7632))
  - `io.opentelemetry.cxf-jaxrs-3.2` → `io.opentelemetry.jaxrs-2.0-cxf-3.2`
  - `io.opentelemetry.jersey-2.0` → `io.opentelemetry.jaxrs-2.0-jersey-2.0`
    or `io.opentelemetry.jaxrs-3.0-jersey-3.0` as appropriate
  - `io.opentelemetry.resteasy-3.0` → `io.opentelemetry.jaxrs-2.0-resteasy-3.0`
  - `io.opentelemetry.resteasy-3.1` → `io.opentelemetry.jaxrs-2.0-resteasy-3.1`
  - `io.opentelemetry.resteasy-6.0` → `io.opentelemetry.jaxrs-3.0-resteasy-6.0`
  - `io.opentelemetry.jws-1.1` → `io.opentelemetry.jaxws-jws-api-1.1`
  - `io.opentelemetry.vertx-kafka-client-3.5` → `io.opentelemetry.vertx-kafka-client-3.6`
  - `io.opentelemetry.hibernate-4.3` → `io.opentelemetry.hibernate-procedure-call-4.3`
- All methods in all `*Getter` classes in `instrumentation-api-semconv` have been renamed
  to use the `get*()` naming scheme
  ([#7619](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7619))
- Return interfaces instead of concrete implementations in `instrumentation-api-semconv`
  ([#7658](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7658))

### 📈 Enhancements

- Support Spring Web 6 in library instrumentation
  ([#7551](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7551))
- Add gRPC request metadata instrumentation
  ([#7011](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7011))
- Pass context to content length http metrics meters
  ([#7506](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7506))
- Update SQL span name for procedures
  ([#7557](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7557))
- Ratpack services OpenTelemetry
  ([#7477](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7477))
- Ignore janino classloader
  ([#7710](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7710))
- Improve type resolution cache for classes in java package
  ([#7714](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7714))
- End netty client span before callbacks
  ([#7737](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7737))
- Support slf4j to log4j2
  ([#7656](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7656))
- Add `net.host.port` to the `http.server.active_requests` metric
  ([#7757](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7757))
- Allow JDBC library instrumentation to use a custom OpenTelemetry instance to be more DI
  (e.g. Spring Boot) friendly
  ([#7697](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7697))
- Dubbo: don't create spans for calls inside the same jvm
  ([#7761](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7761))
- Bridge OpenTelemetry metrics batch API
  ([#7762](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7762))
- Support Spring Boot 3 in autoconfigure module
  ([#7784](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7784))
- Change Spring Scheduling to not capture span for one-time jobs (only repeated jobs)
  ([#7760](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7760))
- Add instrumentation for hibernate 6
  ([#7773](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7773))
- Put `http.route` attribute onto `http.server.duration` on Play framework request processing
  ([#7801](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7801))
- Add Jakarta JSF 3.0+ instrumentation
  ([#7786](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7786))
- Capture `net.sock.peer.addr` more reliably in grpc client instrumentation
  ([#7742](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7742))

### 🛠️ Bug fixes

- Fix potential startup failure
  ([#7567](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7567))
- Fix NoClassDefFoundError happening when snakeyaml is used on a custom JRE
  ([#7598](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7598))
- Fix inconsistent handling of `net.peer.port` in HTTP instrumentations
  ([#7618](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7618))
- Fix concurrency issue in OpenTelemetryDriver
  ([#7628](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7628))
- Fix ClassCastException with redisson batch with atomic write option
  ([#7743](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7743))
- Ensure kafka configuration remains serializable
  ([#7754](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7754),
  [#7789](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7789))

## Version 1.22.1 (2023-01-14)

- Fix potential startup failure
  ([#7567](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7567))

## Version 1.22.0 (2023-01-12)

### 📈 Enhancements

- Add resource injection for getResourceAsStream
  ([#7476](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7476))
- GRPC: Adds peer socket address when the client call is ready
  ([#7451](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7451))
- Make OpenTelemetryAppender.Builder public
  ([#7521](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7521))
- Disable internal TaskScheduler spans in Spring Kafka instrumentation
  ([#7553](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7553))
- Support Spring Web MVC in library instrumentation
  ([#7552](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7552))
- Instrument JMS 3.0 (Jakarta)
  ([#7418](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7418))
- Shade `application.io.opentelemetry` in agent extension class loader
  ([#7519](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7519))
- Use new reactor contextWrite when available (from reactor 3.4.0)
  ([#7538](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7538))
- Make config file available in early agent initialization phase
  ([#7550](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7550))
- Support Spring JMS 6.0
  ([#7438](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7438))

### 🛠️ Bug fixes

- Preserve original exception in jetty http client wrappers
  ([#7455](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7455))
- Propagate original exception through kafka wrapper
  ([#7452](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7452))
- Fix class file load error when using spring-guice together with spring-web instrumentation
  ([#7447](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7447))
- Fix NPE in servlet AsyncListener on OpenLiberty
  ([#7498](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7498))

### 🧰 Tooling

- Muzzle logs should be logged using the io.opentelemetry.\* logger name
  ([#7446](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7446))

## Version 1.21.0 (2022-12-13)

### 📈 Enhancements

- Remove deprecated HTTP semconv code
  ([#7259](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7259))
- Rocketmq 5: set context for async callback
  ([#7238](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7238))
- HTTP semconv: filter out default peer/host ports
  ([#7258](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7258))
- Allow multiple YAML configuration files for JMX rules
  ([#7284](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7284))
- OpenTelemetryDriver better support for native images
  ([#7089](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7089))
- Support Spring Kafka 3
  ([#7271](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7271))
- Add instrumentation for opentelemetry-extension-kotlin
  ([#7341](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7341))
- Populate `messaging.kafka.message.offset` in all kafka instrumentations
  ([#7374](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7374))
- More flexible cgroupv2 container id parsing (and podman support)
  ([#7361](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7361))
- Instrument spring-web 6 & spring-webmvc 6
  ([#7366](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7366))
- Change log4j MapMessage attribute names
  ([#7397](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7397))
- Don't call Thread#setContextClassLoader()
  ([#7391](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7391))

### 🛠️ Bug fixes

- Webflux instrumentation doesn't capture `http.status_code` in some cases
  ([#7251](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7251))
- Clean WeakConcurrentMap from background thread
  ([#6240](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6240))
- Update gwt rpc span from INTERNAL to SERVER
  ([#7342](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7342))
- JMXMetricInsight should log nothing at INFO level
  ([#7367](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7367))
- Fix NullPointerException when uri is null
  ([#7387](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7387))
- Agent context storage wrapper should not override other wrappers
  ([#7355](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7355))
- Do not install GC metrics when GarbageCollectionNotificationInfo is not available
  ([#7405](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7405))
- Fix connection pool timeouts unit
  ([#7404](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7404))

### 🧰 Tooling

- Allow disabling muzzle checks for specific methods
  ([#7289](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7289))

## Version 1.20.2 (2022-11-22)

### 🛠️ Bug fixes

- Fix the boms
  ([#7252](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7252))

## Version 1.20.1 (2022-11-19)

### 📈 Enhancements

- Update SDK version from 1.19.0 to 1.20.1
  ([#7223](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7223))

## Version 1.20.0 (2022-11-17)

Instrumentation annotations module is declared stable in this release
(`io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.20.0`).

### Migration notes

- Renamed configuration property `otel.instrumentation.kafka.client-propagation.enabled` to
  `otel.instrumentation.kafka.producer-propagation.enabled` and update code so that it only affects
  producer propagation
  ([#6957](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6957))
- Deprecated RocketMQ configuration property `otel.instrumentation.rocketmq-client.propagation`
  ([#6958](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6958))
- Deprecated `HttpServerAttributesExtractor#create(HttpServerAttributesGetter)` and
  `HttpServerAttributesExtractor#builder(HttpServerAttributesGetter)`
  ([#7020](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7020))
- Renamed annotation instrumentation property name for the recent
  `opentelemetry-instrumentation-annotations` package from
  `otel.instrumentation.opentelemetry-annotations.exclude-methods` to
  `otel.instrumentation.opentelemetry-instrumentation-annotations.exclude-methods`
  ([#7196](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7196))

### 🌟 New javaagent instrumentation

- OpenSearch 1.x and 2.x
  ([#6998](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6998))
- JMX Metric Insight
  ([#6573](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6573))

### 📈 Enhancements

- Add meter version to runtime metrics
  ([#6874](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6874))
- Auto-detect service name based on the jar name
  ([#6817](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6817))
- okhttp: run our interceptor before other interceptors
  ([#6997](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6997))
- Implement producer part of RocketMQ new client instrumentation
  ([#6884](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6884))
- Ignore presto-jdbc shaded okhttp3 connection pool.
  ([#7031](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7031))
- Record memory usage after garbage collection
  ([#6963](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6963))
- Ignore trino shaded okhttp pool
  ([#7114](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7114))
- Ignore Nashorn's class loader for performance reasons
  ([#7116](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7116))
- Add gRPC library configuration for additionalServerExtractors
  ([#7155](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7155))
- Implement consumer part of rocketmq new client instrumentation
  ([#7019](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7019))
- Support cgroup v2
  ([#7167](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7167))
- Replace `runtime.jvm.gc.time` / `runtime.jvm.gc.count` metrics with
  `process.runtime.jvm.gc.duration` histogram
  ([#6964](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6964))

### 🛠️ Bug fixes

- End reactor-netty HTTP client span properly on `Mono#timeout()`
  ([#6891](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6891))
- Capture net.host.name for netty
  ([#6892](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6892))
- 6929 - fixing unit for runtime.jvm.gc.count
  ([#6930](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6930))
- fix spring-webflux cast to PathPattern throws ClassCastException
  ([#6872](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6872))
- Fix metric units
  ([#6931](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6931))
- Fix RocketMQ ClassCastException on hook conflict
  ([#6940](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6940))
- Handle UnsupportedClassVersionError in ServiceLoader on jdk9
  ([#7090](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7090))
- Avoid NPE when DirectWithAttributesChannel class is not available
  ([#7133](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7133))

### 🧰 Tooling

- Revert removing the mavenCentral repo from the muzzle check plugin
  ([#6937](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6937))
- Make java.sql classes available to the agent and extensions
  ([#7038](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7038))

## Version 1.19.2 (2022-11-02)

### 🛠️ Bug fixes

- Bring back two public methods in `opentelemetry-instrumentation-api-semconv` that were mistakenly
  removed in v1.19.1 without a deprecation cycle
  ([#7020](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/7020))

## Version 1.19.1 (2022-10-19)

### 🛠️ Bug fixes

- Capture `net.host.name` on netty SERVER spans
  ([#6892](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6892))

## Version 1.19.0 (2022-10-13)

### Migration notes

- Deprecated `HttpServerAttributesGetter.serverName()`, and removed `http.host` and
  `http.server_name` attributes
  ([#6709](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6709))
- Removed previously deprecated configuration flags (see previous release notes for deprecations)
  ([#6771](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6771))
- The restlet-1 instrumentation name has changed from `restlet-1.0` to `restlet-1.1`
  ([#6106](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6106))

### 🌟 New library instrumentation

- Netty 4.1
  ([#6820](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6820))

### 📈 Enhancements

- Move in resource providers from core repo
  ([#6574](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6574))
- Propagate client span context in doOnRequest
  ([#6621](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6621))
- Update attribute key of rocketmq's message tag to use name from semantic conventions
  (`messaging.rocketmq.message_tag`)
  ([#6677](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6677))
- Avoid muzzle matcher warning for the spring-boot-actuator-autoconfigure instrumentation
  ([#6695](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6695))
- Add marker attribute for Log4j 2
  ([#6680](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6680))
- Add marker attribute for Logback
  ([#6652](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6652))
- Add daemon attribute to process.runtime.jvm.threads.count
  ([#6635](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6635))
- Link JMS receive span with the producer span
  ([#6804](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6804))
- Link RabbitMQ receive span with the producer span
  ([#6808](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6808))
- Run context customizers before span start instead of after
  ([#6634](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6634))
- Strip sensitive data from the url
  ([#6417](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6417))
- Extract `net.peer.{name,port}` on start for CLIENT spans
  ([#6828](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6828))

### 🛠️ Bug fixes

- Fix scheduled job experimental attributes property
  ([#6633](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6633))
- Fix AutoConfigurationCustomizer.addPropertiesSupplier not taking into account configuration-file
  ([#6697](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6697))
- Fix Dubbo NPE and trace propagation issue
  ([#6640](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6640))
- Fix directory separator in ProcessResource attributes
  ([#6716](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6716))
- Fix instrumentation for tomcat 10.1.0
  ([#6766](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6766))
- Fix instrumentation name for jaxrs-2.0-annotations
  ([#6770](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6770))
- Fix instrumentation for vert.x 4.3.4
  ([#6809](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6809))
- Fix Restlet v2 `Message#getAttributes` calls
  ([#6796](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6796))
- Guard against null HttpContext
  ([#6792](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6792))

## Version 1.18.0 (2022-09-14)

The `opentelemetry-instrumentation-api` artifact is declared stable in this release.

### Migration notes

- There were a few late-breaking changes in `opentelemetry-instrumentation-api`, prior to it being
  declared stable:
  - `InstrumenterBuilder.addAttributesExtractors(AttributesExtractor...)` was removed, use instead
    `addAttributesExtractors(AttributesExtractor)` or
    `addAttributesExtractors(Iterable<AttributesExtractor>)`
  - `SpanLinksExtractor.extractFromRequest()` was removed, use instead manual extraction
  - `ErrorCauseExtractor.jdk()` was renamed to `ErrorCauseExtractor.getDefault()`
  - `ClassNames` utility was removed with no direct replacement
- The deprecated `io.opentelemetry.instrumentation.api.config.Config` and related classes
  have been removed
  ([#6501](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6501))
- Net attributes getters were updated to reflect latest specification changes
  ([#6503](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6503))
- The deprecated `Ordered` interface was removed from the `opentelemetry-javaagent-extension-api`,
  use instead the `Ordered` interface from `opentelemetry-sdk-extension-autoconfigure-spi`
  ([#6589](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6589))

### 📈 Enhancements

- Add Spring Boot service name guesser / ResourceProvider
  ([#6516](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6516))
- Move micrometer shim library instrumentation back
  ([#6538](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6538))
- Add grpc status code to metrics attrs
  ([#6556](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6556))
- Add mongo sanitization configuration
  ([#6541](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6541))
- Add kafka client metrics to the javaagent instrumentation
  ([#6533](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6533))
- Add experimental span attribute job.system
  ([#6586](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6586))
- Add code attributes for Logback
  ([#6591](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6591))
- Reactor instrumentation: do not make root context current
  ([#6593](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6593))

### 🛠️ Bug fixes

- Fix default-enabled config
  ([#6491](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6491))
- Fix new jdbc javaagent config
  ([#6492](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6492))
- Fix jaxrs async instrumentation race
  ([#6523](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6523))
- Fix spring webmvc instrumentation name
  ([#6557](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6557))
- Fix spring boot `@WithSpan` handling
  ([#6619](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6619))

## Version 1.17.0 (2022-08-18)

### Migration notes

- The `@WithSpan` and `@SpanAttribute` annotations has been moved from the
  `io.opentelemetry:opentelemetry-extension-annotations` artifact to the
  `io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations` in order to live and
  evolve alongside the instrumentation itself. The instrumentation will continue to support the old
  artifact for backwards compatibility, but new annotation-based features will only be built out and
  supported with the new annotation artifact.
- `InstrumenterBuilder.newInstrumenter()` is renamed to `InstrumenterBuilder.buildInstrumenter()`
  ([#6363](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6363))
- `io.opentelemetry.instrumentation.api.config.Config` is deprecated
  ([#6360](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6360))
- `HttpCommonAttributesGetter.requestContentLengthUncompressed()` and
  `responseContentLengthUncompressed` are deprecated
  ([#6383](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6383))
- Ktor 2.0 instrumentation name is changed from `io.opentelemetry.ktor-1.0` to
  `io.opentelemetry.ktor-2.0`
  ([#6452](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6452))
- `io.opentelemetry.opentelemetry-annotations-1.0` instrumentation name is changed to
  `io.opentelemetry.opentelemetry-instrumentation-annotations-1.16`
  ([#6450](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6450))
- Liberty instrumentation names are changed from `io.opentelemetry.liberty` and
  `io.opentelemetry.liberty-dispatcher` to `io.opentelemetry.liberty-20.0` and
  `io.opentelemetry.liberty-dispatcher-20.0`
  ([#6456](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6456))
- The 2-arg variant of HttpCommonAttributesGeter#statusCode() is deprecated
  ([#6466](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6466))
- The `opentelemetry-spring-starter` artifact has been renamed to
  `opentelemetry-spring-boot-starter`, the `opentelemetry-zipkin-exporter-starter` artifact has been
  renamed to `opentelemetry-zipkin-spring-boot-starter`, and the
  `opentelemetry-zipkin-exporter-starter` artifact has been renamed to
  `opentelemetry-zipkin-spring-boot-starter`
  ([#6453](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6453))
- Update net semantic convention changes based on recent specification changes:
  `net.peer.ip` renamed to `net.sock.peer.addr`, `net.host.ip` renamed to `net.sock.host.addr`,
  `net.peer.name` renamed to `net.sock.peer.name` for socket-level instrumentation,
  and `NetClientAttributesGetter.peerIp()`, `NetServerAttributesGetter.peerIp()`, and
  `NetServerAttributesGetter.peerPort()` are deprecated
  ([#6268](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6268))

### 📈 Enhancements

- Move buffer pool metrics out of experimental
  ([#6370](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6370))
- Add code attributes to several instrumentations
  ([#6365](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6365))
- Add http.client|server.request|response.size metrics
  ([#6376](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6376))
- Add Kafka instrumentation to the Spring Boot starter
  ([#6371](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6371))
- Extract HTTP request & response content length from headers
  ([#6415](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6415))
- Support DataDirect and Tibco Jdbc URLs
  ([#6420](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6420))
- Set http.route in spring-autoconfigure webmvc instrumentation
  ([#6414](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6414))
- Grizzly: capture all matching request & response headers
  ([#6463](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6463))
- Capture messaging header value as span attribute
  ([#6454](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6454))
- Add JDBC-specific sanitizer property
  ([#6472](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6472))

### 🛠️ Bug fixes

- Fix duplicate spans for Quarkus gRPC server
  ([#6356](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6356))
- Update Kafka library instrumentation to support version 3.0.0 and later
  ([#6457](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6457))
- Mongodb: avoid duplicate tracing
  ([#6465](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6465))
- Fix netty instrumentation NoSuchElementException
  ([#6469](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6469))

## Version 1.16.0 (2022-07-19)

### Migration notes

- Config has been replaced by ConfigProperties in Javaagent extensions SPIs
- The deprecated TimeExtractor has been removed
- The `opentelemetry-instrumentation-api-annotation-support` artifact has been renamed to
  `opentelemetry-instrumentation-annotation-support`
- The `opentelemetry-annotations` instrumentation suppression key has been renamed to
  `opentelemetry-extension-annotations`
- The 'otel.javaagent.experimental.use-noop-api' flag has been removed, as this capability is now
  available via the `otel.experimental.sdk.enabled` flag

### 🌟 New javaagent instrumentation

- C3P0 connection pool metrics
  ([#6174](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6174))
- JVM buffer pool metrics
  ([#6177](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6177))
- Kafka client metrics
  ([#6138](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6138))
- dropwizard-metrics to OpenTelemetry metrics bridge
  ([#6259](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6259))

### 🌟 New library instrumentation

- C3P0 connection pool metrics
  ([#6174](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6174))
- JVM buffer pool metrics
  ([#6177](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6177))
- Kafka client metrics
  ([#6138](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6138))
- Add metrics & micrometer support to spring-boot-autoconfigure
  ([#6270](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6270))
- Spring Kafka library instrumentation
  ([#6283](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6283))

### 📈 Enhancements

- Update GraphQL instrumentation to match spec
  ([#6179](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6179))
- Make rpc.grpc.status_code required
  ([#6184](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6184))
- Always pass Context when recording HttpServerMetrics
  ([#6223](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6223))
- Capture enduser.id in servlet instrumentation
  ([#6225](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6225))
- Support metric view configuration file in the Javaagent
  ([#6228](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6228))
- Aws sdk2 sqs context propagation
  ([#6199](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6199))
- More Spring JMS support
  ([#6308](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6308))
- Hikaricp: Avoid registering duplicate metrics
  ([#6325](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6325))

### 🛠️ Bug fixes

- Fix liberty net.peer.port
  ([#6274](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6274))

## Version 1.15.0 (2022-06-16)

### Migration notes

- The `opentelemetry-jboss-logmanager-1.1` artifact has been renamed to
  `opentelemetry-jboss-logmanager-appender-1.1`
- The play instrumentation name has changed from `play` to `play-mvc`
  ([#6106](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6106))
- The vertx-http-client instrumentation name has changed from `vertx-client` to `vertx-http-client`
  ([#6106](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6106))
- The metric `process.runtime.java.memory.max` has been renamed
  to `process.runtime.java.memory.limit`
  ([#6161](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6161))

### 🌟 New javaagent instrumentation

- JVM classes metrics
  ([#6069](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6069))
- JVM threads metrics
  ([#6070](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6070))
- Vibur DBCP connection pool metrics
  ([#6092](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6092))
- tomcat-jdbc connection pool metrics
  ([#6102](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6102))
- JVM cpu metrics
  ([#6107](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6107))
- Oracle UCP connection pool metrics
  ([#6099](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6099))
- Apache DBCP2 datasource metrics
  ([#6175](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6175))
- Add instrumentation for JAX-RS 3.0
  ([#6136](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6136))

### 🌟 New library instrumentation

- JVM classes metrics
  ([#6069](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6069))
- JVM threads metrics
  ([#6070](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6070))
- Vibur DBCP connection pool metrics
  ([#6092](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6092))
- tomcat-jdbc connection pool metrics
  ([#6102](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6102))
- JVM cpu metrics
  ([#6107](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6107))
- Oracle UCP connection pool metrics
  ([#6099](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6099))
- Apache DBCP2 datasource metrics
  ([#6175](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6175))

### 📈 Enhancements

- Enable grizzly instrumentation by default
  ([#6049](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6049))
- Instrumentation for redisson 3.17.2+
  ([#6096](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6096))
- Instrumentation for jboss-logmanager getMdcCopy()
  ([#6112](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6112))
- Allow specifying a comma separated list of extensions
  ([#6137](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6137))

### 🛠️ Bug fixes

- Fix redisson ClassCastException
  ([#6054](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6054))
- Fix debug logging
  ([#6085](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6085))
- HttpURLConnection instrumentation can capture wrong http.method
  ([#6053](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6053))
- fix bug: get return type in a wrong way for method instrumentation
  ([#6118](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6118))
- Show correct runnable name in spring scheduling actuator
  ([#6140](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6140))
- Fix ClassCastException in JDBC instrumentation
  ([#6088](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6088))

### 🧰 Tooling

- Remove TimeExtractor and use internal API for setting start/end timestamps
  ([#6051](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6051))
- Change SpanStatusExtractor to use a builder that can set status description
  ([#6035](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6035))
- Make gRPC metadata available to AttributeExtractors
  ([#6125](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6125))

## Version 1.14.0 (2022-05-17)

### Migration notes

- The `opentelemetry-log4j-appender-2.16` artifact has been renamed to
  `opentelemetry-log4j-appender-2.17`
- The `opentelemetry-log4j-context-data-2.16-autoconfigure` artifact has been renamed to
  `opentelemetry-log4j-context-data-2.17-autoconfigure`
- Micrometer library instrumentation has been removed as it has been moved to the core repo and
  is now published under `io.opentelemetry:opentelemetry-micrometer1-shim`
- The rxjava javaagent instrumentation names for suppression have changed to `rxjava`
- `AgentListener#beforeAgent()` has been deprecated, as it is not expect to be needed by extensions
  authors
- `ConfigPropertySource` has been deprecated in favor of `ConfigCustomizer`
- Several changes in the Instrumentation API
  - `RequestMetrics` has been renamed to `OperationMetrics`
  - `RequestListener` has been renamed to `OperationListener`
  - `ErrorCauseExtractor#extractCause()` has been renamed to `extract()`
  - `ContextCustomizer` and `RequestListener` `start()`/`end()` methods have been renamed to
    `onStart()`/`onEnd()`
- The `opentelemetry-javaagent-instrumentation-api` artifact has been merged into the
  `opentelemetry-javaagent-extension-api` artifact

### 🌟 New javaagent instrumentation

- Add jboss-logmanager mdc support
  ([#5842](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5842))
- Spring-kafka single record instrumentation
  ([#5904](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5904))
- Add metrics instrumentation for grpc
  ([#5923](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5923))
- Add vertx-kafka-client instrumentation
  ([#5973](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5973),
  [#5982](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5982))
- Hide the GC runtime metrics behind an experimental config flag
  ([#5990](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5990))
- Add HikariCP connection pool metrics
  ([#6003](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6003))

### 🌟 New library instrumentation

- Add metrics instrumentation for grpc
  ([#5923](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5923))
- Add HikariCP library instrumentation
  ([#6023](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6023))

### 📈 Enhancements

- Enable span suppression by SpanKey by default
  ([#5779](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5779))
- record exception in dubbo high version
  ([#5892](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5892))
- Introduce LocalRootSpan (replacing ServerSpan)
  ([#5896](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5896))
- Add javaagent<->application context bridge for HttpRouteHolder
  ([#5838](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5838))
- Exclude spring temporary type matching class loader
  ([#5912](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5912))
- Load agent classes child first
  ([#5950](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5950))
- Avoid looking up annotation types during type matching
  ([#5906](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5906))
- Add an SPI for customizing Config just before it's set
  ([#6010](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6010))

### 🛠️ Bug fixes

- Fix duplicate class error on Android build
  ([#5882](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5882))
- Avoid npe in netty 4.1 instrumentation
  ([#5902](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5902))
- Fix logging exporter autoconfiguration issue
  ([#5928](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5928))
- fix NPE for commons-httpclient v3.1
  ([#5949](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5949))
- Exclude duplicate project classes from inst/
  ([#5957](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5957))
- Ignore known problematic jdbc wrappers
  ([#5967](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5967))
- Fix default enabled for runtime and oshi metrics
  ([#5989](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5989))
- Mitigate against another kafka leak
  ([#6021](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6021))

## Version 1.13.1 (2022-04-22)

### 🛠️ Bug fixes

- Fix duplicate class error on Android build
  ([#5882](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5882))
- Fix spring-kafka single record instrumentation
  ([#5904](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5904))

## Version 1.13.0 (2022-04-19)

### Migration notes

- Micrometer instrumentation is now automatically applied to spring-boot-actuator apps
- Some configuration properties have been renamed:
  - `otel.instrumentation.common.experimental.suppress-controller-spans`
    → `otel.instrumentation.common.experimental.controller-telemetry.enabled`
    (important: note that the meaning is inverted)
  - `otel.instrumentation.common.experimental.suppress-view-spans`
    → `otel.instrumentation.common.experimental.view-telemetry.enabled`
    (important: note that the meaning is inverted)
  - `otel.instrumentation.netty.always-create-connect-span`
    → `otel.instrumentation.netty.connection-telemetry.enabled`
  - `otel.instrumentation.reactor-netty.always-create-connect-span`
    → `otel.instrumentation.reactor-netty.connection-telemetry.enabled`
- Runtime memory metric names were updated to reflect
  [semantic conventions](https://github.com/open-telemetry/opentelemetry-specification/blob/v1.13.0/specification/metrics/semantic_conventions/runtime-environment-metrics.md#jvm-metrics)
- Micrometer library instrumentation has been deprecated as it has been moved to the core repo and
  is now published under `io.opentelemetry:opentelemetry-micrometer1-shim`
- Library instrumentation entry points have been renamed from `*Tracing` to `*Telemetry`

### 🌟 New javaagent instrumentation

- GraphQL
  ([#5583](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5583))
- JBoss Log Manager
  ([#5737](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5737))
- Apache HttpClient 5.x async client
  ([#5697](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5697))

### 🌟 New library instrumentation

- GraphQL
  ([#5583](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5583))
- Ktor 2
  ([#5797](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5797))

### 📈 Enhancements

- Elasticsearch rest client low cardinality span names
  ([#5584](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5584))
- Implement "Prometheus mode" for better micrometer->OTel->Prometheus support
  ([#5537](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5537))
- Apply micrometer instrumentation to spring-boot-actuator apps
  ([#5666](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5666))
- Sql sanitizer: handle double quoted table names
  ([#5699](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5699))
- Spring Boot Starter service-name is constant
  ([#5359](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5359))
- Update runtime memory metrics to reflect semantic conventions
  ([#5718](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5718))
- change rpc type in apache dubbo
  ([#5432](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5432))
- Rework context propagation to redisson async callback
  ([#5748](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5748))
- Resolve end strategy after WithSpan method instead of before.
  ([#5756](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5756))
- Allow scanning instrumented reactor publishers and only allow registe…
  ([#5755](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5755))
- Faster type matching
  ([#5724](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5724))
- Use UnsynchronizedAppenderBase as base class for our logback appender
  ([#5818](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5818))
- Do not set the http.route attribute in JSF instrumentations
  ([#5819](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5819))
- Use micrometer1-shim in micrometer javaagent instrumentation, deprecate library instrumentation
  ([#5820](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5820))
- Rename netty `always-create-connect-span` property to `connection-telemetry`
  ([#5834](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5834))
- Update the http.route attribute even for not sampled server spans
  ([#5844](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5844))

### 🛠️ Bug fixes

- Fix possible deadlock
  ([#5585](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5585))
- Prevent possible deadlock in downstream distro
  ([#5830](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5830))
- Limit problems from kafka iterator instrumentation thread context leak
  ([#5826](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5826))

### 🧰 Tooling

- Remove deprecated methods from instrumentation-api and library instrumentations
  ([#5575](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5575))
- Split out RpcAttributesGetter
  ([#5548](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5548))
- Run tests with jdk17
  ([#5598](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5598))
- Split out MessagingAttributesGetter
  ([#5626](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5626))
- Run Gradle and compile code with Java 17
  ([#5623](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5623))
- Rename library entrypoints to Telemetry
  ([#5624](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5624))
- Add InstrumenterBuilder.addRequestListener
  ([#5655](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5655))
- Simplify HttpUrlConnection instrumentation
  ([#5673](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5673))
- Convert all logging statements from slf4j to jul
  ([#5674](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5674))
- Allows passing an OpenTelemetry instance to registerObservers() methods.
  ([#5716](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5716))
- Make it possible to register multiple helper resources under the same…
  ([#5703](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5703))
- Split out instrumentation-api-semconv
  ([#5721](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5721))
- Move ClassNames & SpanNames to .util package
  ([#5746](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5746))
- Library instrumentation should read its version from a file
  ([#5692](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5692))
- Rename view and controller suppression config properties
  ([#5747](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5747))
- Deprecate AttributesExtractor#set() method
  ([#5749](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5749))
- Cleanup Config & ConfigBuilder API
  ([#5733](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5733))
- Instrumenter instrumentation version and schema url
  ([#5752](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5752))

## Version 1.12.1 (2022-03-17)

### 🛠️ Bug fixes

- Elasticsearch rest client low cardinality span name
  ([#5584](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5584))
- Fix possible deadlock
  ([#5585](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5585))

## Version 1.12.0 (2022-03-12)

### 🌟 New javaagent instrumentation

- Add Azure SDK instrumentation
  ([#5467](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5467))

### 📈 Enhancements

- Use repository interface name in spring data operation name
  ([#5352](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5352))
- Change the way Micrometer LongTaskTimer is bridged
  ([#5338](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5338))
- Updates to http.server_name
  ([#5369](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5369))
- Support forwarded proto field and x-forwarded-proto
  ([#5357](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5357))
- Remove server span peer name
  ([#5404](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5404))
- Add peer service support back to couchbase26
  ([#5451](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5451))
- Remove duplicative JAXRS HttpClient instrumentation
  ([#5430](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5430))
- Add experimental thread attributes for logs
  ([#5474](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5474))
- Add log4j 1.2 appender MDC capture
  ([#5475](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5475))
- Capture logback formatted message
  ([#5497](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5497))
- Add JBoss java.util.logging support
  ([#5498](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5498))
- Disable the messaging receive span telemetry by default
  ([#5500](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5500))
- Remove old experimental properties in CapturedHttpHeaders
  ([#5524](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5524))
- Avoid conflicts in Micrometer description mapping
  ([#5452](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5452))

### 🛠️ Bug fixes

- Fix another reverse lookup
  ([#5393](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5393))
- Regression in loading the prometheus exporter
  ([#5408](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5408))
- Fix muzzle failure on calls to primitive array clone
  ([#5405](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5405))
- Fix regression in spring-scheduling span name
  ([#5436](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5436))
- Fix android desugaring for HashMap.forEach
  ([#5468](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5468))
- Fix duplicate class definition of ContextDataProvider
  ([#5528](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5528))
- Do not propagate gRPC deadline when propagating OTel context via javaagent
  ([#5543](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5543))

### 🧰 Tooling

- Split out CodeAttributesGetter
  ([#5342](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5342))
- Add prometheus smoke test
  ([#5417](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5417))
- Set custom gRPC client/server span name extractor
  ([#5244](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5244))
- Split out DbClientAttributesGetter and SqlClientAttributesGetter
  ([#5354](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5354))
- Add builders for setting optional attributes on HTTP extractors
  ([#5347](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5347))
- Write http server tests in java
  ([#5501](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5501))
- Deprecate CapturedHttpHeaders and replace it with builder methods
  ([#5533](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5533))
- Update to Groovy 4
  ([#5532](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5532))

## Version 1.11.1 (2022-02-21)

### 🛠️ Bug fixes

- Regression in loading the prometheus exporter
  ([#5408](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5408))

## Version 1.11.0 (2022-02-11)

### Migration notes

- The previous release (1.10.0) deprecated the entire `io.opentelemetry.instrumentation.api.tracer`
  package in the `instrumentation-api` artifact, and the package was removed in this release.
- The javaagent `-slim` artifact has been dropped in this release, because the exporters are much
  smaller now and there is no longer a significant size difference between the `-slim` and default
  artifacts.
- The `opentelemetry-aws-lambda-1.0` has been split into two artifacts
  `opentelemetry-aws-lambda-core-1.0` and `opentelemetry-aws-lambda-events-2.2`.

### 🌟 New javaagent instrumentation

- Spring RMI
  instrumentation ([#5033](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5033))

### 🌟 New library instrumentation

- Ratpack
  httpclient ([#4787](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4787))

### 📈 Enhancements

- Add `http.route` to server spans where route was already being captured for span name
  ([#5086](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5086),
  [#5240](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5240),
  [#5242](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5242),
  [#5241](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5241),
  [#5239](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5239))
- Use RPC attributes from spec for AWS SDK
  ([#5166](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5166))
- SdkTracerProvider auto closed as separate Context Bean
  ([#5124](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5124)) (#5125)
- Support redisson 3.16.8+
  ([#5201](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5201))
- Support AWS SDK v1 request object subclasses.
  ([#5231](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5231))
- Remove slim artifact
  ([#5251](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5251))
- kotlinx-coroutines-reactor context propagation
  ([#5196](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5196))
- Log a warning whenever GlobalOpenTelemetry.set() is called
  ([#5264](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5264))
- Use `http.route` in `HttpServerMetrics`
  ([#5266](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5266))
- Use VirtualField for associating netty listener with wrapper
  ([#5282](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5282))
- Add code attributes to spring-scheduling spans
  ([#5306](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5306))
- Propagate context into redisson async callback
  ([#5313](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5313))
- Add max measurements to Micrometer Timer & DistributionSummary
  ([#5303](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5303))
- Make it possible to configure base time unit used by the Micrometer bridge
  ([#5304](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5304))
- Make HttpClientMetrics report low cardinality metrics
  ([#5319](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5319))
- Implement NamingConvention support in Micrometer bridge
  ([#5328](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5328))
- Add net.peer.name and net.peer.port attributes for grpc client span
  ([#5324](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5324))
- Split lambda instrumentation into core and events
  ([#5326](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5326))
- Add jaeger remote sampler to agent
  ([#5346](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5346))
- Weak cache optimization
  ([#5344](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5344))

### 🛠️ Bug fixes

- Several micrometer instrumentation fixes
  ([#5118](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5118))
- Fix serialisation exception on default lambda events
  ([#4724](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4724))
- NoSuchMethodError when using agent and modules (JPMS)
  ([#5169](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5169))
- Spring boot cloud gateway, context propagation broken
  ([#5188](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5188))
- Akka http server span names are always akka.request #3478
  ([#5150](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5150))
- Recover from duplicate class definition errors
  ([#5185](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5185))
- Fix serialization for quartz JobExecutionContext
  ([#5263](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5263))
- End jedis span when operation actually ends
  ([#5256](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5256))
- Enable oshi ProcessMetrics in javaagent
  ([#5281](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5281))
- Add missing return type matchers to the executor instrumentation
  ([#5294](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5294))
- Fix triggering of DNS lookup
  ([#5297](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5297))
- Avoid potential for reverse name lookup
  ([#5305](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5305))
- Fix WeakConcurrentMap memory leak
  ([#5316](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5316))
- AWS Lambda instrumentation requires jackson and lambda-events on the classpath
  ([#5326](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5326))

### 🧰 Tooling

- Convert InstrumentationTestRunner from interface to abstract class
  ([#5112](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5112))
- Parameterize VirtualField field type
  ([#5165](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5165))
- Remove old TraceUtils and use InstrumentationTestRunner#run\*Span() (almost) everywhere
  ([#5160](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5160))
- Remove deprecated tracer API
  ([#5175](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5175))
- Remove HttpServerTest#extraAttributes() method
  ([#5176](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5176))
- Rename ServerSpanNaming to HttpRouteHolder
  ([#5211](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5211))
- Don't run testLatestDeps on alpha/beta/rc versions
  ([#5258](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5258))
- Refactor HTTP attributes extractors to use composition over inheritance
  ([#5030](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5030),
  [#5194](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5194)
  [#5267](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5267))
- Refactor AttributesExtractor so that it extracts route from Context
  ([#5288](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5288))
- Publish gradle-plugins to Maven Central
  ([#5333](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5333))

## Version 1.10.1 (2022-01-27)

### 🛠️ Bug fixes

- Regression in project reactor context propagation instrumentation
  ([#5188](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5188))
- Recover from duplicate class definition errors
  ([#5185](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5185))
- StringIndexOutOfBoundsException in AWS SDK v1 when using request object subclass
  ([#5231](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5231))
- Update to OTel SDK 1.10.1
  ([#5218](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5218))

## Version 1.10.0 (2022-01-15)

### Migration notes

- The `opentelemetry-log4j-2.13.2` artifact has been renamed
  to `opentelemetry-context-data-2.16-autoconfigure`
- The `opentelemetry-logback-1.0` artifact has been renamed to `opentelemetry-logback-mdc-1.0`
- The `opentelemetry-ratpack-1.4` artifact has been renamed to `opentelemetry-ratpack-1.7`
  and only supports Ratpack 1.7 and above now

### 🌟 New javaagent instrumentation

- Logback appender instrumentation to send logs through the OpenTelemetry logging pipeline
  ([#4939](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4939),
  [#4968](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4968))
- Log4j 2.x appender instrumentation to send logs through the OpenTelemetry logging pipeline
  ([#4944](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4944),
  [#4959](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4959),
  [#4966](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4966))
- Log4j 1.2 appender instrumentation to send logs through the OpenTelemetry logging pipeline
  ([#4943](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4943))
- java.util.logging instrumentation to send logs through the OpenTelemetry logging pipeline
  ([#4941](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4941))
- Micrometer instrumentation to send micrometer metrics through the OpenTelemetry metrics pipeline
  ([#4919](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4919),
  [#5001](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5001),
  [#5017](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5017))

### 🌟 New library instrumentation

- Logback appender to send logs through the OpenTelemetry logging pipeline
  ([#4984](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4984))
- Log4j 2.x appender to send logs through the OpenTelemetry logging pipeline
  ([#4375](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4375),
  [#4907](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4907))
- Ktor instrumentation
  ([#4983](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4983))
- Micrometer instrumentation to send micrometer metrics through the OpenTelemetry metrics pipeline
  ([#5063](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5063))

### 📈 Enhancements

- Renamed Apache Dubbo and Log4j MDC library artifacts
  ([#4779](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4779))
- Added http4 handler to camel instrumentation
  ([#4650](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4650))
- Capture servlet request parameters
  ([#4703](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4703))
- Propagate Context instead of SpanContext in Kafka instrumentation
  ([#4806](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4806))
- Instrument ContextPropagationOperator to bridge lib/agent calls
  ([#4786](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4786))
- Add shouldStart() call to Armeria server instrumentation
  ([#4843](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4843))
- Capture RPC metrics
  ([#4838](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4838))
- Update log4j library base version
  ([#4914](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4914))
- Rename log4j-thread-context modules to log4j-context-data
  ([#4957](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4957))
- Support latest oshi version
  ([#4993](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4993))
- Support latest RxJava version
  ([#4954](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4954))
- Create producer span from spring integration instrumentation
  ([#4932](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4932))
- Capture servlet request parameters at the end of the request
  ([#5019](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5019))
- Don't extract deprecated cassandra keyspace attribute
  ([#5041](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5041))
- Add OTLP logs exporters
  ([#5060](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5060),
  [#5088](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5088))
- End redisson span when the operation actually ends
  ([#5073](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5073))
- Don't capture high-cardinality http.target as metrics attribute
  ([#5081](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5081))

### 🛠️ Bug fixes

- Remove virtual field interfaces from reflection results
  ([#4722](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4722))
- Work around jvm crash on early 1.8
  ([#4345](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4345))
- Fix http.url handing in vert.x 3 http client
  ([#4739](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4739))
- Capture missing RMI spans
  ([#4764](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4764))
- Avoid crashing on early 1.8 openjdk vms
  ([#4789](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4789))
- Fix Quarkus correlation
  ([#4883](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4883))
- Fix RabbitMQ instrumentation consumption on empty headers
  ([#4903](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4903))
- Fix NPE in Apache HttpClient 4.0 instrumentation
  ([#4913](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4913))
- Fix NPE in RocketMQ instrumentation
  ([#4901](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4901))
- Fix time units in HTTP & RPC metrics
  ([#4963](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4963))
- Fix some gauge types
  ([#4962](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4962))
- Fix context propagation for undertow async dispatch
  ([#4950](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4950))
- Fix redefinition failure on openj9
  ([#5009](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5009))
- Fix NPE in rmi server instrumentation
  ([#5042](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/5042))

### 🧰 Tooling

- Merge start and end time extractors
  ([#4692](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4692))
- Move cache implementations to internal package
  ([#4746](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4746))
- Reorganize shared servlet code (intro
  to [#4317](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4317))
  ([#4785](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4785))
- Move `AppServerBridge` and `MappingResolver` to `servlet-common:bootstrap`
  ([#4817](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4817))
- Move `ServletContextPath` to `servlet-common:bootstrap`
  ([#4824](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4824))
- Expose AutoConfiguredOpenTelemetrySdk to AgentListener
  ([#4831](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4831))
- Deprecate the Tracer API
  ([#4868](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4868))
- Remove ConfigPropertiesAdapter as it's no longer needed
  ([#4888](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4888))
- Refactor `ServerSpanNaming` (in preparation for `http.route`)
  ([#4852](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4852))
- Move SpanKey to internal package
  ([#4869](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4869))

## Version 1.9.2 (2022-01-07)

### 🛠️ Bug fixes

- Fix reactor-netty memory/connection leak
  ([#4867](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4867))

## Version 1.9.1 (2021-12-01)

### 🛠️ Bug fixes

- Shade class references within AWS SDK service files
  ([#4752](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4752))

## Version 1.9.0 (2021-11-25)

### 📈 Enhancements

- Don't report 400 level as error for SERVER spans
  ([#4403](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4403))
- Netty instrumentation now captures `http.scheme`
  ([#4446](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4446),
  [#4576](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4576))
- Stabilize HTTP headers capturing configuration property names
  ([#4459](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4459))
- Add metrics to remaining http instrumentation
  ([#4541](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4541))
- Remove hibernate session spans
  ([#4538](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4538))
- Support Jedis 4
  ([#4555](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4555))
- Trace DNS resolution in Netty 4.1 and reactor-netty
  ([#4587](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4587),
  [#4627](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4627))
- Update garbage collector instruments to be async counters
  ([#4600](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4600))
- Update HTTP metrics view to match the specification
  ([#4556](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4556))
- Improve Spring Integration interceptor ordering
  ([#4602](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4602))
- Support Restlet 2.0
  ([#4535](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4535))
- Improved capture of couchbase queries
  ([#4615](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4615))
- Trace SSL handshakes in netty 4.0 and 4.1
  ([#4635](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4635),
  [#4604](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4604))
- Capture exception on finatra controller spans
  ([#4669](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4669))
- Capture exception on async servlet spans
  ([#4677](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4677))
- Automatic AWS library instrumentor
  ([#4607](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4607))
- Support spring boot 2.6.0
  ([#4687](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4687))

### 🛠️ Bug fixes

- Preserve caught netty exception in the context instead of calling end()
  ([#4413](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4413))
- Extract net attributes both on start and on end in netty HTTP client
  ([#4420](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4420))
- Capture metric dimensions from end attributes also
  ([#4430](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4430))
- Avoid logging servlet3 muzzle failure when running on servlet2
  ([#4474](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4474))
- Fix JettyHttpClient9TracingInterceptor NullPointerException
  ([#4527](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4527))
- Fix context propagation in tomcat thread pool
  ([#4521](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4521))
- Add missing java.util.logging.Logger methods to PatchLogger
  ([#4540](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4540))
- Fix ClassCircularityError when running with security manager
  ([#4557](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4557))
- Fix sun.misc.Unsafe generation on Java 17
  ([#4558](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4558))
- Fix IndexOutOfBounds in apache http clients
  ([#4575](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4575))
- Fix RMI instrumentation on Java 17
  ([#4577](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4577))
- Fix Spring Integration instrumentation name
  ([#4601](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4601))
- Capture instrumentation version provided by application tracer correctly when using agent
  ([#4630](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4630))
- Ensure that netty 4.0 instrumentation is not applied to 4.1
  ([#4626](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4626))
- Fix muzzle problems happening when netty without SSL is used
  ([#4631](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4631))
- Fix memory leak when using ktor-client-java
  ([#4637](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4637))
- Fix quartz instrumentation name
  ([#4657](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4657))
- Fix Spring Integration context leak
  ([#4673](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4673))
- Fix exemplars
  ([#4678](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4678))
- Fix and enforce Android support
  ([#4671](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4671),
  [#4667](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4667)
  [#4505](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4505))

### 🧰 Tooling

- Migrate to Spock 2
  ([#4458](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4458))
- Rename `newBuilder()` to `builder()`
  ([#4475](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4475))
- Sync gradle-plugins version with main project
  ([#4248](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4248))
- Muzzle match only once in each class loader
  ([#4543](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4543))
- Inject helper resources only once
  ([#4573](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4573))
- Improve muzzle check for constructors
  ([#4591](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4591))
- Add version to the InstrumenterBuilder and Instrumenter
  ([#4611](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4611))
- Add a ClassAndMethod class to Instrumentation API
  ([#4619](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4619))
- Implement a dedicated reactor-netty 1.0 instrumentation
  ([#4662](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4662))
- Remove gRPC dependency for export
  ([#4674](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4674))
- Start using Gradle Enterprise instance
  ([#4663](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4663))

## Version 1.8.0 - Bad Release

Due to an issue in the publishing infrastructure, a bad release was published as 1.8.0. Do not use
it.

## Version 1.7.0 (2021-10-19)

### 📈 Enhancements

- Change the default javaagent artifact to have exporters, introduce new `-slim` artifact,
  and deprecate the `-all` artifact
  ([#4106](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4106))
- Support jlinked images without jdk.unsupported module
  ([#4154](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4154))
  ([#4124](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4124))
- Added experimental option to suppress messaging receive spans
  ([#4187](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4187))
  ([#4204](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4204))
- Refine 404 handling in Restlet instrumentation
  ([#4206](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4206))
- Remove dynamo db.name attribute
  ([#4208](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4208))
- Remove capturing http.url in server instrumentation in favor of http.scheme, http.host
  and http.target
  ([#4209](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4209))
- Intern db info to reduce memory consumption
  ([#4263](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4263))
- Better JAX-RS async cancel handling
  ([#4279](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4279))
- Extract HTTP request/response headers as span attributes
  ([#4237](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4237),
  [#4309](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4309),
  [#4320](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4320),
  [#4321](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4321),
  [#4328](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4328),
  [#4395](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4395))
- Support kafka streams 3
  ([#4236](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4236))
- AWS lambda - improvements in custom type handling in wrappers, SQS event wrapper added
  ([#4254](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4254))
- Add code attributes to quartz spans
  ([#4332](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4332))
- Collect more attributes from servlet instrumenter
  ([#4356](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4356))
- Allow reactor instrumentation to pick up spans from reactor context
  ([#4159](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4159))
- Hide virtual field accessor interface methods from reflection
  ([#4390](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4390))

### 🛠️ Bug fixes

- Fix spring context reload issue
  ([#4051](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4051))
- Remove incorrect lettuce db.statement attribute
  ([#4160](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4160))
- Fix tomcat async spans
  ([#4339](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4339))

### 🧰 Tooling

- Add start/end time to RequestListener
  ([#4155](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4155))
- Add context customizer hook to Instrumenter API
  ([#4167](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4167))
- Transform lambda classes
  ([#4182](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4182))
- Separate HTTP client/server AttributesExtractors
  ([#4195](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4195))
- Introduce muzzle-specific interface to InstrumentationModule
  ([#4207](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4207))
- Make it possible to use InstrumentationContext (now VirtualField) from library instrumentation
  ([#4218](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4218))
- Add functionality to generate API changes
  ([#4285](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4285))
- Split NetAttributesExtractor into NetClientAttributesExtractor and NetServerAttributesExtractor
  ([#4287](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4287))
- Back VirtualField with a volatile field
  ([#4355](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4355))
- Convert AttributesExtractor to interface
  ([#4363](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4363))
- Rename some `*InstrumenterBuilder` classes to `*InstrumenterFactory`
  ([#4391](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4391))
- rename `newBuilder()` to `builder()`
  ([#4407](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4407))

## Version 1.6.2 (2021-09-30)

### 🛠️ Bug fixes

- The 1.6.1 patch release was not backwards compatible with javaagent extensions built against 1.6.0
  ([#4245](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4245))

## Version 1.6.1 (2021-09-29)

### 🛠️ Bug fixes

- Fix possible JDBC instrumentation deadlock
  ([#4191](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4191))

## Version 1.6.0 (2021-09-18)

### 🌟 New javaagent instrumentation

- Add instrumentation for Quartz 2.0
  ([#4017](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4017))
- Restlet instrumentation
  ([#3946](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3946))

### 🌟 New library instrumentation

- Add instrumentation for Quartz 2.0
  ([#4017](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4017))
- Restlet instrumentation
  ([#3946](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3946))

### 📈 Enhancements

- Extract Jedis 3 net attributes from InetSocketAddress
  ([#3912](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3912))
- Add option to suppress controller and view spans
  ([#3865](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3865))
- Capture http.user_agent for AsyncHttpClient
  ([#3930](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3930))
- Instrument spring-kafka batch message listeners
  ([#3922](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3922))
- Use unsafe to inject classes to the bootstrap class loader
  ([#4026](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4026))
- Some performance optimizations
  ([#4004](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4004),
  [#4006](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4006),
  [#4008](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4008),
  [#4013](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4013),
  [#4014](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4014))
- Do not fallback to net attributes for http.client_ip
  ([#4063](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4063))
- Start a CONSUMER span for Kafka poll()
  ([#4041](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4041))
- Support otlp http exporter
  ([#4068](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4068))
- Better grpc events
  ([#4098](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4098))

### 🛠️ Bug fixes

- Bridge span keys defined in instrumentation-api
  ([#3911](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3911))
- Hide generated fields and methods from reflection
  ([#3948](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3948))
- Remove invalid message header
  ([#3958](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3958))
- Fix memleak in the Netty ChannelPipeline instrumentation
  ([#4053](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4053))
- Fix grpc instrumentation of callbacks
  ([#4097](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4097))
- Bridge trace builder
  ([#4090](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4090))
- Remove original handler when removelast in netty
  ([#4123](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4123))

### 🧰 Tooling

- Deprecate old extensions
  ([#3825](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3825))
- Add request parameter to EndTimeExtractor
  ([#3947](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3947))
- Make Config behave exactly as SDK DefaultConfigProperties
  ([#4035](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4035))
- Fix extension shading
  ([#4064](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4064))
- Add error parameter to EndTimeExtractor and AttributesExtractor#onEnd()
  ([#3988](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3988))
- Allow adding multiple ContextStore fields to one key class
  ([#4067](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4067),
  [#4084](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4084),
  [#4110](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4110))

## Version 1.5.3 (2021-08-31)

### 🛠️ Bug fixes

- Fix parsing of unclean map values in Config
  ([#4032](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/4032))

## Version 1.5.2 (2021-08-27)

### 🛠️ Bug fixes

- Fix unbounded metrics cardinality, which in particular causes memory leak when not using any
  metrics exporter
  ([#3972](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3972))

## Version 1.5.1 (2021-08-25)

### 🛠️ Bug fixes

- Fix broken Android level 21-25 support in OkHttp 3 library instrumentation
  ([#3910](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3910))
- Fix maven central pom file for the opentelemetry-javaagent artifact
  ([#3929](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3929))
- Fix maven central pom file for the opentelemetry-agent-for-testing artifact
  ([#3935](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3935))

## Version 1.5.0 (2021-08-22)

### 🌟 New library instrumentation

- Library instrumentation for Apache HTTP Client 4.3
  ([#3623](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3623))
- Library instrumentation for Ratpack server
  ([#3749](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3749))

### 📈 Enhancements

- Support Couchbase 3.2.0
  ([#3645](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3645))
- Handle port and IPv6 in forwarded headers
  ([#3651](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3651))
- Set real Hibernate span name on method entry to help samplers
  ([#3603](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3603))
- Reduce overhead of unsampled requests
  ([#3681](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3681))
- Sanitize SQL in Apache Camel instrumentation
  ([#3683](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3683),
  [#3717](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3717))
- Add option to create span on new netty connection
  ([#3707](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3707))
- Propagate context into jdk http client callback
  ([#3719](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3719))
- Instrument Tomcat executor to support async servlets in new Tomcat 9.0.52 release
  ([#3789](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3789))
- Add otlp-logging exporter
  ([#3807](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3807))
- Add new option to support capturing nested client spans of different types
  ([#3691](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3691))
- Propagate context to lettuce callbacks
  ([#3839](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3839))
- Instrument ForkJoinTask.fork()
  ([#3849](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3849))
- Implement a Call.Factory for okhttp 3.x+ library instrumentation
  ([#3812](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3812))
- Record exception in Dubbo instrumentation
  ([#3851](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3851))
- Propagate context to elasticsearch callbacks
  ([#3858](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3858),
  [#3861](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3861))
- Added Vertx http client 4 instrumentation
  ([#3665](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3665))
- Make empty agent bridged context equal root context
  ([#3869](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3869))

### 🛠️ Bug fixes

- Fix OkHttp 3 correlation when using callback under concurrency
  ([#3669](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3669),
  [#3676](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3676))
- Fix Netty span not captured on read timeout
  ([#3613](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3613))
- Fix Netty connection failure handling when listener is lambda
  ([#3569](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3569))
- Fix NullPointerException in Apache HttpAsyncClient instrumentation
  ([#3692](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3692))
- Fix NullPointerException in Tomcat instrumentation
  ([#3705](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3705))
- Fix Apache HttpClient telemetry when host and absolute URI are used
  ([#3694](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3694))
- Fix JDK http client should propagate even when sampled out
  ([#3736](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3736))
- Limit netty exception capture to netty spans
  ([#3809](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3809))
- Fix jetty httpclient returning empty response when instrumented
  ([#3831](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3831),
  [#3833](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3833))
- Don't clobber user decorators in Armeria client instrumentation
  ([#3873](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3873))
- Use valid Java identifiers for message keys
  ([#3863](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3863))
- Fix ClassNotFoundException: HandlerMappingResourceNameFilter in some ear deployments
  ([#3718](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3718))

### 🧰 Tooling

- Improve extension sample documentation and add it to the README file
  ([#3656](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3656))
- Extract muzzle check plugin
  ([#3657](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3657))
- Move instrumentation specific classes out of javaagent-instrumentation-api
  ([#3604](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3604))
- Publish muzzle plugins to Gradle Plugin Portal
  ([#3720](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3720),
  [#3763](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3763))
- Fill `http.client_ip` in ServerInstrumenter
  ([#3756](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3756))
- Fix printMuzzleReferences gradle task
  ([#3808](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3808))
- Introduce stable property for external extensions
  ([#3823](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3823))
- Run tests on j9 JVM during CI
  ([#3764](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3764))
- Support looking up a ContextStore from outside of Advice
  ([#3827](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3827))
- Deprecate MetricExporterFactory
  ([#3862](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3862))
- Don't pass configuration to SDK autoconfigure through system props
  ([#3866](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3866))
- Rename Config get\*Property() methods to get\*()
  ([#3881](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3881))

## Version 1.4.0 (2021-07-20)

### ☢️ Behavioral changes

- Updated all instrumentation names to `io.opentelemetry.{libName}-{libVersion}`
  ([#3411](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3411))
- Updated RabbitMQ to follow semantic conventions
  ([#3425](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3425))

### 🌟 New javaagent instrumentation

- Jetty 9 HTTP client instrumentation
  ([#3079](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3079))

### 🌟 New library instrumentation

- Jetty 9 HTTP client instrumentation
  ([#3079](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3079))
- Jdbc instrumentation
  ([#3367](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3367))

### 📈 Enhancements

- Make @RabbitListener propagate context properly
  ([#3339](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3339))
- Add peer.service to grpc javaagent instrumentation
  ([#3357](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3357))
- Propagate context to cassandra4 callbacks
  ([#3371](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3371))
- Update Armeria instrumentation to support new Armeria 1.9.0 release
  ([#3407](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3407))
- Context propagation for ratpack Execution.fork()
  ([#3416](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3416))

### 🛠️ Bug fixes

- Fix Kafka stream instrumentation to support Kafka 2.6 and above
  ([#3438](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3438))
- Fix Dubbo trace/span cross-process propagation
  ([#3442](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3442))
- Fix `peer.service` configuration mapping
  ([#3378](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3378))

### 🧰 Tooling

- Hide Config#create() method and use builder everywhere
  ([#3338](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3338))
- Ignore task classes using IgnoredTypesConfigurer
  ([#3380](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3380))
- Exclude duplicate classes from final jars
  ([#3432](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3432),
  [#3430](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3430))
- Port AsyncSpanEndStrategy to Instrumenter API
  ([#3262](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3262))
- Rename `opentelemetry-javaagent-api` artifact to `opentelemetry-javaagent-instrumentation-api`
  ([#3513](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3513))

## Version 1.3.1 (2021-06-23)

### 🛠️ Bug fixes

- Fix incorrect dependency in published BOM
  ([#3376](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3376))
- Fix UnsupportedOperationException with reactor-rabbitmq
  ([#3381](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3381))
- Fix Spring JMS not being instrumented
  ([#3359](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3359))

## Version 1.3.0 (2021-06-17)

### ☢️ Behavioral changes

- Update agent logger prefix
  ([#3007](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3007))
- Remove khttp instrumentation
  ([#3087](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3087))
- Enable akka actor instrumentation by default
  ([#3173](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3173))

### 🌟 New javaagent instrumentation

- Spring Integration javaagent instrumentation
  ([#3295](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3295))

### 🌟 New library instrumentation

- Spring Integration library instrumentation
  ([#3120](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3120))

### 📈 Enhancements

- Support peer-service-mapping in OkHttp3 instrumentation
  ([#3063](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3063))
- Low cardinality span names for Hibernate spans
  ([#3106](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3106))
- Propagate context to armeria callbacks
  ([#3108](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3108))
- Add attributes to netty connection failure span
  ([#3115](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3115))
- Defer initialization of OpenTelemetry in spring-boot-autoconfigure
  ([#3171](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3171))
- Support couchbase 3.1.6
  ([#3194](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3194))
- New experimental support for agent extensions
  ([#2881](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2881),
  [#3071](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3071),
  [#3226](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3226),
  [#3237](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3237))
- Propagate context to akka http callbacks
  ([#3263](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3263))

### 🛠️ Bug fixes

- Remove Netty instrumented handler wrapper when original handler is removed
  ([#3026](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3026))
- Fix memory leak when Netty handler is a lambda
  ([#3059](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3059))
- Fix race condition on Undertow
  ([#2992](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2992))
- Remove db.connection_string from redis instrumentation
  ([#3094](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3094))
- Fix context propagation leak in Akka instrumentation
  ([#3099](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3099))
- Fix webflux handler span sporadically not ending
  ([#3150](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3150))
- End span on cancellation of subscription to reactive publishers
  ([#3153](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3153))
- End span on cancellation of Guava future
  ([#3175](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3175))
- Create Netty connection failure span only when first operation fails
  ([#3228](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3228))
- Internal instrumentation should always be enabled by default
  ([#3257](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3257))
- Fix context propagation leak in Akka HTTP instrumentation
  ([#3264](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3264))
- Only include exporters in the `-all` jar
  ([#3286](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3286))
- Fix ForkJoinPool sometimes not instrumented
  ([#3293](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3293))

### 🧰 Tooling

- Migrate MuzzlePlugin to Java
  ([#2996](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2996),
  [#3017](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3017))
- Refactor TypeInstrumentation#transformers() method
  ([#3019](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3019))
- Change a couple of Longs to Integers in Instrumenter API
  ([#3043](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3043))
- Add peer.service to Instrumenter API
  ([#3050](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3050))
- Add response type parameter to db attributes extractor
  ([#3093](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3093))
- Add optimized Attributes implementation for Instrumenter
  ([#3136](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3136))
- Rename ComponentInstaller to AgentListener and add #order() method
  ([#3182](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3182))
- Update ByteBuddy
  ([#3254](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3254))
- Introduce IgnoredTypesConfigurer SPI to enable defining per-module ignores
  ([#3219](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3219))
- Extract agent shadow configuration to conventions script
  ([#3256](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3256))
- Deprecate SpanExporterFactory in favor of ConfigurableSpanExporterProvider
  ([#3299](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3299))
- Refactor span names class
  ([#3281](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3281))
- Move http client/server testing dependencies to internal package
  ([#3305](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/3305))

## Version 1.2.0 (2021-05-14)

### ☢️ Behavioral changes

- AWS lambda flush timeout raised to 10 seconds
  ([#2855](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2855))
- `SERVER` span names improved for Spring MVC, Grails, Wicket, and Struts
  ([#2814](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2814))
- `SERVER` span names improved for Servlet filters
  ([#2887](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2887))
- `SERVER` span names improved for Resteasy
  ([#2900](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2900))
- `SERVER` span names improved for Jersey and CXF
  ([#2919](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2919))
- JAX-RS `@ApplicationPath` annotation captured as part of `SERVER` span name
  ([#2824](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2824))
- RequestDispatcher `forward()` and `include()` internal spans removed
  ([#2816](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2816))
- Raised gRPC min version supported to 1.6 in order to use new gRPC context bridge API
  ([#2948](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2948))

### 🌟 New javaagent instrumentation

- RxJava 3
  ([#2794](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2794))

### 🌟 New library instrumentation

- RxJava 3
  ([#2794](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2794))

### 📈 Enhancements

- Support sub-millisecond precision for start/end times on Java 9+
  ([#2600](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2600))
- `@WithSpan` async support added for methods returning async Reactor 3.x types
  ([#2714](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2714))
- `@WithSpan` async support added for methods returning Guava ListenableFuture
  ([#2811](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2811))
- Semantic attributes `code.namespace` and `code.function` captured on JAX-RS internal spans
  ([#2805](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2805))
- Context propagated to reactor-netty callbacks
  ([#2850](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2850))

### 🛠️ Bug fixes

- gRPC context bridging issues
  ([#2564](https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/2564),
  [#2959](https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/2959))
- URL credentials of the form `https://username:password@www.example.com/` no longer captured
  ([#2707](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2707))
- Spring MVC instrumentation can cause Spring MVC to misroute requests under some conditions
  ([#2815](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2815))
- RxJava2 NoSuchFieldError
  ([#2836](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2836))
- Duplicate http client tracing headers
  ([#2842](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2842))
- Netty 4.1 listeners could not be removed by application
  ([#2851](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2851))
- NPE caused in gRPC ProtoReflectionService
  ([#2876](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2876))
- Context leak when using Ratpack
  ([#2910](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2910))
- Context leak when using Jetty
  ([#2920](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2920))
- Servlet instrumentation overwrites setStatus that was set manually earlier
  ([#2929](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2929))
- Spans not captured on interface default methods annotated with JAX-RS annotations
  ([#2930](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2930))

### 🧰 Tooling

- Documented how to write InstrumentationModule line by line
  ([#2793](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2793))
- New instrumenter API used in JMS instrumentation
  ([#2803](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2803))
- Instrumenter API improvements
  ([#2860](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2860))
- Muzzle checks whether used fields are actually declared somewhere
  ([#2870](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2870))
- Extracted javaagent-extension-api from tooling & spi
  ([#2879](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2879))
  - You no longer have to depend on the `javaagent-tooling` module to implement custom
    instrumentations: a new `javaagent-extension-api` module was introduced, containing all the
    necessary instrumentation classes and interfaces;
  - `InstrumentationModule` and `TypeInstrumentation` were moved to
    the `io.opentelemetry.javaagent.extension.instrumentation` package;
  - `AgentElementMatchers`, `ClassLoaderMatcher` and `NameMatchers` were moved to
    the `io.opentelemetry.javaagent.extension.matcher` package;
  - A new SPI `AgentExtension` was introduced: it replaces `ByteBuddyAgentCustomizer`;
  - `InstrumentationModule#getOrder()` was renamed to `order()`;
  - `InstrumentationModule#additionalHelperClassNames()` has been removed;
    use `isHelperClass(String)` instead if you use the muzzle compile plugin. If you're not using
    muzzle, you can override `getMuzzleHelperClassNames()` directly instead;
  - `InstrumentationModule#getAllHelperClassNames()` has been removed; you can
    call `getMuzzleHelperClassNames()` to retrieve all helper class names instead.

## Version 1.1.0 (2021-04-14)

### ☢️ Behavioral changes

- Update servlet attribute names for log injection, from `traceId` and `spanId` to `trace_id` and
  `span_id`
  ([#2593](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2593))
- Renamed `runtime.jvm.gc.collection` metric to `runtime.jvm.gc.time`
  ([#2616](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2616))

### 🌟 New javaagent instrumentation

- Elasticsearch 7
  ([#2514](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2514),
  [#2528](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2528))
- Couchbase 3.1
  ([#2524](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2524))
- Grails
  ([#2512](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2512))
- RocketMQ
  ([#2263](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2263))
- Lettuce 6
  ([#2589](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2589))
- Servlet 5
  ([#2609](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2609))
- Vaadin web framework
  ([#2619](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2619))
- GWT
  ([#2652](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2652))
- Tapestry web framework
  ([#2690](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2690))
- `@WithSpan` support for methods returning CompletableFuture
  ([#2530](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2530))
- `@WithSpan` support for methods returning async RxJava 2 types
  ([#2530](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2530))

### 🌟 New library instrumentation

- Library instrumentation for AWS SDK v1
  ([#2525](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2525))
- Library instrumentation for Lettuce 5.1
  ([#2533](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2533))
- RocketMQ
  ([#2263](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2263))
- Lettuce 6
  ([#2589](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2589))
- Spring Boot Autoconfigure support for `@WithSpan` methods returning CompletableFuture
  ([#2618](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2618))
- Spring Boot Autoconfigure support for `@WithSpan` methods returning async RxJava 2 types
  ([#2530](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2530))

### 📈 Improvements

- Move attributes to span builder for use by samplers
  ([#2587](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2587))
- Apache Camel - SNS propagation
  ([#2562](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2562))
- Apache Camel - S3 to SQS propagation
  ([#2583](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2583))
- Added `runtime.jvm.gc.count` metric
  ([#2616](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2616))
- Support reactor netty `HttpClient.from` construction
  ([#2650](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2650))
- Improve akka instrumentation
  ([#2737](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2737))
- Record internal metric for SQL cache misses
  ([#2747](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2747))
- End Netty 4.1 client and server spans when the response has completed, instead of when the
  response has started
  ([#2641](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2641))

### 🛠️ Bug fixes

- Fix RestTemplateInterceptor so that it calls endExceptionally() on exception
  ([#2516](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2516))
- Fix app failure under Eclipse OSGi
  ([#2521](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2521))
- Fix undertow span ending too early
  ([#2560](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2560))
- Fix context leak in AWS SDK 2.2 and RocketMQ instrumentations
  ([#2637](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2637))
- Fix hang when a webflux http request is made inside of another webflux http request
  (e.g. auth filter)
  ([#2646](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2646))
- Fix `@WithSpan` instrumentation breaking Java 6 classes
  ([#2699](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2699))
- Fix context not propagated over JMS when explicit destination used
  ([#2702](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2702))
- Fix StackOverflowError if jdbc driver implementation of Connection getMetaData calls Statement
  execute
  ([#2756](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2756))

### 🧰 Tooling

- Make muzzle reference creation package(s) configurable
  ([#2615](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2615))
- Instrumentations now can skip defining context store manually
  ([#2775](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2775))
- New Instrumenter API
  ([#2596](https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/2596))
