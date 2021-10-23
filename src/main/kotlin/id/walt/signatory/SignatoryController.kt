package id.walt.signatory

import id.walt.vclib.Helpers.encode
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document

data class IssueCredentialRequest(val templateId: String, val config: ProofConfig)

object SignatoryController {
    fun listTemplates(ctx: Context) {
        ctx.json(Signatory.getService().listTemplates())
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

    val signatory = Signatory.getService()

    fun issueCredential(ctx: Context) {
        val req = ctx.bodyAsClass<IssueCredentialRequest>()

        ctx.result(signatory.issue(req.templateId, req.config))
    }

    fun issueCredentialDocs() = document().operation {
        it.summary("Issue a credential").operationId("issue").addTagsItem("Credentials")
    }.body<IssueCredentialRequest>().json<String>("200")
}
