package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import kotlinx.serialization.Serializable
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.crypto.KeyId
import org.letstrust.services.key.KeyFormat
import org.letstrust.services.key.KeyService

@Serializable
data class GenKeyRequest(
    val keyAlgorithm: KeyAlgorithm,
)

@Serializable
data class ImportKeyRequest(
    val keyId: String,
    val jwkKey: String,
)

@Serializable
data class ExportKeyRequest(
    val keyAlias: String,
    val format: KeyFormat = KeyFormat.JWK,
)

object KeyController {

    @OpenApi(
        summary = "Generate key",
        operationId = "genKey",
        tags = ["Key Management"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(GenKeyRequest::class)],
            true,
            "The desired key algorithm and other parameters"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(KeyId::class)], "Key ID"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun gen(ctx: Context) {
        val genKeyReq = ctx.bodyAsClass(GenKeyRequest::class.java)
        ctx.json(KeyService.generate(genKeyReq.keyAlgorithm))
    }

    @OpenApi(
        summary = "Load public key",
        operationId = "loadKey",
        tags = ["Key Management"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun load(ctx: Context) {
        ctx.json(KeyService.export(ctx.pathParam("id")))
    }

    @OpenApi(
        summary = "Delete key",
        operationId = "deleteKey",
        tags = ["Key Management"],
        //pathParams = [OpenApiParam("keyId", String::class, "The key ID")],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
            true,
            "ID of key to be deleted"
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
        summary = "Exports public and private key part (if supported by underlying keystore)",
        operationId = "exportKey",
        tags = ["Key Management"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(ExportKeyRequest::class)],
            true,
            "Exports the key in JWK or PEM format"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "The key in the desired formant"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun export(ctx: Context) {
        val req = ctx.bodyAsClass(ExportKeyRequest::class.java)
        ctx.result(KeyService.export(req.keyAlias, req.format))
    }

    @OpenApi(
        summary = "List of key IDs",
        operationId = "listKeys",
        tags = ["Key Management"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<String>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun list(ctx: Context) {
        val keyIds = ArrayList<String>()
        KeyService.listKeys().forEach { key -> keyIds.add(key.keyId.id) }
        ctx.json(keyIds)
    }

    @OpenApi(
        summary = "Import key",
        operationId = "importKey",
        tags = ["Key Management"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(ImportKeyRequest::class)],
            true,
            "Imports the key (JWK format) to the key store"
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
