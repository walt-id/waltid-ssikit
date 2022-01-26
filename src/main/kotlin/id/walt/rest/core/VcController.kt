package id.walt.rest.core

import id.walt.services.vc.JsonLdCredentialService
import id.walt.signatory.ProofConfig
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document
import kotlinx.serialization.Serializable

@Serializable
data class CreateVcRequest(
    val issuerDid: String,
    val subjectDid: String?,
    val credentialOffer: String?,
    val templateId: String? = null,
    val domain: String? = null,
    val nonce: String? = null,
)

@Serializable
data class PresentVcRequest(
    val vc: String,
    val holderDid: String,
    val domain: String?,
    val challenge: String?
)

@Serializable
data class VerifyVcRequest(
    val vcOrVp: String
)

object VcController {

    private val credentialService = JsonLdCredentialService.getService()

    fun load(ctx: Context) {
        ctx.json("todo - load")
    }

    fun loadDocs() = document().operation {
        it.summary("Load VC").operationId("loadVc").addTagsItem("Verifiable Credentials")
    }.body<String> { it.description("ID of the DID to be loaded") }.json<String>("200")

    fun delete(ctx: Context) {
        ctx.json("todo - delete")
    }

    fun deleteDocs() = document().operation {
        it.summary("Delete VC").operationId("deleteVc").addTagsItem("Verifiable Credentials")
    }.body<String> { it.description("ID of VC to be deleted") }.json<String>("200")

    fun create(ctx: Context) {
        val createVcReq = ctx.bodyAsClass(CreateVcRequest::class.java)
        ctx.contentType(ContentType.JSON).result(
            credentialService.sign(
                createVcReq.credentialOffer!!,
                ProofConfig(
                    issuerDid = createVcReq.issuerDid,
                    domain = createVcReq.domain,
                    nonce = createVcReq.nonce
                )
            )
        )
    }

    fun createDocs() = document().operation {
        it.summary("Create VC").operationId("createVc").addTagsItem("Verifiable Credentials")
    }
        .body<CreateVcRequest> { it.description("Defines the credential issuer, holder and optionally a credential template  -  TODO: build credential based on the request e.g. load template, substitute values") }
        .json<String>("200") { it.description("The signed credential") }

    fun present(ctx: Context) {
        val presentVcReq = ctx.bodyAsClass(PresentVcRequest::class.java)
        ctx.result(
            credentialService.present(
                listOf(presentVcReq.vc),
                presentVcReq.holderDid,
                presentVcReq.domain,
                presentVcReq.challenge,
                null
            )
        )
    }

    fun presentDocs() = document().operation {
        it.summary("Present VC").operationId("presentVc").addTagsItem("Verifiable Credentials")
    }.body<PresentVcRequest> { it.description("Defines the VC to be presented") }
        .json<String>("200") { it.description("The signed presentation") }

    fun verify(ctx: Context) {
        val verifyVcReq = ctx.bodyAsClass(VerifyVcRequest::class.java)
        ctx.json(credentialService.verify(verifyVcReq.vcOrVp))
    }

    fun verifyDocs() = document().operation {
        it.summary("Verify VC").operationId("verifyVc").addTagsItem("Verifiable Credentials")
    }.body<VerifyVcRequest> { it.description("VC to be verified") }
        .json<VerifyVcRequest>("200") { it.description("Verification result object") }

    fun list(ctx: Context) {
        ctx.json(credentialService.listVCs())
    }

    fun listDocs() = document().operation {
        it.summary("List VCs").operationId("listVcs").addTagsItem("Verifiable Credentials")
    }.json<Array<String>>("200")

    fun import(ctx: Context) {
        ctx.json("todo - import")
    }

    fun importDocs() = document().operation {
        it.summary("Import VC").operationId("importVc").addTagsItem("Verifiable Credentials")
    }.body<String>().json<String>("200")

}
