package id.walt.rest.essif.wallets.ti

import id.walt.rest.ErrorResponse
import id.walt.services.essif.EssifServer
import id.walt.services.essif.enterprisewallet.EnterpriseWalletService
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document


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

    val enterpriseWalletService = EnterpriseWalletService()

//    @OpenApi(
//        summary = "Creates and registers DID on the EBSI Blockchain",
//        operationId = "createDid",
//        tags = ["Trusted Issuer"],
//        requestBody = OpenApiRequestBody(
//            [OpenApiContent(String::class)],
//            true,
//            "Verifiable Authorization"
//        ),
//        responses = [
//            OpenApiResponse("200", [OpenApiContent(String::class)], "Created DID"),
//            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
//            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
//        ]
//    )
    fun createDid(ctx: Context) {
        ctx.json(enterpriseWalletService.createDid())
    }

//    @OpenApi(
//        summary = "Generates the DID ownership response and fetches the requested credential.",
//        operationId = "getVerifiableCredential",
//        tags = ["Trusted Issuer"],
//        requestBody = OpenApiRequestBody(
//            [OpenApiContent(String::class)],
//            //[OpenApiContent(GetVcRequest::class)],
//            true,
//            "DID ownership request"
//        ),
//        responses = [
//            OpenApiResponse("200", [OpenApiContent(String::class)], "Verifiable Credential"),
//            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
//            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
//        ]
//    )
    fun getVerifiableCredential(ctx: Context) {
        //TODO: implement
        val getVcReq = ""//ctx.bodyAsClass(GetVcRequest::class.java)
        ctx.json(enterpriseWalletService.getVerifiableCredential("getVcReq.didOwnershipReq", "getVcReq.did"))
    }

//    @OpenApi(
//        summary = "Generates a DID-SIOP Auth Request",
//        operationId = "generateAuthenticationRequest",
//        tags = ["Trusted Issuer"],
//        responses = [
//            OpenApiResponse("200", [OpenApiContent(String::class)], "DID Auth Request"),
//            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
//            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
//        ]
//    )
    fun generateAuthenticationRequestDocs() = document()
    .operation { it.summary("Generates a DID-SIOP Auth Request.").operationId("generateAuthenticationRequest").addTagsItem("Trusted Issuer")   }
    .json<String>("200") { it.description("DID Auth Request") }
    .json<ErrorResponse>("400") { it.description("Bad request") }
    .json<ErrorResponse>("500") { it.description("Server Error") }
    fun generateAuthenticationRequest(ctx: Context) {
        ctx.result(EssifServer.generateAuthenticationRequest())
    }

//    @OpenApi(
//        summary = "Establishes a mutual authenticated DID-SIOP session",
//        operationId = "generateAuthenticationRequest",
//        tags = ["Trusted Issuer"],
//        responses = [
//            OpenApiResponse("200", [OpenApiContent(String::class)], "Encrypted access token"),
//            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
//            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
//        ]
//    )
    fun openSessionDocs() = document()
    .operation { it.summary("Establishes a mutual authenticated DID-SIOP session.").operationId("openSession").addTagsItem("Trusted Issuer")   }
    .json<String>("200") { it.description("Encrypted access token") }
    .json<ErrorResponse>("400") { it.description("Bad request") }
    .json<ErrorResponse>("500") { it.description("Server Error") }
    fun openSession(ctx: Context) {
        ctx.result(EssifServer.openSession(ctx.body()))
    }

}
