/**
 * Created by Michael Avoyan on 10/05/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.usecases

import io.velocitycareerlabs.api.entities.*

internal interface GenerateOffersUseCase {
    fun generateOffers(token: VCLToken,
                       generateOffersDescriptor: VCLGenerateOffersDescriptor,
                       completionBlock: (VCLResult<VCLOffers>) -> Unit)
}