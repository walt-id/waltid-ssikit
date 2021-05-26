package org.letstrust.rest

import cc.vileda.openapi.dsl.components
import cc.vileda.openapi.dsl.externalDocs
import cc.vileda.openapi.dsl.info
import cc.vileda.openapi.dsl.securityScheme
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.util.RouteOverviewPlugin
import io.javalin.plugin.json.FromJsonMapper
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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import mu.KotlinLogging
import org.letstrust.Values

object RestAPI {

    val CORE_API_PORT = 7000
    val ESSIF_API_PORT = 7001

    private val log = KotlinLogging.logger {}

    var coreApi: Javalin? = null
    var essifApi: Javalin? = null

    fun startCoreApi(port: Int = CORE_API_PORT) {
        log.info("Starting LetTrust Wallet API ...\n")

        coreApi = Javalin.create {

            it.apply {
                registerPlugin(RouteOverviewPlugin("/api-routes"))

                registerPlugin(OpenApiPlugin(OpenApiOptions(InitialConfigurationCreator {
                    OpenAPI().apply {
                        info {
                            title = "LetsTrust Wallet API"
                            description = "The LetsTrust public API documentation"
                            contact = Contact().apply {
                                name = "LetsTrust"
                                url = "https://letstrust.org"
                                email = "office@letstrust.org"
                            }
                            version = Values.version
                        }
                        servers = listOf(
                            Server().description("Local testing server").url("http://localhost:$port"),
                            Server().description("LetsTrust").url("https://wallet-api.letstrust.org")
                        )
                        externalDocs {
                            description = "LetsTrust Docs"
                            url = "https://docs.letstrust.org/api"
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
                    swagger(SwaggerOptions("/v1/swagger").title("LetsTrust API"))
                    reDoc(ReDocOptions("/v1/redoc").title("LetsTrust API"))
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
                    path("templates") {
                        get("", VcController::listTemplates)
                        get(":id", VcController::loadTemplate)
                    }
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
        }.start(port)
    }

    fun startEssifApi(port: Int = ESSIF_API_PORT) {

        log.info("Starting LetsTrust Essif API ...\n")

        essifApi = Javalin.create {

            it.apply {
                registerPlugin(RouteOverviewPlugin("/api-routes"))

                registerPlugin(OpenApiPlugin(OpenApiOptions(InitialConfigurationCreator {
                    OpenAPI().apply {
                        info {
                            title = "LetsTrust ESSIF Connector"
                            description = "The LetsTrust public API documentation"
                            contact = Contact().apply {
                                name = "LetsTrust"
                                url = "https://letstrust.org"
                                email = "office@letstrust.org"
                            }
                            version = Values.version
                        }
                        servers = listOf(
                            Server().description("Local testing server").url("http://localhost:$port"),
                            Server().description("LetsTrust").url("https://essif-connector-api.letstrust.org")
                        )
                        externalDocs {
                            description = "LetsTrust Docs"
                            url = "https://docs.letstrust.org/api"
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
                    swagger(SwaggerOptions("/v1/swagger").title("LetsTrust API"))
                    reDoc(ReDocOptions("/v1/redoc").title("LetsTrust API"))
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
                        post("requestVerifiableAuthorization", EnterpriseWalletController::requestVerifiableAuthorization)
                        post("requestVerifiableCredential", EnterpriseWalletController::requestVerifiableCredential)
                        post("generateDidAuthRequest", EnterpriseWalletController::generateDidAuthRequest)
                        // post("onboardTrustedIssuer", EnterpriseWalletController::onboardTrustedIssuer) not supported yet
                        post("validateDidAuthResponse", EnterpriseWalletController::validateDidAuthResponse)
                        post("getVerifiableCredential", EnterpriseWalletController::getVerifiableCredential)
                        post("token", EnterpriseWalletController::token)
                    }

                }
            }

            JavalinJson.toJsonMapper = object : ToJsonMapper {
                override fun map(obj: Any): String {

                    if (obj is ArrayList<*>) {
                        // TODO: support other list-element types
                        return Json.encodeToString(ListSerializer(String.serializer()),obj as ArrayList<String>)
                    }

                    return Json.encodeToString(serializer(obj.javaClass), obj)
                }
            }
            JavalinJson.fromJsonMapper = object : FromJsonMapper {
                override fun <T> map(json: String, targetClass: Class<T>): T = Json.decodeFromString(serializer(targetClass) as KSerializer<T>, json)
            }

        }.exception(IllegalArgumentException::class.java) { e, ctx ->
            log.error(e.stackTraceToString())
            ctx.json(ErrorResponse(e.message ?: " Illegal argument exception", 400))
            ctx.status(400)
        }.exception(Exception::class.java) { e, ctx ->
            log.error(e.stackTraceToString())
            ctx.json(ErrorResponse(e.message ?: " Unknown application error", 500))
            ctx.status(500)
        }.start(port)
    }

    fun start() {
        startCoreApi()
        startEssifApi()
    }

    fun stopCoreApi() = coreApi?.stop()
    fun stopEssifApi() = essifApi?.stop()

    fun stop() {
        stopCoreApi()
        stopEssifApi()
    }
}
