package id.walt.rest.auditor

import id.walt.auditor.PolicyRegistry
import id.walt.auditor.VerificationPolicy
import id.walt.auditor.VerificationResult
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document
import org.apache.http.HttpStatus

object AuditorRestController {

    fun listPolicies(ctx: Context) {
        ctx.json(PolicyRegistry.listPolicies())
    }

    fun listPoliciesDocs() = document().operation {
        it.summary("List verification policies").operationId("listPolicies").addTagsItem("Verification Policies")
    }.json<Array<VerificationPolicy>>("200")

    fun verifyVP(ctx: Context) {
        when (val res = AuditorRestService.verifyVP(ctx.queryParams("policyList"), ctx.body())) {
            null -> ctx.status(HttpStatus.SC_BAD_REQUEST).result("Unknown policy given")
            else -> ctx.json(res)
        }
    }

    fun verifyVPDocs() = document()
        .operation { it.summary("Verify a W3C VerifiableCredential or VerifiablePresentation").operationId("verifyVP").addTagsItem("Verification Policies") }
        .body<String> { it.description("VC or VP to be verified") }
        .queryParam<String>("policyList") { it.description("Optional comma-separated list for setting the policies to be verified.") }
        .json<VerificationResult>("200") { it.description("Request processed successfully (VP might not be valid)") }
}
