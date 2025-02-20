# Library Instrumentation for Junit version 5.0 and higher

This package contains libraries to help instrument Junit.

## Quickstart

### Add these dependencies to your project

Replace `OPENTELEMETRY_VERSION` with the [latest
release](https://search.maven.org/search?q=g:io.opentelemetry.instrumentation%20AND%20a:opentelemetry-ktor-2.0).

For Maven, add to your `pom.xml` dependencies:

```xml
<dependencies>
  <dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>junit-5.0</artifactId>
    <version>OPENTELEMETRY_VERSION</version>
  </dependency>
</dependencies>
```

For Gradle, add to your dependencies:

```groovy
implementation("io.opentelemetry.instrumentation:junit-5.0:OPENTELEMETRY_VERSION")
```

## Usage

## Initializing instrumentation

Initialize instrumentation by using the `JunitOpenTelemetryTracing` annotation.

```java
@JunitOpenTelemetryTracing
class MyTestClass {

  @Test
  void myTest() {
    //...
  }
}
```
