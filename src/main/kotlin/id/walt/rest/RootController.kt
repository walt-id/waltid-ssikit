package id.walt.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document

object RootController {

    fun rootCoreApi(ctx: Context) {
        ctx.html(
            " <!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<body>\n" +
                    "\n" +
                    "<h1>walt.id Core API</h1>\n" +
                    "<p><a href='/api-routes'>API Routes</a></p>\n" +
                    "<p><a href='/v1/swagger'>Swagger</a></p>\n" +
                    "<p><a href='/v1/redoc'>Redoc</a></p>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html> "
        )
    }

    fun rootSignatoryApi(ctx: Context) {
        ctx.html(
            " <!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<body>\n" +
                    "\n" +
                    "<h1>walt.id Signatory API</h1>\n" +
                    "<p><a href='/api-routes'>API Routes</a></p>\n" +
                    "<p><a href='/v1/swagger'>Swagger</a></p>\n" +
                    "<p><a href='/v1/redoc'>Redoc</a></p>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html> "
        )
    }

    fun rootAuditorApi(ctx: Context) {
        ctx.html(
            " <!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<body>\n" +
                    "\n" +
                    "<h1>walt.id Auditor API</h1>\n" +
                    "<p><a href='/api-routes'>API Routes</a></p>\n" +
                    "<p><a href='/v1/swagger'>Swagger</a></p>\n" +
                    "<p><a href='/v1/redoc'>Redoc</a></p>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html> "
        )
    }


    fun rootEssifApi(ctx: Context) {
        ctx.html(
            " <!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<body>\n" +
                    "\n" +
                    "<h1>walt.id ESSIF API</h1>\n" +
                    "<p><a href='/api-routes'>API Routes</a></p>\n" +
                    "<p><a href='/v1/swagger'>Swagger</a></p>\n" +
                    "<p><a href='/v1/redoc'>Redoc</a></p>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html> "
        )
    }

    fun rootCustodianApi(ctx: Context) {
        ctx.html(
            " <!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<body>\n" +
                    "\n" +
                    "<h1>walt.id Custodian API</h1>\n" +
                    "<p><a href='/api-routes'>API Routes</a></p>\n" +
                    "<p><a href='/v1/swagger'>Swagger</a></p>\n" +
                    "<p><a href='/v1/redoc'>Redoc</a></p>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html> "
        )
    }

    fun healthDocs() = document()
        .operation {
            it.summary("Returns HTTP 200 in case all services are up and running").operationId("health")
        }.json<String>("200")

    fun health(ctx: Context) {
        // TODO: implement: WaltIdServices.checkHealth()
        ctx.html("OK")
    }
}
