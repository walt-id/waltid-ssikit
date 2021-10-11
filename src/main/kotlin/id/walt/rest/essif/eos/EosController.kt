package id.walt.rest.essif.eos

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import id.walt.model.AuthRequestResponse
import id.walt.rest.ErrorResponse
import id.walt.services.essif.TrustedIssuerClient
import io.javalin.plugin.openapi.dsl.document

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

//    @OpenApi(
//        summary = "Request Verifiable Authorization. Returns the DID ownership request.",
//        operationId = "onboards",
//        tags = ["ESSIF Enterprise Wallet"],
//        requestBody = OpenApiRequestBody(
//            [OpenApiContent(String::class)],
//            true,
//            "DID to be registered"
//        ),
//        responses = [
//            OpenApiResponse("200", [OpenApiContent(String::class)], "Request DID ownership"),
//            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
//            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
//        ]
//    )
    fun onboardsDocs() = document()
    .operation { it.summary("Request Verifiable Authorization. Returns the DID ownership request.").operationId("onboards").addTagsItem("ESSIF Enterprise Wallet")   }
    .body<String>() { it.description("DID to be registered") }
    .json<String>("200") { it.description("Request DID ownership") }
    .json<ErrorResponse>("400") { it.description("Bad request") }
    .json<ErrorResponse>("500") { it.description("Server Error") }
    fun onboards(ctx: Context) {
        ctx.json(TrustedIssuerClient.onboards())
    }

//    @OpenApi(
//        summary = "Processes the signed challenge in the scope of DID Auth and if successful, returns the Verifiable Authorization",
//        operationId = "signedChallenge",
//        tags = ["ESSIF Enterprise Wallet"],
//        requestBody = OpenApiRequestBody(
//            [OpenApiContent(String::class)],
//            true,
//            "Signed challenge"
//        ),
//        responses = [
//            OpenApiResponse("200", [OpenApiContent(String::class)], "Verifiable Authorization"),
//            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
//            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
//        ]
//    )
    fun signedChallengeDocs() = document()
    .operation { it.summary("Processes the signed challenge in the scope of DID Auth and if successful, returns the Verifiable Authorization").operationId("signedChallenge").addTagsItem("ESSIF Enterprise Wallet")   }
    .body<String>() { it.description("Signed challenge") }
    .json<String>("200") { it.description("Verifiable Authorization") }
    .json<ErrorResponse>("400") { it.description("Bad request") }
    .json<ErrorResponse>("500") { it.description("Server Error") }
    fun signedChallenge(ctx: Context) {
        ctx.json(TrustedIssuerClient.signedChallenge("signedChallenge"))
    }

//    @OpenApi(
//        summary = "Creates an OIDC authentication request URI",
//        operationId = "requestCredentialUri",
//        tags = ["ESSIF Enterprise Wallet"],
//        responses = [
//            OpenApiResponse("200", [OpenApiContent(String::class)], "OIDC Authentication Request URI"),
//            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
//            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
//        ]
//    )
    fun requestCredentialUriDocs() = document()
    .operation { it.summary("Creates an OIDC authentication request URI.").operationId("requestCredentialUri").addTagsItem("ESSIF Enterprise Wallet")   }
    .json<String>("200") { it.description("OIDC Authentication Request URI") }
    .json<ErrorResponse>("400") { it.description("Bad request") }
    .json<ErrorResponse>("500") { it.description("Server Error") }
    fun requestCredentialUri(ctx: Context) {
        ctx.json(TrustedIssuerClient.requestCredentialUri())
    }

//    @OpenApi(
//        summary = "Auth request",
//        operationId = "authReq",
//        tags = ["ESSIF Enterprise Wallet"],
//        responses = [
//            OpenApiResponse("200", [OpenApiContent(String::class)], ""),
//            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
//            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
//        ]
//    )
    fun authReqDocs() = document()
    .operation { it.summary("Auth request").operationId("authReq").addTagsItem("ESSIF Enterprise Wallet")   }
    .json<String>("200") { it.description("") }
    .json<ErrorResponse>("400") { it.description("Bad request") }
    .json<ErrorResponse>("500") { it.description("Server Error") }
    fun authReq(ctx: Context) {
        println("authReq: " + ctx.body())
        ctx.json(AuthRequestResponse("asdf2weswfsadfdf"))
    }

//    @OpenApi(
//        summary = "Returns the DID ownership request",
//        operationId = "requestVerifiableCredential",
//        tags = ["ESSIF Enterprise Wallet"],
//        requestBody = OpenApiRequestBody(
//            [OpenApiContent(String::class)],
//            true,
//            "Credential request URI"
//        ),
//        responses = [
//            OpenApiResponse("200", [OpenApiContent(String::class)], "DID ownership request"),
//            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
//            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
//        ]
//    )
    fun requestVerifiableCredentialDocs() = document()
    .operation { it.summary("Returns the DID ownership request").operationId("requestVerifiableCredential").addTagsItem("ESSIF Enterprise Wallet")   }
    .body<String>() { it.description("Credential request URI") }
    .json<String>("200") { it.description("DID ownership request") }
    .json<ErrorResponse>("400") { it.description("Bad request") }
    .json<ErrorResponse>("500") { it.description("Server Error") }
    fun requestVerifiableCredential(ctx: Context) {
        ctx.json(TrustedIssuerClient.requestVerifiableCredential())
    }

//    @OpenApi(
//        summary = "todo",
//        operationId = "didOwnershipResponse",
//        tags = ["ESSIF Enterprise Wallet"],
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

//    @OpenApi(
//        summary = "Returns DID Auth Request or the requested credential if a VC Token is presented",
//        operationId = "getCredential",
//        tags = ["ESSIF Enterprise Wallet"],
//        requestBody = OpenApiRequestBody(
//            [OpenApiContent(String::class)],
//            false,
//            "Optional VC Token"
//        ),
//        responses = [
//            OpenApiResponse("200", [OpenApiContent(String::class)], "DID Auth Request"),
//            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
//            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
//        ]
//    )
    fun getCredentialDocs() = document()
    .operation { it.summary("Returns DID Auth Request or the requested credential if a VC Token is presented").operationId("getCredential").addTagsItem("ESSIF Enterprise Wallet")   }
    .json<String>("200") { it.description("DID Auth Request") }
    .json<ErrorResponse>("400") { it.description("Bad request") }
    .json<ErrorResponse>("500") { it.description("Server Error") }
    fun getCredential(ctx: Context) {

        val vcToken = ctx.body()
        //TODO check if vcToken is available and valid

        ctx.json(TrustedIssuerClient.getCredentials(false))
    }


}
