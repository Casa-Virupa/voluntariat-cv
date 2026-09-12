package com.casavirupa.voluntariat.shared.core.utils

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Class that throttles the lambda invocations, so that a new invocation is only called if at least
 * @millis have passed since the last invocation.
 * A common use-case is if we have a button in a screen, and we want to avoid errors due to having
 * multiple consecutive clicks.
 *
 * We can use the same instance for different events/lambdas, or one for each event/lambda.
 */
class Throttler(private val millis: Long = DEFAULT_TIMEOUT_MILLIS) {
    @OptIn(InternalCoroutinesApi::class)
    private val synchronizedObject = SynchronizedObject()
    private var lastInvoke: Instant = Instant.DISTANT_PAST

    fun throttle(block: () -> Unit) {
        if (enoughTimePassed()) block()
    }

    @OptIn(InternalCoroutinesApi::class)
    fun enoughTimePassed(): Boolean {
        synchronized(synchronizedObject) {
            if ((Clock.System.now() - lastInvoke).inWholeMilliseconds > millis) {
                lastInvoke = Clock.System.now()
                return true
            }
            return false
        }
    }
}

private const val DEFAULT_TIMEOUT_MILLIS = 500L
