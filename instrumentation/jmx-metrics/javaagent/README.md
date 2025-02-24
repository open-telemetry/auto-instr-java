# JMX Metric Insight

This subsystem provides a framework for collecting and reporting metrics provided by
[JMX](https://www.oracle.com/technical-resources/articles/javase/jmx.html) through
local [MBeans](https://docs.oracle.com/javase/tutorial/jmx/mbeans/index.html)
available within the instrumented application. The required MBeans and corresponding metrics can be described using a YAML configuration file. The individual metric configurations allow precise metric selection and identification.

The selected JMX metrics are reported using the Java Agent internal SDK. This means that they share the configuration and metric exporter with other metrics collected by the agent and are controlled by the same properties, for example `otel.metric.export.interval` or `otel.metrics.exporter`.
The Open Telemetry resource description for the metrics reported by JMX Metric Insight will be the same as for other metrics exported by the SDK, while the instrumentation scope will be `io.opentelemetry.jmx`.

To control the time interval between MBean detection attempts, one can use the `otel.jmx.discovery.delay` property, which defines the number of milliseconds to elapse between the first and the next detection cycle. JMX Metric Insight may dynamically adjust the time interval between further attempts, but it guarantees that the MBean discovery will run perpetually.

## Predefined metrics

JMX is a popular metrics technology used throughout the JVM (see [runtime metrics](../../runtime-telemetry/runtime-telemetry-java8/library/README.md)), application servers, third-party libraries, and applications.
JMX Metric Insight comes with a number of predefined configurations containing curated sets of JMX metrics for frequently used application servers or frameworks.
To enable collection of the predefined metrics, specify a list of targets as the value for the `otel.jmx.target.system` property. For example

```bash
$ java -javaagent:path/to/opentelemetry-javaagent.jar \
     -Dotel.jmx.target.system=jetty,kafka-broker \
     ... \
     -jar myapp.jar
```

No targets are enabled by default. The supported target environments are listed below.

- [activemq](activemq.md)
- [camel](camel.md)
- [jetty](jetty.md)
- [kafka-broker](kafka-broker.md)
- [tomcat](tomcat.md)
- [wildfly](wildfly.md)
- [hadoop](hadoop.md)

## Configuration Files

To provide your own metric definitions, create one or more YAML configuration files, and specify their location using the `otel.jmx.config` property. Absolute or relative pathnames can be specified. For example

```bash
$ java -javaagent:path/to/opentelemetry-javaagent.jar \
     -Dotel.jmx.config=path/to/config_file.yaml,more_rules.yaml \
     ... \
     -jar myapp.jar
```

### Basic Syntax

Each configuration file can contain multiple entries (which we call _rules_), defining a number of metrics. Each rule must identify a set of MBeans and the name of the MBean attribute to query, along with additional information on how to report the values. Let's look at a simple example.

```yaml
---
rules:
  - bean: java.lang:type=Threading
    mapping:
      ThreadCount:
        metric: my.own.jvm.thread.count
        type: updowncounter
        desc: The current number of threads
        unit: "1"
```

MBeans are identified by unique [ObjectNames](https://docs.oracle.com/javase/8/docs/api/javax/management/ObjectName.html). In the example above, the object name `java.lang:type=Threading` identifies one of the standard JVM MBeans, which can be used to access a number of internal JVM statistics related to threads. For that MBean, we specify its attribute `ThreadCount` which reflects the number of currently active (alive) threads. The values of this attribute will be reported by a metric named `my.own.jvm.thread.count`. The declared OpenTelemetry type of the metric is declared as `updowncounter` which indicates that the value is a sum which can go up or down over time. Metric description and/or unit can also be specified.

All metrics reported by the service are backed by
[asynchronous instruments](https://opentelemetry.io/docs/reference/specification/metrics/api/#synchronous-and-asynchronous-instruments) which can be a
[Counter](https://opentelemetry.io/docs/reference/specification/metrics/api/#asynchronous-counter),
[UpDownCounter](https://opentelemetry.io/docs/reference/specification/metrics/api/#asynchronous-updowncounter), or a
[Gauge](https://opentelemetry.io/docs/reference/specification/metrics/api/#asynchronous-gauge) (the default).

To figure out what MBeans (or ObjectNames) and their attributes are available for your system, check its documentation, or use a universal MBean browsing tool, such as `jconsole`, available for every JDK version.

### Composite Types

The next example shows how the current heap size can be reported.

```yaml
---
rules:
  - bean: java.lang:type=Memory
    mapping:
      HeapMemoryUsage.used:
        metric: my.own.jvm.heap.used
        type: updowncounter
        desc: The current heap size
        unit: By
      HeapMemoryUsage.max:
        metric: my.own.jvm.heap.max
        type: updowncounter
        desc: The maximum allowed heap size
        unit: By
```

The MBean responsible for memory statistics, identified by ObjectName `java.lang:type=Memory` has an attribute named `HeapMemoryUsage`, which is of a `CompositeType`. This type represents a collection of fields with values (very much like the traditional `struct` data type). To access individual fields of the structure we use a dot which separates the MBean attribute name from the field name. The values are reported in bytes, which here we indicate by `By`. In the above example, the current heap size and the maximum allowed heap size will be reported as two metrics, named `my.own.jvm.heap.used`, and `my.own.jvm.heap.max`.

### Measurement Attributes

A more advanced example shows how to report similar metrics related to individual memory pools. A JVM can use a number of memory pools, some of them are part of the heap, and some are for JVM internal use. The number and the names of the memory pools depend on the JVM vendor, the Java version, and may even depend on the java command line options. Since the memory pools, in general, are unknown, we will use wildcard character for specifying memory pool name (in other words, we will use what is known as an ObjectName pattern).

```yaml
---
rules:
  - bean: java.lang:name=*,type=MemoryPool
    metricAttribute:
      pool: param(name)
      type: beanattr(Type)
    mapping:
      Usage.used:
        metric: my.own.jvm.memory.pool.used
        type: updowncounter
        desc: Pool memory currently used
        unit: By
      Usage.max:
        metric: my.own.jvm.memory.pool.max
        type: updowncounter
        desc: Maximum obtainable memory pool size
        unit: By
```

The ObjectName pattern will match a number of MBeans, each for a different memory pool. The number and names of available memory pools, however, will be known only at runtime. To report values for all actual memory pools using only two metrics, we use metric attributes (referenced by the configuration file as `metricAttribute` elements). The first metric attribute, named `pool` will have its value derived from the ObjectName parameter `name` - which corresponds to the memory pool name. The second metric attribute, named `type` will get its value from the corresponding MBean attribute named `Type`. The values of this attribute are strings `HEAP` or `NON_HEAP` classifying the corresponding memory pool. Here the definition of the metric attributes is shared by both metrics, but it is also possible to define them at the individual metric level.

Using the above rule, when running on HotSpot JVM for Java 11, the following combinations of metric attributes will be reported.

- {pool="Compressed Class Space", type="NON_HEAP"}
- {pool="CodeHeap 'non-profiled nmethods'", type="NON_HEAP"}
- {pool="G1 Eden Space", type="HEAP"}
- {pool="G1 Old Gen", type="HEAP"}
- {pool="CodeHeap 'profiled nmethods'", type="NON_HEAP"}
- {pool="Metaspace", type="NON_HEAP"}
- {pool="CodeHeap 'non-nmethods'", type="NON_HEAP"}
- {pool="G1 Survivor Space", type="HEAP"}

**Note**: Heap and memory pool metrics above are given just as examples. The Java Agent already reports such metrics, no additional configuration is needed from the users.

### Mapping multiple MBean attributes to the same metric

Sometimes it is desired to merge several MBean attributes into a single metric, as shown in the next example.

```yaml
---
rules:
  - bean: Catalina:type=GlobalRequestProcessor,name=*
    metricAttribute:
      handler: param(name)
    type: counter
    mapping:
      bytesReceived:
        metric: catalina.traffic
        metricAttribute:
          direction: const(in)
        desc: The number of transmitted bytes
        unit: By
      bytesSent:
        metric: catalina.traffic
        metricAttribute:
          direction: const(out)
        desc: The number of transmitted bytes
        unit: By
```

The referenced MBean has two attributes of interest, `bytesReceived`, and `bytesSent`. We want them to be reported by just one metric, but keeping the values separate by using metric attribute `direction`. This is achieved by specifying the same metric name `catalina.traffic` when mapping the MBean attributes to metrics. There will be two metric attributes provided: `handler`, which has a shared definition, and `direction`, which has its value (`in` or `out`) declared directly as constants, depending on the MBean attribute providing the metric value.

Keep in mind that when defining a metric multiple times like this, its type, unit and description must be exactly the same. Otherwise there will be complaints about attempts to redefine a metric in a non-compatible way.
The example also demonstrates that when specifying a number of MBean attribute mappings within the same rule, the metric type can be declared only once (outside of the `mapping` section).

Even when not reusing the metric name, special care also has to be taken when using ObjectName patterns (or specifying multiple ObjectNames - see the General Syntax section at the bottom of the page). Different ObjectNames matching the pattern must result in using different metric attribute values. Otherwise the same metric will be reported multiple times (using different metric values), which will likely clobber the previous values.

### Making shortcuts

While it is possible to define MBeans based metrics with fine details, sometimes it is desirable to provide the rules in compact format, minimizing the editing effort, but maintaining their efficiency and accuracy. The accepted YAML syntax allows to define some metric properties once per rule, which may lead to reduction in the amount of typing. This is especially visible if many related MBean attributes need to be covered, and is illustrated by the following example.

```yaml
---
rules:
  - bean: kafka.streams:type=stream-thread-metrics,thread-id=*
    metricAttribute:
      threadId: param(thread-id)
    prefix: my.kafka.streams.
    unit: ms
    mapping:
      commit-latency-avg:
      commit-latency-max:
      poll-latency-avg:
      poll-latency-max:
      process-latency-avg:
      process-latency-max:
      punctuate-latency-avg:
      punctuate-latency-max:
      poll-records-avg:
        unit: "1"
      poll-records-max:
        unit: "1"
  - bean: kafka.streams:type=stream-thread-metrics,thread-id=*
    metricAttribute:
      threadId: param(thread-id)
    prefix: my.kafka.streams.
    unit: /s
    type: gauge
    mapping:
      commit-rate:
      process-rate:
      task-created-rate:
      task-closed-rate:
      skipped-records-rate:
  - bean: kafka.streams:type=stream-thread-metrics,thread-id=*
    metricAttribute:
      threadId: param(thread-id)
    prefix: my.kafka.streams.totals.
    unit: "1"
    type: counter
    mapping:
      commit-total:
      poll-total:
      process-total:
      task-created-total:
      task-closed-total:
```

Because we declared metric prefix (here `my.kafka.streams.`) and did not specify actual metric names, the metric names will be generated automatically, by appending the corresponding MBean attribute name to the prefix.
Thus, the above definitions will create several metrics, named `my.kafka.streams.commit-latency-avg`, `my.kafka.streams.commit-latency-max`, and so on. For the first configuration rule, the default unit has been changed to `ms`, which remains in effect for all MBean attribute mappings listed within the rule, unless they define their own unit. Similarly, the second configuration rule defines the unit as `/s`, valid for all the rates reported.

The metric descriptions will remain undefined, unless they are provided by the queried MBeans.

### State Metrics

Some JMX attributes expose current state as a non-numeric MBean attribute, in order to capture those as metrics it is recommended to use the special `state` metric type.
For example, with Tomcat connector, the `Catalina:type=Connector,port=*` MBean has `stateName` (of type `String`), we can define the following rule:

```yaml
---
rules:
  - bean: Catalina:type=Connector,port=*
    mapping:
      stateName:
        type: state
        metric: tomcat.connector
        metricAttribute:
          port: param(port)
          connector_state:
            ok: STARTED
            failed: [STOPPED,FAILED]
            degraded: '*'
```

For a given value of `port`, let's say `8080` This will capture the `tomcat.connector.state` metric of type `updowncounter` with value `0` or `1` and the `state` metric attribute will have a value in [`ok`,`failed`,`degraded`].
For every sample, 3 metrics will be captured for each value of `state` depending on the value of `stateName`:

When `stateName` = `STARTED`, we have:

- `tomcat.connector` value = `1`, attributes `port` = `8080` and `connector_state` = `ok`
- `tomcat.connector` value = `0`, attributes `port` = `8080` and `connector_state` = `failed`
- `tomcat.connector` value = `0`, attributes `port` = `8080` and `connector_state` = `degraded`

When `stateName` = `STOPPED` or `FAILED`, we have:

- `tomcat.connector` value = `0`, attributes `port` = `8080` and `connector_state` = `ok`
- `tomcat.connector` value = `1`, attributes `port` = `8080` and `connector_state` = `failed`
- `tomcat.connector` value = `0`, attributes `port` = `8080` and `connector_state` = `degraded`

For other values of `stateName`, we have:

- `tomcat.connector` value = `0`, attributes `port` = `8080` and `connector_state` = `ok`
- `tomcat.connector` value = `0`, attributes `port` = `8080` and `connector_state` = `failed`
- `tomcat.connector` value = `1`, attributes `port` = `8080` and `connector_state` = `degraded`

Each state key can be mapped to one or more values of the MBean attribute using:
- a string literal or a string array
- a `*` character to provide default option and avoid enumerating all values, this value must be quoted in YAML

Exactly one `*` value must be present in the mapping to ensure all possible values of the MBean attribute can be mapped to a state key.

The default value indicated by `*` does not require a dedicated state key. For example, if we want to have `connector_state` metric attribute with values `on` or `off`, we can use:
```yaml
          connector_state:
            on: STARTED
            off: [STOPPED,FAILED,'*']
```
In the particular case where only two values are defined, we can simplify further by explicitly defining one state and rely on default for the other.
```yaml
          connector_state:
            on: STARTED
            off: '*'
```

### Metric attributes modifiers

JMX attributes values may require modification or normalization in order to fit semantic conventions.

For example, with JVM memory, the `java.lang:name=*,type=MemoryPool` MBeans have `type` attribute with either `HEAP` or `NON_HEAP` value.
However, in the semantic conventions the metric attribute `jvm.memory.type` should be lower-cased to fit the `jvm.memory.used` definition, in this case we can
apply the `lowercase` metric attribute transformation as follows:


```yaml
---
rules:
  - bean: java.lang:name=*,type=MemoryPool
    mapping:
      Usage.used:
        type: updowncounter
        metric: jvm.memory.used
        unit: By
        metricAttribute:
          jvm.memory.pool.name	: param(name)
          jvm.memory.type: lowercase(beanattr(type))
```

For now, only the `lowercase` transformation is supported, other additions might be added in the future if needed.

### General Syntax

Here is the general description of the accepted configuration file syntax. The whole contents of the file is case-sensitive, with exception for `type` as described in the table below.

```yaml
---
rules:                                # start of list of configuration rules
  - bean: <OBJECTNAME>                # can contain wildcards
    metricAttribute:                  # optional metric attributes, they apply to all metrics below
      <ATTRIBUTE1>: param(<PARAM>)    # <PARAM> is used as the key to extract value from actual ObjectName
      <ATTRIBUTE2>: beanattr(<ATTR>)  # <ATTR> is used as the MBean attribute name to extract the value
      <ATTRIBUTE3>: const(<CONST>)    # <CONST> is used as a constant
    prefix: <METRIC_NAME_PREFIX>      # optional, useful for avoiding specifying metric names below
    unit: <UNIT>                      # optional, redefines the default unit for the whole rule
    type: <TYPE>                      # optional, redefines the default type for the whole rule
    mapping:
      <BEANATTR1>:                    # an MBean attribute name defining the metric value
        metric: <METRIC_NAME1>        # metric name will be <METRIC_NAME_PREFIX><METRIC_NAME1>
        type: <TYPE>                  # optional, the default type is gauge
        desc: <DESCRIPTION1>          # optional
        unit: <UNIT1>                 # optional
        metricAttribute:              # optional, will be used in addition to the shared metric attributes above
          <ATTRIBUTE3>: const(<STR>)  # direct value for the metric attribute
      <BEANATTR2>:                    # use a.b to get access into CompositeData
        metric: <METRIC_NAME2>        # optional, the default is the MBean attribute name
        unit: <UNIT2>                 # optional
      <BEANATTR3>:                    # metric name will be <METRIC_NAME_PREFIX><BEANATTR3>
      <BEANATTR4>:                    # metric name will be <METRIC_NAME_PREFIX><BEANATTR4>
  - beans:                            # alternatively, if multiple object names are needed
      - <OBJECTNAME1>                 # at least one object name must be specified
      - <OBJECTNAME2>
    mapping:
      <BEANATTR5>:                    # an MBean attribute name defining the metric value
        metric: <METRIC_NAME5>        # metric name will be <METRIC_NAME5>
        type: updowncounter           # optional
      <BEANATTR6>:                    # metric name will be <BEANATTR6>
```

The following table explains the used terms with more details.

| Syntactic Element | Description |
| ---------------- | --------------- |
| OBJECTNAME | A syntactically valid string representing an ObjectName (see [ObjectName constructor](https://docs.oracle.com/javase/8/docs/api/javax/management/ObjectName.html#ObjectName-java.lang.String-)). |
| ATTRIBUTE | Any well-formed string that can be used as a metric [attribute](https://opentelemetry.io/docs/reference/specification/common/#attribute) key. |
| ATTR | A non-empty string used as a name of the MBean attribute. The MBean attribute value must be a String, otherwise the specified metric attribute will not be used. |
| PARAM | A non-empty string used as a property key in the ObjectName identifying the MBean which provides the metric value. If the ObjectName does not have a property with the given key, the specified metric attribute will not be used. |
| METRIC_NAME_PREFIX | Any non-empty string which will be prepended to the specified metric (instrument) names. |
| METRIC_NAME | Any non-empty string. The string, prefixed by the optional prefix (see above) must satisfy [instrument naming rule](https://opentelemetry.io/docs/reference/specification/metrics/api/#instrument-naming-rule). |
| TYPE | One of `counter`, `updowncounter`, or `gauge`. The default is `gauge`. This value is case insensitive. |
| DESCRIPTION | Any string to be used as human-readable [description](https://opentelemetry.io/docs/reference/specification/metrics/api/#instrument-description) of the metric. If the description is not provided by the rule, an attempt will be made to extract one automatically from the corresponding MBean. |
| UNIT | A string identifying the [unit](https://opentelemetry.io/docs/reference/specification/metrics/api/#instrument-unit) of measurements reported by the metric. Enclose the string in single or double quotes if using unit annotations. |
| STR | Any string to be used directly as the metric attribute value. |
| BEANATTR | A non-empty string representing the MBean attribute defining the metric value. The attribute value must be a number. Special dot-notation _attributeName.itemName_ can be used to access numerical items within attributes of [CompositeType](https://docs.oracle.com/javase/8/docs/api/javax/management/openmbean/CompositeType.html). If a dot happens to be an integral part of the MBean attribute name, it must be escaped by backslash (`\`). |

## Assumptions and Limitations

This version of JMX Metric Insight has a number of limitations.

- MBean attributes with the same name but belonging to different MBeans described by a single metric rule must have the same type (long or double).
- All MBeans which are described by the specified ObjectNames in a single rule must be registered with the same MBeanServer instance.
- While MBeanServers and MBeans can be created dynamically by the application, it is assumed that they will live indefinitely. Their disappearance may not be recognized properly, and may lead to some memory leaks.
