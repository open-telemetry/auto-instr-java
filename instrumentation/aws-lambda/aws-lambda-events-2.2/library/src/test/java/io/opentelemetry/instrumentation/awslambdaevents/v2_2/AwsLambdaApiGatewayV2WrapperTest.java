/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awslambdaevents.v2_2;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent.RequestContext;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.awslambdacore.v1_0.internal.WrappedLambda;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.LibraryInstrumentationExtension;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AwsLambdaApiGatewayV2WrapperTest {

  @RegisterExtension
  public static final InstrumentationExtension testing = LibraryInstrumentationExtension.create();

  @Mock private Context context;

  @BeforeEach
  void setUp() {
    when(context.getFunctionName()).thenReturn("my_function");
    when(context.getAwsRequestId()).thenReturn("1-22-333");
    when(context.getInvokedFunctionArn())
        .thenReturn("arn:aws:lambda:us-east-1:123456789:function:test");
  }

  @AfterEach
  void tearDown() {
    assertThat(testing.forceFlushCalled()).isTrue();
  }

  @Test
  @SetEnvironmentVariable(
      key = WrappedLambda.OTEL_LAMBDA_HANDLER_ENV_KEY,
      value =
          "io.opentelemetry.instrumentation.awslambdaevents.v2_2.AwsLambdaApiGatewayV2WrapperTest$TestRequestHandlerApiGateway::handleRequest")
  void tracedWithHttpPropagation() {
    TracingRequestApiGatewayV2Wrapper wrapper =
        new TracingRequestApiGatewayV2Wrapper(
            testing.getOpenTelemetrySdk(),
            WrappedLambda.fromConfiguration(),
            TracingRequestApiGatewayV2Wrapper::map);

    Map<String, String> headers = new HashMap<>();
    headers.put("traceparent", "00-4fd0b6131f19f39af59518d127b0cafe-0000000000000456-01");
    headers.put("User-Agent", "Test Client");
    headers.put("host", "localhost:123");
    headers.put("X-FORWARDED-PROTO", "http");
    Map<String, String> query = new HashMap<>();
    query.put("a", "b");
    query.put("c", "d");
    RequestContext.Http http =
        RequestContext.Http.builder().withMethod("GET").withPath("/hello/{param}").build();
    RequestContext requestContext = RequestContext.builder().withHttp(http).build();
    APIGatewayV2HTTPEvent input =
        APIGatewayV2HTTPEvent.builder()
            .withRawPath("/hello/world")
            .withBody("hello")
            .withQueryStringParameters(query)
            .withHeaders(headers)
            .withRequestContext(requestContext)
            .build();

    APIGatewayV2HTTPResponse result =
        (APIGatewayV2HTTPResponse) wrapper.handleRequest(input, context);

    assertThat(result.getBody()).isEqualTo("world");
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("GET /hello/{param}")
                        .hasKind(SpanKind.SERVER)
                        .hasTraceId("4fd0b6131f19f39af59518d127b0cafe")
                        .hasParentSpanId("0000000000000456")
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                ResourceAttributes.CLOUD_RESOURCE_ID,
                                "arn:aws:lambda:us-east-1:123456789:function:test"),
                            equalTo(ResourceAttributes.CLOUD_ACCOUNT_ID, "123456789"),
                            equalTo(SemanticAttributes.FAAS_INVOCATION_ID, "1-22-333"),
                            equalTo(SemanticAttributes.FAAS_TRIGGER, "http"),
                            equalTo(SemanticAttributes.HTTP_METHOD, "GET"),
                            equalTo(SemanticAttributes.USER_AGENT_ORIGINAL, "Test Client"),
                            equalTo(
                                SemanticAttributes.HTTP_URL,
                                "http://localhost:123/hello/world?a=b&c=d"),
                            equalTo(SemanticAttributes.HTTP_STATUS_CODE, 200L))));
  }

  @Test
  @SetEnvironmentVariable(
      key = WrappedLambda.OTEL_LAMBDA_HANDLER_ENV_KEY,
      value =
          "io.opentelemetry.instrumentation.awslambdaevents.v2_2.AwsLambdaApiGatewayV2WrapperTest$TestRequestHandlerApiGateway::handleRequest")
  void handlerTraced_empty() {
    TracingRequestApiGatewayV2Wrapper wrapper =
        new TracingRequestApiGatewayV2Wrapper(
            testing.getOpenTelemetrySdk(),
            WrappedLambda.fromConfiguration(),
            TracingRequestApiGatewayV2Wrapper::map);
    APIGatewayV2HTTPResponse result =
        (APIGatewayV2HTTPResponse)
            wrapper.handleRequest(
                APIGatewayV2HTTPEvent.builder().withBody("empty").build(), context);

    assertThat(result.getBody()).isNull();
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("my_function")
                        .hasKind(SpanKind.SERVER)
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                ResourceAttributes.CLOUD_RESOURCE_ID,
                                "arn:aws:lambda:us-east-1:123456789:function:test"),
                            equalTo(ResourceAttributes.CLOUD_ACCOUNT_ID, "123456789"),
                            equalTo(SemanticAttributes.FAAS_INVOCATION_ID, "1-22-333"),
                            equalTo(SemanticAttributes.FAAS_TRIGGER, "http"))));
  }

  @Test
  @SetEnvironmentVariable(
      key = WrappedLambda.OTEL_LAMBDA_HANDLER_ENV_KEY,
      value =
          "io.opentelemetry.instrumentation.awslambdaevents.v2_2.AwsLambdaApiGatewayV2WrapperTest$TestRequestHandlerString::handleRequest")
  void handlerTraced_string() {
    TracingRequestApiGatewayV2Wrapper wrapper =
        new TracingRequestApiGatewayV2Wrapper(
            testing.getOpenTelemetrySdk(),
            WrappedLambda.fromConfiguration(),
            TracingRequestApiGatewayV2Wrapper::map);
    APIGatewayV2HTTPResponse result =
        (APIGatewayV2HTTPResponse)
            wrapper.handleRequest(
                APIGatewayV2HTTPEvent.builder().withBody("\"hello\"").build(), context);

    assertThat(result.getBody()).isEqualTo("\"world\"");
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("my_function")
                        .hasKind(SpanKind.SERVER)
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                ResourceAttributes.CLOUD_RESOURCE_ID,
                                "arn:aws:lambda:us-east-1:123456789:function:test"),
                            equalTo(ResourceAttributes.CLOUD_ACCOUNT_ID, "123456789"),
                            equalTo(SemanticAttributes.FAAS_INVOCATION_ID, "1-22-333"),
                            equalTo(SemanticAttributes.FAAS_TRIGGER, "http"))));
  }

  @Test
  @SetEnvironmentVariable(
      key = WrappedLambda.OTEL_LAMBDA_HANDLER_ENV_KEY,
      value =
          "io.opentelemetry.instrumentation.awslambdaevents.v2_2.AwsLambdaApiGatewayV2WrapperTest$TestRequestHandlerInteger::handleRequest")
  void handlerTraced_integer() {
    TracingRequestApiGatewayV2Wrapper wrapper =
        new TracingRequestApiGatewayV2Wrapper(
            testing.getOpenTelemetrySdk(),
            WrappedLambda.fromConfiguration(),
            TracingRequestApiGatewayV2Wrapper::map);
    APIGatewayV2HTTPResponse result =
        (APIGatewayV2HTTPResponse)
            wrapper.handleRequest(APIGatewayV2HTTPEvent.builder().withBody("1").build(), context);

    assertThat(result.getBody()).isEqualTo("\"world\"");
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("my_function")
                        .hasKind(SpanKind.SERVER)
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                ResourceAttributes.CLOUD_RESOURCE_ID,
                                "arn:aws:lambda:us-east-1:123456789:function:test"),
                            equalTo(ResourceAttributes.CLOUD_ACCOUNT_ID, "123456789"),
                            equalTo(SemanticAttributes.FAAS_INVOCATION_ID, "1-22-333"),
                            equalTo(SemanticAttributes.FAAS_TRIGGER, "http"))));
  }

  @Test
  @SetEnvironmentVariable(
      key = WrappedLambda.OTEL_LAMBDA_HANDLER_ENV_KEY,
      value =
          "io.opentelemetry.instrumentation.awslambdaevents.v2_2.AwsLambdaApiGatewayV2WrapperTest$TestRequestHandlerCustomType::handleRequest")
  void handlerTraced_customType() {
    TracingRequestApiGatewayV2Wrapper wrapper =
        new TracingRequestApiGatewayV2Wrapper(
            testing.getOpenTelemetrySdk(),
            WrappedLambda.fromConfiguration(),
            TracingRequestApiGatewayV2Wrapper::map);
    APIGatewayV2HTTPResponse result =
        (APIGatewayV2HTTPResponse)
            wrapper.handleRequest(
                APIGatewayV2HTTPEvent.builder()
                    .withBody("{\"key\":\"hello\", \"value\":\"General Kenobi\"}")
                    .build(),
                context);

    assertThat(result.getBody()).isEqualTo("\"General Kenobi\"");
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("my_function")
                        .hasKind(SpanKind.SERVER)
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                ResourceAttributes.CLOUD_RESOURCE_ID,
                                "arn:aws:lambda:us-east-1:123456789:function:test"),
                            equalTo(ResourceAttributes.CLOUD_ACCOUNT_ID, "123456789"),
                            equalTo(SemanticAttributes.FAAS_INVOCATION_ID, "1-22-333"),
                            equalTo(SemanticAttributes.FAAS_TRIGGER, "http"))));
  }

  public static class TestRequestHandlerApiGateway
      implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
      if (input.getBody().equals("hello")) {
        return APIGatewayV2HTTPResponse.builder().withStatusCode(200).withBody("world").build();
      } else if (input.getBody().equals("empty")) {
        return new APIGatewayV2HTTPResponse();
      }
      throw new IllegalStateException("bad request");
    }
  }

  public static final class TestRequestHandlerString implements RequestHandler<String, String> {

    @Override
    public String handleRequest(String input, Context context) {
      if (input.equals("hello")) {
        return "world";
      }
      throw new IllegalArgumentException("bad argument");
    }
  }

  public static final class TestRequestHandlerInteger implements RequestHandler<Integer, String> {

    @Override
    public String handleRequest(Integer input, Context context) {
      if (input == 1) {
        return "world";
      }
      throw new IllegalArgumentException("bad argument");
    }
  }

  public static class CustomType {
    public String key;
    public String value;
  }

  public static final class TestRequestHandlerCustomType
      implements RequestHandler<CustomType, String> {

    @Override
    public String handleRequest(CustomType input, Context context) {
      if (input.key.equals("hello")) {
        return input.value;
      }
      throw new IllegalArgumentException("bad argument");
    }
  }
}
