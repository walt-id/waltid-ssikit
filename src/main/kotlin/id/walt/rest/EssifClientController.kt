package id.walt.rest

import id.walt.rest.TrustedIssuerController.enterpriseWalletService
import id.walt.services.essif.EssifClient
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
            "DID to be registered on the EBSI Blockchain; Bearer token to be used to authenticate the user. Get it from here https://app.preprod.ebsi.eu/users-onboarding"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "Onboarding flow completed successfully"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun onboard(ctx: Context) {
        val req = ctx.bodyAsClass(EbsiOnboardRequest::class.java)
        ctx.json(EssifClient.onboard(req.did, req.bearerToken))
    }

    @OpenApi(
        summary = "Runs the ESSIF Authorization API flow",
        operationId = "createDid",
        tags = ["ESSIF Client"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "DID"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "Auth flow executed successfully"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )

    fun authApi(ctx: Context) {
        ctx.json(EssifClient.authApi(ctx.body()))
    }

    @OpenApi(
        summary = "Registers DID on the EBSI Blockchain",
        operationId = "registerDid",
        tags = ["ESSIF Client"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "DID"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "DID registered successfully"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun registerDid(ctx: Context) {
        val did = ctx.body()
        ctx.json(EssifClient.registerDid(did, did))
    }

}
