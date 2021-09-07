package id.walt.rest

import id.walt.rest.TrustedIssuerController.enterpriseWalletService
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse

data class EbsiOnboardRequest(
    val bearerToken: String,
    val did: String
)

object EssifClientController {

    @OpenApi(
        summary = "EBSI onboarding flow, which requests a Verifiable Authorization from the EOS.",
        operationId = "onboard",
        tags = ["ESSIF Client"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(EbsiOnboardRequest::class)],
            true,
            "DID to be registered on the EBSI Blockchain; Bearer token to be used to authenticate the user."
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "Onboarding flow completed successfully"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun onboard(ctx: Context) {
        val req = ctx.bodyAsClass(EbsiOnboardRequest::class.java)
        println(req)
        // ctx.json(EssifFlowRunner.onboard(req.did, req.bearerToken))
        ctx.json("ok")
    }

    @OpenApi(
        summary = "Creates and registers DID on the EBSI Blockchain",
        operationId = "createDid",
        tags = ["ESSIF Client"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "Verifiable Authorization"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "Created DID"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun createDid(ctx: Context) {
        ctx.json(enterpriseWalletService.createDid())
    }


}
