/**
 * Created by Michael Avoyan on 3/16/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

data class VCLCredentialTypes(val all: List<VCLCredentialType>?) {
    val recommendedTypes:List<VCLCredentialType>? get() = all?.filter { it.recommended }
}