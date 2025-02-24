/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.elasticsearch.transport.v5_3.springdata;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.instrumentation.testing.junit.db.SemconvStabilityUtil.maybeStable;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SYSTEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.elasticsearch.cluster.ClusterName.CLUSTER_NAME_SETTING;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.http.BindHttpException;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.transport.BindTransportException;
import org.elasticsearch.transport.Netty3Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import spock.util.environment.Jvm;

@SuppressWarnings("deprecation") // using deprecated semconv
class Elasticsearch53SpringTemplateTest {
  private static final Logger logger =
      LoggerFactory.getLogger(Elasticsearch53SpringTemplateTest.class);

  private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(10);

  @RegisterExtension
  private static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @RegisterExtension static final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();

  private static final String clusterName = UUID.randomUUID().toString();
  private static Node testNode;
  private static ElasticsearchTemplate template;

  @BeforeAll
  static void setUp(@TempDir File esWorkingDir) {
    logger.info("ES work dir: {}", esWorkingDir);

    Settings settings =
        Settings.builder()
            .put("path.home", esWorkingDir.getPath())
            // Since we use listeners to close spans this should make our span closing deterministic
            // which is good for tests
            .put("thread_pool.listener.size", 1)
            .put("transport.type", "netty3")
            .put("http.type", "netty3")
            .put(CLUSTER_NAME_SETTING.getKey(), clusterName)
            .put("discovery.type", "single-node")
            .build();
    testNode =
        new Node(
            new Environment(InternalSettingsPreparer.prepareSettings(settings)),
            Collections.singletonList(Netty3Plugin.class)) {};
    // retry when starting elasticsearch fails with
    // org.elasticsearch.http.BindHttpException: Failed to resolve host [[]]
    // Caused by: java.net.SocketException: No such device (getFlags() failed)
    // or
    // org.elasticsearch.transport.BindTransportException: Failed to resolve host null
    // Caused by: java.net.SocketException: No such device (getFlags() failed)
    await()
        .atMost(Duration.ofSeconds(10))
        .ignoreExceptionsMatching(
            it -> it instanceof BindHttpException || it instanceof BindTransportException)
        .until(
            () -> {
              testNode.start();
              return true;
            });
    Client client = testNode.client();
    testing.runWithSpan(
        "setup",
        () -> {
          // this may potentially create multiple requests and therefore multiple spans, so we wrap
          // this call into a top level trace to get exactly one trace in the result.
          client
              .admin()
              .cluster()
              .prepareHealth()
              .setWaitForYellowStatus()
              .execute()
              .actionGet(TIMEOUT);
          // disable periodic refresh in InternalClusterInfoService as it creates spans that tests
          // don't expect
          client
              .admin()
              .cluster()
              .updateSettings(
                  new ClusterUpdateSettingsRequest()
                      .transientSettings(
                          Collections.singletonMap(
                              "cluster.routing.allocation.disk.threshold_enabled", Boolean.FALSE)));
        });
    testing.waitForTraces(1);
    testing.clearData();

    template = new ElasticsearchTemplate(client);
  }

  @AfterAll
  static void cleanUp() throws Exception {
    testNode.close();
  }

  @BeforeEach
  void prepareTest() {
    // when running on jdk 21 this test occasionally fails with timeout
    Assumptions.assumeTrue(
        Boolean.getBoolean("testLatestDeps") || !Jvm.getCurrent().isJava21Compatible());
  }

