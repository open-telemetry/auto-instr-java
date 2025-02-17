/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jdbc.datasource;

import static io.opentelemetry.instrumentation.testing.junit.db.DbClientMetricsTestUtil.assertDurationMetric;
import static io.opentelemetry.instrumentation.testing.junit.db.SemconvStabilityUtil.maybeStable;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_NAME;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_NAMESPACE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION_BATCH_SIZE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION_NAME;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SQL_TABLE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_STATEMENT;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SYSTEM_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import io.opentelemetry.instrumentation.jdbc.internal.OpenTelemetryConnection;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.LibraryInstrumentationExtension;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

@SuppressWarnings("deprecation") // using deprecated semconv
class JdbcTelemetryTest {

  @RegisterExtension
  static final InstrumentationExtension testing = LibraryInstrumentationExtension.create();

  @Test
  void buildWithDefaults() throws SQLException {
    JdbcTelemetry telemetry = JdbcTelemetry.builder(testing.getOpenTelemetry()).build();
    DataSource dataSource = telemetry.wrap(new TestDataSource());

    testing.runWithSpan(
        "parent", () -> dataSource.getConnection().createStatement().execute("SELECT 1;"));

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent"),
                span -> span.hasName("TestDataSource.getConnection"),
                span ->
                    span.hasName("SELECT dbname")
                        .hasAttribute(equalTo(maybeStable(DB_STATEMENT), "SELECT ?;"))));

    assertDurationMetric(
        testing,
        "io.opentelemetry.jdbc",
        DB_NAMESPACE,
        DB_OPERATION_NAME,
        DB_SYSTEM_NAME,
        SERVER_ADDRESS,
        SERVER_PORT);
  }

  @Test
  void error() throws SQLException {
    JdbcTelemetry telemetry = JdbcTelemetry.builder(testing.getOpenTelemetry()).build();
    DataSource source = spy(new TestDataSource());
    Connection connection = spy(source.getConnection());
    Statement statement = spy(connection.createStatement());
    when(source.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    doThrow(new SQLException("BOOM", "state", 42)).when(statement).execute(Mockito.anyString());
    DataSource dataSource = telemetry.wrap(source);

    assertThatCode(
        () -> testing.runWithSpan(
            "parent", () -> dataSource.getConnection()
                    .createStatement().execute("SELECT 1;"))
    ).isInstanceOf(SQLException.class);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent"),
                span -> span.hasName("TestDataSource.getConnection"),
                span ->
                    span.hasName("SELECT dbname")
                        .hasAttribute(equalTo(DB_RESPONSE_STATUS_CODE, "42"))
                        .hasAttribute(equalTo(ERROR_TYPE, "java.sql.SQLException"))
            ));

    assertDurationMetric(
        testing,
        "io.opentelemetry.jdbc",
        DB_NAMESPACE,
        DB_OPERATION_NAME,
        DB_RESPONSE_STATUS_CODE,
        DB_SYSTEM_NAME,
        ERROR_TYPE,
        SERVER_ADDRESS,
        SERVER_PORT);
  }

  @Test
  void buildWithAllInstrumentersDisabled() throws SQLException {
    JdbcTelemetry telemetry =
        JdbcTelemetry.builder(testing.getOpenTelemetry())
            .setDataSourceInstrumenterEnabled(false)
            .setStatementInstrumenterEnabled(false)
            .build();

    DataSource dataSource = telemetry.wrap(new TestDataSource());

    testing.runWithSpan(
        "parent", () -> dataSource.getConnection().createStatement().execute("SELECT 1;"));

    testing.waitAndAssertTraces(
        trace -> trace.hasSpansSatisfyingExactly(span -> span.hasName("parent")));
  }

  @Test
  void buildWithDataSourceInstrumenterDisabled() throws SQLException {
    JdbcTelemetry telemetry =
        JdbcTelemetry.builder(testing.getOpenTelemetry())
            .setDataSourceInstrumenterEnabled(false)
            .build();

    DataSource dataSource = telemetry.wrap(new TestDataSource());

    testing.runWithSpan(
        "parent", () -> dataSource.getConnection().createStatement().execute("SELECT 1;"));

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent"), span -> span.hasName("SELECT dbname")));
  }

  @Test
  void buildWithStatementInstrumenterDisabled() throws SQLException {
    JdbcTelemetry telemetry =
        JdbcTelemetry.builder(testing.getOpenTelemetry())
            .setStatementInstrumenterEnabled(false)
            .build();

    DataSource dataSource = telemetry.wrap(new TestDataSource());

    testing.runWithSpan(
        "parent", () -> dataSource.getConnection().createStatement().execute("SELECT 1;"));

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent"),
                span -> span.hasName("TestDataSource.getConnection")));
  }

  @Test
  void buildWithSanitizationDisabled() throws SQLException {
    JdbcTelemetry telemetry =
        JdbcTelemetry.builder(testing.getOpenTelemetry())
            .setStatementSanitizationEnabled(false)
            .build();

    DataSource dataSource = telemetry.wrap(new TestDataSource());

    testing.runWithSpan(
        "parent", () -> dataSource.getConnection().createStatement().execute("SELECT 1;"));

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent"),
                span -> span.hasName("TestDataSource.getConnection"),
                span ->
                    span.hasName("SELECT dbname")
                        .hasAttribute(equalTo(maybeStable(DB_STATEMENT), "SELECT 1;"))));
  }

  @Test
  void statementReturnsWrappedConnection() throws SQLException {
    JdbcTelemetry telemetry = JdbcTelemetry.builder(testing.getOpenTelemetry()).build();
    DataSource dataSource = telemetry.wrap(new TestDataSource());
    Connection connection = dataSource.getConnection();
    Statement statement = connection.createStatement();
    assertThat(statement.getConnection()).isInstanceOf(OpenTelemetryConnection.class);
    PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1");
    assertThat(preparedStatement.getConnection()).isInstanceOf(OpenTelemetryConnection.class);
    CallableStatement callableStatement = connection.prepareCall("SELECT 1");
    assertThat(callableStatement.getConnection()).isInstanceOf(OpenTelemetryConnection.class);
  }

  @Test
  void batchStatement() throws SQLException {
    JdbcTelemetry telemetry = JdbcTelemetry.builder(testing.getOpenTelemetry()).build();
    DataSource dataSource = telemetry.wrap(new TestDataSource());

    testing.runWithSpan(
        "parent",
        () -> {
          Statement statement = dataSource.getConnection().createStatement();
          statement.addBatch("INSERT INTO invalid VALUES(1)");
          statement.clearBatch();
          statement.addBatch("INSERT INTO test VALUES(1)");
          statement.addBatch("INSERT INTO test VALUES(2)");
          statement.executeBatch();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent"),
                span -> span.hasName("TestDataSource.getConnection"),
                span ->
                    span.hasName(
                            SemconvStability.emitStableDatabaseSemconv()
                                ? "BATCH INSERT dbname.test"
                                : "dbname")
                        .hasAttributesSatisfying(
                            equalTo(maybeStable(DB_NAME), "dbname"),
                            equalTo(
                                maybeStable(DB_OPERATION),
                                SemconvStability.emitStableDatabaseSemconv()
                                    ? "BATCH INSERT"
                                    : null),
                            equalTo(
                                maybeStable(DB_SQL_TABLE),
                                SemconvStability.emitStableDatabaseSemconv() ? "test" : null),
                            equalTo(
                                maybeStable(DB_STATEMENT),
                                SemconvStability.emitStableDatabaseSemconv()
                                    ? "INSERT INTO test VALUES(?)"
                                    : null),
                            equalTo(
                                DB_OPERATION_BATCH_SIZE,
                                SemconvStability.emitStableDatabaseSemconv() ? 2L : null))));
  }
}
