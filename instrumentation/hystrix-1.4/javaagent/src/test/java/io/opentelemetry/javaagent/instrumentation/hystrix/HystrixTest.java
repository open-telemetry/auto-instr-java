/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.hystrix;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_STACKTRACE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_TYPE;
import static org.junit.jupiter.api.Named.named;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HystrixTest {

  @RegisterExtension
  protected static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @ParameterizedTest
  @MethodSource("provideCommandActionArguments")
  void testCommands(Function<HystrixCommand<String>, String> operation) {

    HystrixCommand<String> command =
        new HystrixCommand<String>(setter()) {
          @Override
          protected String run() throws Exception {
            return tracedMethod();
          }

          private String tracedMethod() {
            testing.runWithSpan("tracedMethod", () -> {});
            return "Hello!";
          }
          ;
        };

    String result = testing.runWithSpan("parent", () -> operation.apply(command));
    assertThat(Objects.equals(result, "Hello!")).isTrue();

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent(),
                span ->
                    span.hasName("ExampleGroup.HystrixTest$1.execute")
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(stringKey("hystrix.command"), "HystrixTest$1"),
                            equalTo(stringKey("hystrix.group"), "ExampleGroup"),
                            equalTo(booleanKey("hystrix.circuit_open"), false)),
                span -> span.hasName("tracedMethod").hasParent(trace.getSpan(1))));
  }

  @ParameterizedTest
  @MethodSource("provideCommandActionArguments")
  void testCommandFallbacks(Function<HystrixCommand<String>, String> operation) {

    HystrixCommand<String> command =
        new HystrixCommand<String>(setter()) {
          @Override
          protected String run() throws Exception {
            throw new IllegalArgumentException();
          }

          @Override
          protected String getFallback() {
            return "Fallback!";
          }
        };

    String result = testing.runWithSpan("parent", () -> operation.apply(command));
    assertThat(Objects.equals(result, "Fallback!")).isTrue();

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent(),
                span ->
                    span.hasName("ExampleGroup.HystrixTest$2.execute")
                        .hasParent(trace.getSpan(0))
                        .hasStatus(StatusData.error())
                        .hasEventsSatisfyingExactly(
                            event ->
                                event
                                    .hasName("exception")
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(
                                            EXCEPTION_TYPE, "java.lang.IllegalArgumentException"),
                                        satisfies(
                                            EXCEPTION_STACKTRACE,
                                            val -> val.isInstanceOf(String.class))))
                        .hasAttributesSatisfyingExactly(
                            equalTo(stringKey("hystrix.command"), "HystrixTest$2"),
                            equalTo(stringKey("hystrix.group"), "ExampleGroup"),
                            equalTo(booleanKey("hystrix.circuit_open"), false)),
                span ->
                    span.hasName("ExampleGroup.HystrixTest$2.fallback")
                        .hasParent(trace.getSpan(1))
                        .hasAttributesSatisfyingExactly(
                            equalTo(stringKey("hystrix.command"), "HystrixTest$2"),
                            equalTo(stringKey("hystrix.group"), "ExampleGroup"),
                            equalTo(booleanKey("hystrix.circuit_open"), false))));
  }

  private static Stream<Arguments> provideCommandActionArguments() {
    return Stream.of(
        Arguments.of(
            named("execute", (Function<HystrixCommand<String>, String>) HystrixCommand::execute)),
        Arguments.of(
            named(
                "queue",
                (Function<HystrixCommand<String>, String>)
                    cmd -> {
                      try {
                        return cmd.queue().get();
                      } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                      }
                    })),
        Arguments.of(
            named(
                "toObservable",
                (Function<HystrixCommand<String>, String>)
                    cmd -> cmd.toObservable().toBlocking().first())),
        Arguments.of(
            named(
                "observe",
                (Function<HystrixCommand<String>, String>)
                    cmd -> cmd.observe().toBlocking().first())),
        Arguments.of(
            named(
                "observe block",
                (Function<HystrixCommand<String>, String>)
                    cmd -> {
                      BlockingQueue<String> queue = new LinkedBlockingQueue<>();
                      cmd.observe()
                          .subscribe(
                              next -> {
                                try {
                                  queue.put(next);
                                } catch (InterruptedException e) {
                                  throw new RuntimeException(e);
                                }
                              });
                      String returnValue;
                      try {
                        returnValue = queue.take();
                      } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                      }
                      return returnValue;
                    })));
  }

  private static HystrixCommand.Setter setter() {
    HystrixCommand.Setter setter =
        HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
    setter.andCommandPropertiesDefaults(
        HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(10_000));
    return setter;
  }
}
