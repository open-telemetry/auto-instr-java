/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kotlinxcoroutines.flow

import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion

fun <REQUEST, RESPONSE> onComplete(flow: Flow<*>, handler: AsyncOperationEndHandler<REQUEST, RESPONSE>, context: Context, request: REQUEST & Any): Flow<*> = flow.onCompletion { cause: Throwable? ->
  handler.handle(context, request, null, cause)
}
