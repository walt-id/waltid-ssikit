package id.walt.rest.essif.wallets

import id.walt.services.essif.userwallet.UserWalletService
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document


object UserWalletController {

    fun createDid(ctx: Context) {
        ctx.json("todo")
        UserWalletService.createDid()
    }

    fun createDidDocs() = document().operation {
        it.summary("Creates and registers DID on the EBSI Blockchain").operationId("createUserDid").addTagsItem("ESSIF User Wallet")
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

    fun didAuthResponse(ctx: Context) {
        ctx.json(UserWalletService.didAuthResponse(ctx.body()))
    }

    fun didAuthResponseDocs() = document().operation {
        it.summary("Generates and sends the DID Auth Response message.").operationId("didAuthResponse")
            .addTagsItem("ESSIF User Wallet")
    }.body<String> { it.description("DID Auth Request") }.json<String>("200") { it.description("VC Token") }

    fun oidcAuthResponse(ctx: Context) {
        ctx.json("todo")
    }

    fun oidcAuthResponseDocs() = document().operation {
        it.summary("Generates a OIDC Auth Response message.").operationId("oidcAuthResponse").addTagsItem("ESSIF User Wallet")
    }.body<String> { it.description("todo") }.json<String>("200") { it.description("OIDC Auth response") }

    fun vcAuthResponse(ctx: Context) {
        val vcToken = UserWalletService.vcAuthResponse("vcExchangeRequest")
        ctx.result(vcToken)
    }

    fun vcAuthResponseDocs() = document().operation {
        it.summary("Generates a VC Auth Response message.").operationId("vcAuthResponse").addTagsItem("ESSIF User Wallet")
    }.body<String> { it.description("VC Exchange Request") }.json<String>("200") { it.description("VC token") }

    fun validateDidAuthRequest(ctx: Context) {
        UserWalletService.validateDidAuthRequest(ctx.body())
        ctx.json("todo")
    }

    fun validateDidAuthRequestDocs() = document().operation {
        it.summary("Validates a DID Auth request.").operationId("validateDidAuthRequest").addTagsItem("ESSIF User Wallet")
    }.body<String> { it.description("DID Auth request") }
        .json<Boolean>("200") { it.description("True, in case the request could be validated.") }
}
