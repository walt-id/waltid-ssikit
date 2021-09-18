package id.walt.rest.essif.wallets

import id.walt.rest.ErrorResponse
import id.walt.services.essif.userwallet.UserWalletService
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import io.javalin.plugin.openapi.dsl.document


object UserWalletController {

    fun createDid(ctx: Context) {
        ctx.json("todo")
        UserWalletService.createDid()
    }

    fun createDidDocs() = document().operation {
        it.summary("Creates and registers DID on the EBSI Blockchain").operationId("createDid").addTagsItem("ESSIF User Wallet")
    }.body<String> { it.description("Verifiable Authorization") }.json<String>("200") { it.description("Created DID") }

    /**
     * By providing a Verifiable Authorization the protocols 'DID Auth' and 'Authenticated Key Exchange Protocol' are executed and if successful, the JWT Access Token for accessing the EBSI services is returned
     */
    fun requestAccessToken(ctx: Context) {
        ctx.json("todo")
        ctx.result(UserWalletService.requestAccessToken(ctx.body()))
    }

    fun requestAccessTokenDocs() = document().operation {
        it.summary("Runs the authentication-protocol and returns the JWT Access Token for accessing the protected EBSI services.")
            .operationId("requestAccessToken").addTagsItem("ESSIF User Wallet")
    }.body<String> { it.description("The Verifiable Authorization") }.json<String>("200") { it.description("JWT Access Token") }

    @OpenApi(
        summary = "Generates and sends the DID Auth Response message.",
        operationId = "didAuthResponse",
        tags = ["ESSIF User Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "DID Auth Request"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "VC Token"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun didAuthResponse(ctx: Context) {
        ctx.json(UserWalletService.didAuthResponse(ctx.body()))
    }

    @OpenApi(
        summary = "Generates a OIDC Auth Response message.",
        operationId = "oidcAuthResponse",
        tags = ["ESSIF User Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "todo"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "OIDC Auth response"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun oidcAuthResponse(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "Generates a VC Auth Response message.",
        operationId = "vcAuthResponse",
        tags = ["ESSIF User Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "VC Exchange Request"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "VC token"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun vcAuthResponse(ctx: Context) {
        val vcToken = UserWalletService.vcAuthResponse("vcExchangeRequest")
        ctx.result(vcToken)
    }

    fun validateDidAuthRequest(ctx: Context) {
        UserWalletService.validateDidAuthRequest(ctx.body())
        ctx.json("todo")
    }

    fun validateDidAuthRequestDocs() = document().operation {
        it.summary("Validates a DID Auth request.").operationId("validateDidAuthRequest").addTagsItem("ESSIF User Wallet")
    }.body<String> { it.description("DID Auth request") }
        .json<Boolean>("200") { it.description("True, in case the request could be validated.") }
}
