/**
 * Created by Michael Avoyan on 3/11/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api

import android.content.Context
import android.os.Build
import io.velocitycareerlabs.BuildConfig
import io.velocitycareerlabs.api.entities.*
import io.velocitycareerlabs.impl.GlobalConfig
import io.velocitycareerlabs.impl.utils.VCLLog
import org.json.JSONObject
import java.lang.Exception

interface VCL {
    fun initialize(
        context: Context,
        environment: VCLEnvironment = VCLEnvironment.PROD,
        successHandler: () -> Unit,
        errorHandler: (VCLError) -> Unit
    )

    val countries: VCLCountries?
    val credentialTypes: VCLCredentialTypes?
    val credentialTypeSchemas: VCLCredentialTypeSchemas?

    fun getPresentationRequest(
        deepLink: VCLDeepLink,
        successHandler: (VCLPresentationRequest) -> Unit,
        errorHandler: (VCLError) -> Unit
    )

    fun submitPresentation(
        presentationSubmission: VCLPresentationSubmission,
        successHandler: (VCLPresentationSubmissionResult) -> Unit,
        errorHandler: (VCLError) -> Unit
    )

    fun getExchangeProgress(
        exchangeDescriptor: VCLExchangeDescriptor,
        successHandler: (VCLExchange) -> Unit,
        errorHandler: (VCLError) -> Unit
    )

    fun searchForOrganizations(
        organizationsSearchDescriptor: VCLOrganizationsSearchDescriptor,
        successHandler: (VCLOrganizations) -> Unit,
        errorHandler: (VCLError) -> Unit
    )

    fun getCredentialManifest(
        credentialManifestDescriptor: VCLCredentialManifestDescriptor,
        successHandler: (VCLCredentialManifest) -> Unit,
        errorHandler: (VCLError) -> Unit
    )

    fun generateOffers(
        generateOffersDescriptor: VCLGenerateOffersDescriptor,
        successHandler: (VCLOffers) -> Unit,
        errorHandler: (VCLError) -> Unit
    )

    fun checkForOffers(
        generateOffersDescriptor: VCLGenerateOffersDescriptor,
        token: VCLToken,
        successHandler: (VCLOffers) -> Unit,
        errorHandler: (VCLError) -> Unit
    )

    fun finalizeOffers(
        finalizeOffersDescriptor: VCLFinalizeOffersDescriptor,
        token: VCLToken,
        successHandler: (VCLJwtVerifiableCredentials) -> Unit,
        errorHandler: (VCLError) -> Unit
    )

    fun getCredentialTypesUIFormSchema(
        credentialTypesUIFormSchemaDescriptor: VCLCredentialTypesUIFormSchemaDescriptor,
        successHandler: (VCLCredentialTypesUIFormSchema) -> Unit,
        errorHandler: (VCLError) -> Unit
    )

    fun getVerifiedProfile(
        verifiedProfileDescriptor: VCLVerifiedProfileDescriptor,
        successHandler: (VCLVerifiedProfile) -> Unit,
        errorHandler: (VCLError) -> Unit
    )

    fun verifyJwt(
        jwt: VCLJWT,
        publicKey: VCLPublicKey,
        successHandler: (Boolean) -> Unit,
        errorHandler: (VCLError) -> Unit
    )

    fun generateSignedJwt(
        payload: JSONObject,
        iss: String,
        successHandler: (VCLJWT) -> Unit,
        errorHandler: (VCLError) -> Unit
    )
}

fun VCL.printVersion(context: Context) {
    VCLLog.d("VCL", "Version: ${GlobalConfig.VersionName}")
    VCLLog.d("VCL", "Build: ${GlobalConfig.VersionCode}")
}