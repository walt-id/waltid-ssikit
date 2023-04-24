package id.walt.signatory.rest

import id.walt.common.KlaxonWithConverters
import id.walt.credentials.w3c.JsonConverter
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.signatory.dataproviders.MergingDataProvider
import id.walt.signatory.revocation.simplestatus2022.SimpleCredentialStatus2022Service
import id.walt.signatory.revocation.TokenRevocationResult
import id.walt.signatory.revocation.statuslist2021.StatusListCredentialStorageService
import io.javalin.http.BadRequestResponse
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.HttpCode
import io.javalin.plugin.openapi.dsl.document
import kotlinx.serialization.json.jsonObject

data class IssueCredentialRequest(
    val templateId: String?,
    val config: ProofConfig,
    val credentialData: Map<String, Any>? = null
)

object SignatoryController {
    val signatory = Signatory.getService()

    fun listTemplates(ctx: Context) {
        ctx.contentType(ContentType.APPLICATION_JSON).result(KlaxonWithConverters().toJsonString(signatory.listTemplates()))
    }

    fun listTemplatesDocs() = document().operation {
        it.summary("List VC templates").operationId("listTemplates").addTagsItem("Verifiable Credentials")
    }.json<Array<String>>("200")

    fun loadTemplate(ctx: Context) {
        ctx.result(Signatory.getService().loadTemplate(ctx.pathParam("id")).encode())
    }

    fun loadTemplateDocs() = document().operation {
        it.summary("Load a VC template").operationId("loadTemplate").addTagsItem("Verifiable Credentials")
    }.pathParam<String>("id") { it.description("Retrieves a single VC template form the data store") }.json<String>("200")

    fun importTemplate(ctx: Context) {
        val templateId = ctx.pathParam("id")
        val template = ctx.body()
        try {
            Signatory.getService().importTemplate(templateId, template)
        } catch (exc: Exception) {
            throw BadRequestResponse("Error importing vc template: ${exc.message}")
        }
    }

    fun importTemplateDocs() = document().operation {
        it.summary("Import a VC template").operationId("importTemplate").addTagsItem("Verifiable Credentials")
    }.pathParam<String>("id").body<String>(contentType = ContentType.JSON).result<String>("200")

    fun removeTemplate(ctx: Context) {
        val templateId = ctx.pathParam("id")
        try {
            Signatory.getService().removeTemplate(templateId)
        } catch (exc: Exception) {
            throw BadRequestResponse("Error removing template: ${exc.message}")
        }
    }

    fun removeTemplateDocs() = document().operation {
        it.summary("Remove VC template").operationId("removeTemplate").addTagsItem("Verifiable Credentials")
    }.pathParam<String>("id").result<String>("200")

    fun issueCredential(ctx: Context) {
        val req = ctx.bodyAsClass<IssueCredentialRequest>()
        if (req.templateId != null && !signatory.hasTemplateId(req.templateId)) {
            throw BadRequestResponse("Template with supplied id does not exist.")
        }
        if (req.templateId == null && req.credentialData == null) {
            throw BadRequestResponse("At least templateId or credentialData (or both) must be provided")
        }

        ctx.result(
            if (req.templateId != null) {
                signatory.issue(
                    req.templateId,
                    req.config,
                    req.credentialData?.let { MergingDataProvider(req.credentialData) },
                    null,
                    false
                )
            } else {
                signatory.issue(
                    W3CCredentialBuilder.fromPartial(
                        VerifiableCredential.fromJsonObject(
                            JsonConverter.toJsonElement(
                                req.credentialData
                            ).jsonObject
                        )
                    ), req.config, null, false
                )
            }
        )
    }

