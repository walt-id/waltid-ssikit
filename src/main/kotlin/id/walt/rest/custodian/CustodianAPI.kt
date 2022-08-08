package id.walt.rest.custodian

import cc.vileda.openapi.dsl.externalDocs
import cc.vileda.openapi.dsl.info
import com.beust.klaxon.Klaxon
import id.walt.Values
import id.walt.rest.ErrorResponse
import id.walt.rest.OpenAPIUtils
import id.walt.rest.RootController
import id.walt.rest.core.DidController
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
import io.swagger.v3.oas.models.servers.Server
import mu.KotlinLogging

object CustodianAPI {

    private val log = KotlinLogging.logger {}

    internal const val DEFAULT_Custodian_API_PORT = 7002
    internal const val DEFAULT_BIND_ADDRESS = "127.0.0.1"

    /**
     * Currently used instance of the Custodian API server
     */
    var custodianApi: Javalin? = null

    /**
     * Start Custodian REST API
     * @param apiTargetUrls (optional): add URLs to Swagger documentation for easy testing
     * @param bindAddress (default: 127.0.0.1): select address to bind on to, e.g. 0.0.0.0 for all interfaces
     * @param port (default: 7001): select port to listen on
     */
    fun start(
        port: Int = DEFAULT_Custodian_API_PORT,
        bindAddress: String = DEFAULT_BIND_ADDRESS,
        apiTargetUrls: List<String> = listOf()
    ) {
        log.info { "Starting walt.id Custodian API ...\n" }

        custodianApi = Javalin.create { config ->

            config.apply {
                registerPlugin(RouteOverviewPlugin("/api-routes"))

                registerPlugin(OpenApiPlugin(OpenApiOptions(InitialConfigurationCreator {
                    OpenAPI().apply {
                        info {
                            title = "walt.id Custodian API"
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
            get("/", documented(OpenAPIUtils.documentedIgnored(), RootController::rootCustodianApi))
            get("health", documented(RootController.healthDocs(), RootController::health))

            path("keys") {
                get("/", documented(CustodianController.listKeysDocs(), CustodianController::listKeys))
                get("{alias}", documented(CustodianController.getKeysDocs(), CustodianController::getKey))
                post("generate", documented(CustodianController.generateKeyDocs(), CustodianController::generateKey))
                post("import", documented(CustodianController.importKeysDocs(), CustodianController::importKey))
                delete("{id}", documented(CustodianController.deleteKeysDocs(), CustodianController::deleteKey))
                post("export", documented(CustodianController.exportKeysDocs(), CustodianController::exportKey))
            }

            path("did") {
                get("", documented(DidController.listDocs(), DidController::list))
                get("{id}", documented(DidController.loadDocs(), DidController::load))
                delete("{id}", documented(DidController.deleteDocs(), DidController::delete))
                post("create", documented(DidController.createDocs(), DidController::create))
                post("resolve", documented(DidController.resolveDocs(), DidController::resolve))
                post("import", documented(DidController.importDocs(), DidController::import))
            }

            path("credentials") {
                get("/", documented(CustodianController.listCredentialsDocs(), CustodianController::listCredentials))
                path("list/") {
                    get("credentialIds", documented(CustodianController.listCredentialIdsDocs(), CustodianController::listCredentialIds))
                }
                get("{id}",  documented(CustodianController.getCredentialDocs(), CustodianController::getCredential))
                put("{alias}",  documented(CustodianController.storeCredentialsDocs(),CustodianController::storeCredential))
                delete("{alias}", documented(CustodianController.deleteCredentialDocs(), CustodianController::deleteCredential))
                post("present", documented(CustodianController.presentCredentialsDocs(), CustodianController::presentCredentials))
                post("presentIds", documented(CustodianController.presentCredentialIdsDocs(), CustodianController::presentCredentialIds))
            }
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
     * Stop Custodian API if it's currently running
     */
    fun stop() = custodianApi?.stop()

}

fun main() {
    CustodianAPI.start()
}
