package id.walt.rest

import cc.vileda.openapi.dsl.components
import cc.vileda.openapi.dsl.externalDocs
import cc.vileda.openapi.dsl.info
import cc.vileda.openapi.dsl.securityScheme
import com.beust.klaxon.Klaxon
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.plugin.json.JavalinJson
import io.javalin.plugin.json.ToJsonMapper
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
import id.walt.Values

object RestAPI {

    private val log = KotlinLogging.logger {}

    val CORE_API_PORT = 7000
    val ESSIF_API_PORT = 7001
    val BIND_ADDRESS = "127.0.0.1"

    var coreApi: Javalin? = null
    var essifApi: Javalin? = null


    fun startCoreApi(port: Int = CORE_API_PORT, bindAddress: String = BIND_ADDRESS, apiTargetUrls: List<String> = listOf()) {
        log.info("Starting walt.id Core API ...\n")

        coreApi = Javalin.create {

            it.apply {
                registerPlugin(RouteOverviewPlugin("/api-routes"))

                registerPlugin(OpenApiPlugin(OpenApiOptions(InitialConfigurationCreator {
                    OpenAPI().apply {
                        info {
                            title = "walt.id Core API"
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
                            description = "Walt Docs"
                            url = "https://docs.walt.id"
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
                    swagger(SwaggerOptions("/v1/swagger").title("walt.id Core API"))
                    reDoc(ReDocOptions("/v1/redoc").title("walt.id Core API"))
//                defaultDocumentation { doc ->
//                    doc.json("5XX", ErrorResponse::class.java)
//                }
                }))

                //addStaticFiles("/static")
            }

            it.enableCorsForAllOrigins()

            it.enableDevLogging()
        }.routes {
            get("", RootController::rootCoreApi)
            get("health", RootController::health)
            path("v1") {
                path("key") {
                    get("", KeyController::list)
                    get(":id", KeyController::load)
                    delete(":id", KeyController::delete)
                    post("gen", KeyController::gen)
                    post("import", KeyController::import)
                    post("export", KeyController::export)
                }
                path("did") {
                    get("", DidController::list)
                    get(":id", DidController::load)
                    delete(":id", DidController::delete)
                    post("create", DidController::create)
                    post("resolve", DidController::resolve)
                    post("import", DidController::import)
                }
                path("vc") {
                    get("", VcController::list)
                    get(":id", VcController::load)
                    delete(":id", VcController::delete)
                    post("create", VcController::create)
                    post("present", VcController::present)
                    post("verify", VcController::verify)
                    post("import", VcController::import)
                }
            }

        }.exception(IllegalArgumentException::class.java) { e, ctx ->
            log.error(e.stackTraceToString())
            ctx.json(ErrorResponse(e.message ?: " Unknown application error", 400))
            ctx.status(400)
        }.exception(Exception::class.java) { e, ctx ->
            log.error(e.stackTraceToString())
            ctx.json(ErrorResponse(e.message ?: " Unknown server error", 500))
            ctx.status(500)
        }.start(bindAddress, port)
    }

    fun startEssifApi(port: Int = ESSIF_API_PORT, bindAddress: String = BIND_ADDRESS, apiTargetUrls: List<String> = listOf()) {

        log.info("Starting Walt Essif API ...\n")

        essifApi = Javalin.create {

            it.apply {
                registerPlugin(RouteOverviewPlugin("/api-routes"))

                registerPlugin(OpenApiPlugin(OpenApiOptions(InitialConfigurationCreator {
                    OpenAPI().apply {
                        info {
                            title = "Walt ESSIF Connector"
                            description = "The Walt public API documentation"
                            contact = Contact().apply {
                                name = "Walt"
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
                            description = "Walt Docs"
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
                    swagger(SwaggerOptions("/v1/swagger").title("Walt API"))
                    reDoc(ReDocOptions("/v1/redoc").title("Walt API"))
//                defaultDocumentation { doc ->
//                    doc.json("5XX", ErrorResponse::class.java)
//                }
                }))

                //addStaticFiles("/static")
            }

            it.enableCorsForAllOrigins()

            it.enableDevLogging()
        }.routes {
            get("", RootController::rootEssifApi)
            get("health", RootController::health)
            path("v1") {
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
                path("ti") {
                    path("credentials") {
                        post("", EosController::getCredential)
                        get(":credentialId", EosController::getCredential)
                    }
                    get("requestCredentialUri", EosController::requestCredentialUri)
                    post("requestVerifiableCredential", EosController::requestVerifiableCredential)
                }
                path("eos") {
                    post("onboard", EosController::onboards)
                    post("signedChallenge", EosController::signedChallenge)
                }
                path("enterprise") {
                    path("wallet") {
                        post("createDid", EnterpriseWalletController::createDid)
                        post(
                            "requestVerifiableAuthorization",
                            EnterpriseWalletController::requestVerifiableAuthorization
                        )
                        post("requestVerifiableCredential", EnterpriseWalletController::requestVerifiableCredential)
                        post("generateDidAuthRequest", EnterpriseWalletController::generateDidAuthRequest)
                        // post("onboardTrustedIssuer", EnterpriseWalletController::onboardTrustedIssuer) not supported yet
                        post("validateDidAuthResponse", EnterpriseWalletController::validateDidAuthResponse)
                        post("getVerifiableCredential", EnterpriseWalletController::getVerifiableCredential)
                        post("token", EnterpriseWalletController::token)
                    }

                }
                path("dummy") {
                    post("authentication-requests", EosController::authReq)
                }
            }
            path("v2") {
                path("trusted-issuer") {
                    post("generateAuthenticationRequest", TrustedIssuerController::generateAuthenticationRequest)
                    post("openSession", TrustedIssuerController::openSession)
                }
            }

            JavalinJson.toJsonMapper = object : ToJsonMapper {
                override fun map(obj: Any): String {
                    return Klaxon().toJsonString(obj)
                }
            }
            /*JavalinJson.fromJsonMapper = object : FromJsonMapper {
                override inline fun <reified T> map(json: String, targetClass: Class<T>): T =
                    Klaxon().parse<T::class>(json)
            }*/

        }.exception(IllegalArgumentException::class.java) { e, ctx ->
            log.error(e.stackTraceToString())
            ctx.json(ErrorResponse(e.message ?: " Illegal argument exception", 400))
            ctx.status(400)
        }.exception(Exception::class.java) { e, ctx ->
            log.error(e.stackTraceToString())
            ctx.json(ErrorResponse(e.message ?: " Unknown application error", 500))
            ctx.status(500)
        }.start(bindAddress, port)
    }

    fun start(apiPort: Int = CORE_API_PORT, essifPort: Int = ESSIF_API_PORT, bindAddress: String = BIND_ADDRESS, apiTargetUrls: List<String> = listOf()) {
        startCoreApi(apiPort, bindAddress, apiTargetUrls)
        startEssifApi(essifPort, bindAddress, apiTargetUrls)
    }

    fun stopCoreApi() = coreApi?.stop()
    fun stopEssifApi() = essifApi?.stop()

    fun stop() {
        stopCoreApi()
        stopEssifApi()
    }
}
