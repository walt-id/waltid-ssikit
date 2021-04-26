package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.letstrust.services.essif.EnterpriseWalletService
import org.letstrust.services.essif.EosService
import org.letstrust.services.essif.UserWalletService


/**
EnterpriseWalletService

EnterpriseWalletService.onboardTrustedIssuer("")
EnterpriseWalletService.token("")
EnterpriseWalletService.createDid()
EnterpriseWalletService.generateDidAuthRequest()
EnterpriseWalletService.validateDidAuthResponse("")
EnterpriseWalletService.requestVerifiableAuthorization("")
EnterpriseWalletService.requestVerifiableCredential("")
EnterpriseWalletService.getVerifiableCredential("", "")
 **/
object EnterpriseWalletController {

    @OpenApi(
        summary = "TODO",
        operationId = "createDid",
        tags = ["ESSIF Enterprise Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "todo"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun createDid(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "TODO",
        operationId = "onboardTrustedIssuer",
        tags = ["ESSIF Enterprise Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "todo"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun onboardTrustedIssuer(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "TODO",
        operationId = "generateDidAuthRequest",
        tags = ["ESSIF Enterprise Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "todo"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun generateDidAuthRequest(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "TODO",
        operationId = "validateDidAuthResponse",
        tags = ["ESSIF Enterprise Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "todo"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun validateDidAuthResponse(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "TODO",
        operationId = "requestVerifiableAuthorization",
        tags = ["ESSIF Enterprise Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "todo"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun requestVerifiableAuthorization(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "TODO",
        operationId = "requestVerifiableCredential",
        tags = ["ESSIF Enterprise Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "todo"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun requestVerifiableCredential(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "TODO",
        operationId = "getVerifiableCredential",
        tags = ["ESSIF Enterprise Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "todo"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun getVerifiableCredential(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "TODO",
        operationId = "token",
        tags = ["ESSIF Enterprise Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "todo"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun token(ctx: Context) {
        ctx.json("todo")
    }
}
