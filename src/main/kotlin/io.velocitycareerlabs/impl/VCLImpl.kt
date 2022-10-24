/**
 * Created by Michael Avoyan on 3/11/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl

import android.content.Context
import io.velocitycareerlabs.api.VCLEnvironment
import io.velocitycareerlabs.api.VCL
import io.velocitycareerlabs.api.entities.*
import io.velocitycareerlabs.impl.domain.models.CredentialTypeSchemasModel
import io.velocitycareerlabs.api.entities.handleResult
import io.velocitycareerlabs.api.printVersion
import io.velocitycareerlabs.impl.data.infrastructure.network.Request
import io.velocitycareerlabs.impl.domain.models.CountriesModel
import io.velocitycareerlabs.impl.domain.models.CredentialTypesModel
import io.velocitycareerlabs.impl.utils.InitializationWatcher
import io.velocitycareerlabs.impl.utils.VCLLog
import org.json.JSONObject

internal class VCLImpl: VCL {
    val TAG = VCLImpl::class.simpleName

    private val ModelsToInitilizeAmount = 3

    private var credentialTypesModel: CredentialTypesModel? = null
    private var credentialTypeSchemasModel: CredentialTypeSchemasModel? = null
    private var countriesModel: CountriesModel? = null

    private val presentationRequestUseCase = VclBlocksProvider.providePresentationRequestUseCase()
    private val presentationSubmissionUseCase =
        VclBlocksProvider.providePresentationSubmissionUseCase()
    private val exchangeProgressUseCase = VclBlocksProvider.provideExchangeProgressUseCase()
    private val organizationsUseCase = VclBlocksProvider.provideOrganizationsUseCase()
    private val credentialManifestUseCase = VclBlocksProvider.provideCredentialManifestUseCase()
//    private val identificationModel = VclBlocksProvider.provideIdentificationModel()
    private val identificationUseCase = VclBlocksProvider.provideIdentificationUseCase()
    private val generateOffersUseCase = VclBlocksProvider.provideGenerateOffersUseCase()
    private val finalizeOffersUseCase = VclBlocksProvider.provideFinalizeOffersUseCase()
    private val credentialTypesUIFormSchemaUseCase =
        VclBlocksProvider.provideCredentialTypesUIFormSchemaUseCase()
    private val verifiedProfileUseCase = VclBlocksProvider.provideVerifiedProfileUseCase()
    private val jwtServiceUseCase = VclBlocksProvider.provideJwtServiceUseCase()

    private var initializationWatcher = InitializationWatcher(ModelsToInitilizeAmount)

    override fun initialize(
        context: Context,
        environment: VCLEnvironment,
        successHandler: () -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        initializationWatcher = InitializationWatcher(ModelsToInitilizeAmount)

        initGlobalConfigurations(environment)

        printVersion(context)

        credentialTypesModel = VclBlocksProvider.provideCredentialTypesModel(context)
        countriesModel = VclBlocksProvider.provideCountryCodesModel(context)
        val completionHandler = {
            initializationWatcher.firstError()?.let { errorHandler(it) }
                ?: successHandler()
        }
        countriesModel?.initialize { result ->
            result.handleResult(
                {
                    if (initializationWatcher.onInitializedModel(null))
                        completionHandler()
                },
                { error ->
                    if (initializationWatcher.onInitializedModel(error))
                        completionHandler()
                })
        }
        credentialTypesModel?.initialize { result ->
            result.handleResult(
                {
                    if (initializationWatcher.onInitializedModel(null)) {
                        completionHandler()
                    } else {
                        if (credentialTypesModel?.data != null) {
                            credentialTypesModel?.data?.let { credentialTypes ->
                                credentialTypeSchemasModel =
                                    VclBlocksProvider.provideCredentialTypeSchemasModel(
                                        context,
                                        credentialTypes
                                    )
                                credentialTypeSchemasModel?.initialize { result ->
                                    result.handleResult(
                                        {
                                            if (initializationWatcher.onInitializedModel(null)) {
                                                completionHandler()
                                            }
                                        },
                                        { error ->
                                            if (initializationWatcher.onInitializedModel(error)) {
                                                completionHandler()
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            errorHandler(VCLError("Failed to get credential type schemas"))
                        }
                    }
                },
                { error ->
                    if (initializationWatcher.onInitializedModel(error, true)) {
                        completionHandler()
                    }
                })
        }
    }

    private fun initGlobalConfigurations(environment: VCLEnvironment) {
        GlobalConfig.CurrentEnvironment = environment
    }

    override val countries: VCLCountries? get() = countriesModel?.data
    override val credentialTypes: VCLCredentialTypes? get() = credentialTypesModel?.data
    override val credentialTypeSchemas: VCLCredentialTypeSchemas? get() = credentialTypeSchemasModel?.data

    override fun getPresentationRequest(
        deepLink: VCLDeepLink,
        successHandler: (VCLPresentationRequest) -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        presentationRequestUseCase.getPresentationRequest(deepLink) { presentationRequestResult ->
            presentationRequestResult.handleResult(
                {
                    successHandler(it)
                },
                {
                    logError("getPresentationRequest", it)
                    errorHandler(it)
                }
            )
        }
    }

    override fun submitPresentation(
        presentationSubmission: VCLPresentationSubmission,
        successHandler: (VCLPresentationSubmissionResult) -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        presentationSubmissionUseCase.submit(presentationSubmission) { presentationSubmissionResult ->
            presentationSubmissionResult.handleResult(
                {
                    successHandler(it)
                },
                {
                    logError("submit presentation", it)
                    errorHandler(it)
                }
            )
        }
    }

    override fun getExchangeProgress(
        exchangeDescriptor: VCLExchangeDescriptor,
        successHandler: (VCLExchange) -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        exchangeProgressUseCase.getExchangeProgress(exchangeDescriptor) { presentationSubmissionResult ->
            presentationSubmissionResult.handleResult(
                {
                    successHandler(it)
                },
                {
                    logError("getExchangeProgress", it)
                    errorHandler(it)
                }
            )
        }
    }

    override fun searchForOrganizations(
        organizationsSearchDescriptor: VCLOrganizationsSearchDescriptor,
        successHandler: (VCLOrganizations) -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        organizationsUseCase.searchForOrganizations(organizationsSearchDescriptor) { organization ->
            organization.handleResult(
                {
                    successHandler(it)
                },
                {
                    logError("searchForOrganizations", it)
                    errorHandler(it)
                }
            )
        }
    }

    override fun getCredentialManifest(
        credentialManifestDescriptor: VCLCredentialManifestDescriptor,
        successHandler: (VCLCredentialManifest) -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        credentialManifestUseCase.getCredentialManifest(credentialManifestDescriptor) { credentialManifest ->
            credentialManifest.handleResult(
                {
                    successHandler(it)
                },
                {
                    logError("getCredentialManifest", it)
                    errorHandler(it)
                }
            )
        }
    }

    override fun generateOffers(
        generateOffersDescriptor: VCLGenerateOffersDescriptor,
        successHandler: (VCLOffers) -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        val identificationSubmission = VCLIdentificationSubmission(
            credentialManifest = generateOffersDescriptor.credentialManifest,
            verifiableCredentials = generateOffersDescriptor.identificationVerifiableCredentials
        )
        identificationUseCase.submit(identificationSubmission) { identificationSubmissionResult ->
            identificationSubmissionResult.handleResult(
                { identificationSubmission ->
                    invokeGenerateOffersUseCase(
                        generateOffersDescriptor = generateOffersDescriptor,
                        token = identificationSubmission.token,
                        successHandler = successHandler,
                        errorHandler = errorHandler
                    )
                },
                {
                    logError("submit identification", it)
                    errorHandler(it)
                }
            )
        }
    }

    override fun checkForOffers(
        generateOffersDescriptor: VCLGenerateOffersDescriptor,
        token: VCLToken,
        successHandler: (VCLOffers) -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        invokeGenerateOffersUseCase(
            generateOffersDescriptor = generateOffersDescriptor,
            token = token,
            successHandler = successHandler,
            errorHandler = errorHandler
        )
    }

    private fun invokeGenerateOffersUseCase(
        generateOffersDescriptor: VCLGenerateOffersDescriptor,
        token: VCLToken,
        successHandler: (VCLOffers) -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        generateOffersUseCase.generateOffers(
            token,
            generateOffersDescriptor
        ) { vnOffersResult ->
            vnOffersResult.handleResult(
                {
                    successHandler(it)
                },
                {
                    logError("generateOffers", it)
                    errorHandler(it)
                }
            )
        }
    }

    override fun finalizeOffers(
        finalizeOffersDescriptor: VCLFinalizeOffersDescriptor,
        token: VCLToken,
        successHandler: (VCLJwtVerifiableCredentials) -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        finalizeOffersUseCase.finalizeOffers(
            token,
            finalizeOffersDescriptor
        ) { jwtVerifiableCredentials ->
            jwtVerifiableCredentials.handleResult(
                {
                    successHandler(it)
                },
                {
                    logError("finalizeOffers", it)
                    errorHandler(it)
                }
            )
        }
    }

    override fun getCredentialTypesUIFormSchema(
        credentialTypesUIFormSchemaDescriptor: VCLCredentialTypesUIFormSchemaDescriptor,
        successHandler: (VCLCredentialTypesUIFormSchema) -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        countriesModel?.data?.let { countries ->
            credentialTypesUIFormSchemaUseCase.getCredentialTypesUIFormSchema(
                credentialTypesUIFormSchemaDescriptor,
                countries
            ) { credentialTypesUIFormSchemaResult ->
                credentialTypesUIFormSchemaResult.handleResult(
                    {
                        successHandler(it)
                    },
                    {
                        logError("getCredentialTypesUIFormSchema", it)
                        errorHandler(it)
                    }
                )
            }
        } ?: run {
            val error = VCLError("No countries for getCredentialTypesUIFormSchema")
            logError("getCredentialTypesUIFormSchema", error)
            errorHandler(error)
        }
    }

    override fun getVerifiedProfile(
        verifiedProfileDescriptor: VCLVerifiedProfileDescriptor,
        successHandler: (VCLVerifiedProfile) -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        verifiedProfileUseCase.getVerifiedProfile(verifiedProfileDescriptor) {
            verifiedProfileResult ->
            verifiedProfileResult.handleResult(
                {
                    successHandler(it)
                },
                {
                    logError("getVerifiedProfile", it)
                    errorHandler(it)
                }
            )
        }
    }

    override fun verifyJwt(
        jwt: VCLJWT,
        publicKey: VCLPublicKey,
        successHandler: (Boolean) -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        jwtServiceUseCase.verifyJwt(jwt, publicKey) { isVerifiedResult ->
            isVerifiedResult.handleResult(
                {
                    successHandler(it)
                },
                {
                    logError("verifyJwt", it)
                    errorHandler(it)
                }
            )
        }
    }

    override fun generateSignedJwt(
        payload: JSONObject,
        iss: String,
        successHandler: (VCLJWT) -> Unit,
        errorHandler: (VCLError) -> Unit
    ) {
        jwtServiceUseCase.generateSignedJwt(payload, iss) { jwtResult ->
            jwtResult.handleResult(
                {
                    successHandler(it)
                },
                {
                    logError("generateSignedJwt", it)
                    errorHandler(it)
                }
            )
        }
    }
}

internal fun VCLImpl.logError(message: String = "", error: Error) {
    VCLLog.e(TAG, "${message}: $error")
}