/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.rmi;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.test.utils.PortUtils;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.semconv.SemanticAttributes;
import io.opentelemetry.semconv.incubating.ExceptionIncubatingAttributes;
import io.opentelemetry.semconv.incubating.RpcIncubatingAttributes;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import org.assertj.core.api.AbstractAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import rmi.app.Greeter;
import rmi.app.Server;

class RmiTest {

  @RegisterExtension
  public static final AgentInstrumentationExtension testing =
      AgentInstrumentationExtension.create();

  private static int registryPort;
  private static Registry serverRegistry;
  private static Registry clientRegistry;

  @RegisterExtension final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();

  @BeforeAll
  static void setUp() throws Exception {
    registryPort = PortUtils.findOpenPort();
    serverRegistry = LocateRegistry.createRegistry(registryPort);
    clientRegistry = LocateRegistry.getRegistry("localhost", registryPort);
  }

  @AfterAll
  static void cleanUp() throws Exception {
    UnicastRemoteObject.unexportObject(serverRegistry, true);
  }

  @Test
  void clientCallCreatesSpans() throws Exception {
    Server server = new Server();
    serverRegistry.rebind(Server.RMI_ID, server);
    autoCleanup.deferCleanup(() -> serverRegistry.unbind(Server.RMI_ID));

    String response =
        testing.runWithSpan(
            "parent",
            () -> {
              Greeter client = (Greeter) clientRegistry.lookup(Server.RMI_ID);
              return client.hello("you");
            });

    assertThat(response).contains("Hello you");
    assertThat(testing.waitForTraces(1))
        .satisfiesExactly(
            trace ->
                assertThat(trace)
                    .satisfiesExactly(
                        span ->
                            assertThat(span)
                                .hasName("parent")
                                .hasKind(SpanKind.INTERNAL)
                                .hasParentSpanId(SpanId.getInvalid()),
                        span ->
                            assertThat(span)
                                .hasName("rmi.app.Greeter/hello")
                                .hasKind(SpanKind.CLIENT)
                                .hasParentSpanId(trace.get(0).getSpanId())
                                .hasAttributesSatisfyingExactly(
                                    equalTo(RpcIncubatingAttributes.RPC_SYSTEM, "java_rmi"),
                                    equalTo(RpcIncubatingAttributes.RPC_SERVICE, "rmi.app.Greeter"),
                                    equalTo(RpcIncubatingAttributes.RPC_METHOD, "hello")),
                        span ->
                            assertThat(span)
                                .hasName("rmi.app.Server/hello")
                                .hasKind(SpanKind.SERVER)
                                .hasAttributesSatisfyingExactly(
                                    equalTo(RpcIncubatingAttributes.RPC_SYSTEM, "java_rmi"),
                                    equalTo(RpcIncubatingAttributes.RPC_SERVICE, "rmi.app.Server"),
                                    equalTo(RpcIncubatingAttributes.RPC_METHOD, "hello"))));
  }

  @Test
  @SuppressWarnings("ReturnValueIgnored")
  void serverBuiltinMethods() throws Exception {
    Server server = new Server();
    serverRegistry.rebind(Server.RMI_ID, server);
    autoCleanup.deferCleanup(() -> serverRegistry.unbind(Server.RMI_ID));

    server.equals(new Server());
    server.getRef();
    server.hashCode();
    server.toString();
    server.getClass();

    assertThat(testing.waitForTraces(0)).isEmpty();
  }

  @Test
  void serviceThrownException() throws Exception {
    Server server = new Server();
    serverRegistry.rebind(Server.RMI_ID, server);
    autoCleanup.deferCleanup(() -> serverRegistry.unbind(Server.RMI_ID));

    Throwable thrown =
        catchThrowableOfType(
            () ->
                testing.runWithSpan(
                    "parent",
                    () -> {
                      Greeter client = (Greeter) clientRegistry.lookup(Server.RMI_ID);
                      client.exceptional();
                    }),
            IllegalStateException.class);

    assertThat(testing.waitForTraces(1))
        .satisfiesExactly(
            trace ->
                assertThat(trace)
                    .satisfiesExactly(
                        span ->
                            assertThat(span)
                                .hasName("parent")
                                .hasKind(SpanKind.INTERNAL)
                                .hasParentSpanId(SpanId.getInvalid()),
                        span ->
                            assertThat(span)
                                .hasName("rmi.app.Greeter/exceptional")
                                .hasKind(SpanKind.CLIENT)
                                .hasParentSpanId(trace.get(0).getSpanId())
                                .hasEventsSatisfyingExactly(
                                    event ->
                                        event
                                            .hasName(SemanticAttributes.EXCEPTION_EVENT_NAME)
                                            .hasAttributesSatisfyingExactly(
                                                equalTo(
                                                    ExceptionIncubatingAttributes.EXCEPTION_TYPE,
                                                    thrown.getClass().getCanonicalName()),
                                                equalTo(
                                                    ExceptionIncubatingAttributes.EXCEPTION_MESSAGE,
                                                    thrown.getMessage()),
                                                satisfies(
                                                    ExceptionIncubatingAttributes
                                                        .EXCEPTION_STACKTRACE,
                                                    AbstractAssert::isNotNull)))
                                .hasAttributesSatisfyingExactly(
                                    equalTo(RpcIncubatingAttributes.RPC_SYSTEM, "java_rmi"),
                                    equalTo(RpcIncubatingAttributes.RPC_SERVICE, "rmi.app.Greeter"),
                                    equalTo(RpcIncubatingAttributes.RPC_METHOD, "exceptional")),
                        span ->
                            assertThat(span)
                                .hasName("rmi.app.Server/exceptional")
                                .hasKind(SpanKind.SERVER)
                                .hasEventsSatisfyingExactly(
                                    event ->
                                        event
                                            .hasName(SemanticAttributes.EXCEPTION_EVENT_NAME)
                                            .hasAttributesSatisfyingExactly(
                                                equalTo(
                                                    ExceptionIncubatingAttributes.EXCEPTION_TYPE,
                                                    thrown.getClass().getCanonicalName()),
                                                equalTo(
                                                    ExceptionIncubatingAttributes.EXCEPTION_MESSAGE,
                                                    thrown.getMessage()),
                                                satisfies(
                                                    ExceptionIncubatingAttributes
                                                        .EXCEPTION_STACKTRACE,
                                                    AbstractAssert::isNotNull)))
                                .hasAttributesSatisfyingExactly(
                                    equalTo(RpcIncubatingAttributes.RPC_SYSTEM, "java_rmi"),
                                    equalTo(RpcIncubatingAttributes.RPC_SERVICE, "rmi.app.Server"),
                                    equalTo(RpcIncubatingAttributes.RPC_METHOD, "exceptional"))));
  }
}
