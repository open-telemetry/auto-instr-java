/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

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
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.testing.assertj.AttributeAssertion;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.testing.internal.armeria.common.HttpData;
import io.opentelemetry.testing.internal.armeria.common.HttpResponse;
import io.opentelemetry.testing.internal.armeria.common.HttpStatus;
import io.opentelemetry.testing.internal.armeria.common.MediaType;
import io.opentelemetry.testing.internal.armeria.common.ResponseHeaders;
import io.opentelemetry.testing.internal.armeria.testing.junit5.server.mock.RecordedRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.ec2.Ec2AsyncClientBuilder;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.Ec2ClientBuilder;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.KinesisClientBuilder;
import software.amazon.awssdk.services.kinesis.model.DeleteStreamRequest;
import software.amazon.awssdk.services.rds.RdsAsyncClient;
import software.amazon.awssdk.services.rds.RdsAsyncClientBuilder;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsClientBuilder;
import software.amazon.awssdk.services.rds.model.DeleteOptionGroupRequest;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@SuppressWarnings("deprecation") // We need to use deprecated APIs for testing older versions.
public abstract class AbstractAws2ClientTest extends AbstractAws2ClientCoreTest {
  private static final String QUEUE_URL = "http://xxx/somequeue";

  // Force localhost instead of relying on mock server because using ip is yet another corner case
  // of the virtual bucket changes introduced by aws sdk v2.18.0. When using IP, there is no way to
  // prefix the hostname with the bucket name as label.
  private final URI clientUri = URI.create("http://localhost:" + server.httpPort());

  private static final String ec2BodyContent =
      "<AllocateAddressResponse xmlns=\"http://ec2.amazonaws.com/doc/2016-11-15/\">"
          + " <requestId>59dbff89-35bd-4eac-99ed-be587EXAMPLE</requestId>"
          + " <publicIp>192.0.2.1</publicIp>"
          + " <domain>standard</domain>"
          + "</AllocateAddressResponse>";

  private static final String rdsBodyContent =
      "<DeleteOptionGroupResponse xmlns=\"http://rds.amazonaws.com/doc/2014-09-01/\">"
          + " <ResponseMetadata><RequestId>0ac9cda2-bbf4-11d3-f92b-31fa5e8dbc99</RequestId></ResponseMetadata>"
          + "</DeleteOptionGroupResponse>";

  private static void assumeSupportedConfig(String operation) {
    Assumptions.assumeFalse(
        operation.equals("SendMessage") && isSqsAttributeInjectionEnabled(),
        "Cannot check Sqs.SendMessage here due to hard-coded MD5.");
  }

