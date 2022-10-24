/**
 * Created by Michael Avoyan on 09/05/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.repositories

import io.velocitycareerlabs.api.entities.VCLResult
import io.velocitycareerlabs.api.entities.VCLCredentialManifestDescriptor

internal interface CredentialManifestRepository {
    fun getCredentialManifest(credentialManifestDescriptor: VCLCredentialManifestDescriptor,
                              completionBlock:(VCLResult<String>) -> Unit)
}