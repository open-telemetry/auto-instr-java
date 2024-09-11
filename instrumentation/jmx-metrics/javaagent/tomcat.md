# Tomcat Metrics

Here is the list of metrics based on MBeans exposed by Tomcat.

| Metric Name                      | Type          | Attributes                 | Description                                                     |
|----------------------------------|---------------|----------------------------|-----------------------------------------------------------------|
| tomcat.session.activeSessions    | UpDownCounter | context                    | The number of active sessions                                   |
| tomcat.request.errorCount        | Gauge         | name                       | The number of errors per second on all request processors       |
| tomcat.request.requestCount      | Gauge         | name                       | The number of requests per second across all request processors |
| tomcat.request.maxTime           | Gauge         | name                       | The longest request processing time                             |
| tomcat.request.processingTime    | Counter       | name                       | Represents the total time for processing all requests           |
| tomcat.network.io                | Counter       | name, network.io.direction | The number of bytes transmitted                                 |
| tomcat.thread.currentThreadCount | UpDownCounter | name                       | Total thread count of the thread pool                           |
| tomcat.thread.currentThreadsBusy | UpDownCounter | name                       | Busy thread count of the thread pool                            |
