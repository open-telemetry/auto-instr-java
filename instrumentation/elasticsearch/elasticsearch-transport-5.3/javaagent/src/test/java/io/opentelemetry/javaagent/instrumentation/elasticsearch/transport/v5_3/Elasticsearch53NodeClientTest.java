/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.elasticsearch.transport.v5_3;

import static org.elasticsearch.cluster.ClusterName.CLUSTER_NAME_SETTING;

import io.opentelemetry.javaagent.instrumentation.elasticsearch.transport.AbstractElasticsearchNodeClientTest;
import java.io.File;
import java.util.Collections;
import java.util.UUID;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.transport.Netty3Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Elasticsearch53NodeClientTest extends AbstractElasticsearchNodeClientTest {
  private static final Logger logger = LoggerFactory.getLogger(Elasticsearch53NodeClientTest.class);

  private static final String clusterName = UUID.randomUUID().toString();
  private static Node testNode;
  private static Client client;

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
    startNode(testNode);

    client = testNode.client();
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
  }

  @AfterAll
  static void cleanUp() throws Exception {
    testNode.close();
  }

  @Override
  public Client client() {
    return client;
  }

  /*
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
                                DbIncubatingAttributes.DB_SYSTEM,
                                DbIncubatingAttributes.DbSystemValues.ELASTICSEARCH),
                            equalTo(DbIncubatingAttributes.DB_OPERATION, "ClusterHealthAction"),
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

  @ParameterizedTest
  @MethodSource("errorArguments")
  void elasticsearchError(Runnable action) {
    assertThatThrownBy(() -> testing.runWithSpan("parent", action::run))
        .isInstanceOf(IndexNotFoundException.class)
        .hasMessage("no such index");

    IndexNotFoundException expectedException = new IndexNotFoundException("no such index");
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
                                DbIncubatingAttributes.DB_SYSTEM,
                                DbIncubatingAttributes.DbSystemValues.ELASTICSEARCH),
                            equalTo(DbIncubatingAttributes.DB_OPERATION, "GetAction"),
                            equalTo(ELASTICSEARCH_ACTION, "GetAction"),
                            equalTo(ELASTICSEARCH_REQUEST, "GetRequest"),
                            equalTo(ELASTICSEARCH_REQUEST_INDICES, "invalid-index")),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void elasticsearchGet() {
    String indexName = "test-index";
    String indexType = "test-type";
    String id = "1";

    CreateIndexResponse indexResult = client.admin().indices().prepareCreate(indexName).get();
    assertThat(indexResult.isAcknowledged()).isTrue();

    client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet(TIMEOUT);
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
                                DbIncubatingAttributes.DB_SYSTEM,
                                DbIncubatingAttributes.DbSystemValues.ELASTICSEARCH),
                            equalTo(DbIncubatingAttributes.DB_OPERATION, "CreateIndexAction"),
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
                                DbIncubatingAttributes.DB_SYSTEM,
                                DbIncubatingAttributes.DbSystemValues.ELASTICSEARCH),
                            equalTo(DbIncubatingAttributes.DB_OPERATION, "ClusterHealthAction"),
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
                                DbIncubatingAttributes.DB_SYSTEM,
                                DbIncubatingAttributes.DbSystemValues.ELASTICSEARCH),
                            equalTo(DbIncubatingAttributes.DB_OPERATION, "GetAction"),
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
                            equalTo(
                                DbIncubatingAttributes.DB_SYSTEM,
                                DbIncubatingAttributes.DbSystemValues.ELASTICSEARCH),
                            equalTo(DbIncubatingAttributes.DB_OPERATION, "IndexAction"),
                            equalTo(ELASTICSEARCH_ACTION, "IndexAction"),
                            equalTo(ELASTICSEARCH_REQUEST, "IndexRequest"),
                            equalTo(ELASTICSEARCH_REQUEST_INDICES, indexName),
                            equalTo(
                                AttributeKey.stringKey("elasticsearch.request.write.type"),
                                indexType),
                            equalTo(
                                AttributeKey.longKey("elasticsearch.request.write.version"), -3),
                            equalTo(AttributeKey.longKey("elasticsearch.response.status"), 201),
                            equalTo(
                                AttributeKey.longKey("elasticsearch.shard.replication.total"), 2),
                            equalTo(
                                AttributeKey.longKey("elasticsearch.shard.replication.successful"),
                                1),
                            equalTo(
                                AttributeKey.longKey("elasticsearch.shard.replication.failed"),
                                0))),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("GetAction")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                DbIncubatingAttributes.DB_SYSTEM,
                                DbIncubatingAttributes.DbSystemValues.ELASTICSEARCH),
                            equalTo(DbIncubatingAttributes.DB_OPERATION, "GetAction"),
                            equalTo(ELASTICSEARCH_ACTION, "GetAction"),
                            equalTo(ELASTICSEARCH_REQUEST, "GetRequest"),
                            equalTo(ELASTICSEARCH_REQUEST_INDICES, indexName),
                            equalTo(ELASTICSEARCH_TYPE, indexType),
                            equalTo(ELASTICSEARCH_ID, id),
                            equalTo(ELASTICSEARCH_VERSION, 1))));
  }

   */
}
