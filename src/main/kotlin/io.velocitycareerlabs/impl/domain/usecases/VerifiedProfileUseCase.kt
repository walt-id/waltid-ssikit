/**
 * Created by Michael Avoyan on 10/28/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.usecases

import io.velocitycareerlabs.api.entities.VCLResult
import io.velocitycareerlabs.api.entities.VCLVerifiedProfile
import io.velocitycareerlabs.api.entities.VCLVerifiedProfileDescriptor

internal interface VerifiedProfileUseCase {
    fun getVerifiedProfile(
        verifiedProfileDescriptor: VCLVerifiedProfileDescriptor,
        completionBlock: (VCLResult<VCLVerifiedProfile>) -> Unit
    )
}