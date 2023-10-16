/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jdbc.datasource;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.jdbc.internal.JdbcInstrumenterFactory;

public class JdbcTelemetryBuilder {

  private final OpenTelemetry openTelemetry;
  private boolean dataSourceInstrumenterEnabled = true;
  private boolean statementInstrumenterEnabled = true;
  private boolean statementSanitizationEnabled = true;

  protected JdbcTelemetryBuilder(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  /** Configures whether spans are created for JDBC Connections. Enabled by default. */
  @CanIgnoreReturnValue
  public JdbcTelemetryBuilder setDataSourceInstrumenterEnabled(boolean enabled) {
    this.dataSourceInstrumenterEnabled = enabled;
    return this;
  }

  /** Configures whether spans are created for JDBC Statements. Enabled by default. */
  @CanIgnoreReturnValue
  public JdbcTelemetryBuilder setStatementInstrumenterEnabled(boolean enabled) {
    this.statementInstrumenterEnabled = enabled;
    return this;
  }

  /** Configures whether JDBC Statements are sanitized. Enabled by default. */
  @CanIgnoreReturnValue
  public JdbcTelemetryBuilder setStatementSanitizationEnabled(boolean enabled) {
    this.statementSanitizationEnabled = enabled;
    return this;
  }

  /** Returns a new {@link JdbcTelemetry} with the settings of this {@link JdbcTelemetryBuilder}. */
  public JdbcTelemetry build() {
    return new JdbcTelemetry(
        JdbcInstrumenterFactory.createDataSourceInstrumenter(
            openTelemetry, dataSourceInstrumenterEnabled),
        JdbcInstrumenterFactory.createStatementInstrumenter(
            openTelemetry, statementInstrumenterEnabled, statementSanitizationEnabled));
  }
}
