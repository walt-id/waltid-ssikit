package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.letstrust.services.vc.CredentialService


data class CreateVcRequest(
    val subjectDid: String,
    val issuerDid: String?
)

data class PresentVcRequest(
    val vcId: String
)

data class VerifyVcRequest(
    val vcOrVp: String
)

object VcController {

    @OpenApi(
        summary = "Create VC",
        operationId = "createVc",
        tags = ["vc"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(CreateVcRequest::class)],
            true,
            "Create a Verifiable Credential"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessResponse::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun create(ctx: Context) {
        CredentialService.sign("issuer", "", "", "")
        ctx.json("todo")
    }

    @OpenApi(
        summary = "Present VC",
        operationId = "presentVc",
        tags = ["vc"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(PresentVcRequest::class)],
            true,
            "Resolve DID"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun present(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "Verify VC",
        operationId = "verifyVc",
        tags = ["vc"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(VerifyVcRequest::class)],
            true,
            "Resolve DID"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun verify(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "List VCs",
        operationId = "listVcs",
        tags = ["vc"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<String>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun list(ctx: Context) {
        ctx.json(CredentialService.listVCs())
    }

}
