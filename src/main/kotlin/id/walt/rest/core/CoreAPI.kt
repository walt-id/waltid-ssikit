package id.walt.rest.core

import cc.vileda.openapi.dsl.components
import cc.vileda.openapi.dsl.externalDocs
import cc.vileda.openapi.dsl.info
import cc.vileda.openapi.dsl.securityScheme
import com.beust.klaxon.Klaxon
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import id.walt.Values
import id.walt.rest.DidController
import id.walt.rest.ErrorResponse
import id.walt.rest.OpenAPIUtils.documentedIgnored
import id.walt.rest.RootController
import id.walt.rest.RootController.healthDocumentation
import id.walt.rest.VcController
import id.walt.rest.core.KeyController.deleteDocumentation
import id.walt.rest.core.KeyController.exportDocumentation
import id.walt.rest.core.KeyController.genDocumentation
import id.walt.rest.core.KeyController.importDocumentation
import id.walt.rest.core.KeyController.listDocumentation
import id.walt.rest.core.KeyController.loadDocumentation
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


object CoreAPI {

    private val log = KotlinLogging.logger {}

    internal const val DEFAULT_CORE_API_PORT = 7000

    internal const val DEFAULT_BIND_ADDRESS = "127.0.0.1"

    /**
     * Currently used instance of the Core API server
     */
    var coreApi: Javalin? = null

    /**
     * Start Core REST API
     * @param apiTargetUrls (optional): add URLs to Swagger documentation for easy testing
     * @param bindAddress (default: 127.0.0.1): select address to bind on to, e.g. 0.0.0.0 for all interfaces
     * @param port (default: 7000): select port to listen on
     */
    fun start(
        port: Int = DEFAULT_CORE_API_PORT,
        bindAddress: String = DEFAULT_BIND_ADDRESS,
        apiTargetUrls: List<String> = listOf()
    ) {
        log.info { "Starting walt.id Core API ...\n" }

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
                            description = "walt.id Docs"
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


                this.jsonMapper(object : JsonMapper {
                    override fun toJsonString(obj: Any): String {
                        return Klaxon().toJsonString(obj)
                    }

                    override fun <T : Any?> fromJsonString(json: String, targetClass: Class<T>): T {
                        return JavalinJackson().fromJsonString(json, targetClass)
                    }

                })

                //addStaticFiles("/static")
            }

            it.enableCorsForAllOrigins()

            it.enableDevLogging()
        }.routes {
            get("", documented(documentedIgnored(), RootController::rootCoreApi))
            get("health", documented(healthDocumentation(), RootController::health))
            path("v1") {
                path("key") {
                    get("", documented(listDocumentation(), KeyController::list))
                    get("{id}", documented(loadDocumentation(), KeyController::load))
                    delete("{id}", documented(deleteDocumentation(), KeyController::delete))
                    post("gen", documented(genDocumentation(), KeyController::gen))
                    post("import", documented(importDocumentation(), KeyController::import))
                    post("export", documented(exportDocumentation(), KeyController::export))
                }
                path("did") {
                    get("", DidController::list)
                    get("{id}", DidController::load)
                    delete("{id}", DidController::delete)
                    post("create", DidController::create)
                    post("resolve", DidController::resolve)
                    post("import", DidController::import)
                }
                path("vc") {
                    get("", VcController::list)
                    get("{id}", VcController::load)
                    delete("{id}", VcController::delete)
                    post("create", VcController::create)
                    post("present", VcController::present)
                    post("verify", VcController::verify)
                    post("import", VcController::import)
                }
            }
        }.exception(InvalidFormatException::class.java) { e, ctx ->
            log.error(e.stackTraceToString())
            ctx.json(ErrorResponse(e.message ?: " Unknown application error", 400))
            ctx.status(400)
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


    /**
     * Stop Core API if it's currently running
     */
    fun stop() = coreApi?.stop()
}
