/**
 * Created by Michael Avoyan on 4/20/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

data class VCLOrganizations(val all: List<VCLOrganization>) {
    companion object CodingKeys {
        const val KeyResult = "result"
    }
}