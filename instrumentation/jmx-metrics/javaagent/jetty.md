# Jetty Metrics

Here is the list of metrics based on MBeans exposed by Jetty.

| Metric Name                    | Type          | Attributes   | Description                                          |
|--------------------------------|---------------|--------------|------------------------------------------------------|
| jetty.session.sessionsCreated  | Counter       | resource     | The number of sessions established in total          |
| jetty.session.sessionTimeTotal | Counter       | resource     | The total time sessions have been active             |
| jetty.session.sessionTimeMax   | Gauge         | resource     | The maximum amount of time a session has been active |
| jetty.session.sessionTimeMean  | Gauge         | resource     | The mean time sessions remain active                 |
| jetty.thread.busyThreads       | UpDownCounter |              | The current number of busy threads                   |
| jetty.thread.idleThreads       | UpDownCounter |              | The current number of idle threads                   |
| jetty.thread.maxThreads        | UpDownCounter |              | The maximum number of threads in the pool            |
| jetty.thread.queueSize         | UpDownCounter |              | The current number of threads in the queue           |
| jetty.io.selectCount           | Counter       | resource, id | The number of select calls                           |
| jetty.logging.LoggerCount      | UpDownCounter |              | The number of registered loggers by name             |
