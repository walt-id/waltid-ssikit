package id.walt.rest.essif

import cc.vileda.openapi.dsl.components
import cc.vileda.openapi.dsl.externalDocs
import cc.vileda.openapi.dsl.info
import cc.vileda.openapi.dsl.securityScheme
import com.beust.klaxon.Klaxon
import id.walt.Values
import id.walt.rest.ErrorResponse
import id.walt.rest.OpenAPIUtils.documentedIgnored
import id.walt.rest.RootController
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.plugin.json.JavalinJackson
import io.javalin.plugin.json.JsonMapper
import io.javalin.plugin.openapi.InitialConfigurationCreator
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.dsl.documented
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import mu.KotlinLogging


object EssifAPI {

    private val log = KotlinLogging.logger {}

    internal const val DEFAULT_ESSIF_API_PORT = 7004
    internal const val DEFAULT_BIND_ADDRESS = "127.0.0.1"

    /**
     * Currently used instance of the ESSIF API server
     */
    var essifApi: Javalin? = null

    /**
     * Start ESSIF REST API
     * @param apiTargetUrls (optional): add URLs to Swagger documentation for easy testing
     * @param bindAddress (default: 127.0.0.1): select address to bind on to, e.g. 0.0.0.0 for all interfaces
     * @param port (default: 7001): select port to listen on
     */
    fun start(
        port: Int = DEFAULT_ESSIF_API_PORT,
        bindAddress: String = DEFAULT_BIND_ADDRESS,
        apiTargetUrls: List<String> = listOf()
    ) {

        log.info { "Starting walt.id Essif API ...\n" }

        essifApi = Javalin.create { config ->

            config.apply {
                registerPlugin(RouteOverviewPlugin("/api-routes"))

                registerPlugin(OpenApiPlugin(OpenApiOptions(InitialConfigurationCreator {
                    OpenAPI().apply {
                        info {
                            title = "walt.id ESSIF API"
                            description = "The walt.id public API documentation"
                            contact = Contact().apply {
                                name = "walt.id"
                                url = "https://walt.id"
                                email = "office@walt.id"
                            }
                            version = Values.version
                        }
                        servers = listOf(
                            Server().url("/"),
                            *apiTargetUrls.map { Server().url(it) }.toTypedArray()
                        )
                        externalDocs {
                            description = "walt.id Docs"
                            url = "https://docs.walt.id/api"
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
                    path("/v1/api-documentation")
                    swagger(SwaggerOptions("/v1/swagger").title("walt.id API"))
                    reDoc(ReDocOptions("/v1/redoc").title("walt.id API"))
//                defaultDocumentation { doc ->
//                    doc.json("5XX", ErrorResponse::class.java)
//                }
                }))


                this.jsonMapper(object : JsonMapper {
                    override fun toJsonString(obj: Any): String {
                        return Klaxon().toJsonString(obj)
                    }

                    override fun <T : Any?> fromJsonString(json: String, targetClass: Class<T>): T {
                        return JavalinJackson().fromJsonString(json, targetClass)
                    }
                })
                /*JavalinJson.fromJsonMapper = object : FromJsonMapper {
                    override inline fun <reified T> map(json: String, targetClass: Class<T>): T =
                        Klaxon().parse<T::class>(json)
                }*/

                //addStaticFiles("/static")
            }

            config.enableCorsForAllOrigins()

            config.enableDevLogging()
        }.routes {
            get("/", documented(documentedIgnored(), RootController::rootEssifApi))
            get("health", documented(RootController.healthDocs(), RootController::health))

            path("v1") {
//                path("trusted-issuer") {
//                    post(
//                        "generateAuthenticationRequest",
//                        documented(
//                            TrustedIssuerController.generateAuthenticationRequestDocs(),
//                            TrustedIssuerController::generateAuthenticationRequest
//                        )
//                    )
//                    post(
//                        "openSession",
//                        documented(TrustedIssuerController.openSessionDocs(), TrustedIssuerController::openSession)
//                    )
//                }
                path("client") {
                    post("onboard", documented(EssifClientController.onboardDocs(), EssifClientController::onboard))
                    post("auth", documented(EssifClientController.authApiDocs(), EssifClientController::authApi))
                    post(
                        "registerDid",
                        documented(EssifClientController.registerDidDocs(), EssifClientController::registerDid)
                    )
                    path("timestamp") {
                        post(
                            "",
                            documented(
                                EssifClientController.createTimestampDocs(),
                                EssifClientController::createTimestamp
                            )
                        )
                        get(
                            "id/{timestampId}",
                            documented(
                                EssifClientController.getByTimestampIdDocs(),
                                EssifClientController::getByTimestampId
                            )
                        )
                        get(
                            "txhash/{txhash}",
                            documented(
                                EssifClientController.getByTransactionHashDocs(),
                                EssifClientController::getByTransactionHash
                            )
                        )
                    }
                }
            }

//            path("test") {
//                path("user") {
//                    path("wallet") {
//                        post("createDid", documented(UserWalletController.createDidDocs(), UserWalletController::createDid))
//                        post(
//                            "requestAccessToken",
//                            documented(UserWalletController.requestAccessTokenDocs(), UserWalletController::requestAccessToken)
//                        )
//                        post(
//                            "validateDidAuthRequest",
//                            documented(
//                                UserWalletController.validateDidAuthRequestDocs(),
//                                UserWalletController::validateDidAuthRequest
//                            )
//                        )
//                        post(
//                            "didAuthResponse",
//                            documented(UserWalletController.didAuthResponseDocs(), UserWalletController::didAuthResponse)
//                        )
//                        post(
//                            "vcAuthResponse",
//                            documented(UserWalletController.vcAuthResponseDocs(), UserWalletController::vcAuthResponse)
//                        )
//                        post(
//                            "oidcAuthResponse",
//                            documented(UserWalletController.oidcAuthResponseDocs(), UserWalletController::oidcAuthResponse)
//                        )
//                    }
//                }
//                path("ti") {
//                    path("credentials") {
//                        post("", documented(EosController.getCredentialDocs(), EosController::getCredential))
//                        get("{credentialId}", documented(EosController.getCredentialDocs(), EosController::getCredential))
//                    }
//                    get("requestCredentialUri", documented(EosController.requestCredentialUriDocs(), EosController::requestCredentialUri))
//                    post("requestVerifiableCredential", documented(EosController.requestVerifiableCredentialDocs(), EosController::requestVerifiableCredential))
//                }
//                path("eos") {
//                    post("onboard", documented(EosController.onboardsDocs(), EosController::onboards))
//                    post("signedChallenge", documented(EosController.signedChallengeDocs(),EosController::signedChallenge))
//                }
//                path("enterprise") {
//                    path("wallet") {
//                        post(
//                            "createDid",
//                            documented(EnterpriseWalletController.createDidDocs(), EnterpriseWalletController::createDid)
//                        )
//                        post(
//                            "requestVerifiableAuthorization",
//                            documented(
//                                EnterpriseWalletController.requestVerifiableAuthorizationDocs(),
//                                EnterpriseWalletController::requestVerifiableAuthorization
//                            )
//                        )
//                        post(
//                            "requestVerifiableCredential",
//                            documented(
//                                EnterpriseWalletController.requestVerifiableCredentialDocs(),
//                                EnterpriseWalletController::requestVerifiableCredential
//                            )
//                        )
//                        post(
//                            "generateDidAuthRequest",
//                            documented(
//                                EnterpriseWalletController.generateDidAuthRequestDocs(),
//                                EnterpriseWalletController::generateDidAuthRequest
//                            )
//                        )
//                        // post("onboardTrustedIssuer", EnterpriseWalletController::onboardTrustedIssuer) not supported yet
//                        post(
//                            "validateDidAuthResponse",
//                            documented(
//                                EnterpriseWalletController.validateDidAuthResponseDocs(),
//                                EnterpriseWalletController::validateDidAuthResponse
//                            )
//                        )
//                        post(
//                            "getVerifiableCredential",
//                            documented(
//                                EnterpriseWalletController.getVerifiableCredentialDocs(),
//                                EnterpriseWalletController::getVerifiableCredential
//                            )
//                        )
//                        post("token", documented(EnterpriseWalletController.tokenDocs(), EnterpriseWalletController::token))
//                        post("authentication-requests", EosController::authReq)
//                    }
//
//                }
//            }
        }.exception(IllegalArgumentException::class.java) { e, ctx ->
            log.error { e.stackTraceToString() }
            ctx.json(ErrorResponse(e.message ?: " Illegal argument exception", 400))
            ctx.status(400)
        }.exception(Exception::class.java) { e, ctx ->
            log.error { e.stackTraceToString() }
            ctx.json(ErrorResponse(e.message ?: " Unknown application error", 500))
            ctx.status(500)
        }.start(bindAddress, port)
    }

    /**
     * Stop ESSIF API if it's currently running
     */
    fun stop() = essifApi?.stop()

}
