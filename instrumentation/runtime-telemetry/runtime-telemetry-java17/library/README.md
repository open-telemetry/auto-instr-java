The main entry point is the `RuntimeMetrics` class in the package `io.opentelemetry.instrumentation.runtimemetrics.java17`:

```java
// Initialize JfrTelemetry
RuntimeMetrics runtimeMetrics = RuntimeMetrics.create(openTelemetry);

// Close JfrTelemetry to stop listening for JFR events
runtimeMetrics.close();
```

`RuntimeMetrics` uses two underlying implementations to gather the full set of metric data, JFR and JMX.
The metrics gathered by the two implementations are mutually exclusive and the union of them produces
the full set of available metrics.
The JMX component is reused from the `io.opentelemetry.instrumentation.runtimemetrics.java8` package.
The JFR component uses JFR streaming and is only available in JAVA 17.
It works by subscribing to certain JFR events, and using relevant bits of information
from the events to produce telemetry data like metrics. The code is divided into "handlers", which
listen for specific events and produce relevant telemetry. The handlers are organized into
features (i.e `JfrFeature`), which represent a category of telemetry and can be toggled on and
off. `RuntimeMetrics` evaluates which features are enabled, and only listens for the events required
by the handlers associated with those features.

Enable or disable a feature as follows:

```
RuntimeMetrics runtimeMetrics = RuntimeMetrics.builder(openTelemetry)
  .enableFeature(JfrFeature.BUFFER_METRICS)
  .disableFeature(JfrFeature.LOCK_METRICS)
  .build();
```

The following table describes the set of `JfrFeatures` available, whether each is enabled by
default, and the telemetry each produces:

<!-- DO NOT MANUALLY EDIT. Regenerate table following changes to instrumentation using ./gradlew generateDocs -->
<!-- generateDocsStart -->

| JfrFeature                | Default Enabled | Metrics                                                                                                           |
|---------------------------|-----------------|-------------------------------------------------------------------------------------------------------------------|
| BUFFER_METRICS            | `false`         | `jvm.buffer.count`, `jvm.buffer.memory.limit`, `jvm.buffer.memory.used`                                           |
| CLASS_LOAD_METRICS        | `false`         | `jvm.class.count`, `jvm.class.loaded`, `jvm.class.unloaded`                                                       |
| CONTEXT_SWITCH_METRICS    | `true`          | `jvm.cpu.context_switch`                                                                                          |
| CPU_COUNT_METRICS         | `true`          | `jvm.cpu.limit`                                                                                                   |
| CPU_UTILIZATION_METRICS   | `false`         | `jvm.cpu.recent_utilization`, `jvm.system.cpu.utilization`                                                        |
| GC_DURATION_METRICS       | `false`         | `jvm.gc.duration`                                                                                                 |
| LOCK_METRICS              | `true`          | `jvm.cpu.longlock`                                                                                                |
| MEMORY_ALLOCATION_METRICS | `true`          | `jvm.memory.allocation`                                                                                           |
| MEMORY_POOL_METRICS       | `false`         | `jvm.memory.committed`, `jvm.memory.init`, `jvm.memory.limit`, `jvm.memory.used`, `jvm.memory.used_after_last_gc` |
| NETWORK_IO_METRICS        | `true`          | `jvm.network.io`, `jvm.network.time`                                                                              |
| THREAD_METRICS            | `false`         | `jvm.thread.count`                                                                                                |
