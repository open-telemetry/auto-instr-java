/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.common.client.internal

import io.opentelemetry.instrumentation.ktor.v2_0.common.client.AbstractKtorClientTelemetryBuilder
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.logging.Level
import java.util.logging.Logger

/**
 * This class is internal and experimental. Its APIs are unstable and can change at any time. Its
 * APIs (or a version of them) may be promoted to the public stable API in the future, but no
 * guarantees are made.
 */
class Experimental {

  companion object {
    private val logger: Logger = Logger.getLogger(Experimental::class.java.name)

    private val emitExperimentalTelemetryMethod = getEmitExperimentalTelemetry()

    fun setEmitExperimentalTelemetry(builder: AbstractKtorClientTelemetryBuilder?, emitExperimentalTelemetry: Boolean) {
      if (emitExperimentalTelemetryMethod != null) {
        try {
          emitExperimentalTelemetryMethod.invoke(builder, emitExperimentalTelemetry)
        } catch (e: IllegalAccessException) {
          logger.log(Level.FINE, e.message, e)
        } catch (e: InvocationTargetException) {
          logger.log(Level.FINE, e.message, e)
        }
      }
    }

    private fun getEmitExperimentalTelemetry(): Method? {
      try {
        val method = AbstractKtorClientTelemetryBuilder::class.java.getDeclaredMethod(
          "setEmitExperimentalHttpClientMetrics",
          Boolean::class.javaPrimitiveType
        )
        method.setAccessible(true)
        return method
      } catch (e: NoSuchMethodException) {
        logger.log(Level.FINE, e.message, e)
        return null
      }
    }
  }
}
