package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.services.key.KeyManagementService

data class GenKeyRequest(
    val keyAlgorithm: KeyAlgorithm,
)

data class ImportKeyRequest(
    val keyId: String,
    val jwkKey: String,
)

data class ExportKeyRequest(
    val keyId: String,
    val keyAlgorithm: KeyAlgorithm,
)

object KeyController {

    @OpenApi(
        summary = "Generate key",
        operationId = "genKey",
        tags = ["Key Management"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(GenKeyRequest::class)],
            true,
            "the desired key algorithm and other parameters"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "Key ID"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun gen(ctx: Context) {
        ctx.json(KeyManagementService.generate(KeyAlgorithm.EdDSA_Ed25519).id)
    }

    @OpenApi(
        summary = "List of keyIds",
        operationId = "listKeys",
        tags = ["Key Management"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<String>::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun list(ctx: Context) {
        val keyIds = ArrayList<String>()
        KeyManagementService.listKeys().forEach { key -> keyIds.add(key.keyId.id) }
        ctx.json(keyIds)
    }

    @OpenApi(
        summary = "Import key",
        operationId = "importKey",
        tags = ["Key Management"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(ImportKeyRequest::class)],
            true,
            "Imports the key in JWK format"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun import(ctx: Context) {
        ctx.json("todo")
    }

    @OpenApi(
        summary = "Export key",
        operationId = "exportKey",
        tags = ["Key Management"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(ExportKeyRequest::class)],
            true,
            "Exports the key in JWK format"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun export(ctx: Context) {
        ctx.json(KeyManagementService.export(ctx.pathParam("keyAlias")))
    }
}
