/**
 * Created by Michael Avoyan on 11/05/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.repositories

import io.velocitycareerlabs.api.entities.VCLResult
import io.velocitycareerlabs.api.entities.VCLToken
import io.velocitycareerlabs.api.entities.VCLFinalizeOffersDescriptor

internal interface FinalizeOffersRepository {
    fun finalizeOffers(token: VCLToken,
                       finalizeOffersDescriptor: VCLFinalizeOffersDescriptor,
                       completionBlock: (VCLResult<List<String>>) -> Unit)
}