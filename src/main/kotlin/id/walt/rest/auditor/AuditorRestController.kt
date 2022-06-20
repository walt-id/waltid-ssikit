package id.walt.rest.auditor

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import id.walt.auditor.*
import id.walt.auditor.dynamic.DynamicPolicy
import id.walt.auditor.dynamic.DynamicPolicyArg
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.nestedVCsConverter
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.HttpCode
import io.javalin.plugin.openapi.dsl.document
import org.apache.http.HttpStatus

object AuditorRestController {

    fun listPolicies(ctx: Context) {
        ctx.json(PolicyRegistry.listPolicyInfo())
    }

    fun listPoliciesDocs() = document().operation {
        it.summary("List verification policies").operationId("listPolicies").addTagsItem("Verification Policies")
    }.json<Array<VerificationPolicy>>("200")

    fun verifyVP(ctx: Context) {
        val verificationRequest = VerifiableCredential.klaxon.parse<VerificationRequest>(ctx.body()) ?: throw BadRequestResponse("Could not parse verification request object")

        val policies = verificationRequest.policies.map { pol -> PolicyRegistry.getPolicyWithJsonArg(pol.policy, pol.argument?.let { JsonObject(pol.argument) }) }
        val results = verificationRequest.credentials.map { cred ->
            Auditor.getService().verify(cred, policies)
        }

        ctx.json(VerificationResponse(valid = results.all { it.valid }, results = results))
    }

    fun verifyVPDocs() = document()
        .operation { it.summary("Verify a W3C VerifiableCredential or VerifiablePresentation").operationId("verifyVP").addTagsItem("Verification Policies") }
        .body<VerificationRequest> { it.description("VC or VP verification request object") }
        .jsonArray<VerificationResponse>("200") { it.description("Request processed successfully (VP might not be valid)") }

    fun createDynamicPolicy(ctx: Context) {
        val dynArg = Klaxon().parse<DynamicPolicyArg>(ctx.body()) ?: throw BadRequestResponse("Could not parse dynamic policy argument")
        val name = ctx.pathParam("name")
        val update = ctx.queryParam("update")?.toBoolean() ?: false
        val download = ctx.queryParam("downloadPolicy")?.toBoolean() ?: false
        val success = PolicyRegistry.createSavedPolicy(name, dynArg, update, download)
        if(!success)
            ctx.status(HttpCode.BAD_REQUEST).result("Failed to create dynamic policy")
    }

    fun createDynamicPolicyDocs() = document().operation {
        it.summary("Create dynamic verification policy").operationId("createDynamicPolicy").addTagsItem("Verification Policies")
    }
        .pathParam<String>("name")
        .queryParam<Boolean>("update")
        .queryParam<Boolean>("downloadPolicy")
        .body<DynamicPolicyArg>()
        .json<DynamicPolicyArg>("200")

    fun deleteDynamicPolicy(ctx: Context) {
        val name = ctx.pathParam("name")
        if(!PolicyRegistry.contains(name)) {
            ctx.status(HttpCode.NOT_FOUND).result("Policy not found")
        } else if(!PolicyRegistry.isMutable(name)) {
            ctx.status(HttpCode.FORBIDDEN).result("Policy cannot be removed")
        } else {
            if (!PolicyRegistry.deleteSavedPolicy(name)) {
                ctx.status(HttpCode.INTERNAL_SERVER_ERROR).result("Failed to remove policy")
            } else {
                ctx.result("Policy removed")
            }
        }
    }

    fun deleteDynamicPolicyDocs() = document().operation {
        it.summary("Delete a dynamic verification policy").operationId("deletePolicy").addTagsItem("Verification Policies")
    }.pathParam<String>("name")
}
