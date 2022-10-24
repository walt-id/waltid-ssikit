/**
 * Created by Michael Avoyan on 13/06/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.repositories

import io.velocitycareerlabs.api.entities.VCLCountries
import io.velocitycareerlabs.api.entities.VCLCredentialTypesUIFormSchema
import io.velocitycareerlabs.api.entities.VCLResult
import io.velocitycareerlabs.api.entities.VCLCredentialTypesUIFormSchemaDescriptor

internal interface CredentialTypesUIFormSchemaRepository {
    fun getCredentialTypesUIFormSchema(
        credentialTypesUIFormSchemaDescriptor: VCLCredentialTypesUIFormSchemaDescriptor,
        countries: VCLCountries,
        completionBlock: (VCLResult<VCLCredentialTypesUIFormSchema>) -> Unit
    )
}