package id.walt.rest.core

import cc.vileda.openapi.dsl.components
import cc.vileda.openapi.dsl.externalDocs
import cc.vileda.openapi.dsl.info
import cc.vileda.openapi.dsl.securityScheme
import com.beust.klaxon.Klaxon
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import id.walt.Values
import id.walt.rest.ErrorResponse
import id.walt.rest.OpenAPIUtils.documentedIgnored
import id.walt.rest.RootController
import id.walt.rest.RootController.healthDocs
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

                val mapper: ObjectMapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .findAndAddModules()
                    .build()

                this.jsonMapper(object : JsonMapper {
                    override fun toJsonString(obj: Any): String {
                        return Klaxon().toJsonString(obj)
                    }

                    override fun <T : Any?> fromJsonString(json: String, targetClass: Class<T>): T {
                        return JavalinJackson(mapper).fromJsonString(json, targetClass)
                    }
                })

                //addStaticFiles("/static")
            }

            it.enableCorsForAllOrigins()

            it.enableDevLogging()
        }.routes {
            get("", documented(documentedIgnored(), RootController::rootCoreApi))
            get("health", documented(healthDocs(), RootController::health))
            path("v1") {
                path("key") {
                    get("", documented(KeyController.listDocs(), KeyController::list))
                    get("{id}", documented(KeyController.loadDocs(), KeyController::load))
                    delete("{id}", documented(KeyController.deleteDocs(), KeyController::delete))
                    post("gen", documented(KeyController.genDocs(), KeyController::gen))
                    post("import", documented(KeyController.importDocs(), KeyController::import))
                    post("export", documented(KeyController.exportDocs(), KeyController::export))
                }
                path("did") {
                    get("", documented(DidController.listDocs(), DidController::list))
                    get("{id}", documented(DidController.loadDocs(), DidController::load))
                    delete("{id}", documented(DidController.deleteDocs(), DidController::delete))
                    post("create", documented(DidController.createDocs(), DidController::create))
                    post("resolve", documented(DidController.resolveDocs(), DidController::resolve))
                    post("import", documented(DidController.importDocs(), DidController::import))
                }
                path("vc") {
                    get("", documented(VcController.listDocs(), VcController::list))
                    get("{id}", documented(VcController.loadDocs(), VcController::load))
                    delete("{id}", documented(VcController.deleteDocs(), VcController::delete))
                    post("create", documented(VcController.createDocs(), VcController::create))
                    post("present", documented(VcController.presentDocs(), VcController::present))
                    post("verify", documented(VcController.verifyDocs(), VcController::verify))
                    post("import", documented(VcController.importDocs(), VcController::import))
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
