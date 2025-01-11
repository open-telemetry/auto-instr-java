# Running the tests

## Java versions

Open Telemetry Auto Instrumentation's minimal supported version is java 8.
All jar files that we produce, unless noted otherwise, have bytecode
compatibility with the java 8 runtime. Our test suite is executed against
java 8, all LTS versions and the latest non-LTS version.

Some libraries that we auto-instrument may have higher minimal requirements.
In these cases, we compile and test the corresponding auto-instrumentation with
higher java versions as required by the libraries. The resulting classes will
have a higher bytecode level, but since it will match the library's java version,
no runtime problems arise.

## Instrumentation tests

Executing `./gradlew instrumentation:test` will run tests for all supported
auto-instrumentations using that java version which runs the Gradle build
itself. These tests usually use the minimal supported version of the
instrumented library.

### Executing tests with specific java version

We run all tests on `Java 21` by default, along with Java 8, 11, 17, and 23. To run on
a specific version, set the `testJavaVersion` gradle property to the desired major
version, e.g., `./gradlew test -PtestJavaVersion=8`, `./gradlew test -PtestJavaVersion=23`.
If you don't have a JDK of these versions installed, Gradle will automatically download
it for you.

### Executing tests against the latest versions of libraries under instrumentation

This is done as part of the nightly build in order to catch when a new version of a library is
released that breaks our instrumentation tests.

To run these tests locally, add `-PtestLatestDeps=true` to your existing `gradlew` command line.

### Executing single test

Executing `./gradlew :instrumentation:<INSTRUMENTATION_NAME>:test --tests <TEST FILE NAME>` will run only the selected test.

### Debugging tests

If you have trouble debugging tests in IntelliJ IDEA (because the classpath is not correct),
you can use remote debugging:

1. Run the tests with the `--debug-jvm` flag.
2. Create a new remote debug configuration in IntelliJ IDEA.

This example shows how to debug a single test:

```shell
./gradlew :instrumentation:spring:spring-batch-3.0:javaagent:test --tests "io.opentelemetry.javaagent.instrumentation.spring.batch.v3_0.basic.JavaConfigBatchJobTest.shouldTraceTaskletJobStep" --debug-jvm
```

### How to prevent linting and formatting warnings from failing tests

During local development, you may want to ignore lint warnings when running tests.

To ignore warnings, formatting issues, or other non-fatal issues in tests, use

```
./gradlew test -Ddev=true -x spotlessCheck -x checkstyleMain
```

The `dev` flag will ignore warnings in tests.

## Smoke tests

The smoke tests are not run as part of a global `test` task since they take a long time and are
not relevant for most contributions. Explicitly specify `:smoke-tests:test` to run them.

If you need to run a specific smoke test suite:

```
./gradlew :smoke-tests:test -PsmokeTestSuite=payara
```

If you are on Windows and you want to run the tests using linux containers:

```
USE_LINUX_CONTAINERS=1 ./gradlew :smoke-tests:test -PsmokeTestSuite=payara
```

If you want to run a specific smoke test:

```
./gradlew :smoke-tests:test --tests '*SpringBootSmokeTest*'
```

## OpenTelemetry starter smoke tests

Smoke tests for the [OpenTelemetry Spring starter](../../instrumentation/spring/starters/spring-boot-starter/README.md)
can be executed in a JVM (`./gradlew smoke-tests-otel-starter:test`) or as Spring Native tests (`./gradlew smoke-tests-otel-starter:nativeTest`).

## GraalVM native test

To execute all the instrumentation tests runnable as GraalVM native executables:

```
./gradlew nativeTest
```

[A Github workflow](../../.github/workflows/native-tests-daily.yml) executes the native tests every day.

## Docker disk space

Some of the instrumentation tests (and all of the smoke tests) spin up docker containers via
[testcontainers](https://www.testcontainers.org/). If you run out of space, you may wish to prune
old containers, images and volumes using `docker system prune --volumes`.

## Configuring Docker alternative

For some container environments, such as rootless Podman or a remotely hosted Docker,
testcontainers may need additional configuration to successfully run the tests.
The following environment variables can be used for configuration:
- `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE` - The location of the Docker socket on the host. Default is `/var/run/docker.sock`
- `TESTCONTAINERS_HOST_OVERRIDE` - The hostname used for container-to-container communication. Default Docker is `localhost`, but rootless Podman uses `host.containers.internal`

# Troubleshooting CI Test Failures

See [Troubleshooting CI Test Failures](../../CONTRIBUTING.md#troubleshooting-pr-build-failures) for common issues and solutions.

# Debugging

For information on debugging tests or instrumentation, see [Debugging](debugging.md).
