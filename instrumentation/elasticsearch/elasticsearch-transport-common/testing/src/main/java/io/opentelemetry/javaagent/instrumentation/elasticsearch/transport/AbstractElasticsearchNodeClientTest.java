/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.elasticsearch.transport;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.instrumentation.testing.junit.db.SemconvStabilityUtil.maybeStable;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SYSTEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.util.ThrowingSupplier;
import io.opentelemetry.sdk.testing.assertj.AttributeAssertion;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("deprecation") // using deprecated semconv
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractElasticsearchNodeClientTest extends AbstractElasticsearchClientTest {

  private Stream<Arguments> healthArguments() {
    return Stream.of(
        Arguments.of(
            named(
                "sync",
                (ThrowingSupplier<ClusterHealthStatus, Exception>) this::clusterHealthSync)),
        Arguments.of(
            named(
                "async",
                (ThrowingSupplier<ClusterHealthStatus, Exception>) this::clusterHealthAsync)));
  }

  @ParameterizedTest
  @MethodSource("healthArguments")
  void elasticsearchStatus(ThrowingSupplier<ClusterHealthStatus, Exception> supplier)
      throws Exception {
    ClusterHealthStatus clusterHealthStatus = testing.runWithSpan("parent", supplier);

    assertThat(clusterHealthStatus.name()).isEqualTo("GREEN");

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName("ClusterHealthAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH),
                            equalTo(maybeStable(DB_OPERATION), "ClusterHealthAction"),
                            equalTo(ELASTICSEARCH_ACTION, "ClusterHealthAction"),
                            equalTo(ELASTICSEARCH_REQUEST, "ClusterHealthRequest")),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  private Stream<Arguments> errorArguments() {
    return Stream.of(
        Arguments.of(
            named("sync", (Runnable) () -> prepareGetSync("invalid-index", "test-type", "1"))),
        Arguments.of(
            named("async", (Runnable) () -> prepareGetAsync("invalid-index", "test-type", "1"))));
  }

  protected String getIndexNotFoundMessage() {
    return "no such index";
  }

  @ParameterizedTest
  @MethodSource("errorArguments")
  void elasticsearchError(Runnable action) {
    IndexNotFoundException expectedException =
        new IndexNotFoundException(getIndexNotFoundMessage());
    assertThatThrownBy(() -> testing.runWithSpan("parent", action::run))
        .isInstanceOf(IndexNotFoundException.class)
        .hasMessage(expectedException.getMessage());

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("parent")
                        .hasKind(SpanKind.INTERNAL)
                        .hasNoParent()
                        .hasStatus(StatusData.error())
                        .hasException(expectedException),
                span ->
                    span.hasName("GetAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasStatus(StatusData.error())
                        .hasException(expectedException)
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH),
                            equalTo(maybeStable(DB_OPERATION), "GetAction"),
                            equalTo(ERROR_TYPE, "org.elasticsearch.index.IndexNotFoundException"),
                            equalTo(ELASTICSEARCH_ACTION, "GetAction"),
                            equalTo(ELASTICSEARCH_REQUEST, "GetRequest"),
                            equalTo(ELASTICSEARCH_REQUEST_INDICES, "invalid-index")),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  protected void waitYellowStatus() {
    client()
        .admin()
        .cluster()
        .prepareHealth()
        .setWaitForYellowStatus()
        .execute()
        .actionGet(TIMEOUT);
  }

  @Test
  void elasticsearchGet() {
    String indexName = "test-index";
    String indexType = "test-type";
    String id = "1";

    Client client = client();
    CreateIndexResponse indexResult = client.admin().indices().prepareCreate(indexName).get();
    assertThat(indexResult.isAcknowledged()).isTrue();

    waitYellowStatus();
    GetResponse emptyResult = client.prepareGet(indexName, indexType, id).get();
    assertThat(emptyResult.isExists()).isFalse();
    assertThat(emptyResult.getId()).isEqualTo(id);
    assertThat(emptyResult.getType()).isEqualTo(indexType);
    assertThat(emptyResult.getIndex()).isEqualTo(indexName);

    IndexResponse createResult =
        client.prepareIndex(indexName, indexType, id).setSource(Collections.emptyMap()).get();
    assertThat(createResult.getId()).isEqualTo(id);
    assertThat(createResult.getType()).isEqualTo(indexType);
    assertThat(createResult.getIndex()).isEqualTo(indexName);
    assertThat(createResult.status().getStatus()).isEqualTo(201);
    cleanup.deferCleanup(() -> client.admin().indices().prepareDelete(indexName).get());

    GetResponse result = client.prepareGet(indexName, indexType, id).get();
    assertThat(result.isExists()).isTrue();
    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getType()).isEqualTo(indexType);
    assertThat(result.getIndex()).isEqualTo(indexName);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("CreateIndexAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH),
                            equalTo(maybeStable(DB_OPERATION), "CreateIndexAction"),
                            equalTo(ELASTICSEARCH_ACTION, "CreateIndexAction"),
                            equalTo(ELASTICSEARCH_REQUEST, "CreateIndexRequest"),
                            equalTo(ELASTICSEARCH_REQUEST_INDICES, indexName))),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("ClusterHealthAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH),
                            equalTo(maybeStable(DB_OPERATION), "ClusterHealthAction"),
                            equalTo(ELASTICSEARCH_ACTION, "ClusterHealthAction"),
                            equalTo(ELASTICSEARCH_REQUEST, "ClusterHealthRequest"))),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("GetAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH),
                            equalTo(maybeStable(DB_OPERATION), "GetAction"),
                            equalTo(ELASTICSEARCH_ACTION, "GetAction"),
                            equalTo(ELASTICSEARCH_REQUEST, "GetRequest"),
                            equalTo(ELASTICSEARCH_REQUEST_INDICES, indexName),
                            equalTo(ELASTICSEARCH_TYPE, indexType),
                            equalTo(ELASTICSEARCH_ID, id),
                            equalTo(ELASTICSEARCH_VERSION, -1))),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("IndexAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            addIndexActionAttributes(
                                equalTo(
                                    maybeStable(DB_SYSTEM),
                                    DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH),
                                equalTo(maybeStable(DB_OPERATION), "IndexAction"),
                                equalTo(ELASTICSEARCH_ACTION, "IndexAction"),
                                equalTo(ELASTICSEARCH_REQUEST, "IndexRequest"),
                                equalTo(ELASTICSEARCH_REQUEST_INDICES, indexName),
                                equalTo(stringKey("elasticsearch.request.write.type"), indexType),
                                equalTo(longKey("elasticsearch.response.status"), 201),
                                equalTo(longKey("elasticsearch.shard.replication.total"), 2),
                                equalTo(longKey("elasticsearch.shard.replication.successful"), 1),
                                equalTo(longKey("elasticsearch.shard.replication.failed"), 0)))),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("GetAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH),
                            equalTo(maybeStable(DB_OPERATION), "GetAction"),
                            equalTo(ELASTICSEARCH_ACTION, "GetAction"),
                            equalTo(ELASTICSEARCH_REQUEST, "GetRequest"),
                            equalTo(ELASTICSEARCH_REQUEST_INDICES, indexName),
                            equalTo(ELASTICSEARCH_TYPE, indexType),
                            equalTo(ELASTICSEARCH_ID, id),
                            equalTo(ELASTICSEARCH_VERSION, 1))));
  }

  protected boolean hasWriteVersion() {
    return true;
  }

  private List<AttributeAssertion> addIndexActionAttributes(AttributeAssertion... assertions) {
    List<AttributeAssertion> result = new ArrayList<>(Arrays.asList(assertions));
    if (hasWriteVersion()) {
      result.add(equalTo(longKey("elasticsearch.request.write.version"), -3));
    }
    return result;
  }
}
