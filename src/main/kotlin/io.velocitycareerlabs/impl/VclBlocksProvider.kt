/**
 * Created by Michael Avoyan on 3/14/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl

import io.velocitycareerlabs.api.entities.VCLCredentialTypes
import io.velocitycareerlabs.impl.data.infrastructure.db.CacheServiceImpl
import io.velocitycareerlabs.impl.data.infrastructure.executors.ExecutorImpl
import io.velocitycareerlabs.impl.data.infrastructure.jwt.JwtServiceImpl
import io.velocitycareerlabs.impl.data.infrastructure.network.NetworkServiceImpl
import io.velocitycareerlabs.impl.data.models.CountriesModelImpl
import io.velocitycareerlabs.impl.data.models.CredentialTypeSchemasModelImpl
import io.velocitycareerlabs.impl.data.models.CredentialTypesModelImpl
import io.velocitycareerlabs.impl.data.repositories.*
import io.velocitycareerlabs.impl.data.usecases.*
import io.velocitycareerlabs.impl.domain.models.CountriesModel
import io.velocitycareerlabs.impl.domain.models.CredentialTypeSchemasModel
import io.velocitycareerlabs.impl.domain.models.CredentialTypesModel
import io.velocitycareerlabs.impl.domain.usecases.*

internal object VclBlocksProvider {
        fun provideCredentialTypeSchemasModel(
                credentialTypes: VCLCredentialTypes
        ): CredentialTypeSchemasModel =
                CredentialTypeSchemasModelImpl(
                        CredentialTypeSchemasUseCaseImpl(
                                CredentialTypeSchemaRepositoryImpl(
                                        NetworkServiceImpl(),
                                        CacheServiceImpl()
                                ),
                                credentialTypes,
                                ExecutorImpl()
                        )
                )

        fun provideCredentialTypesModel(): CredentialTypesModel =
                CredentialTypesModelImpl(
                        CredentialTypesUseCaseImpl(
                                CredentialTypesRepositoryImpl(
                                        NetworkServiceImpl(),
                                        CacheServiceImpl()
                                ),
                                ExecutorImpl()
                        )
                )

        fun provideCountryCodesModel(): CountriesModel =
                CountriesModelImpl(
                        CountriesUseCaseImpl(
                                CountriesRepositoryImpl(
                                        NetworkServiceImpl(),
                                        CacheServiceImpl()
                                ),
                                ExecutorImpl()
                        )
                )

        fun providePresentationRequestUseCase(): PresentationRequestUseCase =
                PresentationRequestUseCaseImpl(
                        PresentationRequestRepositoryImpl(
                                NetworkServiceImpl()
                        ),
                        ResolveKidRepositoryImpl(
                                NetworkServiceImpl()
                        ),
                        JwtServiceRepositoryImpl(
                                JwtServiceImpl()
                        ),
                        ExecutorImpl()
                )

        fun providePresentationSubmissionUseCase(): PresentationSubmissionUseCase =
                PresentationSubmissionUseCaseImpl(
                        PresentationSubmissionRepositoryImpl(
                                NetworkServiceImpl()
                        ),
                        JwtServiceRepositoryImpl(
                                JwtServiceImpl()
                        ),
                        ExecutorImpl()
                )

        fun provideOrganizationsUseCase(): OrganizationsUseCase =
                OrganizationsUseCaseImpl(
                        OrganizationsRepositoryImpl(
                                NetworkServiceImpl()
                        ),
                        ExecutorImpl()
                )

        fun provideCredentialManifestUseCase(): CredentialManifestUseCase =
                CredentialManifestUseCaseImpl(
                        CredentialManifestRepositoryImpl(
                                NetworkServiceImpl()
                        ),
                        ResolveKidRepositoryImpl(
                                NetworkServiceImpl()
                        ),
                        JwtServiceRepositoryImpl(
                                JwtServiceImpl()
                        ),
                        ExecutorImpl()
                )

//        fun provideIdentificationModel(): IdentificationModel =
//                IdentificationModelImpl(
//                        IdentificationSubmissionUseCaseImpl(
//                                IdentificationSubmissionRepositoryImpl(
//                                        NetworkServiceImpl()
//                                ),
//                                JwtServiceRepositoryImpl(
//                                        JwtServiceImpl()
//                                ),
//                                ExecutorImpl()
//                        )
//                )

        fun provideIdentificationUseCase(): IdentificationSubmissionUseCase =
                IdentificationSubmissionUseCaseImpl(
                        IdentificationSubmissionRepositoryImpl(
                                NetworkServiceImpl()
                        ),
                        JwtServiceRepositoryImpl(
                                JwtServiceImpl()
                        ),
                        ExecutorImpl()
                )

        fun provideExchangeProgressUseCase(): ExchangeProgressUseCase =
                ExchangeProgressUseCaseImpl(
                        ExchangeProgressRepositoryImpl(
                                NetworkServiceImpl()
                        ),
                        ExecutorImpl()
                )

        fun provideGenerateOffersUseCase(): GenerateOffersUseCase =
                GenerateOffersUseCaseImpl(
                        GenerateOffersRepositoryImpl(
                                NetworkServiceImpl()
                        ),
                        ExecutorImpl()
                )

        fun provideFinalizeOffersUseCase(): FinalizeOffersUseCase =
                FinalizeOffersUseCaseImpl(
                        FinalizeOffersRepositoryImpl(
                                NetworkServiceImpl()
                        ),
                        JwtServiceRepositoryImpl(
                                JwtServiceImpl()
                        ),
                        ExecutorImpl()
                )

        fun provideCredentialTypesUIFormSchemaUseCase(): CredentialTypesUIFormSchemaUseCase =
                CredentialTypesUIFormSchemaUseCaseImpl(
                        CredentialTypesUIFormSchemaRepositoryImpl(
                                NetworkServiceImpl()
                        ),
                        ExecutorImpl()
                )

        fun provideVerifiedProfileUseCase(): VerifiedProfileUseCase =
                VerifiedProfileUseCaseImpl(
                        VerifiedProfileRepositoryImpl(
                                NetworkServiceImpl()
                        ),
                        ExecutorImpl()
                )

        fun provideJwtServiceUseCase(): JwtServiceUseCase =
                JwtServiceUseCaseImpl(
                        JwtServiceRepositoryImpl(
                                JwtServiceImpl()
                        ),
                        ExecutorImpl()
                )
}
