package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.letstrust.crypto.Key
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.model.DidMethod
import org.letstrust.services.did.DidService
import org.letstrust.services.key.KeyManagementService

data class CreateDidRequest(
    val method: DidMethod,
    val keyId: String?
)

data class ResolveDidRequest(
    val did: String
)

data class ListDidRequest(
    val keyId: String,
    val keyAlgorithm: KeyAlgorithm,
)

object DidController {

    @OpenApi(
        summary = "Create DID",
        operationId = "createDid",
        tags = ["Decentralized Identifiers"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(CreateDidRequest::class)],
            true,
            "the desired key algorithm and other parameters"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessResponse::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun create(ctx: Context) {
        DidService.create(DidMethod.key)
        ctx.json("todo")
    }

    @OpenApi(
        summary = "Resolve DID",
        operationId = "resolve DID",
        tags = ["Decentralized Identifiers"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(ResolveDidRequest::class)],
            true,
            "Resolve DID"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun resolve(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "List DIDs",
        operationId = "listDids",
        tags = ["Decentralized Identifiers"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<String>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun list(ctx: Context) {
        ctx.json(DidService.listDids())
    }

}
