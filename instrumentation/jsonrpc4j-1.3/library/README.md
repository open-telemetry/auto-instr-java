# Library Instrumentation for jsonrpc4j 1.3.3+

Provides OpenTelemetry instrumentation for [jsonrpc4j](https://github.com/briandilley/jsonrpc4j) server.

## Quickstart

### Add the following dependencies to your project

Replace `OPENTELEMETRY_VERSION` with the [latest release](https://search.maven.org/search?q=g:io.opentelemetry.instrumentation%20AND%20a:opentelemetry-jsonrpc4j-1.3).

For Maven, add the following to your `pom.xml` dependencies:

```xml
<dependencies>
  <dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-jsonrpc4j-1.3</artifactId>
    <version>OPENTELEMETRY_VERSION</version>
  </dependency>
</dependencies>
```

For Gradle, add the following to your dependencies:

```groovy
implementation("io.opentelemetry.instrumentation:opentelemetry-jsonrpc4j-1.3:OPENTELEMETRY_VERSION")
```

### Usage

The instrumentation library provides the implementation of `InvocationListener` to provide OpenTelemetry-based spans and context propagation.

```java
// For server-side, attatch the invocation listener to your service.
JsonRpcBasicServer configureServer(OpenTelemetry openTelemetry, JsonRpcBasicServer server) {
  JsonRpcTelemetry jsonrpcTelemetry = JsonRpcTelemetry.create(openTelemetry);
  return server.setInvocationListener(jsonrpcTelemetry.newServerInvocationListener());
}
```