    fun issueCredentialDocs() = document().operation {
        it.summary("Issue a credential").operationId("issue").addTagsItem("Credentials").description(
            "Based on a template (maintained in the VcLib), this call creates a W3C Verifiable Credential. Note that the '<b>templateId</b>, <b>issuerDid</b>, and the <b>subjectDid</b>, are mandatory parameters. All other parameters are optional. <br><br> This is a example request, that also demonstrates how to populate the credential with custom data: the <br><br>{<br>" + "  \"templateId\": \"VerifiableId\",<br>" + "  \"config\": {<br>" + " &nbsp;&nbsp;&nbsp;&nbsp;   \"issuerDid\": \"did:ebsi:zuathxHtXTV8psijTjtuZD7\",<br>" + " &nbsp;&nbsp;&nbsp;&nbsp;   \"subjectDid\": \"did:key:z6MkwfgBDSMRqXaJtw5DjhkJdDsDmRNSrvrM1L6UMBDtvaSX\"<br>" + " &nbsp;&nbsp;&nbsp;&nbsp; },<br>" + "  \"credentialData\": {<br>" + " &nbsp;&nbsp;&nbsp;&nbsp;   \"credentialSubject\": {<br>" + " &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;     \"firstName\": \"Severin\"<br>" + " &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;   }<br>" + " &nbsp;&nbsp;&nbsp;&nbsp; }<br>" + "}<br>"
        )
    }.body<IssueCredentialRequest>().json<String>("200")

    fun issueCredentialFromJson(ctx: Context) {
        val credentialJson = ctx.body()
        val issuerId = ctx.queryParam("issuerId") ?: throw BadRequestResponse("issuerId must be specified")
        val subjectId = ctx.queryParam("subjectId") ?: throw BadRequestResponse("subjectId must be specified")
        val proofType = ctx.queryParam("proofType")?.let { ProofType.valueOf(it) } ?: ProofType.LD_PROOF
        ctx.result(
            signatory.issue(
                W3CCredentialBuilder.fromPartial(credentialJson),
                ProofConfig(issuerId, subjectId, proofType = proofType)
            )
        )
    }

    fun issueCredentialFromJsonDocs() = document().operation {
        it.summary("Issue a credential from JSON data").operationId("issueCredentialFromJson").addTagsItem("Credentials")
            .description(
                "Based on JSON data, this call creates a W3C Verifiable Credential. Note that the '<b>issuerDid</b>, and the <b>subjectDid</b>, are mandatory parameters. All other parameters are optional."
            )
    }.queryParam<String>("issuerId")
        .queryParam<String>("subjectId")
        .queryParam<ProofType>("proofType")
        .body<String>().json<String>("200")

    fun checkRevokedDocs() = document().operation {
        it.summary("Check if credential is revoked").operationId("checkRevoked").addTagsItem("Revocations")
            .description("Based on a revocation-token, this method will check if this token is still valid or has already been revoked.")
    }.json<TokenRevocationResult>("200")

    fun checkRevoked(ctx: Context) {
        ctx.json(SimpleCredentialStatus2022Service.checkRevoked(ctx.pathParam("id")))
    }

    fun revokeDocs() = document().operation {
        it.summary("Revoke a credential").operationId("revoke").addTagsItem("Revocations")
            .description("Based on the <b>not-delegated</b> revocation-token, a credential with a specific delegated revocation-token can be revoked on this server.")
    }.result<String>("201")

    fun revoke(ctx: Context) {
        SimpleCredentialStatus2022Service.revokeToken(ctx.pathParam("id"))
        ctx.status(201)
    }

    fun statusDocs() = document().operation {
        it.summary("Get StatusList2021Credential").operationId("status").addTagsItem("Credentials")
            .description("Fetch the StatusList2021Credential based on id")
    }.json<VerifiableCredential>("200")

    fun status(ctx: Context) {
        StatusListCredentialStorageService.getService().fetch(ctx.pathParam("id"))?.let {
            ctx.json(it).status(HttpCode.OK)
        } ?: let {
            val error = mapOf("error" to "StatusList2021Credential not found for id: ${ctx.pathParam("id")}")
            ctx.json(error).status(HttpCode.NOT_FOUND)
        }
    }
}
