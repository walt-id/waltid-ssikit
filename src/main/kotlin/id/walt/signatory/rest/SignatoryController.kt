package id.walt.signatory.rest

import id.walt.common.KlaxonWithConverters
import id.walt.common.KotlinxJsonObjectField
import id.walt.credentials.w3c.JsonConverter
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.sdjwt.SDMap
import id.walt.sdjwt.SDMapBuilder
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.signatory.dataproviders.MergingDataProvider
import id.walt.signatory.revocation.CredentialStatusClientService
import id.walt.signatory.revocation.RevocationStatus
import id.walt.signatory.revocation.TokenRevocationStatus
import id.walt.signatory.revocation.simplestatus2022.SimpleCredentialStatus2022StorageService
import id.walt.signatory.revocation.statuslist2021.storage.StatusListCredentialStorageService
import io.javalin.http.BadRequestResponse
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.HttpCode
import io.javalin.plugin.openapi.dsl.document
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

data class IssueCredentialRequest(
    val templateId: String?,
    val config: ProofConfig,
    @KotlinxJsonObjectField val credentialData: JsonObject? = null
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
        val req = KlaxonWithConverters().parse<IssueCredentialRequest>(ctx.body()) ?: throw BadRequestResponse("Cannot parse IssueCredentialRequest body")
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
            "Based on a template (maintained in the VcLib), this call creates a W3C Verifiable Credential. Note that the '<b>templateId</b>, <b>issuerDid</b>, and the <b>subjectDid</b>, are mandatory parameters. All other parameters are optional. <br><br> This is a example request, that also demonstrates how to populate the credential with custom data: the <br><br>{<br>" + "  \"templateId\": \"VerifiableId\",<br>" + "  \"config\": {<br>" + " &nbsp;&nbsp;&nbsp;&nbsp;   \"issuerDid\": \"did:ebsi:zuathxHtXTV8psijTjtuZD7\",<br>" + " &nbsp;&nbsp;&nbsp;&nbsp;   \"subjectDid\": \"did:key:z6MkwfgBDSMRqXaJtw5DjhkJdDsDmRNSrvrM1L6UMBDtvaSX\",<br>" + " &nbsp;&nbsp;&nbsp;&nbsp; \"selectiveDisclosure\": \n" +
                    SDMapBuilder().addField("credentialSubject", false, SDMapBuilder().addField("firstName", true).build()).build().toJSON().toString() + "\n" +
                    "<br>},<br>" + "  \"credentialData\": {<br>" + " &nbsp;&nbsp;&nbsp;&nbsp;   \"credentialSubject\": {<br>" + " &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;     \"firstName\": \"Severin\"<br>" + " &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;   }<br>" + " &nbsp;&nbsp;&nbsp;&nbsp; }<br>" + "}<br>"
        )
    }.body<String>()
    { it.description("IssueCredentialRequest: templateId: String, config: ProofConfig, credentialData: JsonObject") }.json<String>("200")

    fun issueCredentialFromJson(ctx: Context) {
        val credentialJson = ctx.body()
        val issuerId = ctx.queryParam("issuerId") ?: throw BadRequestResponse("issuerId must be specified")
        val subjectId = ctx.queryParam("subjectId") ?: throw BadRequestResponse("subjectId must be specified")
        val proofType = ctx.queryParam("proofType")?.let { ProofType.valueOf(it) } ?: ProofType.LD_PROOF
        val sdPaths = ctx.queryParams("sd")
        val sdMap = SDMap.generateSDMap(sdPaths)
        ctx.result(
            signatory.issue(
                W3CCredentialBuilder.fromPartial(credentialJson),
                ProofConfig(issuerId, subjectId, proofType = proofType, selectiveDisclosure = sdMap)
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
        .queryParam<String>("sd", isRepeatable = true)
        .body<String>().json<String>("200")

    fun statusDocs() = document().operation {
        it.summary("Get StatusList2021Credential").operationId("status").addTagsItem("Credentials")
            .description("Fetch the StatusList2021Credential based on id")
    }.json<String>("200")

    fun status(ctx: Context) {
        StatusListCredentialStorageService.getService().fetch(ctx.pathParam("id"))?.let {
            ctx.result(it.toJson()).status(HttpCode.OK)
        } ?: let {
            val error = mapOf("error" to "StatusList2021Credential not found for id: ${ctx.pathParam("id")}")
            ctx.json(error).status(HttpCode.NOT_FOUND)
        }
    }

    fun tokenDocs() = document().operation {
        it.summary("Get the credential's specific delegated revocation-token that can be revoked on this server.")
            .operationId("token").addTagsItem("Credentials")
            .description("Get the revocation token based on id")
    }.json<TokenRevocationStatus>("200")

    fun token(ctx: Context) {
        SimpleCredentialStatus2022StorageService.checkRevoked(ctx.pathParam("id")).let {
            ctx.json(it).status(HttpCode.OK)
        }
    }

    fun checkRevokedDocs() = document().operation {
        it.summary("Check if credential is revoked").operationId("checkRevoked").addTagsItem("Revocations")
            .description("The revocation status is checked based on credential's credential-status property.")
    }.body<String> {
        it.description("Verifiable credential to be checked for revocation status.")
    }.json<RevocationStatus>("200")

    fun checkRevoked(ctx: Context) = runCatching {
        CredentialStatusClientService.check(ctx.body().toVerifiableCredential())
    }.onSuccess {
        ctx.json(it)
    }.onFailure {
        ctx.json(it.localizedMessage)
    }

    fun revokeDocs() = document().operation {
        it.summary("Revoke a credential").operationId("revoke").addTagsItem("Revocations")
            .description("The credential will be revoked based on its credential-status property on the current server.")
    }.body<String> {
        it.description("Verifiable credential to be revoked.")
    }.json<String>("201")

    fun revoke(ctx: Context) = runCatching {
        CredentialStatusClientService.revoke(ctx.body().toVerifiableCredential())
    }.onSuccess {
        ctx.status(if (it.succeed) HttpCode.OK else HttpCode.NOT_FOUND).json(it.message)
    }.onFailure { ctx.status(HttpCode.NOT_FOUND).json(it.localizedMessage) }
}
