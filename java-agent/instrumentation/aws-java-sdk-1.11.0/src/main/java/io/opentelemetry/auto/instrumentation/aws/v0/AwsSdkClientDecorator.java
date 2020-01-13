package io.opentelemetry.auto.instrumentation.aws.v0;

import com.amazonaws.AmazonWebServiceResponse;
import com.amazonaws.Request;
import com.amazonaws.Response;
import io.opentelemetry.auto.api.MoreTags;
import io.opentelemetry.auto.decorator.HttpClientDecorator;
import io.opentelemetry.auto.instrumentation.api.AgentSpan;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AwsSdkClientDecorator extends HttpClientDecorator<Request, Response> {
  public static final AwsSdkClientDecorator DECORATE = new AwsSdkClientDecorator();

  static final String COMPONENT_NAME = "java-aws-sdk";

  private final Map<String, String> serviceNames = new ConcurrentHashMap<>();
  private final Map<Class, String> operationNames = new ConcurrentHashMap<>();

  @Override
  public AgentSpan onRequest(final AgentSpan span, final Request request) {
    // Call super first because we override the resource name below.
    super.onRequest(span, request);

    final String awsServiceName = request.getServiceName();
    final Class<?> awsOperation = request.getOriginalRequest().getClass();

    span.setAttribute("aws.agent", COMPONENT_NAME);
    span.setAttribute("aws.service", awsServiceName);
    span.setAttribute("aws.operation", awsOperation.getSimpleName());
    span.setAttribute("aws.endpoint", request.getEndpoint().toString());

    span.setAttribute(
        MoreTags.RESOURCE_NAME,
        remapServiceName(awsServiceName) + "." + remapOperationName(awsOperation));

    return span;
  }

  @Override
  public AgentSpan onResponse(final AgentSpan span, final Response response) {
    if (response.getAwsResponse() instanceof AmazonWebServiceResponse) {
      final AmazonWebServiceResponse awsResp = (AmazonWebServiceResponse) response.getAwsResponse();
      span.setAttribute("aws.requestId", awsResp.getRequestId());
    }
    return super.onResponse(span, response);
  }

  private String remapServiceName(final String serviceName) {
    if (!serviceNames.containsKey(serviceName)) {
      serviceNames.put(serviceName, serviceName.replace("Amazon", "").trim());
    }
    return serviceNames.get(serviceName);
  }

  private String remapOperationName(final Class<?> awsOperation) {
    if (!operationNames.containsKey(awsOperation)) {
      operationNames.put(awsOperation, awsOperation.getSimpleName().replace("Request", ""));
    }
    return operationNames.get(awsOperation);
  }

  @Override
  protected String service() {
    return COMPONENT_NAME;
  }

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"aws-sdk"};
  }

  @Override
  protected String component() {
    return COMPONENT_NAME;
  }

  @Override
  protected String method(final Request request) {
    return request.getHttpMethod().name();
  }

  @Override
  protected URI url(final Request request) throws URISyntaxException {
    return new URI(request.getEndpoint().toString());
  }

  @Override
  protected String hostname(final Request request) {
    return null;
  }

  @Override
  protected Integer port(final Request request) {
    return null;
  }

  @Override
  protected Integer status(final Response response) {
    return response.getHttpResponse().getStatusCode();
  }
}
