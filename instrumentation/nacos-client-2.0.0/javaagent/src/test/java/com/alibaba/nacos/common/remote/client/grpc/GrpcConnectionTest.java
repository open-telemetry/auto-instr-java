/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alibaba.nacos.common.remote.client.grpc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.grpc.auto.Payload;
import com.alibaba.nacos.api.grpc.auto.RequestGrpc;
import com.alibaba.nacos.api.remote.PayloadRegistry;
import com.alibaba.nacos.api.remote.request.Request;
import com.alibaba.nacos.api.remote.response.Response;
import com.alibaba.nacos.common.remote.client.RpcClient;
import com.alibaba.nacos.shaded.com.google.common.util.concurrent.ListenableFuture;
import com.alibaba.nacos.shaded.io.grpc.ManagedChannel;
import com.alibaba.nacos.shaded.io.grpc.stub.StreamObserver;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0.NacosClientTestHelper;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GrpcConnectionTest {

  @RegisterExtension
  private static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Mock private Executor executor;

  @Mock private ManagedChannel channel;

  @Mock private StreamObserver<Payload> payloadStreamObserver;

  @Mock RequestGrpc.RequestFutureStub grpcFutureServiceStub;

  @Mock ListenableFuture<Payload> future;

  Payload responsePayload;

  Payload errorResponsePayload;

  GrpcConnection connection;

  @BeforeAll
  public static void setUpBeforeClass() {
    PayloadRegistry.init();
  }

  @BeforeEach
  public void setUp() throws Exception {
    connection = new GrpcConnection(new RpcClient.ServerInfo(), executor);
    connection.setChannel(channel);
    connection.setPayloadStreamObserver(payloadStreamObserver);
    connection.setGrpcFutureServiceStub(grpcFutureServiceStub);
    when(grpcFutureServiceStub.request(any(Payload.class))).thenReturn(future);
    responsePayload = GrpcUtils.convert(NacosClientTestHelper.SUCCESS_RESPONSE);
    errorResponsePayload = GrpcUtils.convert(NacosClientTestHelper.ERROR_RESPONSE);
  }

  @AfterEach
  public void tearDown() {
    connection.close();
  }

  @ParameterizedTest
  @MethodSource("requestProvider")
  public void requestSuccessResponse(Request request)
      throws NacosException, ExecutionException, InterruptedException, TimeoutException {
    when(future.get(-1, TimeUnit.MILLISECONDS)).thenReturn(responsePayload);
    Response response = connection.request(request, -1);
    assertNotNull(response);
    assertTrue(response.isSuccess());
    testing.waitAndAssertTraces(
        trace -> {
          trace.hasSpansSatisfyingExactly(
              span -> {
                span.hasName(NacosClientTestHelper.NACOS_CLIENT_REQUEST_NAME_MAP.get(request))
                    .hasKind(SpanKind.INTERNAL)
                    .hasStatus(StatusData.unset())
                    .hasAttributesSatisfyingExactly(
                        NacosClientTestHelper.requestAttributeAssertions(
                            connection.getClass().getName(), "request", request));
              });
        });
    testing.clearData();
  }

  @ParameterizedTest
  @MethodSource("requestProvider")
  public void requestErrorResponse(Request request)
      throws NacosException, ExecutionException, InterruptedException, TimeoutException {
    when(future.get(-1, TimeUnit.MILLISECONDS)).thenReturn(errorResponsePayload);
    Response response = connection.request(request, -1);
    assertNotNull(response);
    assertFalse(response.isSuccess());
    testing.waitAndAssertTraces(
        trace -> {
          trace.hasSpansSatisfyingExactly(
              span -> {
                span.hasName(NacosClientTestHelper.NACOS_CLIENT_REQUEST_NAME_MAP.get(request))
                    .hasKind(SpanKind.INTERNAL)
                    .hasStatus(StatusData.error())
                    .hasAttributesSatisfyingExactly(
                        NacosClientTestHelper.requestAttributeAssertions(
                            connection.getClass().getName(), "request", request));
              });
        });
    testing.clearData();
  }

  private static List<Request> requestProvider() {
    return NacosClientTestHelper.REQUEST_LIST;
  }
}
