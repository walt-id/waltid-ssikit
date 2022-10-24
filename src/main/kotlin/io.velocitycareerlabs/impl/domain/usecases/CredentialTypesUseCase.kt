/**
 * Created by Michael Avoyan on 3/11/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.usecases

import io.velocitycareerlabs.api.entities.VCLCredentialTypes
import io.velocitycareerlabs.api.entities.VCLResult

internal interface CredentialTypesUseCase {
    fun getCredentialTypes(completionBlock:(VCLResult<VCLCredentialTypes>) -> Unit)
}