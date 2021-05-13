package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import kotlinx.serialization.Serializable
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.model.DidMethod
import org.letstrust.services.did.DidService

@Serializable
data class CreateDidRequest(
    val method: DidMethod,
    val keyAlias: String? = null
)

@Serializable
data class ResolveDidRequest(
    val did: String
)

@Serializable
data class ListDidRequest(
    val keyId: String,
    val keyAlgorithm: KeyAlgorithm,
)

object DidController {


    @OpenApi(
        summary = "List DIDs",
        operationId = "listDids",
        tags = ["Decentralized Identifiers"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<String>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun list(ctx: Context) {
        ctx.json(DidService.listDids())
    }

    @OpenApi(
        summary = "Load DID",
        operationId = "loadDid",
        tags = ["Decentralized Identifiers"],
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
        ctx.json("todo")
    }

    @OpenApi(
        summary = "Delete DID",
        operationId = "deleteDid",
        tags = ["Decentralized Identifiers"],
        //pathParams = [OpenApiParam("keyId", String::class, "The key ID")],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "ID of DID to be deleted"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun delete(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "Create DID",
        operationId = "createDid",
        tags = ["Decentralized Identifiers"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(CreateDidRequest::class)],
            true,
            "Defines the DID method and optionally the key to be used"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "Identifier of the created DID"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun create(ctx: Context) {
        val createDidReq = ctx.bodyAsClass(CreateDidRequest::class.java)

        if (createDidReq.method.equals(DidMethod.ebsi)) {
            throw IllegalArgumentException("DID method EBSI not supported")
        } else {
            ctx.result(DidService.create(createDidReq.method, createDidReq.keyAlias))
        }
    }

    @OpenApi(
        summary = "Resolve DID",
        operationId = "resolve DID",
        tags = ["Decentralized Identifiers"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(ResolveDidRequest::class)],
            true,
            "Identifier to be resolved"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "DID document of the resolved DID"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun resolve(ctx: Context) {
        ctx.json(DidService.resolve(ctx.bodyAsClass(ResolveDidRequest::class.java).did))
    }


    @OpenApi(
        summary = "Import DID",
        operationId = "importDid",
        tags = ["Decentralized Identifiers"],
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
        ctx.json("todo")
    }


}
