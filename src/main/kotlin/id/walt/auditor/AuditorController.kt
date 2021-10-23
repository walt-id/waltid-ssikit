package id.walt.auditor

import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document
import org.apache.http.HttpStatus

object AuditorController {

    fun listPolicies(ctx: Context) {
        ctx.json(PolicyRegistry.listPolicies())
    }

    fun listPoliciesDocs() = document().operation {
        it.summary("List verification policies").operationId("listPolicies").addTagsItem("Verification Policies")
    }.json<Array<VerificationPolicy>>("200")

    fun verifyVP(ctx: Context) {

        val policyList = ctx.queryParams("policyList")?.let { it[0].split(",").map { it.trim() } }
        val policies = policyList.ifEmpty { listOf(PolicyRegistry.defaultPolicyId) }
        if (policies.any { !PolicyRegistry.contains(it) }) {
            ctx.status(HttpStatus.SC_BAD_REQUEST).result("Unknown policy given")
        } else {
            ctx.json(
                AuditorService.verify(ctx.body(), policies.map { PolicyRegistry.getPolicy(it) })
            )
        }
    }

    fun verifyVPDocs() = document()
        .operation { it.summary("Verify a W3C VerifiableCredential or VerifiablePresentation").operationId("verifyVP").addTagsItem("Verification Policies") }
        .body<String> { it.description("VC or VP to be verified") }
        .queryParam<String>("policyList") { it.description("Optional comma-separated list for setting the policies to be verified.") }
        .json<String>("200") { it.description("Request processed successfully (VP might not be valid)") }
}
