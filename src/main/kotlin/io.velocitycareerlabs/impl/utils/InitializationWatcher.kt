/**
 * Created by Michael Avoyan on 3/20/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.utils

import io.velocitycareerlabs.api.entities.VCLError

internal class InitializationWatcher(private val initAmount: Int) {
    private var initCount = 0

    private var errors: ArrayList<VCLError> = arrayListOf()

    fun onInitializedModel(error: VCLError?, enforceFailure: Boolean=false): Boolean{
        initCount++
        error?.let{ errors.add(it) }
        return isInitializationComplete(enforceFailure)
    }
    fun firstError(): VCLError? {
        return if(errors.isNotEmpty()) errors.first()
        else null
    }
    private fun isInitializationComplete(enforceFailure: Boolean): Boolean{
        return initCount == initAmount || enforceFailure
    }
}