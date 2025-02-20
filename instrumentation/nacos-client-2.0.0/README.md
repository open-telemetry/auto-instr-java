## Enhancement Methods
- `com.alibaba.nacos.common.remote.client.grpc.GrpcConnection#request`
- `com.alibaba.nacos.common.remote.client.RpcClient#handleServerRequest`

## Enable Configuration

| Configuration Items                                 | Default Value  |
|:----------------------------------------------------|:---------------|
| `otel.instrumentation.nacos-client.default-enabled` | `false`        |

##  Span Info Details
<table border="1">
  <thead>
    <tr>
      <th>Request Child Class</th>
      <th>SpanName</th>
      <th>Additional Tags</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>InstanceRequest</td>
      <td>Nacos/{$(lnstanceRequest.getType()}</td>
      <td rowspan="5">nacos.namespace nacos.group nacos.service.name</td>
    </tr>
    <tr>
      <td>ServiceQueryRequest</td>
      <td>Nacos/queryService</td>
    </tr>
    <tr>
      <td>SubscribeServiceRequest</td>
      <td>Nacos/subscribeService,Nacos/unsubscribeService</td>
    </tr>
    <tr>
      <td>ServicelistRequest</td>
      <td>Nacos/getServicelist</td>
    </tr>
    <tr>
      <td>ConfigQueryRequest</td>
      <td>Nacos/queryConfig</td>
    </tr>
    <tr>
      <td>ConfigPublishRequest</td>
      <td>Nacos/publishConfig</td>
      <td rowspan="3">nacos.data.id nacos.group nacos.tenant</td>
    </tr>
    <tr>
      <td>ConfigRemoveRequest</td>
      <td>Nacos/removeConfig</td>
    </tr>
    <tr>
      <td>ConfigChangeNotifyRequest</td>
      <td>Nacos/notifyConfigChange</td>
    </tr>
    <tr>
      <td>NotifySubscriberRequest</td>
      <td>Nacos/notifySubscribeChange</td>
      <td>nacos.group nacos.service.name</td>
    </tr>
  </tbody>
</table>