  @SuppressWarnings("deprecation") // uses deprecated semconv
  private void clientAssertions(
      String service, String operation, String method, Object response, String requestId) {
    assertThat(response).isNotNull();

    RecordedRequest request = server.takeRequest();
    assertThat(request).isNotNull();
    assertThat(request.request().headers().get("X-Amzn-Trace-Id")).isNotNull();
    assertThat(request.request().headers().get("traceparent")).isNull();

    if (service.equals("SNS") && operation.equals("Publish")) {
      String content = request.request().content(Charset.defaultCharset());
      boolean containsId =
          content.contains(
              getTesting().spans().get(0).getTraceId()
                  + "-"
                  + getTesting().spans().get(0).getSpanId());
      boolean containsTp = content.contains("=traceparent");
      if (isSqsAttributeInjectionEnabled()) {
        assertThat(containsId).isTrue();
        assertThat(containsTp).isTrue();
      } else {
        assertThat(containsId).isFalse();
        assertThat(containsTp).isFalse();
      }
    }

    List<AttributeAssertion> attributes =
        new ArrayList<>(
            asList(
                // Starting with AWS SDK V2 2.18.0, the s3 sdk will prefix the hostname with the
                // bucket name in case the bucket name is a valid DNS label, even in the case that
                // we are using an endpoint override. Previously the sdk was only doing that if
                // endpoint had "s3" as label in the FQDN. Our test assert both cases so that we
                // don't need to know what version is being tested.
                satisfies(SERVER_ADDRESS, v -> v.matches("somebucket.localhost|localhost")),
                equalTo(SERVER_PORT, server.httpPort()),
                equalTo(HTTP_REQUEST_METHOD, method),
                equalTo(HTTP_RESPONSE_STATUS_CODE, 200),
                equalTo(RPC_SYSTEM, "aws-api"),
                equalTo(RPC_SERVICE, service),
                equalTo(RPC_METHOD, operation),
                equalTo(stringKey("aws.agent"), "java-aws-sdk"),
                equalTo(AWS_REQUEST_ID, requestId)));

    if (service.equals("S3")) {
      attributes.addAll(
          new ArrayList<>(
              asList(
                  satisfies(
                      URL_FULL,
                      val ->
                          val.satisfiesAnyOf(
                              v ->
                                  assertThat(v)
                                      .startsWith(
                                          "http://somebucket.localhost:" + server.httpPort()),
                              v ->
                                  assertThat(v)
                                      .startsWith(
                                          "http://localhost:"
                                              + server.httpPort()
                                              + "/somebucket"))),
                  equalTo(stringKey("aws.bucket.name"), "somebucket"))));
    } else {
      attributes.addAll(
          new ArrayList<>(
              asList(
                  equalTo(SERVER_ADDRESS, "localhost"),
                  satisfies(
                      URL_FULL, val -> val.startsWith("http://localhost:" + server.httpPort())))));
    }

    if (service.equals("Kinesis")) {
      attributes.add(equalTo(stringKey("aws.stream.name"), "somestream"));
    }

    if (service.equals("Sns")) {
      attributes.add(equalTo(MESSAGING_DESTINATION_NAME, "somearn"));
    }

    if (service.equals("Sqs") && operation.equals("CreateQueue")) {
      attributes.add(equalTo(stringKey("aws.queue.name"), "somequeue"));
    }

    if (service.equals("Sqs") && operation.equals("SendMessage")) {
      attributes.addAll(
          new ArrayList<>(
              asList(
                  equalTo(stringKey("aws.queue.url"), QUEUE_URL),
                  equalTo(MESSAGING_DESTINATION_NAME, "somequeue"),
                  equalTo(MESSAGING_OPERATION, "publish"),
                  satisfies(MESSAGING_MESSAGE_ID, val -> val.isInstanceOf(String.class)),
                  equalTo(MESSAGING_SYSTEM, AWS_SQS))));
    }

    String evaluatedOperation;
    SpanKind operationKind;
    if (operation.equals("SendMessage")) {
      evaluatedOperation = "somequeue publish";
      operationKind = SpanKind.PRODUCER;
    } else {
      operationKind = SpanKind.CLIENT;
      evaluatedOperation = service + "." + operation;
    }

    getTesting()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasName(evaluatedOperation)
                            .hasKind(operationKind)
                            .hasNoParent()
                            .hasAttributesSatisfyingExactly(attributes)));
  }

  private static Stream<Arguments> provideS3Arguments() {
    return Stream.of(
        Arguments.of(
            "CreateBucket",
            "PUT",
            (Function<S3Client, Object>)
                c -> c.createBucket(CreateBucketRequest.builder().bucket("somebucket").build()),
            (Function<S3AsyncClient, Future<?>>)
                c -> c.createBucket(CreateBucketRequest.builder().bucket("somebucket").build()),
            ""),
        Arguments.of(
            "GetObject",
            "GET",
            (Function<S3Client, Object>)
                c ->
                    c.getObject(
                        GetObjectRequest.builder().bucket("somebucket").key("somekey").build()),
            (Function<S3AsyncClient, Future<?>>)
                c ->
                    c.getObject(
                        GetObjectRequest.builder().bucket("somebucket").key("somekey").build(),
                        AsyncResponseTransformer.toBytes()),
            "1234567890"));
  }

  @ParameterizedTest
  @MethodSource("provideS3Arguments")
  void testS3SendOperationRequestWithBuilder(
      String operation, String method, Function<S3Client, Object> call) throws Exception {
    S3ClientBuilder builder = S3Client.builder();
    if (Boolean.getBoolean("testLatestDeps")) {
      Method forcePathStyleMethod =
          S3ClientBuilder.class.getMethod("forcePathStyle", Boolean.class);
      forcePathStyleMethod.invoke(builder, true);
    }
    configureSdkClient(builder);
    S3Client client =
        builder
            .endpointOverride(clientUri)
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(CREDENTIALS_PROVIDER)
            .build();

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, ""));

    Object response = call.apply(client);

    assertThat(response.getClass().getSimpleName())
        .satisfiesAnyOf(
            v -> assertThat(v).startsWith(operation),
            v -> assertThat(response).isInstanceOf(ResponseInputStream.class));
    clientAssertions("S3", operation, method, response, "UNKNOWN");
  }

  @ParameterizedTest
  @MethodSource("provideS3Arguments")
  void testS3AsyncSendOperationRequestWithBuilder(
      String operation,
      String method,
      Function<S3Client, Object> call,
      Function<S3AsyncClient, Future<?>> asyncCall,
      String body)
      throws NoSuchMethodException,
          InvocationTargetException,
          IllegalAccessException,
          ExecutionException,
          InterruptedException {
    S3AsyncClientBuilder builder = S3AsyncClient.builder();
    if (Boolean.getBoolean("testLatestDeps")) {
      Method forcePathStyleMethod =
          S3AsyncClientBuilder.class.getMethod("forcePathStyle", Boolean.class);
      forcePathStyleMethod.invoke(builder, true);
    }
    configureSdkClient(builder);
    S3AsyncClient client =
        builder
            .endpointOverride(clientUri)
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(CREDENTIALS_PROVIDER)
            .build();

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, body));

    Future<?> response = asyncCall.apply(client);
    response.get();

    clientAssertions("S3", operation, method, response, "UNKNOWN");
  }

  @Test
  void testKinesisSendOperationRequestWithBuilder() {
    KinesisClientBuilder builder = KinesisClient.builder();
    configureSdkClient(builder);
    KinesisClient client =
        builder
            .endpointOverride(clientUri)
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(CREDENTIALS_PROVIDER)
            .build();

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, ""));

    Object response =
        client.deleteStream(DeleteStreamRequest.builder().streamName("somestream").build());

    assertThat(response.getClass().getSimpleName())
        .satisfiesAnyOf(
            v -> assertThat(v).startsWith("DeleteStream"),
            v -> assertThat(response).isInstanceOf(ResponseInputStream.class));
    clientAssertions("Kinesis", "DeleteStream", "POST", response, "UNKNOWN");
  }

  private static Stream<Arguments> provideSqsArguments() {
    return Stream.of(
        Arguments.of(
            "CreateQueue",
            "7a62c49f-347e-4fc4-9331-6e8e7a96aa73",
            (Callable<HttpResponse>)
                () -> {
                  String content;
                  if (!Boolean.getBoolean("testLatestDeps")) {
                    content =
                        "<CreateQueueResponse>"
                            + " <CreateQueueResult><QueueUrl>https://queue.amazonaws.com/123456789012/MyQueue</QueueUrl></CreateQueueResult>"
                            + "   <ResponseMetadata><RequestId>7a62c49f-347e-4fc4-9331-6e8e7a96aa73</RequestId></ResponseMetadata>"
                            + " </CreateQueueResponse>";
                    return HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, content);
                  }
                  content =
                      "{"
                          + " \"QueueUrl\":\"https://queue.amazonaws.com/123456789012/MyQueue\""
                          + "}";
                  ResponseHeaders headers =
                      ResponseHeaders.builder(HttpStatus.OK)
                          .contentType(MediaType.PLAIN_TEXT_UTF_8)
                          .add("x-amzn-RequestId", "7a62c49f-347e-4fc4-9331-6e8e7a96aa73")
                          .build();
                  return HttpResponse.of(headers, HttpData.of(StandardCharsets.UTF_8, content));
                },
            (Function<SqsClient, Object>)
                c -> c.createQueue(CreateQueueRequest.builder().queueName("somequeue").build())),
        Arguments.of(
            "SendMessage",
            "27daac76-34dd-47df-bd01-1f6e873584a0",
            (Callable<HttpResponse>)
                () -> {
                  String content;
                  if (!Boolean.getBoolean("testLatestDeps")) {
                    content =
                        "<SendMessageResponse>"
                            + " <SendMessageResult>"
                            + "   <MD5OfMessageBody>d41d8cd98f00b204e9800998ecf8427e</MD5OfMessageBody>"
                            + "   <MD5OfMessageAttributes>3ae8f24a165a8cedc005670c81a27295</MD5OfMessageAttributes>"
                            + "   <MessageId>5fea7756-0ea4-451a-a703-a558b933e274</MessageId>"
                            + " </SendMessageResult>"
                            + " <ResponseMetadata><RequestId>27daac76-34dd-47df-bd01-1f6e873584a0</RequestId></ResponseMetadata>"
                            + "</SendMessageResponse>";
                    return HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, content);
                  }
                  content =
                      "{"
                          + " \"MD5OfMessageBody\":\"d41d8cd98f00b204e9800998ecf8427e\","
                          + " \"MD5OfMessageAttributes\":\"3ae8f24a165a8cedc005670c81a27295\","
                          + " \"MessageId\":\"5fea7756-0ea4-451a-a703-a558b933e274\""
                          + "}";
                  ResponseHeaders headers =
                      ResponseHeaders.builder(HttpStatus.OK)
                          .contentType(MediaType.PLAIN_TEXT_UTF_8)
                          .add("x-amzn-RequestId", "27daac76-34dd-47df-bd01-1f6e873584a0")
                          .build();
                  return HttpResponse.of(headers, HttpData.of(StandardCharsets.UTF_8, content));
                },
            (Function<SqsClient, Object>)
                c ->
                    c.sendMessage(
                        SendMessageRequest.builder().queueUrl(QUEUE_URL).messageBody("").build())));
  }

  @ParameterizedTest
  @MethodSource("provideSqsArguments")
  void testSqsSendOperationRequestWithBuilder(
      String operation,
      String requestId,
      Callable<HttpResponse> serverResponse,
      Function<SqsClient, Object> call)
      throws Exception {
    assumeSupportedConfig(operation);

    SqsClientBuilder builder = SqsClient.builder();
    configureSdkClient(builder);
    SqsClient client =
        builder
            .endpointOverride(clientUri)
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(CREDENTIALS_PROVIDER)
            .build();

    server.enqueue(serverResponse.call());
    Object response = call.apply(client);

    assertThat(response.getClass().getSimpleName())
        .satisfiesAnyOf(
            v -> assertThat(v).startsWith(operation),
            v -> assertThat(response).isInstanceOf(ResponseInputStream.class));
    clientAssertions("Sqs", operation, "POST", response, requestId);
  }

  @ParameterizedTest
  @MethodSource("provideSqsArguments")
  void testSqsAsyncSendOperationRequestWithBuilder(
      String operation,
      String requestId,
      Callable<HttpResponse> serverResponse,
      Function<SqsClient, Object> call)
      throws Exception {
    assumeSupportedConfig(operation);

    SqsAsyncClientBuilder builder = SqsAsyncClient.builder();
    configureSdkClient(builder);
    SqsAsyncClient client =
        builder
            .endpointOverride(clientUri)
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(CREDENTIALS_PROVIDER)
            .build();

    server.enqueue(serverResponse.call());
    Object response = call.apply(wrapClient(SqsClient.class, SqsAsyncClient.class, client));

    clientAssertions("Sqs", operation, "POST", response, requestId);
  }

  private static Stream<Arguments> provideSnsArguments() {
    return Stream.of(
        Arguments.of(
            (Function<SnsClient, Object>)
                c ->
                    c.publish(
                        PublishRequest.builder()
                            .message("somemessage")
                            .topicArn("somearn")
                            .build()),
            Arguments.of(
                (Function<SnsClient, Object>)
                    c ->
                        c.publish(
                            PublishRequest.builder()
                                .message("somemessage")
                                .targetArn("somearn")
                                .build()))));
  }

  @ParameterizedTest
  @MethodSource("provideSnsArguments")
  void testSnsSendOperationRequestWithBuilder(Function<SnsClient, Object> call) {
    SnsClientBuilder builder = SnsClient.builder();
    configureSdkClient(builder);
    SnsClient client =
        builder
            .endpointOverride(clientUri)
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(CREDENTIALS_PROVIDER)
            .build();

    String body =
        "<PublishResponse xmlns=\"https://sns.amazonaws.com/doc/2010-03-31/\">"
            + "    <PublishResult>"
            + "        <MessageId>567910cd-659e-55d4-8ccb-5aaf14679dc0</MessageId>"
            + "    </PublishResult>"
            + "    <ResponseMetadata>"
            + "        <RequestId>d74b8436-ae13-5ab4-a9ff-ce54dfea72a0</RequestId>"
            + "    </ResponseMetadata>"
            + "</PublishResponse>";

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, body));
    Object response = call.apply(client);

    assertThat(response.getClass().getSimpleName())
        .satisfiesAnyOf(
            v -> assertThat(v).startsWith("Publish"),
            v -> assertThat(response).isInstanceOf(ResponseInputStream.class));
    clientAssertions("Sns", "Publish", "POST", response, "d74b8436-ae13-5ab4-a9ff-ce54dfea72a0");
  }

  @Test
  void testSnsAsyncSendOperationRequestWithBuilder() {
    SnsAsyncClientBuilder builder = SnsAsyncClient.builder();
    configureSdkClient(builder);
    SnsAsyncClient client =
        builder
            .endpointOverride(clientUri)
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(CREDENTIALS_PROVIDER)
            .build();

    String body =
        "<PublishResponse xmlns=\"https://sns.amazonaws.com/doc/2010-03-31/\">"
            + "    <PublishResult>"
            + "        <MessageId>94f20ce6-13c5-43a0-9a9e-ca52d816e90b</MessageId>"
            + "    </PublishResult>"
            + "    <ResponseMetadata>"
            + "        <RequestId>f187a3c1-376f-11df-8963-01868b7c937a</RequestId>"
            + "    </ResponseMetadata>"
            + "</PublishResponse>";

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, body));
    Object response = client.publish(r -> r.message("hello").topicArn("somearn"));

    clientAssertions("Sns", "Publish", "POST", response, "f187a3c1-376f-11df-8963-01868b7c937a");
  }

  @Test
  void testEc2SendOperationRequestWithBuilder() {
    Ec2ClientBuilder builder = Ec2Client.builder();
    configureSdkClient(builder);
    Ec2Client client =
        builder
            .endpointOverride(clientUri)
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(CREDENTIALS_PROVIDER)
            .build();

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, ec2BodyContent));
    Object response = client.allocateAddress();

    assertThat(response.getClass().getSimpleName())
        .satisfiesAnyOf(
            v -> assertThat(v).startsWith("AllocateAddress"),
            v -> assertThat(response).isInstanceOf(ResponseInputStream.class));
    clientAssertions(
        "Ec2", "AllocateAddress", "POST", response, "59dbff89-35bd-4eac-99ed-be587EXAMPLE");
  }

  @Test
  void testEc2AsyncSendOperationRequestWithBuilder() {
    Ec2AsyncClientBuilder builder = Ec2AsyncClient.builder();
    configureSdkClient(builder);
    Ec2AsyncClient client =
        builder
            .endpointOverride(clientUri)
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(CREDENTIALS_PROVIDER)
            .build();

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, ec2BodyContent));
    Object response = client.allocateAddress();

    clientAssertions(
        "Ec2", "AllocateAddress", "POST", response, "59dbff89-35bd-4eac-99ed-be587EXAMPLE");
  }

  @Test
  void testRdsSendOperationRequestWithBuilder() {
    RdsClientBuilder builder = RdsClient.builder();
    configureSdkClient(builder);
    RdsClient client =
        builder
            .endpointOverride(clientUri)
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(CREDENTIALS_PROVIDER)
            .build();

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, rdsBodyContent));
    Object response = client.deleteOptionGroup(DeleteOptionGroupRequest.builder().build());

    assertThat(response.getClass().getSimpleName())
        .satisfiesAnyOf(
            v -> assertThat(v).startsWith("DeleteOptionGroup"),
            v -> assertThat(response).isInstanceOf(ResponseInputStream.class));
    clientAssertions(
        "Rds", "DeleteOptionGroup", "POST", response, "0ac9cda2-bbf4-11d3-f92b-31fa5e8dbc99");
  }

  @Test
  void testRdsAsyncSendOperationRequestWithBuilder() {
    RdsAsyncClientBuilder builder = RdsAsyncClient.builder();
    configureSdkClient(builder);
    RdsAsyncClient client =
        builder
            .endpointOverride(clientUri)
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(CREDENTIALS_PROVIDER)
            .build();

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, rdsBodyContent));
    Object response = client.deleteOptionGroup(DeleteOptionGroupRequest.builder().build());

    clientAssertions(
        "Rds", "DeleteOptionGroup", "POST", response, "0ac9cda2-bbf4-11d3-f92b-31fa5e8dbc99");
  }

  // TODO: Without AOP instrumentation of the HTTP client, we cannot model retries as
  // spans because of https://github.com/aws/aws-sdk-java-v2/issues/1741. We should at least tweak
  // the instrumentation to add Events for retries instead.
  @Test
  void testTimeoutAndRetryErrorsAreNotCaptured() {
    // One retry so two requests.
    server.enqueue(HttpResponse.delayed(HttpResponse.of(HttpStatus.OK), Duration.ofSeconds(5)));
    server.enqueue(HttpResponse.delayed(HttpResponse.of(HttpStatus.OK), Duration.ofSeconds(5)));
    S3ClientBuilder builder =
        S3Client.builder()
            .overrideConfiguration(
                createOverrideConfigurationBuilder()
                    .retryPolicy(
                        software.amazon.awssdk.core.retry.RetryPolicy.builder()
                            .numRetries(1)
                            .build())
                    .build())
            .endpointOverride(clientUri)
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(CREDENTIALS_PROVIDER)
            .httpClientBuilder(ApacheHttpClient.builder().socketTimeout(Duration.ofMillis(50)));

    S3Client client = builder.build();

    Throwable thrown =
        catchThrowable(
            () ->
                client.getObject(
                    GetObjectRequest.builder().bucket("somebucket").key("somekey").build()));

    assertThat(thrown).isInstanceOf(SdkClientException.class);

    getTesting()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("S3.GetObject")
                            .hasKind(SpanKind.CLIENT)
                            .hasStatus(StatusData.error())
                            .hasException(thrown)
                            .hasNoParent()
                            .hasAttributesSatisfyingExactly(
                                // Starting with AWS SDK V2 2.18.0, the s3 sdk will prefix the
                                // hostname with the bucket name in case the bucket name is a valid
                                // DNS label, even in the case that we are using an endpoint
                                // override. Previously the sdk was only doing that if endpoint had
                                // "s3" as label in the FQDN. Our test assert both cases so that we
                                // don't need to know what version is being tested.
                                satisfies(
                                    SERVER_ADDRESS,
                                    v -> v.matches("somebucket.localhost|localhost")),
                                satisfies(
                                    URL_FULL,
                                    val ->
                                        val.satisfiesAnyOf(
                                            v ->
                                                assertThat(v)
                                                    .isEqualTo(
                                                        "http://somebucket.localhost:"
                                                            + server.httpPort()
                                                            + "/somekey"),
                                            v ->
                                                assertThat(v)
                                                    .isEqualTo(
                                                        "http://localhost:"
                                                            + server.httpPort()
                                                            + "/somebucket/somekey"))),
                                equalTo(SERVER_PORT, server.httpPort()),
                                equalTo(HTTP_REQUEST_METHOD, "GET"),
                                equalTo(RPC_SYSTEM, "aws-api"),
                                equalTo(RPC_SERVICE, "S3"),
                                equalTo(RPC_METHOD, "GetObject"),
                                equalTo(stringKey("aws.agent"), "java-aws-sdk"),
                                equalTo(stringKey("aws.bucket.name"), "somebucket"))));
  }

  // regression test for
  // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/13124
  // verify that scope is not leaked on exception
  @Test
  void testS3ListNullBucket() {
    S3ClientBuilder builder = S3Client.builder();
    configureSdkClient(builder);
    S3Client client =
        builder
            .endpointOverride(clientUri)
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(CREDENTIALS_PROVIDER)
            .build();

    assertThatThrownBy(() -> client.listObjectsV2(b -> b.bucket(null)))
        .isInstanceOf(SdkException.class);

    assertThat(Context.current()).isEqualTo(Context.root());
  }
}
