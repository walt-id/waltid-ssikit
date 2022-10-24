/**
 * Created by Michael Avoyan on 3/11/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

data class VCLError(
        val description: String? = null,
        val code: Int? = null
): Error(description)

sealed class VCLResult<out R> {
        data class Success<out T>(val data: T) : VCLResult<T>()
        data class Failure(val error: VCLError) : VCLResult<Nothing>()
}

fun <T> VCLResult<T>.handleResult(successHandler:(d: T) -> Unit, errorHandler: (error: VCLError) -> Unit) {
        when (this) {
                is VCLResult.Success -> {
                        successHandler(this.data)
                }
                is VCLResult.Failure -> {
                        errorHandler(this.error)
                }
        }
}

val <T> VCLResult<T>.data: T?
        get() = (this as? VCLResult.Success)?.data

enum class VCLErrorCodes(val value: Int) {
        NetworkError(1)
}
