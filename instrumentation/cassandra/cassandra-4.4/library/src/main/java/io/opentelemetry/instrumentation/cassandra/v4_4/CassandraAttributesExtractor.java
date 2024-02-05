/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.cassandra.v4_4;

import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.core.metadata.EndPoint;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint;
import com.datastax.oss.driver.internal.core.metadata.SniEndPoint;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.SemanticAttributes;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CassandraAttributesExtractor
    implements AttributesExtractor<CassandraRequest, ExecutionInfo> {

  private static final Logger logger = LoggerFactory.getLogger(CassandraAttributesExtractor.class);

  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, CassandraRequest request) {}

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      CassandraRequest request,
      @Nullable ExecutionInfo executionInfo,
      @Nullable Throwable error) {
    if (executionInfo == null) {
      return;
    }

    Node coordinator = executionInfo.getCoordinator();
    if (coordinator != null) {
      try {
        updateServerAddressAndPort(attributes, coordinator);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        logger.error("Error while extracting server address and port", e);
      }

      if (coordinator.getDatacenter() != null) {
        attributes.put(SemanticAttributes.DB_CASSANDRA_COORDINATOR_DC, coordinator.getDatacenter());
      }
      if (coordinator.getHostId() != null) {
        attributes.put(
            SemanticAttributes.DB_CASSANDRA_COORDINATOR_ID, coordinator.getHostId().toString());
      }
    }
    attributes.put(
        SemanticAttributes.DB_CASSANDRA_SPECULATIVE_EXECUTION_COUNT,
        executionInfo.getSpeculativeExecutionCount());

    Statement<?> statement = (Statement<?>) executionInfo.getRequest();
    String consistencyLevel;
    DriverExecutionProfile config =
        request.getSession().getContext().getConfig().getDefaultProfile();
    if (statement.getConsistencyLevel() != null) {
      consistencyLevel = statement.getConsistencyLevel().name();
    } else {
      consistencyLevel = config.getString(DefaultDriverOption.REQUEST_CONSISTENCY);
    }
    attributes.put(SemanticAttributes.DB_CASSANDRA_CONSISTENCY_LEVEL, consistencyLevel);

    if (statement.getPageSize() > 0) {
      attributes.put(SemanticAttributes.DB_CASSANDRA_PAGE_SIZE, statement.getPageSize());
    } else {
      int pageSize = config.getInt(DefaultDriverOption.REQUEST_PAGE_SIZE);
      if (pageSize > 0) {
        attributes.put(SemanticAttributes.DB_CASSANDRA_PAGE_SIZE, pageSize);
      }
    }

    Boolean idempotent = statement.isIdempotent();
    if (idempotent == null) {
      idempotent = config.getBoolean(DefaultDriverOption.REQUEST_DEFAULT_IDEMPOTENCE);
    }
    attributes.put(SemanticAttributes.DB_CASSANDRA_IDEMPOTENCE, idempotent);
  }

  private static void updateServerAddressAndPort(AttributesBuilder attributes, Node coordinator)
      throws NoSuchFieldException, IllegalAccessException {
    EndPoint endPoint = coordinator.getEndPoint();
    if (endPoint instanceof DefaultEndPoint) {
      InetSocketAddress address = ((DefaultEndPoint) endPoint).resolve();
      attributes.put(SemanticAttributes.SERVER_ADDRESS, address.getHostName());
      attributes.put(SemanticAttributes.SERVER_PORT, address.getPort());
    } else if (endPoint instanceof SniEndPoint) {
      SniEndPoint sniEndPoint = (SniEndPoint) endPoint;
      Field privateField = sniEndPoint.getClass().getDeclaredField("proxyAddress");
      privateField.setAccessible(true);
      Object object = privateField.get(sniEndPoint);
      if (object instanceof InetSocketAddress) {
        InetSocketAddress address = (InetSocketAddress) object;
        attributes.put(SemanticAttributes.SERVER_ADDRESS, address.getHostName());
        attributes.put(SemanticAttributes.SERVER_PORT, address.getPort());
      }
    }
  }
}
