package id.walt.signatory

import id.walt.vclib.Helpers.encode
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiParam
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import id.walt.rest.ErrorResponse
import io.javalin.plugin.openapi.dsl.document

object SignatoryController {
    fun listTemplates(ctx: Context) {
        ctx.json(Signatory.getService().listTemplates())
    }

    fun listTemplatesDocumentation() = document().operation {
        it.summary("List VC templates").operationId("listTemplates").addTagsItem("Verifiable Credentials")
    }.json<Array<String>>("200")

    fun loadTemplate(ctx: Context) {
        ctx.result(Signatory.getService().loadTemplate(ctx.pathParam("id")).encode())
    }

    fun loadTemplateDocumentation() = document().operation {
        it.summary("Loads a VC template").operationId("loadTemplate").addTagsItem("Verifiable Credentials")
    }.pathParam<String>("id") { it.description("Retrieves a single VC template form the data store") } .json<String>("200")
}
