package id.walt.rest.essif.wallets

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

object EnterpriseWalletController {

    private val enterpriseWalletService = EnterpriseWalletService.getService()

    fun createDid(ctx: Context) {
        ctx.json(enterpriseWalletService.createDid())
    }

    fun createDidDocs() = document().operation {
        it.summary("Creates and registers DID on the EBSI Blockchain").operationId("createEnterpriseDid")
            .addTagsItem("ESSIF Enterprise Wallet")
    }.body<String> { it.description("Verifiable Authorization") }.json<String>("200") { it.description("Created DID") }

    fun getVerifiableCredential(ctx: Context) {
        //TODO: implement
        val getVcReq = ""//ctx.bodyAsClass(GetVcRequest::class.java)
        ctx.json(enterpriseWalletService.getVerifiableCredential("getVcReq.didOwnershipReq", "getVcReq.did"))
    }

    fun getVerifiableCredentialDocs() = document().operation {
        it.summary("Generates the DID ownership response and fetches the requested credential.")
            .operationId("getVerifiableCredential").addTagsItem("ESSIF Enterprise Wallet")
    }.body<String> { it.description("DID ownership request") }.json<String>("200") { it.description("Verifiable Credential") }


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

    fun generateDidAuthRequest(ctx: Context) {
        ctx.json(enterpriseWalletService.generateDidAuthRequest())
    }

    fun generateDidAuthRequestDocs() = document().operation {
        it.summary("Generates a DID Auth Request").operationId("generateDidAuthRequest").addTagsItem("ESSIF Enterprise Wallet")
    }.json<String>("200") { it.description("DID Auth Reqeust") }

    fun validateDidAuthResponse(ctx: Context) {
        ctx.json("todo")
    }

    fun validateDidAuthResponseDocs() = document().operation {
        it.summary("Validates a DID Auth response").operationId("validateDidAuthResponse")
            .addTagsItem("ESSIF Enterprise Wallet")
    }.body<String> { it.description("DID Auth Response") }
        .json<Boolean>("200") { it.description("True, if response could be validated") }

    fun requestVerifiableAuthorization(ctx: Context) {
        ctx.result(enterpriseWalletService.requestVerifiableAuthorization("token"))
    }

    fun requestVerifiableAuthorizationDocs() = document().operation {
        it.summary("Performs DID Auth in order to obtain a Verifiable Authorization")
            .operationId("requestVerifiableAuthorization").addTagsItem("ESSIF Enterprise Wallet")
    }.body<String> { it.description("Access Token") }.json<String>("200") { it.description("Verifiable Authorization") }

    fun requestVerifiableCredential(ctx: Context) {
        ctx.json(enterpriseWalletService.requestVerifiableCredential())
    }

    fun requestVerifiableCredentialDocs() = document().operation {
        it.summary("Request credential").operationId("requestEnterpriseVerifiableCredential").addTagsItem("ESSIF Enterprise Wallet")
    }.body<String> { it.description("Credential Request URI") }.json<String>("200") { it.description("DID ownership response") }

    fun token(ctx: Context) {
        ctx.json(enterpriseWalletService.token("oidcAuthResp"))
    }

    fun tokenDocs() = document().operation {
        it.summary("OIDC Token endpoint").operationId("token").addTagsItem("ESSIF Enterprise Wallet")
    }.body<String> { it.description("oidcAuthResp") }.json<String>("200")
}
