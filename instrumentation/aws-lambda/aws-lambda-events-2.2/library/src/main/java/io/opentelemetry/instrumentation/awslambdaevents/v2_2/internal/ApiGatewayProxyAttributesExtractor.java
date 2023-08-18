/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awslambdaevents.v2_2.internal;

import static io.opentelemetry.instrumentation.awslambdacore.v1_0.internal.MapUtils.emptyIfNull;
import static io.opentelemetry.instrumentation.awslambdacore.v1_0.internal.MapUtils.lowercaseMap;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.FAAS_TRIGGER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.USER_AGENT_ORIGINAL;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent.RequestContext;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.awslambdacore.v1_0.AwsLambdaRequest;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.annotation.Nullable;

final class ApiGatewayProxyAttributesExtractor
    implements AttributesExtractor<AwsLambdaRequest, Object> {
  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, AwsLambdaRequest request) {
    if (request.getInput() instanceof APIGatewayProxyRequestEvent) {
      attributes.put(FAAS_TRIGGER, SemanticAttributes.FaasTriggerValues.HTTP);
      onRequest(attributes, new V1Request((APIGatewayProxyRequestEvent) request.getInput()));
    } else if (request.getInput() instanceof APIGatewayV2HTTPEvent) {
      attributes.put(FAAS_TRIGGER, SemanticAttributes.FaasTriggerValues.HTTP);
      onRequest(attributes, new V2Request((APIGatewayV2HTTPEvent) request.getInput()));
    }
  }

  void onRequest(AttributesBuilder attributes, Request request) {
    attributes.put(HTTP_METHOD, request.getHttpMethod());

    Map<String, String> headers = lowercaseMap(request.getHeaders());
    String userAgent = headers.get("user-agent");
    if (userAgent != null) {
      attributes.put(USER_AGENT_ORIGINAL, userAgent);
    }
    String httpUrl = getHttpUrl(request, headers);
    if (httpUrl != null) {
      attributes.put(HTTP_URL, httpUrl);
    }
  }

  private static String getHttpUrl(Request request, Map<String, String> headers) {
    StringBuilder str = new StringBuilder();

    String scheme = headers.get("x-forwarded-proto");
    if (scheme != null) {
      str.append(scheme).append("://");
    }
    String host = headers.get("host");
    if (host != null) {
      str.append(host);
    }
    String path = request.getPath();
    if (path != null) {
      str.append(path);
    }

    try {
      boolean first = true;
      for (Map.Entry<String, String> entry :
          emptyIfNull(request.getQueryStringParameters()).entrySet()) {
        String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name());
        String value = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name());
        str.append(first ? '?' : '&').append(key).append('=').append(value);
        first = false;
      }
    } catch (UnsupportedEncodingException ignored) {
      // Ignore
    }
    return str.length() == 0 ? null : str.toString();
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      AwsLambdaRequest request,
      @Nullable Object response,
      @Nullable Throwable error) {
    if (response instanceof APIGatewayProxyResponseEvent) {
      Integer statusCode = ((APIGatewayProxyResponseEvent) response).getStatusCode();
      if (statusCode != null) {
        attributes.put(HTTP_STATUS_CODE, statusCode);
      }
    } else if (response instanceof APIGatewayV2HTTPResponse) {
      int statusCode = ((APIGatewayV2HTTPResponse) response).getStatusCode();
      if (statusCode != 0) {
        attributes.put(HTTP_STATUS_CODE, statusCode);
      }
    }
  }

  private interface Request {
    String getHttpMethod();

    String getPath();

    Map<String, String> getHeaders();

    Map<String, String> getQueryStringParameters();
  }

  private static class V1Request implements Request {
    private final APIGatewayProxyRequestEvent request;

    private V1Request(APIGatewayProxyRequestEvent request) {
      this.request = request;
    }

    @Override
    public String getHttpMethod() {
      return request.getHttpMethod();
    }

    @Override
    public String getPath() {
      return request.getPath();
    }

    @Override
    public Map<String, String> getHeaders() {
      return request.getHeaders();
    }

    @Override
    public Map<String, String> getQueryStringParameters() {
      return request.getQueryStringParameters();
    }
  }

  private static class V2Request implements Request {
    private final APIGatewayV2HTTPEvent request;

    private V2Request(APIGatewayV2HTTPEvent request) {
      this.request = request;
    }

    @Override
    public String getHttpMethod() {
      RequestContext requestContext = request.getRequestContext();
      RequestContext.Http http = requestContext != null ? requestContext.getHttp() : null;

      return http != null ? http.getMethod() : null;
    }

    @Override
    public String getPath() {
      return request.getRawPath();
    }

    @Override
    public Map<String, String> getHeaders() {
      return request.getHeaders();
    }

    @Override
    public Map<String, String> getQueryStringParameters() {
      return request.getQueryStringParameters();
    }
  }

  ApiGatewayProxyAttributesExtractor() {}
}
