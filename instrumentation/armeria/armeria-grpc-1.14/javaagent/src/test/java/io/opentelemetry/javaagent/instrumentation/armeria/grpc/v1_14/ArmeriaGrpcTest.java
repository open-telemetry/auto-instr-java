/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.armeria.grpc.v1_14;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.linecorp.armeria.client.grpc.GrpcClients;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;
import example.GreeterGrpc;
import example.Helloworld;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.incubating.MessageIncubatingAttributes;
import io.opentelemetry.semconv.incubating.RpcIncubatingAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ArmeriaGrpcTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  private static final AttributeKey<Long> RPC_REQUEST_BODY_SIZE =
      AttributeKey.longKey("rpc.request.body.size");
  private static final AttributeKey<Long> RPC_RESPONSE_BODY_SIZE =
      AttributeKey.longKey("rpc.response.body.size");

  @RegisterExtension
  static final ServerExtension server =
      new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
          sb.service(
              GrpcService.builder()
                  .addService(
                      new GreeterGrpc.GreeterImplBase() {
                        @Override
                        public void sayHello(
                            Helloworld.Request request,
                            StreamObserver<Helloworld.Response> responseObserver) {
                          responseObserver.onNext(
                              Helloworld.Response.newBuilder()
                                  .setMessage("Hello " + request.getName())
                                  .build());
                          responseObserver.onCompleted();
                        }
                      })
                  .build());
        }
      };

  @SuppressWarnings("deprecation") // using deprecated semconv
  @Test
  void grpcInstrumentation() {
    GreeterGrpc.GreeterBlockingStub client =
        GrpcClients.builder(server.httpUri()).build(GreeterGrpc.GreeterBlockingStub.class);

    Helloworld.Request request = Helloworld.Request.newBuilder().setName("test").build();
    Helloworld.Response response = testing.runWithSpan("parent", () -> client.sayHello(request));

    assertThat(response.getMessage()).isEqualTo("Hello test");
    int requestSerializedSize = request.getSerializedSize();
    int responseSerializedSize = response.getSerializedSize();

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName("example.Greeter/SayHello")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(RpcIncubatingAttributes.RPC_SYSTEM, "grpc"),
                            equalTo(RpcIncubatingAttributes.RPC_SERVICE, "example.Greeter"),
                            equalTo(RpcIncubatingAttributes.RPC_METHOD, "SayHello"),
                            equalTo(
                                RpcIncubatingAttributes.RPC_GRPC_STATUS_CODE,
                                (long) Status.Code.OK.value()),
                            equalTo(ServerAttributes.SERVER_ADDRESS, "127.0.0.1"),
                            equalTo(ServerAttributes.SERVER_PORT, (long) server.httpPort()),
                            equalTo(RPC_RESPONSE_BODY_SIZE, responseSerializedSize),
                            equalTo(RPC_REQUEST_BODY_SIZE, requestSerializedSize))
                        .hasEventsSatisfyingExactly(
                            event ->
                                event
                                    .hasName("message")
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(MessageIncubatingAttributes.MESSAGE_TYPE, "SENT"),
                                        equalTo(MessageIncubatingAttributes.MESSAGE_ID, 1L)),
                            event ->
                                event
                                    .hasName("message")
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(
                                            MessageIncubatingAttributes.MESSAGE_TYPE, "RECEIVED"),
                                        equalTo(MessageIncubatingAttributes.MESSAGE_ID, 2L))),
                span ->
                    span.hasName("example.Greeter/SayHello")
                        .hasKind(SpanKind.SERVER)
                        .hasParent(trace.getSpan(1))
                        .hasAttributesSatisfyingExactly(
                            equalTo(RpcIncubatingAttributes.RPC_SYSTEM, "grpc"),
                            equalTo(RpcIncubatingAttributes.RPC_SERVICE, "example.Greeter"),
                            equalTo(RpcIncubatingAttributes.RPC_METHOD, "SayHello"),
                            equalTo(
                                RpcIncubatingAttributes.RPC_GRPC_STATUS_CODE,
                                (long) Status.Code.OK.value()),
                            equalTo(ServerAttributes.SERVER_ADDRESS, "127.0.0.1"),
                            equalTo(ServerAttributes.SERVER_PORT, server.httpPort()),
                            equalTo(RPC_REQUEST_BODY_SIZE, responseSerializedSize),
                            equalTo(RPC_RESPONSE_BODY_SIZE, requestSerializedSize))
                        .hasEventsSatisfyingExactly(
                            event ->
                                event
                                    .hasName("message")
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(
                                            MessageIncubatingAttributes.MESSAGE_TYPE, "RECEIVED"),
                                        equalTo(MessageIncubatingAttributes.MESSAGE_ID, 1L)),
                            event ->
                                event
                                    .hasName("message")
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(MessageIncubatingAttributes.MESSAGE_TYPE, "SENT"),
                                        equalTo(MessageIncubatingAttributes.MESSAGE_ID, 2L)))));
  }
}
