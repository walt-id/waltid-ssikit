/**
 * Created by Michael Avoyan on 18/07/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.extensions

import org.json.JSONArray

internal fun <T> List<T>.toJsonArray(): JSONArray {
    val retVal = JSONArray()
    forEach {
        retVal.put(it)
    }
    return retVal
}