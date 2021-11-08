package id.walt.signatory.rest

import cc.vileda.openapi.dsl.components
import cc.vileda.openapi.dsl.externalDocs
import cc.vileda.openapi.dsl.info
import cc.vileda.openapi.dsl.securityScheme
import com.beust.klaxon.Klaxon
import com.fasterxml.jackson.databind.ObjectMapper
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

object SignatoryRestAPI {

    val SIGNATORY_API_PORT = 7001
    val BIND_ADDRESS = "127.0.0.1"

    private val log = KotlinLogging.logger {}

    var signatoryApi: Javalin? = null

    fun start(
        port: Int = SIGNATORY_API_PORT,
        bindAddress: String = BIND_ADDRESS,
        apiTargetUrls: List<String> = listOf()
    ) {

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
                    swagger(SwaggerOptions("/v1/swagger").title("walt.id Signatory API"))
                    reDoc(ReDocOptions("/v1/redoc").title("walt.id Signatory API"))
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

            }

            it.enableCorsForAllOrigins()

            it.enableDevLogging()
        }.routes {
            get("", documented(documentedIgnored(), RootController::rootSignatoryApi))
            get("health", documented(RootController.healthDocs(), RootController::health))
            path("v1") {
                path("credentials") {
                    post("issue", documented(SignatoryController.issueCredentialDocs(), SignatoryController::issueCredential))
                }
                path("templates") {
                    get("", documented(SignatoryController.listTemplatesDocs(), SignatoryController::listTemplates))
                    get("{id}", documented(SignatoryController.loadTemplateDocs(), SignatoryController::loadTemplate))
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
