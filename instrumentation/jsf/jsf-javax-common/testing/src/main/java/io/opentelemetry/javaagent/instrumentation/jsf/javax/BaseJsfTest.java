/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jsf.javax;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static java.util.Collections.addAll;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpServerUsingTest;
import io.opentelemetry.instrumentation.testing.junit.http.HttpServerInstrumentationExtension;
import io.opentelemetry.sdk.testing.assertj.SpanDataAssert;
import io.opentelemetry.sdk.testing.assertj.TraceAssert;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.UserAgentAttributes;
import io.opentelemetry.testing.internal.armeria.common.AggregatedHttpRequest;
import io.opentelemetry.testing.internal.armeria.common.AggregatedHttpResponse;
import io.opentelemetry.testing.internal.armeria.common.HttpData;
import io.opentelemetry.testing.internal.armeria.common.HttpMethod;
import io.opentelemetry.testing.internal.armeria.common.MediaType;
import io.opentelemetry.testing.internal.armeria.common.QueryParams;
import io.opentelemetry.testing.internal.armeria.common.RequestHeaders;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

public abstract class BaseJsfTest extends AbstractHttpServerUsingTest<Server> {
  @RegisterExtension
  public static final InstrumentationExtension testing =
      HttpServerInstrumentationExtension.forAgent();

  protected abstract String getJsfVersion();

  @BeforeAll
  protected void setUp() {
    startServer();
  }

  @AfterAll
  protected void cleanUp() {
    cleanupServer();
  }

  @Override
  protected Server setupServer() throws Exception {
    List<String> configurationClasses = new ArrayList<>();
    addAll(configurationClasses, WebAppContext.getDefaultConfigurationClasses());
    configurationClasses.add(AnnotationConfiguration.class.getName());

    WebAppContext webAppContext = new WebAppContext();
    webAppContext.setContextPath(getContextPath());
    webAppContext.setConfigurationClasses(configurationClasses);
    // set up test application
    webAppContext.setBaseResource(Resource.newSystemResource("test-app-" + getJsfVersion()));
    // add additional resources for test app
    Resource extraResource = Resource.newSystemResource("test-app-" + getJsfVersion() + "-extra");
    if (extraResource != null) {
      webAppContext.getMetaData().addWebInfJar(extraResource);
    }
    webAppContext.getMetaData().getWebInfClassesDirs().add(Resource.newClassPathResource("/"));

    Server jettyServer = new Server(port);
    jettyServer.setHandler(webAppContext);
    jettyServer.start();

    return jettyServer;
  }

  @Override
  protected String getContextPath() {
    return "/jetty-context";
  }

  @Override
  protected void stopServer(Server server) throws Exception {
    server.stop();
    server.destroy();
  }

