/**
 * Created by Michael Avoyan on 10/05/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.repositories

import io.velocitycareerlabs.api.entities.VCLOffers
import io.velocitycareerlabs.api.entities.VCLResult
import io.velocitycareerlabs.api.entities.VCLToken
import io.velocitycareerlabs.api.entities.VCLGenerateOffersDescriptor

internal interface GenerateOffersRepository {
    fun generateOffers(token: VCLToken,
                       generateOffersDescriptor: VCLGenerateOffersDescriptor,
                       completionBlock: (VCLResult<VCLOffers>) -> Unit)
}