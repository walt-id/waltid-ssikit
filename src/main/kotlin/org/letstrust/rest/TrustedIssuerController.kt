package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import kotlinx.serialization.Serializable
import org.letstrust.services.essif.EnterpriseWalletService
import org.letstrust.services.essif.EosService
import org.letstrust.services.essif.EssifServer
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

object TrustedIssuerController {

    @OpenApi(
        summary = "Creates and registers DID on the EBSI Blockchain",
        operationId = "createDid",
        tags = ["Trusted Issuer"],
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
        tags = ["Trusted Issuer"],
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

    @OpenApi(
        summary = "Generates a DID-SIOP Auth Request",
        operationId = "generateAuthenticationRequest",
        tags = ["Trusted Issuer"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "DID Auth Reqeust"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun generateAuthenticationRequest(ctx: Context) {
        ctx.result(EssifServer.generateAuthenticationRequest())
    }


    @OpenApi(
        summary = "Request credential",
        operationId = "requestVerifiableCredential",
        tags = ["Trusted Issuer"],
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

}
