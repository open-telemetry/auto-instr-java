/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.twitterutilstats.v23_11;

import com.google.common.annotations.VisibleForTesting;
import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.finagle.stats.StatsReceiverProxy;

public class OtelStatsReceiverProxy implements StatsReceiverProxy {
  private static final OtelStatsReceiver INSTANCE = new OtelStatsReceiver();

  @VisibleForTesting
  static OtelStatsReceiver getInstance() {
    return INSTANCE;
  }

  @Override
  public StatsReceiver self() {
    return INSTANCE;
  }
}
