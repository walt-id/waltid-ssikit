/**
 * Created by Michael Avoyan on 06/05/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

enum class VCLServiceType {
    Issuer,
    Inspector,
    CredentialAgentOperator,
    NodeOperator,
    TrustRoot,
    NotaryIssuer,
    IdentityIssuer,
    HolderAppProvider
}