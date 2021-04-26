package org.letstrust.rest

import cc.vileda.openapi.dsl.components
import cc.vileda.openapi.dsl.externalDocs
import cc.vileda.openapi.dsl.info
import cc.vileda.openapi.dsl.securityScheme
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.apibuilder.*
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.plugin.openapi.InitialConfigurationCreator
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import mu.KotlinLogging

object RestAPI {

    private val log = KotlinLogging.logger {}

    fun start() {
        println("Starting Let's Trust API App...\n")

        Javalin.create {

            it.apply {
                registerPlugin(RouteOverviewPlugin("/api-routes"))

                registerPlugin(OpenApiPlugin(OpenApiOptions(InitialConfigurationCreator {
                    OpenAPI().apply {
                        info {
                            title = "Let's Trust Core API"
                            description = "The Let's Trust public API documentation"
                            contact = Contact().apply {
                                name = "SSI Fabric GmbH"
                                url = "https://letstrust.id"
                                email = "office@letstrust.id"
                            }
                            version = "1.0"
                        }
                        servers = listOf(
                            Server().description("Let's Trust").url("https://core-api.letstrust.io"),
                            Server().description("Local testing server").url("http://localhost:7000")
                        )
                        externalDocs {
                            description = "Let's Trust Docs"
                            url = "https://docs.letstrust.io/api"
                        }

                        components {
                            securityScheme {
                                name = "bearerAuth"
                                type = SecurityScheme.Type.HTTP
                                scheme = "bearer"
                                `in` = SecurityScheme.In.HEADER
                                description = "HTTP Bearer Token authentication"
                                bearerFormat = "JWT"
                            }
                        }
                    }
                }).apply {
                    path("/api-documentation")
                    swagger(SwaggerOptions("/swagger").title("Let's Trust API"))
                    reDoc(ReDocOptions("/redoc").title("Let's Trust API"))
//                defaultDocumentation { doc ->
//                    doc.json("5XX", ErrorResponse::class.java)
//                }
                }))

                //addStaticFiles("/static")
            }

            it.enableCorsForAllOrigins()

            it.enableDevLogging()
        }.routes {
            path("v1") {
                path("key") {
                    post("gen", KeyController::gen)
                    get("list", KeyController::list)
                    post("import", KeyController::import)
                    post("export", KeyController::export)
                }
                path("did") {
                    post("create", DidController::create)
                    post("resolve", DidController::resolve)
                    get("list", DidController::list)
                }
                path("vc") {
                    post("create", VcController::create)
                    post("present", VcController::present)
                    post("verify", VcController::verify)
                    get("list", VcController::list)
                }
            }

        }.exception(IllegalArgumentException::class.java) { e, ctx ->
            log.error(e.stackTraceToString())
            ctx.status(400)
        }.exception(Exception::class.java) { e, ctx ->
            log.error(e.stackTraceToString())
            ctx.status(500)
        }.start(7000)


        Javalin.create {

            it.apply {
                registerPlugin(RouteOverviewPlugin("/api-routes"))

                registerPlugin(OpenApiPlugin(OpenApiOptions(InitialConfigurationCreator {
                    OpenAPI().apply {
                        info {
                            title = "Let's Trust ESSIF API"
                            description = "The Let's Trust public API documentation"
                            contact = Contact().apply {
                                name = "SSI Fabric GmbH"
                                url = "https://letstrust.id"
                                email = "office@letstrust.id"
                            }
                            version = "1.0"
                        }
                        servers = listOf(
                            Server().description("Let's Trust").url("https://core-api.letstrust.io"),
                            Server().description("Local testing server").url("http://localhost:7000")
                        )
                        externalDocs {
                            description = "Let's Trust Docs"
                            url = "https://docs.letstrust.io/api"
                        }

                        components {
                            securityScheme {
                                name = "bearerAuth"
                                type = SecurityScheme.Type.HTTP
                                scheme = "bearer"
                                `in` = SecurityScheme.In.HEADER
                                description = "HTTP Bearer Token authentication"
                                bearerFormat = "JWT"
                            }
                        }
                    }
                }).apply {
                    path("/api-documentation")
                    swagger(SwaggerOptions("/swagger").title("Let's Trust API"))
                    reDoc(ReDocOptions("/redoc").title("Let's Trust API"))
//                defaultDocumentation { doc ->
//                    doc.json("5XX", ErrorResponse::class.java)
//                }
                }))

                //addStaticFiles("/static")
            }

            it.enableCorsForAllOrigins()

            it.enableDevLogging()
        }.routes {
            path("v1") {
                path("essif") {
                    path("ti") {
                        path("credentials"){
                            get("",  EosController::getCredential)
                            get(":credentialId",  EosController::getCredential)
                        }
                        post("requestCredentialUri", EosController::requestCredentialUri)
                        post("requestVerifiableCredential", EosController::requestVerifiableCredential)
                        post("signedChallenge", EosController::signedChallenge)
                    }
                    path("eos") {
                        post("onboard", EosController::onboards)
                    }
                    path("user") {
                        path("wallet") {
                            post("createDid", UserWalletController::createDid)
                            post("requestAccessToken", UserWalletController::requestAccessToken)
                            post("validateDidAuthRequest", UserWalletController::validateDidAuthRequest)
                            post("didAuthResponse", UserWalletController::didAuthResponse)
                            post("vcAuthResponse", UserWalletController::vcAuthResponse)
                            post("oidcAuthResponse", UserWalletController::oidcAuthResponse)
                        }
                    }
                    path("enterprise") {
                        path("wallet") {
                            post("createDid", EnterpriseWalletController::createDid)
                            post("requestVerifiableAuthorization", EnterpriseWalletController::requestVerifiableAuthorization)
                            post("requestVerifiableCredential", EnterpriseWalletController::requestVerifiableCredential)
                            post("generateDidAuthRequest", EnterpriseWalletController::generateDidAuthRequest)
                            // post("onboardTrustedIssuer", EnterpriseWalletController::onboardTrustedIssuer) not supported yet
                            post("validateDidAuthResponse", EnterpriseWalletController::validateDidAuthResponse)
                            get("getVerifiableCredential", EnterpriseWalletController::getVerifiableCredential)
                            post("token", EnterpriseWalletController::token)
                        }
                    }
                }
            }

        }.exception(IllegalArgumentException::class.java) { e, ctx ->
            log.error(e.stackTraceToString())
            ctx.status(400)
        }.exception(Exception::class.java) { e, ctx ->
            log.error(e.stackTraceToString())
            ctx.status(500)
        }.start(7001)
    }
}
