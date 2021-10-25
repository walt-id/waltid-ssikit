package id.walt.signatory.rest

import id.walt.signatory.ProofConfig
import id.walt.signatory.Signatory
import id.walt.vclib.Helpers.encode
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document

data class IssueCredentialRequest(val templateId: String, val config: ProofConfig)

object SignatoryController {
    val signatory = Signatory.getService()

    fun listTemplates(ctx: Context) {
        ctx.json(signatory.listTemplates())
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

    fun issueCredential(ctx: Context) {
        val req = ctx.bodyAsClass<IssueCredentialRequest>()
        if (!signatory.listTemplates().contains(req.templateId)) {
            ctx.status(404).result("Template with supplied id does not exist.")
            return
        }

        ctx.result(signatory.issue(req.templateId, req.config))
    }

    fun issueCredentialDocs() = document().operation {
        it.summary("Issue a credential").operationId("issue").addTagsItem("Credentials")
    }.body<IssueCredentialRequest>().json<String>("200")
}
