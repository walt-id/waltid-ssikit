package id.walt.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiResponse

object RootController {

    @OpenApi(
        ignore = true, // Hide this endpoint in the documentation
        summary = "HTML page with links to the API doc",
        operationId = "rootCoreApi",
        responses = [
            OpenApiResponse("200"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun rootCoreApi(ctx: Context) {
        ctx.html(
            " <!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<body>\n" +
                    "\n" +
                    "<h1>walt.id core API</h1>\n" +
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

    @OpenApi(
        ignore = true, // Hide this endpoint in the documentation
        summary = "HTML page with links to the API doc",
        operationId = "rootCoreApi",
        responses = [
            OpenApiResponse("200"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
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

    @OpenApi(
        summary = "HTML page with links to the API doc",
        operationId = "rootCustodianApi",
        responses = [
            OpenApiResponse("200"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
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

    @OpenApi(
        summary = "Returns HTTP 200 in case all services are up and running",
        operationId = "health",
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful request"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun health(ctx: Context) {
        // TODO: implement: WaltIdServices.checkHealth()
        ctx.html("OK")
    }
}
