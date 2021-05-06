package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import kotlinx.serialization.Serializable
import org.letstrust.services.vc.CredentialService

@Serializable
data class CreateVcRequest(
    val issuerDid: String?,
    val subjectDid: String?,
    val credentialOffer: String?,
    val templateId: String? = null,
    val domain: String? = null,
    val nonce: String? = null,
)

@Serializable
data class PresentVcRequest(
    val vc: String,
    val domain: String?,
    val challenge: String?
)

@Serializable
data class VerifyVcRequest(
    val vcOrVp: String
)

object VcController {

    @OpenApi(
        summary = "Create VC based",
        operationId = "createVc",
        tags = ["Verifiable Credentials"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(CreateVcRequest::class)],
            true,
            "Defines the credential issuer, holder and optionally a credential template  -  TODO: build credential based on the request e.g. load template, substitute values"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "The signed credential"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun create(ctx: Context) {
        val createVcReq = ctx.bodyAsClass(CreateVcRequest::class.java)
        // TODO build credential based on the request e.g. load template, substitute values
        ctx.result(CredentialService.sign(createVcReq.issuerDid!!, createVcReq.credentialOffer!!, createVcReq.domain, createVcReq.nonce))
    }

    @OpenApi(
        summary = "Present VC",
        operationId = "presentVc",
        tags = ["Verifiable Credentials"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(PresentVcRequest::class)],
            true,
            "Defines the VC to be presented"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "The signed presentation"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun present(ctx: Context) {
        val presentVcReq = ctx.bodyAsClass(PresentVcRequest::class.java)
        ctx.result(CredentialService.present(presentVcReq.vc, presentVcReq.domain, presentVcReq.challenge))

    }

    @OpenApi(
        summary = "Verify VC",
        operationId = "verifyVc",
        tags = ["Verifiable Credentials"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(VerifyVcRequest::class)],
            true,
            "VC to be verified"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun verify(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "List VCs",
        operationId = "listVcs",
        tags = ["Verifiable Credentials"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<String>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun list(ctx: Context) {
        ctx.json(CredentialService.listVCs())
    }

}
