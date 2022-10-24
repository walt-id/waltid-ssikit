/**
 * Created by Michael Avoyan on 12/9/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.repositories

import io.velocitycareerlabs.api.entities.VCLCountries
import io.velocitycareerlabs.api.entities.VCLResult

internal interface CountriesRepository {
    fun getCountries(completionBlock: (VCLResult<VCLCountries>) -> Unit)
}