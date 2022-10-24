/**
 * Created by Michael Avoyan on 8/05/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import io.velocitycareerlabs.impl.utils.VCLLog
import org.json.JSONObject
import java.lang.Exception

data class VCLOrganization(val payload: JSONObject) {

    val TAG = VCLOrganization::class.simpleName

    val serviceCredentialAgentIssuers: List<VCLServiceCredentialAgentIssuer>
        get() = parseServiceCredentialAgentIssuers()

    private fun parseServiceCredentialAgentIssuers(): List<VCLServiceCredentialAgentIssuer> {
        val retVal = mutableListOf<VCLServiceCredentialAgentIssuer>()
        try {
            payload.optJSONArray(CodingKeys.KeyService)?.let { serviceJsonArr ->
                for (i in 0 until serviceJsonArr.length()) {
                    serviceJsonArr.optJSONObject(i)
                        ?.let { retVal.add(VCLServiceCredentialAgentIssuer(it)) }
                }
            }
        } catch (ex: Exception) {
            VCLLog.e(TAG, "", ex)
        }
        return retVal
    }

    companion object CodingKeys {
        const val KeyService = "service"
    }
}
