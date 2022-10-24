/**
 * Created by Michael Avoyan on 30/05/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.usecases

import io.velocitycareerlabs.api.entities.VCLExchange
import io.velocitycareerlabs.api.entities.VCLResult
import io.velocitycareerlabs.api.entities.VCLExchangeDescriptor

internal interface ExchangeProgressUseCase {
    fun getExchangeProgress(exchangeDescriptor: VCLExchangeDescriptor,
                            completionBlock: (VCLResult<VCLExchange>) -> Unit)
}