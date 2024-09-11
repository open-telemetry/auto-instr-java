# Wildfly Metrics

Here is the list of metrics based on MBeans exposed by Wildfly.

| Metric Name                                        | Type          | Attributes                               | Description                                                             |
|----------------------------------------------------|---------------|------------------------------------------|-------------------------------------------------------------------------|
| wildfly.network.io                                 | Counter       | server, network.io.direction             | Total number of bytes transferred                                       |
| wildfly.request.errorCount                         | Counter       | server, listener                         | The number of 500 responses that have been sent by this listener        |
| wildfly.request.requestCount                       | Counter       | server, listener                         | The number of requests this listener has served                         |
| wildfly.request.processingTime                     | Counter       | server, listener                         | The total processing time of all requests handed by this listener       |
| wildfly.session.expiredSession                     | Counter       | deployment                               | Number of sessions that have expired                                    |
| wildfly.session.rejectedSessions                   | Counter       | deployment                               | Number of rejected sessions                                             |
| wildfly.session.sessionsCreated                    | Counter       | deployment                               | Total sessions created                                                  |
| wildfly.session.activeSessions                     | UpDownCounter | deployment                               | Number of active sessions                                               |
| wildfly.db.client.connection.count                 | Gauge         | data_source, db.client.connections.state | The number of open jdbc connections                                     |
| wildfly.db.client.rollback.count                   | Counter       | cause                                    | The total number of transactions rolled back                            |
| wildfly.db.client.transaction.NumberOfTransactions | Counter       |                                          | The total number of transactions (top-level and nested) created         |
