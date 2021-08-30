package id.walt.auditor

import id.walt.rest.ErrorResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.apache.http.HttpStatus

object AuditorController {
    @OpenApi(
        summary = "List verification policies",
        operationId = "listPolicies",
        tags = ["Verification Policies"],
        responses = [
            OpenApiResponse(
                "200",
                content = [OpenApiContent(from = VerificationPolicy::class, isArray = true, type = "application/json")]
            ),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun listPolicies(ctx: Context) {
        ctx.json(PolicyRegistry.listPolicies())
    }

    @OpenApi(
        summary = "Verify a W3C VerifiablePresentation",
        operationId = "verifyVP",
        tags = ["Verification"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "VP to be verified"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "Request processed successfully (VP might not be valid)"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun verifyVP(ctx: Context) {
        val policies = ctx.queryParams("policy").ifEmpty { listOf(PolicyRegistry.defaultPolicyId) }
        if (policies.any { !PolicyRegistry.contains(it) }) {
            ctx.status(HttpStatus.SC_BAD_REQUEST).result("Unknown policy given")
        } else {
            ctx.json(
                AuditorService.verify(ctx.body(), policies.map { PolicyRegistry.getPolicy(it) })
            )
        }
    }
}
