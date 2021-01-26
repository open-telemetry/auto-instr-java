/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package springdata

import io.opentelemetry.instrumentation.test.InMemoryTraceUtils

import static io.opentelemetry.api.trace.Span.Kind.CLIENT
import static io.opentelemetry.instrumentation.test.utils.TraceUtils.runUnderTrace
import static org.elasticsearch.cluster.ClusterName.CLUSTER_NAME_SETTING

import io.opentelemetry.instrumentation.test.AgentTestRunner
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import java.util.concurrent.atomic.AtomicLong
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.common.io.FileSystemUtils
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.env.Environment
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.node.InternalSettingsPreparer
import org.elasticsearch.node.Node
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.transport.Netty3Plugin
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate
import org.springframework.data.elasticsearch.core.ResultsExtractor
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import spock.lang.Shared

class Elasticsearch53SpringTemplateTest extends AgentTestRunner {
  public static final long TIMEOUT = 10000 // 10 seconds

  // Some ES actions are not caused by clients and seem to just happen from time to time.
  // We will just ignore these actions in traces.
  // TODO: check if other ES tests need this protection and potentially pull this into global class
  public static final Set<String> IGNORED_ACTIONS = ["NodesStatsAction", "IndicesStatsAction"] as Set

  @Shared
  Node testNode
  @Shared
  File esWorkingDir
  @Shared
  String clusterName = UUID.randomUUID().toString()

  @Shared
  ElasticsearchTemplate template

  def setupSpec() {

    esWorkingDir = File.createTempDir("test-es-working-dir-", "")
    esWorkingDir.deleteOnExit()
    println "ES work dir: $esWorkingDir"

    def settings = Settings.builder()
      .put("path.home", esWorkingDir.path)
    // Since we use listeners to close spans this should make our span closing deterministic which is good for tests
      .put("thread_pool.listener.size", 1)
      .put("transport.type", "netty3")
      .put("http.type", "netty3")
      .put(CLUSTER_NAME_SETTING.getKey(), clusterName)
      .build()
    testNode = new Node(new Environment(InternalSettingsPreparer.prepareSettings(settings)), [Netty3Plugin])
    testNode.start()
    runUnderTrace("setup") {
      // this may potentially create multiple requests and therefore multiple spans, so we wrap this call
      // into a top level trace to get exactly one trace in the result.
      testNode.client().admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet(TIMEOUT)
    }
    InMemoryTraceUtils.waitForTraces(1)

    template = new ElasticsearchTemplate(testNode.client())
  }

  def cleanupSpec() {
    testNode?.close()
    if (esWorkingDir != null) {
      FileSystemUtils.deleteSubDirectories(esWorkingDir.toPath())
      esWorkingDir.delete()
    }
  }

