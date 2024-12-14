package io.opentelemetry.instrumentation.awssdk.v2_2;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.api.internal.ConfigPropertiesUtil;
import io.opentelemetry.sdk.testing.assertj.SpanDataAssert;
import io.opentelemetry.sdk.testing.assertj.TraceAssert;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.UrlAttributes.URL_FULL;
import static io.opentelemetry.semconv.incubating.AwsIncubatingAttributes.AWS_REQUEST_ID;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_MESSAGE_ID;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_OPERATION;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_SYSTEM;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MessagingSystemIncubatingValues.AWS_SQS;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_METHOD;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_SERVICE;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_SYSTEM;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractAws2SqsSuppressReceiveSpansTest extends AbstractAws2SqsBaseTest {

  boolean isSqsAttributeInjectionEnabled() {
    // See io.opentelemetry.instrumentation.awssdk.v2_2.autoconfigure.TracingExecutionInterceptor
    return ConfigPropertiesUtil.getBoolean("otel.instrumentation.aws-sdk.experimental-use-propagator-for-messaging", false);
  }

  @Override
  @SuppressWarnings("deprecation") // using deprecated semconv
  protected void assertSqsTraces(Boolean withParent, Boolean captureHeaders) {
    List<Consumer<TraceAssert>> traceAsserts =
        new ArrayList<>(
            Arrays.asList(
                trace ->
                    trace.hasSpansSatisfyingExactly(
                        span ->
                            span.hasName("Sqs.CreateQueue")
                                .hasKind(SpanKind.CLIENT)
                                .hasNoParent()
                                .hasAttributesSatisfyingExactly(
                                    equalTo(stringKey("aws.agent"), "java-aws-sdk"),
                                    equalTo(stringKey("aws.queue.name"), "testSdkSqs"),
                                    satisfies(
                                        AWS_REQUEST_ID,
                                        val ->
                                            val.satisfiesAnyOf(
                                                v ->
                                                    assertThat(v)
                                                        .isEqualTo(
                                                            "00000000-0000-0000-0000-000000000000"),
                                                v -> assertThat(v).isEqualTo("UNKNOWN"))),
                                    equalTo(RPC_SYSTEM, "aws-api"),
                                    equalTo(RPC_SERVICE, "Sqs"),
                                    equalTo(RPC_METHOD, "CreateQueue"),
                                    equalTo(HTTP_REQUEST_METHOD, "POST"),
                                    equalTo(HTTP_RESPONSE_STATUS_CODE, 200),
                                    satisfies(
                                        URL_FULL, v -> v.startsWith("http://localhost:" + sqsPort)),
                                    equalTo(SERVER_ADDRESS, "localhost"),
                                    equalTo(SERVER_PORT, sqsPort))),
                trace ->
                    trace.hasSpansSatisfyingExactly(
                        span ->
                            span.hasName("testSdkSqs publish")
                                .hasKind(SpanKind.PRODUCER)
                                .hasNoParent()
                                .hasAttributesSatisfyingExactly(
                                    equalTo(stringKey("aws.agent"), "java-aws-sdk"),
                                    equalTo(
                                        stringKey("aws.queue.url"),
                                        "http://localhost:" + sqsPort + "/000000000000/testSdkSqs"),
                                    satisfies(
                                        AWS_REQUEST_ID,
                                        val ->
                                            val.satisfiesAnyOf(
                                                v ->
                                                    assertThat(v)
                                                        .isEqualTo(
                                                            "00000000-0000-0000-0000-000000000000"),
                                                v -> assertThat(v).isEqualTo("UNKNOWN"))),
                                    equalTo(RPC_SYSTEM, "aws-api"),
                                    equalTo(RPC_SERVICE, "Sqs"),
                                    equalTo(RPC_METHOD, "SendMessage"),
                                    equalTo(HTTP_REQUEST_METHOD, "POST"),
                                    equalTo(HTTP_RESPONSE_STATUS_CODE, 200),
                                    satisfies(
                                        URL_FULL, v -> v.startsWith("http://localhost:" + sqsPort)),
                                    equalTo(SERVER_ADDRESS, "localhost"),
                                    equalTo(SERVER_PORT, sqsPort),
                                    equalTo(MESSAGING_SYSTEM, AWS_SQS),
                                    equalTo(MESSAGING_DESTINATION_NAME, "testSdkSqs"),
                                    equalTo(MESSAGING_OPERATION, "publish"),
                                    satisfies(
                                        MESSAGING_MESSAGE_ID, v -> v.isInstanceOf(String.class))),
                        span ->
                            span.hasName("testSdkSqs process")
                                .hasKind(SpanKind.CONSUMER)
                                .hasParent(trace.getSpan(0))
                                .hasTotalRecordedLinks(0)
                                .hasAttributesSatisfyingExactly(
                                    equalTo(stringKey("aws.agent"), "java-aws-sdk"),
                                    equalTo(RPC_SYSTEM, "aws-api"),
                                    equalTo(RPC_SERVICE, "Sqs"),
                                    equalTo(RPC_METHOD, "ReceiveMessage"),
                                    equalTo(HTTP_REQUEST_METHOD, "POST"),
                                    equalTo(HTTP_RESPONSE_STATUS_CODE, 200),
                                    satisfies(
                                        URL_FULL, v -> v.startsWith("http://localhost:" + sqsPort)),
                                    equalTo(SERVER_ADDRESS, "localhost"),
                                    equalTo(SERVER_PORT, sqsPort),
                                    equalTo(MESSAGING_SYSTEM, AWS_SQS),
                                    equalTo(MESSAGING_DESTINATION_NAME, "testSdkSqs"),
                                    equalTo(MESSAGING_OPERATION, "process"),
                                    satisfies(
                                        MESSAGING_MESSAGE_ID, v -> v.isInstanceOf(String.class))
                                ),
                        span -> span.hasName("process child")
                            .hasParent(trace.getSpan(1)).hasAttributes(Attributes.empty()))));

    if (withParent) {
      /*
       * This span represents HTTP "sending of receive message" operation. It's always single,
       * while there can be multiple CONSUMER spans (one per consumed message).
       * This one could be suppressed (by IF in TracingRequestHandler#beforeRequest but then
       * HTTP instrumentation span would appear)
       */
      traceAsserts.add(
          trace -> trace.hasSpansSatisfyingExactly(
              span -> span.hasName("parent").hasNoParent(),
              span -> span.hasName("Sqs.ReceiveMessage")
                  .hasKind(SpanKind.CLIENT)
                  .hasTotalRecordedLinks(0)
                  .hasAttributesSatisfyingExactly(
                      equalTo(stringKey("aws.agent"), "java-aws-sdk"),
                      equalTo(
                          stringKey("aws.queue.url"),
                          "http://localhost:" + sqsPort + "/000000000000/testSdkSqs"),
                      satisfies(
                          AWS_REQUEST_ID,
                          val ->
                              val.satisfiesAnyOf(
                                  v ->
                                      assertThat(v)
                                          .isEqualTo(
                                              "00000000-0000-0000-0000-000000000000"),
                                  v -> assertThat(v).isEqualTo("UNKNOWN"))),
                      equalTo(RPC_SYSTEM, "aws-api"),
                      equalTo(RPC_SERVICE, "Sqs"),
                      equalTo(RPC_METHOD, "ReceiveMessage"),
                      equalTo(HTTP_REQUEST_METHOD, "POST"),
                      equalTo(HTTP_RESPONSE_STATUS_CODE, 200),
                      satisfies(
                          URL_FULL, v -> v.startsWith("http://localhost:" + sqsPort)),
                      equalTo(SERVER_ADDRESS, "localhost"),
                      equalTo(SERVER_PORT, sqsPort))));
    }

    getTesting().waitAndAssertTraces(traceAsserts);
  }


  @Test
  @SuppressWarnings("deprecation") // using deprecated semconv
  void testBatchSqsProducerConsumerServicesSync() throws URISyntaxException {
    SqsClientBuilder builder = SqsClient.builder();
    configureSdkClient(builder);
    SqsClient client = configureSqsClient(builder.build());

    client.createQueue(createQueueRequest);
    client.sendMessageBatch(sendMessageBatchRequest);

    ReceiveMessageResponse response = client.receiveMessage(receiveMessageBatchRequest);

    int totalAttrs =
        response.messages().stream().mapToInt(message -> message.messageAttributes().size()).sum();

    assertThat(response.messages().size()).isEqualTo(3);

    // +2: 3 messages, 2x traceparent, 1x not injected due to too many attrs
    assertThat(totalAttrs).isEqualTo(18 + (isSqsAttributeInjectionEnabled() ? 2 : 0));

    List<Consumer<TraceAssert>> traceAsserts =
        new ArrayList<>(
            Arrays.asList(
                trace ->
                    trace.hasSpansSatisfyingExactly(
                        span -> span.hasName("Sqs.CreateQueue").hasKind(SpanKind.CLIENT)),
                trace -> {
                  List<Consumer<SpanDataAssert>> spanAsserts = new ArrayList<>(
                      singletonList(
                          span ->
                              span.hasName("testSdkSqs publish")
                                  .hasKind(SpanKind.PRODUCER)
                                  .hasNoParent()
                                  .hasAttributesSatisfyingExactly(
                                      equalTo(stringKey("aws.agent"), "java-aws-sdk"),
                                      equalTo(
                                          stringKey("aws.queue.url"),
                                          "http://localhost:" + sqsPort
                                              + "/000000000000/testSdkSqs"),
                                      satisfies(
                                          AWS_REQUEST_ID,
                                          val ->
                                              val.satisfiesAnyOf(
                                                  v ->
                                                      assertThat(v.trim())
                                                          .isEqualTo(
                                                              "00000000-0000-0000-0000-000000000000"),
                                                  v -> assertThat(v.trim()).isEqualTo("UNKNOWN"))),
                                      equalTo(RPC_SYSTEM, "aws-api"),
                                      equalTo(RPC_SERVICE, "Sqs"),
                                      equalTo(RPC_METHOD, "SendMessageBatch"),
                                      equalTo(HTTP_REQUEST_METHOD, "POST"),
                                      equalTo(HTTP_RESPONSE_STATUS_CODE, 200),
                                      satisfies(URL_FULL,
                                          v -> v.startsWith("http://localhost:" + sqsPort)),
                                      equalTo(SERVER_ADDRESS, "localhost"),
                                      equalTo(SERVER_PORT, sqsPort),
                                      equalTo(MESSAGING_SYSTEM, AWS_SQS),
                                      equalTo(MESSAGING_DESTINATION_NAME, "testSdkSqs"),
                                      equalTo(MESSAGING_OPERATION, "publish"))));

                  int iterationNumber = isXrayInjectionEnabled() ? 3 : 2;
                  for (int i = 1; i <= iterationNumber; i++) {
                    spanAsserts.add(
                        span -> span.hasName("testSdkSqs process")
                            .hasKind(SpanKind.CONSUMER)
                            .hasParent(trace.getSpan(0))
                            .hasTotalRecordedLinks(0)
                            .hasAttributesSatisfyingExactly(
                                equalTo(stringKey("aws.agent"), "java-aws-sdk"),
                                equalTo(RPC_SYSTEM, "aws-api"),
                                equalTo(RPC_SERVICE, "Sqs"),
                                equalTo(RPC_METHOD, "ReceiveMessage"),
                                equalTo(HTTP_REQUEST_METHOD, "POST"),
                                equalTo(HTTP_RESPONSE_STATUS_CODE, 200),
                                satisfies(
                                    URL_FULL,
                                    v -> v.startsWith("http://localhost:" + sqsPort)),
                                equalTo(SERVER_ADDRESS, "localhost"),
                                equalTo(SERVER_PORT, sqsPort),
                                equalTo(MESSAGING_SYSTEM, AWS_SQS),
                                equalTo(MESSAGING_DESTINATION_NAME, "testSdkSqs"),
                                equalTo(MESSAGING_OPERATION, "process"),
                                satisfies(
                                    MESSAGING_MESSAGE_ID, v -> v.isInstanceOf(String.class))));
                  }
                  trace.hasSpansSatisfyingExactly(spanAsserts);
                }
            ));

    if (!isXrayInjectionEnabled()) {
      traceAsserts.add(
          trace ->
              trace.hasSpansSatisfyingExactly(
                  span -> span.hasName("testSdkSqs process")
                      .hasKind(SpanKind.CONSUMER)
                      // TODO This is not nice at all, and can also happen if producer is not instrumented
                      .hasNoParent()
                      .hasTotalRecordedLinks(0)
                      .hasAttributesSatisfyingExactly(
                          equalTo(stringKey("aws.agent"), "java-aws-sdk"),
                          equalTo(RPC_SYSTEM, "aws-api"),
                          equalTo(RPC_SERVICE, "Sqs"),
                          equalTo(RPC_METHOD, "ReceiveMessage"),
                          equalTo(HTTP_REQUEST_METHOD, "POST"),
                          equalTo(HTTP_RESPONSE_STATUS_CODE, 200),
                          satisfies(
                              URL_FULL,
                              v -> v.startsWith("http://localhost:" + sqsPort)),
                          equalTo(SERVER_ADDRESS, "localhost"),
                          equalTo(SERVER_PORT, sqsPort),
                          equalTo(MESSAGING_SYSTEM, AWS_SQS),
                          equalTo(MESSAGING_DESTINATION_NAME, "testSdkSqs"),
                          equalTo(MESSAGING_OPERATION, "process"),
                          satisfies(
                              MESSAGING_MESSAGE_ID, v -> v.isInstanceOf(String.class)))));
    }
    getTesting().waitAndAssertTraces(traceAsserts);
  }
}