  @Test
  void elasticsearchError() {
    String indexName = "invalid-index";
    assertThatThrownBy(() -> template.refresh(indexName))
        .isInstanceOf(IndexNotFoundException.class);

    IndexNotFoundException expectedException = new IndexNotFoundException("no such index");
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("RefreshAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasStatus(StatusData.error())
                        .hasException(expectedException)
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH),
                            equalTo(maybeStable(DB_OPERATION), "RefreshAction"),
                            equalTo(ERROR_TYPE, "org.elasticsearch.index.IndexNotFoundException"),
                            equalTo(stringKey("elasticsearch.action"), "RefreshAction"),
                            equalTo(stringKey("elasticsearch.request"), "RefreshRequest"),
                            equalTo(stringKey("elasticsearch.request.indices"), indexName))));
  }

  @Test
  void elasticsearchGet() {
    String indexName = "test-index";
    String indexType = "test-type";
    String id = "1";

    template.createIndex(indexName);
    autoCleanup.deferCleanup(() -> template.deleteIndex(indexName));
    template
        .getClient()
        .admin()
        .cluster()
        .prepareHealth()
        .setWaitForYellowStatus()
        .execute()
        .actionGet(TIMEOUT);

    NativeSearchQuery query =
        new NativeSearchQueryBuilder()
            .withIndices(indexName)
            .withTypes(indexType)
            .withIds(Collections.singleton(id))
            .build();
    assertThat(template.queryForIds(query)).isEmpty();

    String result =
        template.index(
            new IndexQueryBuilder()
                .withObject(new Doc())
                .withIndexName(indexName)
                .withType(indexType)
                .withId(id)
                .build());
    template.refresh(Doc.class);
    assertThat(result).isEqualTo(id);
    assertThat(template.queryForList(query, Doc.class))
        .satisfiesExactly(doc -> assertThat(doc).isEqualTo(new Doc()));

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
                            equalTo(stringKey("elasticsearch.action"), "CreateIndexAction"),
                            equalTo(stringKey("elasticsearch.request"), "CreateIndexRequest"),
                            equalTo(stringKey("elasticsearch.request.indices"), indexName))),
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
                            equalTo(stringKey("elasticsearch.action"), "ClusterHealthAction"),
                            equalTo(stringKey("elasticsearch.request"), "ClusterHealthRequest"))),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SearchAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH),
                            equalTo(maybeStable(DB_OPERATION), "SearchAction"),
                            equalTo(stringKey("elasticsearch.action"), "SearchAction"),
                            equalTo(stringKey("elasticsearch.request"), "SearchRequest"),
                            equalTo(stringKey("elasticsearch.request.indices"), indexName),
                            equalTo(stringKey("elasticsearch.request.search.types"), indexType))),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("IndexAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH),
                            equalTo(maybeStable(DB_OPERATION), "IndexAction"),
                            equalTo(stringKey("elasticsearch.action"), "IndexAction"),
                            equalTo(stringKey("elasticsearch.request"), "IndexRequest"),
                            equalTo(stringKey("elasticsearch.request.indices"), indexName),
                            equalTo(stringKey("elasticsearch.request.write.type"), indexType),
                            equalTo(longKey("elasticsearch.request.write.version"), -3),
                            equalTo(longKey("elasticsearch.response.status"), 201),
                            equalTo(longKey("elasticsearch.shard.replication.failed"), 0),
                            equalTo(longKey("elasticsearch.shard.replication.successful"), 1),
                            equalTo(longKey("elasticsearch.shard.replication.total"), 2))),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("RefreshAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH),
                            equalTo(maybeStable(DB_OPERATION), "RefreshAction"),
                            equalTo(stringKey("elasticsearch.action"), "RefreshAction"),
                            equalTo(stringKey("elasticsearch.request"), "RefreshRequest"),
                            equalTo(stringKey("elasticsearch.request.indices"), indexName),
                            equalTo(longKey("elasticsearch.shard.broadcast.failed"), 0),
                            equalTo(longKey("elasticsearch.shard.broadcast.successful"), 5),
                            equalTo(longKey("elasticsearch.shard.broadcast.total"), 10))),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SearchAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH),
                            equalTo(maybeStable(DB_OPERATION), "SearchAction"),
                            equalTo(stringKey("elasticsearch.action"), "SearchAction"),
                            equalTo(stringKey("elasticsearch.request"), "SearchRequest"),
                            equalTo(stringKey("elasticsearch.request.indices"), indexName),
                            equalTo(stringKey("elasticsearch.request.search.types"), indexType))));
  }

  @Test
  void resultsExtractor() {
    String indexName = "test-index-extract";
    testing.runWithSpan(
        "setup",
        () -> {
          template.createIndex(indexName);
          autoCleanup.deferCleanup(() -> template.deleteIndex(indexName));
          testNode
              .client()
              .admin()
              .cluster()
              .prepareHealth()
              .setWaitForYellowStatus()
              .execute()
              .actionGet(TIMEOUT);

          template.index(
              new IndexQueryBuilder()
                  .withObject(new Doc(1, "doc a"))
                  .withIndexName(indexName)
                  .withId("a")
                  .build());
          template.index(
              new IndexQueryBuilder()
                  .withObject(new Doc(2, "doc b"))
                  .withIndexName(indexName)
                  .withId("b")
                  .build());
          template.refresh(indexName);
        });
    testing.waitForTraces(1);
    testing.clearData();

    NativeSearchQuery query = new NativeSearchQueryBuilder().withIndices(indexName).build();
    AtomicLong hits = new AtomicLong();
    List<Map<String, Object>> results = new ArrayList<>();
    Map<Integer, Long> bucketTags = new HashMap<>();

    template.query(
        query,
        (ResultsExtractor<Doc>)
            response -> {
              hits.addAndGet(response.getHits().getTotalHits());
              results.addAll(
                  StreamSupport.stream(response.getHits().spliterator(), false)
                      .map(SearchHit::getSource)
                      .collect(Collectors.toList()));
              if (response.getAggregations() != null) {
                InternalNested internalNested = response.getAggregations().get("tag");
                if (internalNested != null) {
                  Terms terms = internalNested.getAggregations().get("count_agg");
                  List<? extends Terms.Bucket> buckets = terms.getBuckets();
                  for (Terms.Bucket bucket : buckets) {
                    bucketTags.put(Integer.valueOf(bucket.getKeyAsString()), bucket.getDocCount());
                  }
                }
              }
              return null;
            });

    assertThat(hits.get()).isEqualTo(2);
    assertThat(results.get(0)).isEqualTo(ImmutableMap.of("id", "2", "data", "doc b"));
    assertThat(results.get(1)).isEqualTo(ImmutableMap.of("id", "1", "data", "doc a"));
    assertThat(bucketTags).isEmpty();

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SearchAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH),
                            equalTo(maybeStable(DB_OPERATION), "SearchAction"),
                            equalTo(stringKey("elasticsearch.action"), "SearchAction"),
                            equalTo(stringKey("elasticsearch.request"), "SearchRequest"),
                            equalTo(stringKey("elasticsearch.request.indices"), indexName))));
  }
}
