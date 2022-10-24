/**
 * Created by Michael Avoyan on 4/14/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

data class VCLVerifiableCredential(val inputDescriptor: String, val jwtVc: String)