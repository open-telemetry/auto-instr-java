/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseListener;
import org.opensearch.client.RestClient;
import org.opensearch.testcontainers.OpensearchContainer;
import org.testcontainers.utility.DockerImageName;

public class OpenSearchRestTest {
  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  static OpensearchContainer opensearch;
  static RestClient client;

  static HttpHost httpHost;

  @BeforeAll
  static void setUpOpenSearch()
      throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

    opensearch =
        new OpensearchContainer(DockerImageName.parse("opensearchproject/opensearch:1.3.6"))
            .withSecurityEnabled();
    // limit memory usage
    opensearch.withEnv("OPENSEARCH_JAVA_OPTS", "-Xmx256m -Xms256m");
    opensearch.start();

    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(
        AuthScope.ANY,
        new UsernamePasswordCredentials(opensearch.getUsername(), opensearch.getPassword()));

    SSLContext sslContext =
        SSLContextBuilder.create().loadTrustMaterial(null, new TrustAllStrategy()).build();

    httpHost = HttpHost.create(opensearch.getHttpHostAddress());
    client =
        RestClient.builder(httpHost)
            .setHttpClientConfigCallback(
                httpClientBuilder ->
                    httpClientBuilder
                        .setSSLContext(sslContext)
                        .setDefaultCredentialsProvider(credentialsProvider))
            .build();
  }

  @AfterAll
  static void tearDownOpenSearch() {
    opensearch.stop();
  }

  @Test
  void shouldGetStatusWithTraces() throws IOException {

    client.performRequest(new Request("GET", "_cluster/health"));

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("GET")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.DB_SYSTEM, "opensearch"),
                            equalTo(SemanticAttributes.DB_OPERATION, "GET"),
                            equalTo(SemanticAttributes.DB_STATEMENT, "GET _cluster/health"),
                            equalTo(
                                SemanticAttributes.NET_TRANSPORT,
                                SemanticAttributes.NetTransportValues.IP_TCP)),
                span ->
                    span.hasName("GET")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                SemanticAttributes.NET_TRANSPORT,
                                SemanticAttributes.NetTransportValues.IP_TCP),
                            equalTo(SemanticAttributes.NET_PEER_NAME, httpHost.getHostName()),
                            equalTo(SemanticAttributes.NET_PEER_PORT, httpHost.getPort()),
                            equalTo(SemanticAttributes.HTTP_METHOD, "GET"),
                            equalTo(
                                SemanticAttributes.HTTP_FLAVOR,
                                SemanticAttributes.HttpFlavorValues.HTTP_1_1),
                            equalTo(
                                SemanticAttributes.HTTP_URL, httpHost.toURI() + "/_cluster/health"),
                            equalTo(SemanticAttributes.HTTP_STATUS_CODE, 200L),
                            equalTo(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, 0L),
                            equalTo(SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH, 415L))));
  }

  @Test
  void shouldGetStatusAsyncWithTraces() throws Exception {
    AtomicReference<Response> requestResponse = new AtomicReference<>(null);
    AtomicReference<Exception> exception = new AtomicReference<>(null);
    CountDownLatch countDownLatch = new CountDownLatch(1);

    ResponseListener responseListener =
        new ResponseListener() {
          @Override
          public void onSuccess(Response response) {
            testing.runWithSpan(
                "callback",
                () -> {
                  requestResponse.set(response);
                  countDownLatch.countDown();
                });
          }

          @Override
          public void onFailure(Exception e) {
            testing.runWithSpan(
                "callback",
                () -> {
                  exception.set(e);
                  countDownLatch.countDown();
                });
          }
        };

    testing.runWithSpan(
        "client",
        () -> {
          client.performRequestAsync(new Request("GET", "_cluster/health"), responseListener);
        });
    countDownLatch.await();

    if (exception.get() != null) {
      throw exception.get();
    }

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("client").hasKind(SpanKind.INTERNAL),
                span ->
                    span.hasName("GET")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.DB_SYSTEM, "opensearch"),
                            equalTo(SemanticAttributes.DB_OPERATION, "GET"),
                            equalTo(SemanticAttributes.DB_STATEMENT, "GET _cluster/health"),
                            equalTo(
                                SemanticAttributes.NET_TRANSPORT,
                                SemanticAttributes.NetTransportValues.IP_TCP)),
                span ->
                    span.hasName("GET")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(1))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                SemanticAttributes.NET_TRANSPORT,
                                SemanticAttributes.NetTransportValues.IP_TCP),
                            equalTo(SemanticAttributes.NET_PEER_NAME, httpHost.getHostName()),
                            equalTo(SemanticAttributes.NET_PEER_PORT, httpHost.getPort()),
                            equalTo(SemanticAttributes.HTTP_METHOD, "GET"),
                            equalTo(
                                SemanticAttributes.HTTP_FLAVOR,
                                SemanticAttributes.HttpFlavorValues.HTTP_1_1),
                            equalTo(
                                SemanticAttributes.HTTP_URL, httpHost.toURI() + "/_cluster/health"),
                            equalTo(SemanticAttributes.HTTP_STATUS_CODE, 200L),
                            equalTo(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, 0L),
                            equalTo(SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH, 415L)),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }
}
