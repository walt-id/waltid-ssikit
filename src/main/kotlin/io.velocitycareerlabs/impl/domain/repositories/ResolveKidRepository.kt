/**
 * Created by Michael Avoyan on 4/20/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.repositories

import io.velocitycareerlabs.api.entities.VCLPublicKey
import io.velocitycareerlabs.api.entities.VCLResult

internal interface ResolveKidRepository {
    fun getPublicKey(keyID: String, completionBlock: (VCLResult<VCLPublicKey>) -> Unit)
}