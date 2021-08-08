package id.walt.signatory

import cc.vileda.openapi.dsl.components
import cc.vileda.openapi.dsl.externalDocs
import cc.vileda.openapi.dsl.info
import cc.vileda.openapi.dsl.securityScheme
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
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
import id.walt.Values
import id.walt.rest.ErrorResponse
import id.walt.rest.KeyController
import id.walt.rest.RootController

object SignatoryRestAPI {

    val SIGNATORY_API_PORT = 7002
    val BIND_ADDRESS = "127.0.0.1"
    var signatoryApiUrl = ""

    private val log = KotlinLogging.logger {}

    var signatoryApi: Javalin? = null

    fun start(bindAddress: String = BIND_ADDRESS, port: Int = SIGNATORY_API_PORT, additionalApiServers: List<String> = listOf()) {

        signatoryApi = Javalin.create {

            it.apply {
                registerPlugin(RouteOverviewPlugin("/api-routes"))

                registerPlugin(OpenApiPlugin(OpenApiOptions(InitialConfigurationCreator {
                    OpenAPI().apply {
                        info {
                            title = "walt.id Signatory API"
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
                            *additionalApiServers.map { Server().url(it) }.toTypedArray()
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
                    swagger(SwaggerOptions("/v1/swagger").title("walt.id Signatory API"))
                    reDoc(ReDocOptions("/v1/redoc").title("walt.id Signatory API"))
                }))

            }

            it.enableCorsForAllOrigins()

            it.enableDevLogging()
        }.routes {
            get("", RootController::rootSignatoryApi)
            get("health", RootController::health)
            path("v1") {
                path("credentials") {
                    post("issue", KeyController::import)
                }
                path("templates") {
                    get("", SignatoryController::listTemplates)
                    get(":id", SignatoryController::loadTemplate)
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

    fun stop() = signatoryApi?.stop()
}
