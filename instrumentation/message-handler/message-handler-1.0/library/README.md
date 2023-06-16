# Message Handler

This package contains instrumentation for message systems.

The instrumentation will receive or process messages and wrap the calls in a span with appropriate attributes and span links.

## Available Message Receivers
- `SqsMessageReceiver` - Receive SQS messages
