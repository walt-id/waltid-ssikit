package id.walt.rest.essif

import id.walt.services.essif.EssifClient
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document

data class EbsiOnboardRequest(
    val bearerToken: String,
    val did: String
)

object EssifClientController {

    fun onboard(ctx: Context) {
        val req = ctx.bodyAsClass(EbsiOnboardRequest::class.java)
        ctx.json(EssifClient.onboard(req.did, req.bearerToken))
    }

    fun onboardDocs() = document().operation {
        it.summary("EBSI onboarding flow, which requests a Verifiable Authorization from the EOS.").operationId("onboard")
            .addTagsItem("ESSIF Client")
    }
        .body<EbsiOnboardRequest> { it.description("DID to be registered on the EBSI Blockchain; Bearer token to be used to authenticate the user. Get it from here https://app.preprod.ebsi.eu/users-onboarding") }
        .json<String>("200") { it.description("Onboarding flow completed successfully") }

    fun authApi(ctx: Context) {
        ctx.json(EssifClient.authApi(ctx.body()))
    }

    fun authApiDocs() = document().operation {
        it.summary("Runs the ESSIF Authorization API flow").operationId("createDid").addTagsItem("ESSIF Client")
    }.body<String> { it.description("DID") }.json<String>("200") { it.description("Auth flow executed successfully") }

    fun registerDid(ctx: Context) {
        val did = ctx.body()
        ctx.json(EssifClient.registerDid(did, did))
    }

    fun registerDidDocs() = document().operation {
        it.summary("Registers DID on the EBSI Blockchain").operationId("registerDid").addTagsItem("ESSIF Client")
    }.body<String> { it.description("DID") }.json<String>("200") { it.description("DID registered successfully") }

}
