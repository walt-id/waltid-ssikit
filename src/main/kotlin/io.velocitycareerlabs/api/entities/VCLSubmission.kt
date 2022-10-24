/**
 * Created by Michael Avoyan on 8/05/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import org.json.JSONArray
import org.json.JSONObject
import java.util.*

abstract class VCLSubmission(
    val submitUri: String,
    val iss: String,
    val exchangeId: String,
    val presentationDefinitionId: String,
    val verifiableCredentials: List<VCLVerifiableCredential>,
    val vendorOriginContext: String? = null
) {
    val payload get() = generatePayload()

    private fun generatePayload(): JSONObject {
        val retVal = JSONObject()
        retVal.putOpt(VCLSubmission.KeyJti, UUID.randomUUID().toString())
        val vp = JSONObject()
            .putOpt(
                VCLSubmission.KeyType,
                VCLSubmission.ValueVerifiablePresentation
            )
            .putOpt(VCLSubmission.KeyPresentationSubmission, JSONObject()
                .putOpt(VCLSubmission.KeyId, UUID.randomUUID().toString())
                .putOpt(VCLSubmission.KeyDefinitionId, presentationDefinitionId)
                .putOpt(VCLSubmission.KeyDescriptorMap, JSONArray(verifiableCredentials.mapIndexed { index, credential ->
                    JSONObject()
                        .putOpt(VCLSubmission.KeyId, credential.inputDescriptor)
                        .putOpt(VCLSubmission.KeyPath, "$.verifiableCredential[$index]")
                        .putOpt(VCLSubmission.KeyFormat, VCLSubmission.ValueJwtVc) })))
        vp.putOpt(VCLSubmission.KeyVerifiableCredential, JSONArray(verifiableCredentials.map { credential -> credential.jwtVc }))
        vendorOriginContext?.let { vp.putOpt(VCLSubmission.KeyVendorOriginContext, vendorOriginContext) }
        retVal.putOpt(VCLSubmission.KeyVp, vp)
        return retVal
    }

    companion object CodingKeys {
        const val KeyJti = "jti"
        const val KeyId = "id"
        const val KeyVp = "vp"
        const val KeyDid = "did"
        const val KeyType = "type"
        const val KeyPresentationSubmission = "presentation_submission"
        const val KeyDefinitionId = "definition_id"
        const val KeyDescriptorMap = "descriptor_map"
        const val KeyExchangeId = "exchange_id"
        const val KeyJwtVp = "jwt_vp"
        const val KeyPath = "path"
        const val KeyFormat = "format"
        const val KeyVerifiableCredential = "verifiableCredential"
        const val KeyVendorOriginContext = "vendorOriginContext"
        const val KeyInputDescriptor = "input_descriptor"

        const val ValueJwtVc = "jwt_vc"
        const val ValueVerifiablePresentation = "VerifiablePresentation"
    }
}

