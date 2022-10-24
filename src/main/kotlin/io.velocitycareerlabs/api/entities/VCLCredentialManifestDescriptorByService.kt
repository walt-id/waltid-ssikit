/**
 * Created by Michael Avoyan on 8/05/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import io.velocitycareerlabs.impl.extensions.encode
import java.net.URI

class VCLCredentialManifestDescriptorByService(
    service: VCLService,
    credentialTypes: List<String>? = null,
    pushDelegate: VCLPushDelegate? = null
): VCLCredentialManifestDescriptor(
    uri = service.serviceEndpoint,
    credentialTypes = credentialTypes,
    pushDelegate = pushDelegate
) {
//    TODO: validate credentialTypes by services.credentialTypes

    override val endpoint =  generateQueryParams()?.let { queryParams ->
        val originUri = URI(uri)
        val allQueryParams = ( originUri.query?.let { "&" } ?: "?" ) + queryParams
        uri + allQueryParams
    } ?: uri

    private fun generateQueryParams(): String? {
        val pCredentialTypes = credentialTypes?.let { credTypes ->
            credTypes.map { it.encode() }.joinToString(separator = "&") { "$KeyCredentialTypes=$it" } }
        val pPushDelegate = pushDelegate?.let {
            "$KeyPushDelegatePushUrl=${it.pushUrl.encode()}&" + "$KeyPushDelegatePushToken=${it.pushToken}"
        }
        val qParams = listOfNotNull(pCredentialTypes, pPushDelegate).filter { it.isNotBlank() }
        return if(qParams.isNotEmpty()) qParams.joinToString("&") else null
    }
}