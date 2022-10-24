/**
 * Created by Michael Avoyan on 09/12/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.models

import io.velocitycareerlabs.api.entities.VCLCountries

internal interface CountriesModel: Model<VCLCountries>, Initializable<VCLCountries>