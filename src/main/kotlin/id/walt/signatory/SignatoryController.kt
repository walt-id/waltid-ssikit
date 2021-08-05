package id.walt.signatory

import id.walt.vclib.Helpers.encode
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import id.walt.rest.ErrorResponse

object SignatoryController {
    @OpenApi(
        summary = "List VC templates",
        operationId = "listTemplates",
        tags = ["Verifiable Credentials"],
        responses = [
            OpenApiResponse(
                "200",
                content = [OpenApiContent(from = String::class, isArray = true, type = "application/json")]
            ),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun listTemplates(ctx: Context) {
        ctx.json(Signatory.getService().listTemplates())
    }

    @OpenApi(
        summary = "Loads a VC template",
        operationId = "loadTemplate",
        tags = ["Verifiable Credentials"],
        pathParams = [
            OpenApiParam(name = "id", description = "Retrieves a single VC template form the data store")
        ],
        responses = [
            //TODO: FIX:  Cannot invoke "io.swagger.v3.oas.models.media.Schema.getName()" because "subtypeModel" is null
            // OpenApiResponse("200", [OpenApiContent(VerifiableCredential::class, type = "application/json")], "Verifiable credential template"),
            OpenApiResponse(
                "200",
                [OpenApiContent(String::class, type = "application/json")],
                "Verifiable credential template"
            ),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun loadTemplate(ctx: Context) {
        ctx.result(Signatory.getService().loadTemplate(ctx.pathParam("id")).encode())
    }
}
