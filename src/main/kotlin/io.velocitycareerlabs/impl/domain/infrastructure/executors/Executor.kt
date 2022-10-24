/**
 * Created by Michael Avoyan on 5/2/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.infrastructure.executors

import android.os.Looper

internal interface Executor {
    fun runOn(looper: Looper?, runnable: Runnable)
    fun runOnMainThread(runnable: Runnable)
    fun runOnBackgroundThread(runnable: Runnable)
    fun waitForTermination()
}