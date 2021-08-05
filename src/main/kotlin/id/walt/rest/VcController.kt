package id.walt.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import kotlinx.serialization.Serializable
import id.walt.services.vc.VCService
import id.walt.services.vc.VerificationResult

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

    private val credentialService = VCService.getService()

    @OpenApi(
        summary = "Load VC",
        operationId = "loadVc",
        tags = ["Verifiable Credentials"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "ID of the DID to be loaded"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun load(ctx: Context) {
        ctx.json("todo - load")
    }

    @OpenApi(
        summary = "Delete VC",
        operationId = "deleteVc",
        tags = ["Verifiable Credentials"],
        //pathParams = [OpenApiParam("keyId", String::class, "The key ID")],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "ID of VC to be deleted"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun delete(ctx: Context) {
        ctx.json("todo - delete")
    }

    @OpenApi(
        summary = "Create VC",
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
        ctx.result(
            credentialService.sign(
                createVcReq.issuerDid!!,
                createVcReq.credentialOffer!!,
                createVcReq.domain,
                createVcReq.nonce
            )
        )
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
        ctx.result(credentialService.present(presentVcReq.vc, presentVcReq.domain, presentVcReq.challenge))

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
            OpenApiResponse("200", [OpenApiContent(VerificationResult::class)], "Verification result object"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun verify(ctx: Context) {
        val verifyVcReq = ctx.bodyAsClass(VerifyVcRequest::class.java)
        ctx.json(credentialService.verify(verifyVcReq.vcOrVp))
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
        ctx.json(credentialService.listVCs())
    }

    @OpenApi(
        summary = "Import VC",
        operationId = "importVc",
        tags = ["Verifiable Credentials"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "Imports the DID to the underlying data store"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun import(ctx: Context) {
        ctx.json("todo - import")
    }

}
