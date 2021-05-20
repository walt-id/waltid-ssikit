package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import io.swagger.v3.oas.annotations.Hidden

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
                    "<h1>Lets Trust Core API</h1>\n" +
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
                    "<h1>Lets Trust ESSIF API</h1>\n" +
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
        // TODO: implement: LetsTrustServices.checkHealth()
        ctx.html("OK")
    }
}