  def "test elasticsearch error"() {
    when:
    template.refresh(indexName)

    then:
    thrown IndexNotFoundException

    and:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "RefreshAction"
          kind CLIENT
          errored true
          errorEvent IndexNotFoundException, "no such index"
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key}" "elasticsearch"
            "${SemanticAttributes.DB_OPERATION.key}" "RefreshAction"
            "elasticsearch.action" "RefreshAction"
            "elasticsearch.request" "RefreshRequest"
            "elasticsearch.request.indices" indexName
          }
        }
      }
    }

    where:
    indexName = "invalid-index"
  }

  def "test elasticsearch get"() {
    expect:
    template.createIndex(indexName)
    template.getClient().admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet(TIMEOUT)

    when:
    NativeSearchQuery query = new NativeSearchQueryBuilder()
      .withIndices(indexName)
      .withTypes(indexType)
      .withIds([id])
      .build()

    then:
    template.queryForIds(query) == []

    when:
    def result = template.index(IndexQueryBuilder.newInstance()
      .withObject(new Doc())
      .withIndexName(indexName)
      .withType(indexType)
      .withId(id)
      .build())
    template.refresh(Doc)

    then:
    result == id
    template.queryForList(query, Doc) == [new Doc()]

    and:
    assertTraces(6) {
      trace(0, 1) {
        span(0) {
          name "CreateIndexAction"
          kind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key}" "elasticsearch"
            "${SemanticAttributes.DB_OPERATION.key}" "CreateIndexAction"
            "elasticsearch.action" "CreateIndexAction"
            "elasticsearch.request" "CreateIndexRequest"
            "elasticsearch.request.indices" indexName
          }
        }
      }
      trace(1, 1) {
        span(0) {
          name "ClusterHealthAction"
          kind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key}" "elasticsearch"
            "${SemanticAttributes.DB_OPERATION.key}" "ClusterHealthAction"
            "elasticsearch.action" "ClusterHealthAction"
            "elasticsearch.request" "ClusterHealthRequest"
          }
        }
      }
      trace(2, 1) {
        span(0) {
          name "SearchAction"
          kind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key}" "elasticsearch"
            "${SemanticAttributes.DB_OPERATION.key}" "SearchAction"
            "elasticsearch.action" "SearchAction"
            "elasticsearch.request" "SearchRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.request.search.types" indexType
          }
        }
      }
      trace(3, 2) {
        span(0) {
          name "IndexAction"
          kind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key}" "elasticsearch"
            "${SemanticAttributes.DB_OPERATION.key}" "IndexAction"
            "elasticsearch.action" "IndexAction"
            "elasticsearch.request" "IndexRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.request.write.type" indexType
            "elasticsearch.request.write.version"(-3)
            "elasticsearch.response.status" 201
            "elasticsearch.shard.replication.failed" 0
            "elasticsearch.shard.replication.successful" 1
            "elasticsearch.shard.replication.total" 2
          }
        }
        span(1) {
          name "PutMappingAction"
          kind CLIENT
          childOf span(0)
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key}" "elasticsearch"
            "${SemanticAttributes.DB_OPERATION.key}" "PutMappingAction"
            "elasticsearch.action" "PutMappingAction"
            "elasticsearch.request" "PutMappingRequest"
          }
        }
      }
      trace(4, 1) {
        span(0) {
          name "RefreshAction"
          kind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key}" "elasticsearch"
            "${SemanticAttributes.DB_OPERATION.key}" "RefreshAction"
            "elasticsearch.action" "RefreshAction"
            "elasticsearch.request" "RefreshRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.shard.broadcast.failed" 0
            "elasticsearch.shard.broadcast.successful" 5
            "elasticsearch.shard.broadcast.total" 10
          }
        }
      }
      trace(5, 1) {
        span(0) {
          name "SearchAction"
          kind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key}" "elasticsearch"
            "${SemanticAttributes.DB_OPERATION.key}" "SearchAction"
            "elasticsearch.action" "SearchAction"
            "elasticsearch.request" "SearchRequest"
            "elasticsearch.request.indices" indexName
            "elasticsearch.request.search.types" indexType
          }
        }
      }
    }

    cleanup:
    template.deleteIndex(indexName)

    where:
    indexName = "test-index"
    indexType = "test-type"
    id = "1"
  }

  def "test results extractor"() {
    setup:
    template.createIndex(indexName)
    testNode.client().admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet(TIMEOUT)

    template.index(IndexQueryBuilder.newInstance()
      .withObject(new Doc(id: 1, data: "doc a"))
      .withIndexName(indexName)
      .withId("a")
      .build())
    template.index(IndexQueryBuilder.newInstance()
      .withObject(new Doc(id: 2, data: "doc b"))
      .withIndexName(indexName)
      .withId("b")
      .build())
    template.refresh(indexName)
    InMemoryTraceUtils.waitForTraces(5)
    InMemoryTraceUtils.clear()

    and:
    def query = new NativeSearchQueryBuilder().withIndices(indexName).build()
    def hits = new AtomicLong()
    List<Map<String, Object>> results = []
    def bucketTags = [:]

    when:
    template.query(query, new ResultsExtractor<Doc>() {

      @Override
      Doc extract(SearchResponse response) {
        hits.addAndGet(response.getHits().totalHits())
        results.addAll(response.hits.collect { it.source })
        if (response.getAggregations() != null) {
          InternalNested internalNested = response.getAggregations().get("tag")
          if (internalNested != null) {
            Terms terms = internalNested.getAggregations().get("count_agg")
            Collection<Terms.Bucket> buckets = terms.getBuckets()
            for (Terms.Bucket bucket : buckets) {
              bucketTags.put(Integer.valueOf(bucket.getKeyAsString()), bucket.getDocCount())
            }
          }
        }
        return null
      }
    })

    then:
    hits.get() == 2
    results[0] == [id: "2", data: "doc b"]
    results[1] == [id: "1", data: "doc a"]
    bucketTags == [:]

    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          name "SearchAction"
          kind CLIENT
          attributes {
            "${SemanticAttributes.DB_SYSTEM.key}" "elasticsearch"
            "${SemanticAttributes.DB_OPERATION.key}" "SearchAction"
            "elasticsearch.action" "SearchAction"
            "elasticsearch.request" "SearchRequest"
            "elasticsearch.request.indices" indexName
          }
        }
      }
    }

    cleanup:
    template.deleteIndex(indexName)

    where:
    indexName = "test-index-extract"
  }
}
