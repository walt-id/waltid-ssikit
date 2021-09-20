package id.walt.rest

import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.custodian.CustodianService
import id.walt.vclib.model.VerifiableCredential
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse

object CustodianController {

    private val custodian = CustodianService.getService()

    /* Keys */

    data class GenerateKeyRequest(val keyAlgorithm: KeyAlgorithm)
    data class StoreKeyRequest(val key: Key)
    data class StoreCredentialRequest(val alias: String, val vc: VerifiableCredential)

    data class ListKeyResponse(val list: List<Key>)

    @OpenApi(
        summary = "Generates a key with a specific key algorithm", operationId = "generateKey", tags = ["Keys"],
        requestBody = OpenApiRequestBody([OpenApiContent(GenerateKeyRequest::class)], true, "Generate Key Request"),
        responses = [OpenApiResponse("200", [OpenApiContent(Key::class)], "Created Key")]
    )
    fun generateKey(ctx: Context) {
        ctx.json(custodian.generateKey(ctx.bodyAsClass<GenerateKeyRequest>().keyAlgorithm))
    }

    @OpenApi(
        summary = "Gets a key specified by its alias", operationId = "getKey", tags = ["Keys"],
        responses = [OpenApiResponse("200", [OpenApiContent(Key::class)], "Key by alias")]
    )
    fun getKey(ctx: Context) {
        ctx.json(custodian.getKey(ctx.pathParam("alias")))
    }

    @OpenApi(
        summary = "Lists all keys the custodian knows of", operationId = "listKeys", tags = ["Keys"],
        responses = [OpenApiResponse("200", [OpenApiContent(ListKeyResponse::class)], "List of Keys")]
    )
    fun listKeys(ctx: Context) {
        ctx.json(ListKeyResponse(custodian.listKeys()))
    }

    @OpenApi(
        summary = "Stores a key", operationId = "storeKey", tags = ["Keys"],
        requestBody = OpenApiRequestBody([OpenApiContent(StoreKeyRequest::class)], true, "Store Key Request"),
        responses = [OpenApiResponse("200")]
    )
    fun storeKey(ctx: Context) {
        custodian.storeKey(ctx.bodyAsClass<StoreKeyRequest>().key)
    }

    @OpenApi(
        summary = "Deletes a specific key", operationId = "deleteKey",
        tags = ["Keys"], responses = [OpenApiResponse("200")]
    )
    fun deleteKey(ctx: Context) {
        custodian.deleteKey(ctx.pathParam("id"))
    }


    /* Credentials */

    data class ListCredentialsResponse(val list: List<VerifiableCredential>)
    data class ListCredentialIdsResponse(val list: List<String>)

    @OpenApi(
        summary = "Gets a specific Credential by id", operationId = "getCredential", tags = ["Credentials"],
        responses = [OpenApiResponse("200", [OpenApiContent(VerifiableCredential::class)], "Created Credential")]
    )
    fun getCredential(ctx: Context) {
        ctx.json(custodian.getCredential(ctx.pathParam("id")))
    }

    @OpenApi(
        summary = "Lists all credentials the custodian knows of", operationId = "listCredentials", tags = ["Credentials"],
        responses = [OpenApiResponse("200", [OpenApiContent(ListCredentialsResponse::class)], "Credential list")]
    )
    fun listCredentials(ctx: Context) {
        ctx.json(ListCredentialsResponse(custodian.listCredentials()))
    }

    @OpenApi(
        summary = "Lists all credential ids the custodian knows of", operationId = "listCredentialIds", tags = ["Credentials"],
        responses = [OpenApiResponse("200", [OpenApiContent(ListCredentialIdsResponse::class)], "Credential id list")]
    )
    fun listCredentialIds(ctx: Context) {
        ctx.json(ListCredentialIdsResponse(custodian.listCredentialIds()))
    }

    @OpenApi(
        summary = "Lists all credential ids the custodian knows of", operationId = "listCredentialIds", tags = ["Credentials"],
        requestBody = OpenApiRequestBody([OpenApiContent(StoreCredentialRequest::class)], true, "Store Credential Request"),
        responses = [OpenApiResponse("200")]
    )
    fun storeCredential(ctx: Context) {
        ctx.bodyAsClass<StoreCredentialRequest>().run { custodian.storeCredential(alias, vc) }
    }

    @OpenApi(
        summary = "Deletes a specific credential by alias", operationId = "deleteCredential",
        tags = ["Credentials"], responses = [OpenApiResponse("200")]
    )
    fun deleteCredential(ctx: Context) {
        custodian.deleteCredential(ctx.pathParam("alias"))
    }

}
