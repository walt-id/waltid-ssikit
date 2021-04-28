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
EOS

EosService.onboards()
EosService.didOwnershipResponse("")
EosService.getCredential("")
EosService.getCredentials()
EosService.requestCredentialUri()
EosService.requestVerifiableCredential("")
EosService.signedChallenge("")

 **/
object EosController {

    @OpenApi(
        summary = "Request Verifiable Authorization. Returns the DID ownership request.",
        operationId = "onboards",
        tags = ["ESSIF Onboarding Service"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "DID to be registered"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "Request DID ownership"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun onboards(ctx: Context) {
        ctx.json(EosService.onboards())
    }

    @OpenApi(
        summary = "Processes the signed challenge in the scope of DID Auth and if successful, returns the Verifiable Authorization",
        operationId = "signedChallenge",
        tags = ["ESSIF Trusted Issuer"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "Signed challenge"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "Verifiable Authorization"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun signedChallenge(ctx: Context) {
        ctx.json(EosService.signedChallenge("signedChallenge"))
    }

    @OpenApi(
        summary = "Creates an OIDC authentication request URI",
        operationId = "requestCredentialUri",
        tags = ["ESSIF Trusted Issuer"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "OIDC Authentication Request URI"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun requestCredentialUri(ctx: Context) {
        ctx.json(EosService.requestCredentialUri())
    }

    @OpenApi(
        summary = "Returns the DID ownership request",
        operationId = "requestVerifiableCredential",
        tags = ["ESSIF Trusted Issuer"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "Credential request URI"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "DID ownership request"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun requestVerifiableCredential(ctx: Context) {
        ctx.json(EosService.requestVerifiableCredential("credentialRequestUri"))
    }

//    @OpenApi(
//        summary = "todo",
//        operationId = "didOwnershipResponse",
//        tags = ["ESSIF Trusted Issuer"],
//        requestBody = OpenApiRequestBody(
//            [OpenApiContent(String::class)],
//            true,
//            "todo"
//        ),
//        responses = [
//            OpenApiResponse("200", [OpenApiContent(String::class)], "todo"),
//            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
//        ]
//    )
//    fun didOwnershipResponse(ctx: Context) {
//        ctx.json("todo")
//    }

    @OpenApi(
        summary = "Returns DID Auth Request or the requested credential if a VC Token is presented",
        operationId = "getCredential",
        tags = ["ESSIF Trusted Issuer"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            false,
            "Optional VC Token"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "DID Auth Request"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun getCredential(ctx: Context) {

        val vcToken = ctx.body()
        //TODO check if vcToken is available and valid

        ctx.json(EosService.getCredentials(false))
    }


}
