package id.walt.rest

import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.model.Jwk
import id.walt.services.key.KeyFormat
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import kotlinx.serialization.Serializable

@Serializable
data class GenKeyRequest(
    val keyAlgorithm: KeyAlgorithm,
)

//@Serializable
//data class ImportKeyRequest(
//    val jwkKey: String,
//)

@Serializable
data class ExportKeyRequest(
    val keyAlias: String,
    val format: KeyFormat = KeyFormat.JWK,
    val exportPrivate: Boolean = false
)

object KeyController {

    private val keyService = KeyService.getService()

    @OpenApi(
        summary = "Generate key",
        operationId = "genKey",
        tags = ["Key Management"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(GenKeyRequest::class)],
            true,
            "The desired key algorithm (ECDSA_Secp256k1 or EdDSA_Ed25519)"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(KeyId::class)], "Key ID"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "Bad request"),
            OpenApiResponse("500", [OpenApiContent(ErrorResponse::class)], "Server Error"),
        ]
    )
    fun gen(ctx: Context) {
        val genKeyReq = ctx.bodyAsClass(GenKeyRequest::class.java)
        ctx.json(keyService.generate(genKeyReq.keyAlgorithm))
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
        ctx.json(
            keyService.export(
                ctx.pathParam("id"),
                exportKeyType = KeyType.PUBLIC
            )
        )
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
        println(ctx.body())
        ctx.json(keyService.delete(ctx.body()))
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
        ctx.result(
            keyService.export(
                req.keyAlias,
                req.format,
                if (req.exportPrivate) KeyType.PRIVATE else KeyType.PUBLIC
            )
        )
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
        keyService.listKeys().forEach { key -> keyIds.add(key.keyId.id) }
        ctx.json(keyIds)
    }

    @OpenApi(
        summary = "Import key",
        operationId = "importKey",
        tags = ["Key Management"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(String::class)],
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
        // val req = ctx.bodyAsClass(ImportKeyRequest::class.java)
        ctx.json(keyService.import(ctx.body()))
    }

}
