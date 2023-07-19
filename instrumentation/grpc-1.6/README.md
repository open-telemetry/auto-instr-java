# Settings for the gRPC instrumentation

| System property | Type | Default | Description                                                  |
|---|---|---|--------------------------------------------------------------|
| `otel.instrumentation.grpc.experimental-span-attributes` | Boolean | `false` | Enable the capture of experimental span attributes.          |
| `otel.instrumentation.grpc.propagate-grpc-deadline` | Boolean | `false` | Allow gRPC contexts to propagate cancellation and deadlines. |
