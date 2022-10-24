/**
 * Created by Michael Avoyan on 4/12/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.usecases

import io.velocitycareerlabs.api.entities.VCLDeepLink
import io.velocitycareerlabs.api.entities.VCLPresentationRequest
import io.velocitycareerlabs.api.entities.VCLResult

internal interface PresentationRequestUseCase {
    fun getPresentationRequest(deepLink: VCLDeepLink, completionBlock: (VCLResult<VCLPresentationRequest>) -> Unit)
}