package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import kotlinx.serialization.Serializable
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

//@Serializable
//data class GetVcRequest(val did: String, val didOwnershipReq: String)

object EnterpriseWalletController {

    @OpenApi(
        summary = "Creates and registers DID on the EBSI Blockchain",
        operationId = "createDid",
        tags = ["ESSIF Enterprise Wallet"],
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
        ctx.json(EnterpriseWalletService.createDid())
    }

    @OpenApi(
        summary = "Generates the DID ownership response and fetches the requested credential.",
        operationId = "getVerifiableCredential",
        tags = ["ESSIF Enterprise Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            //[OpenApiContent(GetVcRequest::class)],
            true,
            "DID ownership request"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "Verifiable Credential"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun getVerifiableCredential(ctx: Context) {
        //TODO: implement
        val getVcReq = ""//ctx.bodyAsClass(GetVcRequest::class.java)
        ctx.json(EnterpriseWalletService.getVerifiableCredential("getVcReq.didOwnershipReq", "getVcReq.did"))
    }

//    @OpenApi(
//        summary = "TODO",
//        operationId = "onboardTrustedIssuer",
//        tags = ["ESSIF Enterprise Wallet"],
//        requestBody = OpenApiRequestBody(
//            [OpenApiContent(String::class)],
//            true,
//            "todo"
//        ),
//        responses = [
//            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
//            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
//        ]
//    )
//    fun onboardTrustedIssuer(ctx: Context) {
//        EnterpriseWalletService.onboardTrustedIssuer("scanQrUri")
//        ctx.json("todo")
//    }

    @OpenApi(
        summary = "Generates a DID Auth Request",
        operationId = "generateDidAuthRequest",
        tags = ["ESSIF Enterprise Wallet"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "DID Auth Reqeust"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun generateDidAuthRequest(ctx: Context) {
        ctx.json(EnterpriseWalletService.generateDidAuthRequest())
    }

    @OpenApi(
        summary = "Validates a DID Auth response",
        operationId = "validateDidAuthResponse",
        tags = ["ESSIF Enterprise Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "DID Auth Response"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(Boolean::class)], "True, if response could be validated"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun validateDidAuthResponse(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "Performs DID Auth in order to obtain a Verifiable Authorization",
        operationId = "requestVerifiableAuthorization",
        tags = ["ESSIF Enterprise Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "Access Token"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "Verifiable Authorization"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun requestVerifiableAuthorization(ctx: Context) {
        ctx.result(EnterpriseWalletService.requestVerifiableAuthorization("token"))
    }

    @OpenApi(
        summary = "Request credential",
        operationId = "requestVerifiableCredential",
        tags = ["ESSIF Enterprise Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "Credential Request URI"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "DID ownership response"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun requestVerifiableCredential(ctx: Context) {
        ctx.json(EnterpriseWalletService.requestVerifiableCredential())
    }

    @OpenApi(
        summary = "OIDC Token endpoint",
        operationId = "token",
        tags = ["ESSIF Enterprise Wallet"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "oidcAuthResp"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun token(ctx: Context) {
        ctx.json(EnterpriseWalletService.token("oidcAuthResp"))
    }
}
