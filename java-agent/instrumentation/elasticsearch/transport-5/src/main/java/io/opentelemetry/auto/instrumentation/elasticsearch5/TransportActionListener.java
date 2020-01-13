package io.opentelemetry.auto.instrumentation.elasticsearch5;

import static io.opentelemetry.auto.instrumentation.elasticsearch.ElasticsearchTransportClientDecorator.DECORATE;

import com.google.common.base.Joiner;
import io.opentelemetry.auto.instrumentation.api.AgentSpan;
import io.opentelemetry.auto.instrumentation.api.Tags;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.DocumentRequest;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.bulk.BulkShardResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.broadcast.BroadcastResponse;
import org.elasticsearch.action.support.nodes.BaseNodesResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;

public class TransportActionListener<T extends ActionResponse> implements ActionListener<T> {

  private final ActionListener<T> listener;
  private final AgentSpan span;

  public TransportActionListener(
      final ActionRequest actionRequest, final ActionListener<T> listener, final AgentSpan span) {
    this.listener = listener;
    this.span = span;
    onRequest(actionRequest);
  }

  private void onRequest(final ActionRequest request) {
    if (request instanceof IndicesRequest) {
      final IndicesRequest req = (IndicesRequest) request;
      if (req.indices() != null) {
        span.setAttribute("elasticsearch.request.indices", Joiner.on(",").join(req.indices()));
      }
    }
    if (request instanceof SearchRequest) {
      final SearchRequest req = (SearchRequest) request;
      span.setAttribute("elasticsearch.request.search.types", Joiner.on(",").join(req.types()));
    }
    if (request instanceof DocumentRequest) {
      final DocumentRequest req = (DocumentRequest) request;
      span.setAttribute("elasticsearch.request.write.type", req.type());
      span.setAttribute("elasticsearch.request.write.routing", req.routing());
    }
  }

  @Override
  public void onResponse(final T response) {
    if (response.remoteAddress() != null) {
      span.setAttribute(Tags.PEER_HOSTNAME, response.remoteAddress().getHost());
      span.setAttribute(Tags.PEER_HOST_IPV4, response.remoteAddress().getAddress());
      span.setAttribute(Tags.PEER_PORT, response.remoteAddress().getPort());
    }

    if (response instanceof GetResponse) {
      final GetResponse resp = (GetResponse) response;
      span.setAttribute("elasticsearch.type", resp.getType());
      span.setAttribute("elasticsearch.id", resp.getId());
      span.setAttribute("elasticsearch.version", resp.getVersion());
    }

    if (response instanceof BroadcastResponse) {
      final BroadcastResponse resp = (BroadcastResponse) response;
      span.setAttribute("elasticsearch.shard.broadcast.total", resp.getTotalShards());
      span.setAttribute("elasticsearch.shard.broadcast.successful", resp.getSuccessfulShards());
      span.setAttribute("elasticsearch.shard.broadcast.failed", resp.getFailedShards());
    }

    if (response instanceof ReplicationResponse) {
      final ReplicationResponse resp = (ReplicationResponse) response;
      span.setAttribute("elasticsearch.shard.replication.total", resp.getShardInfo().getTotal());
      span.setAttribute(
          "elasticsearch.shard.replication.successful", resp.getShardInfo().getSuccessful());
      span.setAttribute("elasticsearch.shard.replication.failed", resp.getShardInfo().getFailed());
    }

    if (response instanceof IndexResponse) {
      span.setAttribute(
          "elasticsearch.response.status", ((IndexResponse) response).status().getStatus());
    }

    if (response instanceof BulkShardResponse) {
      final BulkShardResponse resp = (BulkShardResponse) response;
      span.setAttribute("elasticsearch.shard.bulk.id", resp.getShardId().getId());
      span.setAttribute("elasticsearch.shard.bulk.index", resp.getShardId().getIndexName());
    }

    if (response instanceof BaseNodesResponse) {
      final BaseNodesResponse resp = (BaseNodesResponse) response;
      if (resp.hasFailures()) {
        span.setAttribute("elasticsearch.node.failures", resp.failures().size());
      }
      span.setAttribute("elasticsearch.node.cluster.name", resp.getClusterName().value());
    }

    try {
      listener.onResponse(response);
    } finally {
      DECORATE.beforeFinish(span);
      span.finish();
    }
  }

  @Override
  public void onFailure(final Exception e) {
    DECORATE.onError(span, e);

    try {
      listener.onFailure(e);
    } finally {
      span.finish();
    }
  }
}
