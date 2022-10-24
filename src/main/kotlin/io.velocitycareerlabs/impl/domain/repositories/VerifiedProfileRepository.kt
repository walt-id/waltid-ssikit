/**
 * Created by Michael Avoyan on 10/28/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.repositories

import io.velocitycareerlabs.api.entities.VCLVerifiedProfileDescriptor
import io.velocitycareerlabs.api.entities.VCLResult
import io.velocitycareerlabs.api.entities.VCLVerifiedProfile

internal interface VerifiedProfileRepository {
    fun getVerifiedProfile(
        verifiedProfileDescriptor: VCLVerifiedProfileDescriptor,
        completionBlock: (VCLResult<VCLVerifiedProfile>) -> Unit
    )
}