  @ParameterizedTest
  @ArgumentsSource(PathTestArgs.class)
  void testPath(String path, String route) {
    AggregatedHttpResponse response =
        client.get(address.resolve(path).toString()).aggregate().join();
    assertThat(response.status().code()).isEqualTo(200);
    assertThat(response.contentUtf8().trim()).isEqualTo("Hello");

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName(getContextPath() + "/hello.xhtml")
                        .hasKind(SpanKind.SERVER)
                        .hasAttributesSatisfyingExactly(
                            equalTo(NetworkAttributes.NETWORK_PROTOCOL_VERSION, "1.1"),
                            equalTo(ServerAttributes.SERVER_ADDRESS, "localhost"),
                            equalTo(ServerAttributes.SERVER_PORT, port),
                            equalTo(NetworkAttributes.NETWORK_PEER_ADDRESS, "127.0.0.1"),
                            satisfies(
                                NetworkAttributes.NETWORK_PEER_PORT,
                                val -> val.isInstanceOf(Long.class)),
                            equalTo(HttpAttributes.HTTP_REQUEST_METHOD, "GET"),
                            equalTo(UrlAttributes.URL_SCHEME, "http"),
                            equalTo(UrlAttributes.URL_PATH, getContextPath() + "/" + path),
                            equalTo(UserAgentAttributes.USER_AGENT_ORIGINAL, TEST_USER_AGENT),
                            equalTo(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200),
                            equalTo(HttpAttributes.HTTP_ROUTE, getContextPath() + "/" + route),
                            satisfies(
                                ClientAttributes.CLIENT_ADDRESS,
                                val ->
                                    val.satisfiesAnyOf(
                                        v -> assertThat(v).isEqualTo(TEST_CLIENT_IP),
                                        v -> assertThat(v).isNull())))));
  }

  static class PathTestArgs implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          Arguments.of("hello.jsf", "*.jsf"), Arguments.of("faces/hello.xhtml", "faces/*"));
    }
  }

  @Test
  void testGreeting() {
    // we need to display the page first before posting data to it
    AggregatedHttpResponse response =
        client.get(address.resolve("greeting.jsf").toString()).aggregate().join();
    Document doc = Jsoup.parse(response.contentUtf8());

    assertThat(response.status().code()).isEqualTo(200);
    assertThat(doc.selectFirst("title").text()).isEqualTo("Hello, World!");

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName(getContextPath() + "/greeting.xhtml")
                        .hasKind(SpanKind.SERVER)
                        .hasNoParent()));

    testing.clearData();

    String viewState = doc.selectFirst("[name=javax.faces.ViewState]").val();
    String formAction = doc.selectFirst("#app-form").attr("action");
    String jsessionid =
        formAction.substring(formAction.indexOf("jsessionid=") + "jsessionid=".length());

    assertThat(viewState).isNotNull();
    assertThat(jsessionid).isNotNull();

    // set up form parameter for post
    QueryParams formBody =
        QueryParams.builder()
            .add("app-form", "app-form")
            // value used for name is returned in app-form:output-message element
            .add("app-form:name", "test")
            .add("app-form:submit", "Say hello")
            .add("app-form_SUBMIT", "1") // MyFaces
            .add("javax.faces.ViewState", viewState)
            .build();

    // use the session created for first request
    AggregatedHttpRequest request2 =
        AggregatedHttpRequest.of(
            RequestHeaders.builder(
                    HttpMethod.POST,
                    address.resolve("greeting.jsf;jsessionid=" + jsessionid).toString())
                .contentType(MediaType.FORM_DATA)
                .build(),
            HttpData.ofUtf8(formBody.toQueryString()));
    AggregatedHttpResponse response2 = client.execute(request2).aggregate().join();
    String responseContent = response2.contentUtf8();
    Document doc2 = Jsoup.parse(responseContent);

    assertThat(response2.status().code()).isEqualTo(200);
    assertThat(doc2.getElementById("app-form:output-message").text()).isEqualTo("Hello test");

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName(getContextPath() + "/greeting.xhtml")
                        .hasKind(SpanKind.SERVER)
                        .hasNoParent(),
                span -> handlerSpan(trace, 0, "#{greetingForm.submit()}", null)));
  }

  List<Consumer<SpanDataAssert>> handlerSpan(
      TraceAssert trace, int parentIndex, String spanName, Exception expectedException) {
    List<Consumer<SpanDataAssert>> assertions =
        new ArrayList<>(
            Arrays.asList(
                span ->
                    span.hasName(spanName)
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(parentIndex))));

    if (expectedException != null) {
      assertions.add(span -> span.hasStatus(StatusData.error()).hasException(expectedException));
    }
    return assertions;
  }

  @Test
  void testException() {
    // we need to display the page first before posting data to it
    AggregatedHttpResponse response =
        client.get(address.resolve("greeting.jsf").toString()).aggregate().join();
    Document doc = Jsoup.parse(response.contentUtf8());

    assertThat(response.status().code()).isEqualTo(200);
    assertThat(doc.selectFirst("title").text()).isEqualTo("Hello, World!");

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName(getContextPath() + "/greeting.xhtml")
                        .hasKind(SpanKind.SERVER)
                        .hasNoParent()));

    testing.clearData();

    String viewState = doc.selectFirst("[name=javax.faces.ViewState]").val();
    String formAction = doc.selectFirst("#app-form").attr("action");
    String jsessionid =
        formAction.substring(formAction.indexOf("jsessionid=") + "jsessionid=".length());

    assertThat(viewState).isNotNull();
    assertThat(jsessionid).isNotNull();

    // set up form parameter for post
    QueryParams formBody =
        QueryParams.builder()
            .add("app-form", "app-form")
            // setting name parameter to "exception" triggers throwing exception in GreetingForm
            .add("app-form:name", "exception")
            .add("app-form:submit", "Say hello")
            .add("app-form_SUBMIT", "1") // MyFaces
            .add("javax.faces.ViewState", viewState)
            .build();

    // use the session created for first request
    AggregatedHttpRequest request2 =
        AggregatedHttpRequest.of(
            RequestHeaders.builder(
                    HttpMethod.POST,
                    address.resolve("greeting.jsf;jsessionid=" + jsessionid).toString())
                .contentType(MediaType.FORM_DATA)
                .build(),
            HttpData.ofUtf8(formBody.toQueryString()));
    AggregatedHttpResponse response2 = client.execute(request2).aggregate().join();

    assertThat(response2.status().code()).isEqualTo(500);
    IllegalStateException expectedException = new IllegalStateException("submit exception");

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName(getContextPath() + "/greeting.xhtml")
                        .hasKind(SpanKind.SERVER)
                        .hasNoParent()
                        .hasStatus(StatusData.error())
                        .hasException(expectedException),
                span -> handlerSpan(trace, 0, "#{greetingForm.submit()}", expectedException)));
  }
}
