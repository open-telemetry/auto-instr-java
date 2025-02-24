/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2.internal;

import static io.opentelemetry.instrumentation.awssdk.v2_2.internal.AwsSdkRequestType.DYNAMODB;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil;
import io.opentelemetry.instrumentation.api.internal.Timer;
import io.opentelemetry.semconv.HttpAttributes;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * AWS request execution interceptor.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class TracingExecutionInterceptor implements ExecutionInterceptor {

  // copied from AwsIncubatingAttributes
  private static final AttributeKey<String> AWS_REQUEST_ID =
      AttributeKey.stringKey("aws.request_id");

  // the class name is part of the attribute name, so that it will be shaded when used in javaagent
  // instrumentation, and won't conflict with usage outside javaagent instrumentation
  private static final ExecutionAttribute<io.opentelemetry.context.Context> CONTEXT_ATTRIBUTE =
      new ExecutionAttribute<>(TracingExecutionInterceptor.class.getName() + ".Context");
  private static final ExecutionAttribute<io.opentelemetry.context.Context>
      PARENT_CONTEXT_ATTRIBUTE =
          new ExecutionAttribute<>(TracingExecutionInterceptor.class.getName() + ".ParentContext");
  private static final ExecutionAttribute<Scope> SCOPE_ATTRIBUTE =
      new ExecutionAttribute<>(TracingExecutionInterceptor.class.getName() + ".Scope");
  private static final ExecutionAttribute<AwsSdkRequest> AWS_SDK_REQUEST_ATTRIBUTE =
      new ExecutionAttribute<>(TracingExecutionInterceptor.class.getName() + ".AwsSdkRequest");
  static final ExecutionAttribute<SdkHttpRequest> SDK_HTTP_REQUEST_ATTRIBUTE =
      new ExecutionAttribute<>(TracingExecutionInterceptor.class.getName() + ".SdkHttpRequest");
  static final ExecutionAttribute<SdkRequest> SDK_REQUEST_ATTRIBUTE =
      new ExecutionAttribute<>(TracingExecutionInterceptor.class.getName() + ".SdkRequest");
  private static final ExecutionAttribute<RequestSpanFinisher> REQUEST_FINISHER_ATTRIBUTE =
      new ExecutionAttribute<>(TracingExecutionInterceptor.class.getName() + ".RequestFinisher");
  static final ExecutionAttribute<TracingList> TRACING_MESSAGES_ATTRIBUTE =
      new ExecutionAttribute<>(TracingExecutionInterceptor.class.getName() + ".TracingMessages");

  private final Instrumenter<ExecutionAttributes, Response> requestInstrumenter;
  private final Instrumenter<SqsReceiveRequest, Response> consumerReceiveInstrumenter;
  private final Instrumenter<SqsProcessRequest, Response> consumerProcessInstrumenter;
  private final Instrumenter<ExecutionAttributes, Response> producerInstrumenter;
  private final Instrumenter<ExecutionAttributes, Response> dynamoDbInstrumenter;
  private final Instrumenter<ExecutionAttributes, Response> bedrockRuntimeInstrumenter;
  private final boolean captureExperimentalSpanAttributes;

  static final AttributeKey<String> HTTP_ERROR_MSG =
      AttributeKey.stringKey("aws.http.error_message");
  static final String HTTP_FAILURE_EVENT = "HTTP request failure";

  Instrumenter<SqsReceiveRequest, Response> getConsumerReceiveInstrumenter() {
    return consumerReceiveInstrumenter;
  }

  Instrumenter<SqsProcessRequest, Response> getConsumerProcessInstrumenter() {
    return consumerProcessInstrumenter;
  }

  @Nullable
  TextMapPropagator getMessagingPropagator() {
    return messagingPropagator;
  }

  boolean shouldUseXrayPropagator() {
    return useXrayPropagator;
  }

  @Nullable private final TextMapPropagator messagingPropagator;
  private final boolean useXrayPropagator;
  private final boolean recordIndividualHttpError;
  private final FieldMapper fieldMapper;

  @SuppressWarnings("TooManyParameters") // internal method
  public TracingExecutionInterceptor(
      Instrumenter<ExecutionAttributes, Response> requestInstrumenter,
      Instrumenter<SqsReceiveRequest, Response> consumerReceiveInstrumenter,
      Instrumenter<SqsProcessRequest, Response> consumerProcessInstrumenter,
      Instrumenter<ExecutionAttributes, Response> producerInstrumenter,
      Instrumenter<ExecutionAttributes, Response> dynamoDbInstrumenter,
      Instrumenter<ExecutionAttributes, Response> bedrockRuntimeInstrumenter,
      boolean captureExperimentalSpanAttributes,
      TextMapPropagator messagingPropagator,
      boolean useXrayPropagator,
      boolean recordIndividualHttpError) {
    this.requestInstrumenter = requestInstrumenter;
    this.consumerReceiveInstrumenter = consumerReceiveInstrumenter;
    this.consumerProcessInstrumenter = consumerProcessInstrumenter;
    this.producerInstrumenter = producerInstrumenter;
    this.dynamoDbInstrumenter = dynamoDbInstrumenter;
    this.bedrockRuntimeInstrumenter = bedrockRuntimeInstrumenter;
    this.captureExperimentalSpanAttributes = captureExperimentalSpanAttributes;
    this.messagingPropagator = messagingPropagator;
    this.useXrayPropagator = useXrayPropagator;
    this.recordIndividualHttpError = recordIndividualHttpError;
    this.fieldMapper = new FieldMapper();
  }

  @Override
  @SuppressWarnings("deprecation") // need to access deprecated signer
  public SdkRequest modifyRequest(
      Context.ModifyRequest context, ExecutionAttributes executionAttributes) {

    // This is the latest point where we can start the span, since we might need to inject
    // it into the request payload. This means that HTTP attributes need to be captured later.

    io.opentelemetry.context.Context parentOtelContext = io.opentelemetry.context.Context.current();
    SdkRequest request = context.request();

    // the request has already been modified, duplicate interceptor?
    if (executionAttributes.getAttribute(SDK_REQUEST_ATTRIBUTE) != null) {
      return request;
    }

    // Ignore presign request. These requests don't run all interceptor methods and the span created
    // here would never be ended and scope closed.
    if (executionAttributes.getAttribute(
            software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute.PRESIGNER_EXPIRATION)
        != null) {
      return request;
    }

    executionAttributes.putAttribute(SDK_REQUEST_ATTRIBUTE, request);
    AwsSdkRequest awsSdkRequest = AwsSdkRequest.ofSdkRequest(request);
    executionAttributes.putAttribute(AWS_SDK_REQUEST_ATTRIBUTE, awsSdkRequest);
    Instrumenter<ExecutionAttributes, Response> instrumenter =
        getInstrumenter(request, awsSdkRequest);

    if (!instrumenter.shouldStart(parentOtelContext, executionAttributes)) {
      // NB: We also skip injection in case we don't start.
      return request;
    }

    RequestSpanFinisher requestFinisher;
    io.opentelemetry.context.Context otelContext;
    Instant requestStart = Instant.now();
    // Skip creating request span for SqsClient.receiveMessage if there is no parent span and also
    // suppress the span from the underlying http client. Request/http client span appears in a
    // separate trace from message producer/consumer spans if there is no parent span just having
    // a trace with only the request/http client span isn't useful.
    if (Span.fromContextOrNull(parentOtelContext) == null
        && "software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest"
            .equals(request.getClass().getName())) {
      otelContext =
          InstrumenterUtil.suppressSpan(instrumenter, parentOtelContext, executionAttributes);
      requestFinisher =
          (finisherOtelContext, finisherExecutionAttributes, response, exception) -> {
            // generate request span when there was an error
            if (exception != null
                && instrumenter.shouldStart(finisherOtelContext, finisherExecutionAttributes)) {
              InstrumenterUtil.startAndEnd(
                  instrumenter,
                  finisherOtelContext,
                  finisherExecutionAttributes,
                  response,
                  exception,
                  requestStart,
                  Instant.now());
            }
          };
    } else {
      otelContext = instrumenter.start(parentOtelContext, executionAttributes);
      requestFinisher = instrumenter::end;
    }

    executionAttributes.putAttribute(PARENT_CONTEXT_ATTRIBUTE, parentOtelContext);
    executionAttributes.putAttribute(CONTEXT_ATTRIBUTE, otelContext);
    executionAttributes.putAttribute(REQUEST_FINISHER_ATTRIBUTE, requestFinisher);

    Span span = Span.fromContext(otelContext);

    try {
      if (awsSdkRequest != null) {
        executionAttributes.putAttribute(AWS_SDK_REQUEST_ATTRIBUTE, awsSdkRequest);
        fieldMapper.mapToAttributes(request, awsSdkRequest, span);
      }
    } catch (Throwable throwable) {
      requestFinisher.finish(otelContext, executionAttributes, null, throwable);
      clearAttributes(executionAttributes);
      throw throwable;
    }

    SdkRequest modifiedRequest =
        SqsAccess.modifyRequest(request, otelContext, useXrayPropagator, messagingPropagator);
    if (modifiedRequest != null) {
      return modifiedRequest;
    }
    modifiedRequest = SnsAccess.modifyRequest(request, otelContext, messagingPropagator);
    if (modifiedRequest != null) {
      return modifiedRequest;
    }
    modifiedRequest = LambdaAccess.modifyRequest(request, otelContext);
    if (modifiedRequest != null) {
      return modifiedRequest;
    }

    // Insert other special handling here, following the same pattern as SQS and SNS.

    return request;
  }

  @Override
  public void afterMarshalling(
      Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
    // the request has already been modified, duplicate interceptor?
    if (executionAttributes.getAttribute(SCOPE_ATTRIBUTE) != null) {
      return;
    }

    io.opentelemetry.context.Context otelContext = getContext(executionAttributes);
    if (otelContext != null
        && executionAttributes
            .getAttribute(SdkExecutionAttribute.CLIENT_TYPE)
            .equals(ClientType.SYNC)) {
      // We can only activate context for synchronous clients, which allows downstream
      // instrumentation like Apache to know about the SDK span.
      executionAttributes.putAttribute(SCOPE_ATTRIBUTE, otelContext.makeCurrent());
    }
  }

  @Override
  public void beforeTransmission(
      Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
    // In beforeTransmission we get access to the finalized http request, including modifications
    // performed by other interceptors and the message signature.
    // It is unlikely that further modifications are performed by the http client performing the
    // request given that this would require the signature to be regenerated.
    //
    // Since we merge the HTTP attributes into an already started span instead of creating a
    // full child span, we have to do some dirty work here.
    //
    // As per HTTP conventions, we should actually only create spans for the "physical" requests but
    // not for the encompassing logical request, see
    // https://github.com/open-telemetry/opentelemetry-specification/blob/v1.20.0/specification/trace/semantic_conventions/http.md#http-request-retries-and-redirects
    // Specific AWS SDK conventions also don't mention this peculiar hybrid span convention, see
    // https://github.com/open-telemetry/opentelemetry-specification/blob/v1.20.0/specification/trace/semantic_conventions/instrumentation/aws-sdk.md
    //
    // TODO: Consider removing net+http conventions & relying on lower-level client instrumentation

    io.opentelemetry.context.Context otelContext = getContext(executionAttributes);
    if (otelContext == null) {
      // No context, no sense in doing anything else (but this is not expected)
      return;
    }

    SdkHttpRequest httpRequest = context.httpRequest();
    executionAttributes.putAttribute(SDK_HTTP_REQUEST_ATTRIBUTE, httpRequest);

    // We ought to pass the parent of otelContext here, but we didn't store it, and it shouldn't
    // make a difference (unless we start supporting the http.resend_count attribute in this
    // instrumentation, which, logically, we can't on this level of abstraction)
    onHttpRequestAvailable(executionAttributes, otelContext, Span.fromContext(otelContext));
  }

  private static void onHttpResponseAvailable(
      ExecutionAttributes executionAttributes,
      io.opentelemetry.context.Context otelContext,
      Span span,
      SdkHttpResponse httpResponse) {
    // For the httpAttributesExtractor dance, see afterMarshalling
    AttributesBuilder builder = Attributes.builder(); // NB: UnsafeAttributes are package-private
    AwsSdkInstrumenterFactory.httpAttributesExtractor.onEnd(
        builder, otelContext, executionAttributes, new Response(httpResponse), null);
    span.setAllAttributes(builder.build());
  }

  private static void onHttpRequestAvailable(
      ExecutionAttributes executionAttributes,
      io.opentelemetry.context.Context parentContext,
      Span span) {
    AttributesBuilder builder = Attributes.builder(); // NB: UnsafeAttributes are package-private
    AwsSdkInstrumenterFactory.httpAttributesExtractor.onStart(
        builder, parentContext, executionAttributes);
    span.setAllAttributes(builder.build());
  }

  @Override
  public SdkHttpRequest modifyHttpRequest(
      Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {

    SdkHttpRequest httpRequest = context.httpRequest();

    if (!useXrayPropagator) {
      return httpRequest;
    }

    io.opentelemetry.context.Context otelContext = getContext(executionAttributes);
    if (otelContext == null) {
      return httpRequest;
    }

    SdkHttpRequest.Builder builder = httpRequest.toBuilder();
    AwsXrayPropagator.getInstance().inject(otelContext, builder, RequestHeaderSetter.INSTANCE);
    return builder.build();
  }

  @Override
  public Optional<InputStream> modifyHttpResponseContent(
      Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {
    Optional<InputStream> responseBody = context.responseBody();
    if (recordIndividualHttpError) {
      String errorMsg = extractHttpErrorAsEvent(context, executionAttributes);
      if (errorMsg != null) {
        return Optional.of(new ByteArrayInputStream(errorMsg.getBytes(Charset.defaultCharset())));
      }
    }
    return responseBody;
  }

  @Override
  public void afterExecution(
      Context.AfterExecution context, ExecutionAttributes executionAttributes) {

    if (executionAttributes.getAttribute(SDK_HTTP_REQUEST_ATTRIBUTE) != null) {
      // Other special handling could be shortcut-&&ed after this (false is returned if not
      // handled).
      Timer timer = Timer.start();
      SqsAccess.afterReceiveMessageExecution(context, executionAttributes, this, timer);
    }

    io.opentelemetry.context.Context otelContext = getContext(executionAttributes);
    if (otelContext != null) {
      // http request has been changed
      executionAttributes.putAttribute(SDK_HTTP_REQUEST_ATTRIBUTE, context.httpRequest());

      Span span = Span.fromContext(otelContext);
      onSdkResponse(span, context.response(), executionAttributes);

      SdkHttpResponse httpResponse = context.httpResponse();

      onHttpResponseAvailable(
          executionAttributes, otelContext, Span.fromContext(otelContext), httpResponse);
      RequestSpanFinisher finisher = executionAttributes.getAttribute(REQUEST_FINISHER_ATTRIBUTE);
      finisher.finish(
          otelContext, executionAttributes, new Response(httpResponse, context.response()), null);
    }
    clearAttributes(executionAttributes);
  }

  private void onSdkResponse(
      Span span, SdkResponse response, ExecutionAttributes executionAttributes) {
    if (response instanceof AwsResponse) {
      span.setAttribute(AWS_REQUEST_ID, ((AwsResponse) response).responseMetadata().requestId());
    }
    if (captureExperimentalSpanAttributes) {
      AwsSdkRequest sdkRequest = executionAttributes.getAttribute(AWS_SDK_REQUEST_ATTRIBUTE);
      if (sdkRequest != null) {
        fieldMapper.mapToAttributes(response, sdkRequest, span);
      }
    }
  }

  private static String extractHttpErrorAsEvent(
      Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
    io.opentelemetry.context.Context otelContext = getContext(executionAttributes);
    if (otelContext != null) {
      Span span = Span.fromContext(otelContext);
      SdkHttpResponse response = context.httpResponse();

      if (response != null && !response.isSuccessful()) {
        int errorCode = response.statusCode();
        // we want to record the error message from http response
        Optional<InputStream> responseBody = context.responseBody();
        if (responseBody.isPresent()) {
          String errorMsg =
              new BufferedReader(
                      new InputStreamReader(responseBody.get(), Charset.defaultCharset()))
                  .lines()
                  .collect(Collectors.joining("\n"));
          Attributes attributes =
              Attributes.of(
                  HttpAttributes.HTTP_RESPONSE_STATUS_CODE,
                  Long.valueOf(errorCode),
                  HTTP_ERROR_MSG,
                  errorMsg);
          span.addEvent(HTTP_FAILURE_EVENT, attributes);
          return errorMsg;
        }
      }
    }
    return null;
  }

  @Override
  public void onExecutionFailure(
      Context.FailedExecution context, ExecutionAttributes executionAttributes) {
    io.opentelemetry.context.Context otelContext = getContext(executionAttributes);
    if (otelContext != null) {
      RequestSpanFinisher finisher = executionAttributes.getAttribute(REQUEST_FINISHER_ATTRIBUTE);
      finisher.finish(otelContext, executionAttributes, null, context.exception());
    }
    clearAttributes(executionAttributes);
  }

  private static void clearAttributes(ExecutionAttributes executionAttributes) {
    Scope scope = executionAttributes.getAttribute(SCOPE_ATTRIBUTE);
    if (scope != null) {
      scope.close();
    }
    executionAttributes.putAttribute(CONTEXT_ATTRIBUTE, null);
    executionAttributes.putAttribute(PARENT_CONTEXT_ATTRIBUTE, null);
    executionAttributes.putAttribute(AWS_SDK_REQUEST_ATTRIBUTE, null);
    executionAttributes.putAttribute(SDK_HTTP_REQUEST_ATTRIBUTE, null);
    executionAttributes.putAttribute(REQUEST_FINISHER_ATTRIBUTE, null);
    executionAttributes.putAttribute(TRACING_MESSAGES_ATTRIBUTE, null);
  }

  /**
   * Returns the {@link Context} stored in the {@link ExecutionAttributes}, or {@code null} if there
   * is no operation set.
   */
  static io.opentelemetry.context.Context getContext(ExecutionAttributes attributes) {
    return attributes.getAttribute(CONTEXT_ATTRIBUTE);
  }

  static io.opentelemetry.context.Context getParentContext(ExecutionAttributes attributes) {
    return attributes.getAttribute(PARENT_CONTEXT_ATTRIBUTE);
  }

  private Instrumenter<ExecutionAttributes, Response> getInstrumenter(
      SdkRequest request, AwsSdkRequest awsSdkRequest) {
    if (SqsAccess.isSqsProducerRequest(request)) {
      return producerInstrumenter;
    }
    if (BedrockRuntimeAccess.isBedrockRuntimeRequest(request)) {
      return bedrockRuntimeInstrumenter;
    }
    if (awsSdkRequest != null && awsSdkRequest.type() == DYNAMODB) {
      return dynamoDbInstrumenter;
    }
    return requestInstrumenter;
  }

  private interface RequestSpanFinisher {
    void finish(
        io.opentelemetry.context.Context otelContext,
        ExecutionAttributes executionAttributes,
        Response response,
        Throwable exception);
  }
}
