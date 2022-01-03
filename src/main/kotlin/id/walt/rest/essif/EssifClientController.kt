package id.walt.rest.essif

import id.walt.services.essif.EssifClient
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document
import kotlinx.serialization.Serializable

@Serializable
data class EbsiOnboardRequest(
    val bearerToken: String,
    val did: String
)

@Serializable
data class EbsiTimestampRequest(
    val did: String,
    val ethDidAlias: String? = null,
    val data: String,
)

object EssifClientController {

    fun onboard(ctx: Context) {
        val req = ctx.bodyAsClass(EbsiOnboardRequest::class.java)
        ctx.json(EssifClient.onboard(req.did, req.bearerToken))
    }

    fun onboardDocs() = document().operation {
        it.summary("EBSI onboarding flow, which requests a Verifiable Authorization from the EOS.")
            .operationId("onboard")
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

    fun createTimestamp(ctx: Context) {
        val req = ctx.bodyAsClass(EbsiTimestampRequest::class.java)
        ctx.result(EssifClient.createTimestamp(req.did,req.ethDidAlias, req.data))
    }

    fun createTimestampDocs() = document().operation {
        it.summary("Creates a timestamp on the EBSI ledger.")
            .operationId("createTimestamp")
            .addTagsItem("ESSIF Client")
    }
        .body<EbsiTimestampRequest> { it.description("The DID (or ETH key alias) indicates which key to be used for creating the timestamp. The data will be written to the data-field of the timestamp.") }
        .json<String>("200") { it.description("Transaction ID of the timestamp request") }

    fun getByTransactionHash(ctx: Context) {
        ctx.json(EssifClient.getByTransactionHash(ctx.pathParam("txhash")) ?: "")
    }

    fun getByTransactionHashDocs() = document().operation {
        it.summary("Get a timestamp based on the transaction Hash").operationId("getByTransactionHash").addTagsItem("ESSIF Client")
    }.json<String>("200") { it.description("The resolved timestamp") }

    fun getByTimestampId(ctx: Context) {
        ctx.json(EssifClient.getByTimestampId(ctx.pathParam("timestampId")) ?: "")
    }

    fun getByTimestampIdDocs() = document().operation {
        it.summary("Get a timestamp based on the timestamp ID").operationId("getByTransactionId").addTagsItem("ESSIF Client")
    }.json<String>("200") { it.description("The resolved timestamp") }
}
