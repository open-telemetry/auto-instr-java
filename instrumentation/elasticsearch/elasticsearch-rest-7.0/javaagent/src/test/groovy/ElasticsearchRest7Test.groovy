/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import groovy.json.JsonSlurper
import io.opentelemetry.instrumentation.test.AgentInstrumentationSpecification
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.apache.http.util.EntityUtils
import org.elasticsearch.client.Request
import org.elasticsearch.client.Response
import org.elasticsearch.client.ResponseListener
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder
import org.testcontainers.elasticsearch.ElasticsearchContainer
import spock.lang.Shared

import java.util.concurrent.CountDownLatch

import static io.opentelemetry.api.trace.SpanKind.CLIENT
import static io.opentelemetry.api.trace.SpanKind.INTERNAL

class ElasticsearchRest7Test extends AgentInstrumentationSpecification {
  @Shared
  ElasticsearchContainer elasticsearch

  @Shared
  HttpHost httpHost

  @Shared
  RestClient client

  def setupSpec() {
    elasticsearch = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:7.10.2")
    // limit memory usage
    elasticsearch.withEnv("ES_JAVA_OPTS", "-Xmx256m -Xms256m")
    elasticsearch.start()

    httpHost = HttpHost.create(elasticsearch.getHttpHostAddress())
    client = RestClient.builder(httpHost)
      .setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
        @Override
        RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder builder) {
          return builder.setConnectTimeout(Integer.MAX_VALUE).setSocketTimeout(Integer.MAX_VALUE)
        }
      })
      .build()
  }

  def cleanupSpec() {
    elasticsearch.stop()
  }

  def "test elasticsearch status"() {
    setup:
    Response response = client.performRequest(new Request("GET", "_cluster/health"))

    Map result = new JsonSlurper().parseText(EntityUtils.toString(response.entity))

    expect:
    result.status == "green"

    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "GET"
          kind CLIENT
          hasNoParent()
          attributes {
            "$SemanticAttributes.DB_SYSTEM" "elasticsearch"
            "$SemanticAttributes.DB_OPERATION" "GET"
            "$SemanticAttributes.DB_STATEMENT" "GET _cluster/health"
            "$SemanticAttributes.NET_TRANSPORT" SemanticAttributes.NetTransportValues.IP_TCP
          }
        }
        span(1) {
          name "GET"
          kind CLIENT
          childOf span(0)
          attributes {
            "$SemanticAttributes.NET_TRANSPORT" SemanticAttributes.NetTransportValues.IP_TCP
            "$SemanticAttributes.NET_PEER_NAME" httpHost.hostName
            "$SemanticAttributes.NET_PEER_PORT" httpHost.port
            "$SemanticAttributes.HTTP_METHOD" "GET"
            "$SemanticAttributes.HTTP_FLAVOR" SemanticAttributes.HttpFlavorValues.HTTP_1_1
            "$SemanticAttributes.HTTP_URL" "${httpHost.toURI()}/_cluster/health"
            "$SemanticAttributes.HTTP_STATUS_CODE" 200
            "$SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH" 0L
            "$SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH" Long
          }
        }
      }
    }
  }

  def "test elasticsearch status async"() {
    setup:
    Response requestResponse = null
    Exception exception = null
    CountDownLatch countDownLatch = new CountDownLatch(1)
    ResponseListener responseListener = new ResponseListener() {
      @Override
      void onSuccess(Response response) {
        runWithSpan("callback") {
          requestResponse = response
          countDownLatch.countDown()
        }
      }

      @Override
      void onFailure(Exception e) {
        runWithSpan("callback") {
          exception = e
          countDownLatch.countDown()
        }
      }
    }
    runWithSpan("parent") {
      client.performRequestAsync(new Request("GET", "_cluster/health"), responseListener)
    }
    countDownLatch.await()

    if (exception != null) {
      throw exception
    }
    Map result = new JsonSlurper().parseText(EntityUtils.toString(requestResponse.entity))

    expect:
    result.status == "green"

    assertTraces(1) {
      trace(0, 4) {
        span(0) {
          name "parent"
          kind INTERNAL
          hasNoParent()
        }
        span(1) {
          name "GET"
          kind CLIENT
          childOf(span(0))
          attributes {
            "$SemanticAttributes.DB_SYSTEM" "elasticsearch"
            "$SemanticAttributes.DB_OPERATION" "GET"
            "$SemanticAttributes.DB_STATEMENT" "GET _cluster/health"
            "$SemanticAttributes.NET_TRANSPORT" SemanticAttributes.NetTransportValues.IP_TCP
          }
        }
        span(2) {
          name "GET"
          kind CLIENT
          childOf span(1)
          attributes {
            "$SemanticAttributes.NET_TRANSPORT" SemanticAttributes.NetTransportValues.IP_TCP
            "$SemanticAttributes.NET_PEER_NAME" httpHost.hostName
            "$SemanticAttributes.NET_PEER_PORT" httpHost.port
            "$SemanticAttributes.HTTP_METHOD" "GET"
            "$SemanticAttributes.HTTP_FLAVOR" SemanticAttributes.HttpFlavorValues.HTTP_1_1
            "$SemanticAttributes.HTTP_URL" "${httpHost.toURI()}/_cluster/health"
            "$SemanticAttributes.HTTP_STATUS_CODE" 200
            "$SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH" 0L
            "$SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH" Long
          }
        }
        span(3) {
          name "callback"
          kind INTERNAL
          childOf(span(0))
        }
      }
    }
  }
}
