/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alibaba.nacos.common.remote.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.alibaba.nacos.api.remote.request.Request;
import com.alibaba.nacos.api.remote.response.Response;
import com.alibaba.nacos.common.remote.ConnectionType;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0.NacosClientTestHelper;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RpcClientTest {
  @RegisterExtension
  private static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  private RpcClient rpcClient;

  @Mock private ServerRequestHandler serverRequestHandler;

  @BeforeEach
  public void setUp() {
    rpcClient =
        new RpcClient("testRpcClient") {
          @Override
          public ConnectionType getConnectionType() {
            return ConnectionType.GRPC;
          }

          @Override
          public int rpcPortOffset() {
            return 0;
          }

          @Override
          public Connection connectToServer(ServerInfo serverInfo) throws Exception {
            return null;
          }
        };
    rpcClient.serverRequestHandlers = Collections.singletonList(serverRequestHandler);
  }

  @ParameterizedTest
  @MethodSource("requestProvider")
  public void handleServerRequestSuccessResponse(Request request) {
    when(serverRequestHandler.requestReply(any(Request.class)))
        .thenReturn(NacosClientTestHelper.SUCCESS_RESPONSE);
    Response response = rpcClient.handleServerRequest(request);
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
                            rpcClient.getClass().getName(), "handleServerRequest", request));
              });
        });
    testing.clearData();
  }

  @ParameterizedTest
  @MethodSource("requestProvider")
  public void handleServerRequestErrorResponse(Request request) {
    when(serverRequestHandler.requestReply(any(Request.class)))
        .thenReturn(NacosClientTestHelper.ERROR_RESPONSE);
    Response response = rpcClient.handleServerRequest(request);
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
                            rpcClient.getClass().getName(), "handleServerRequest", request));
              });
        });
    testing.clearData();
  }

  private static List<Request> requestProvider() {
    return NacosClientTestHelper.REQUEST_LIST;
  }
}
