package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.letstrust.services.essif.UserWalletService



object UserWalletController {

    @OpenApi(
        summary = "Creates and registers DID on the EBSI Blockchain",
        operationId = "createDid",
        tags = ["ESSIF User Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "Verifiable Authorization"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "Created DID"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun createDid(ctx: Context) {
        ctx.json("todo")
        UserWalletService.createDid()
    }

    /**
     * By providing a Verifiable Authorization the protocols 'DID Auth' and 'Authenticated Key Exchange Protocol' are executed and if successful, the JWT Access Token for accessing the EBSI services is returned
     */
    @OpenApi(
        summary = "Runs the authentication-protocol and returns the JWT Access Token for accessing the protected EBSI services.",
        operationId = "requestAccessToken",
        tags = ["ESSIF User Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "The Verifiable Authorization"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "JWT Access Token"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun requestAccessToken(ctx: Context) {
        ctx.json("todo")
        ctx.result(UserWalletService.requestAccessToken(ctx.body()))
    }

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

    @OpenApi(
        summary = "Validates a DID Auth request.",
        operationId = "validateDidAuthRequest",
        tags = ["ESSIF User Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "DID Auth request"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(Boolean::class)], "True, in case the request could be validated."),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun validateDidAuthRequest(ctx: Context) {
        UserWalletService.validateDidAuthRequest(ctx.body())
        ctx.json("todo")
    }
}
