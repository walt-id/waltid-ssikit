/**
 * Created by Michael Avoyan on 12/05/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

class VCLIdentificationSubmission(
    credentialManifest: VCLCredentialManifest,
    verifiableCredentials: List<VCLVerifiableCredential>
) : VCLSubmission(
    submitUri = credentialManifest.submitPresentationUri,
    iss = credentialManifest.iss,
    exchangeId = credentialManifest.exchangeId,
    presentationDefinitionId = credentialManifest.presentationDefinitionId,
    verifiableCredentials = verifiableCredentials,
    vendorOriginContext = credentialManifest.vendorOriginContext
)