package id.walt.rest.core

import id.walt.common.KlaxonWithConverters
import id.walt.common.prettyPrint
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.rest.core.requests.did.*
import id.walt.services.did.*
import io.javalin.http.Context
import io.javalin.http.HttpCode
import io.javalin.plugin.openapi.dsl.document
import kotlinx.serialization.Serializable

//@Serializable
//data class CreateDidRequest(
//    val method: DidMethod,
//    val keyAlias: String? = null,
//    val didWebDomain: String? = null,
//    val didWebPath: String? = null,
//)

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

    fun list(ctx: Context) {
        ctx.json(DidService.listDids())
    }

    fun listDocs() = document().operation {
        it.summary("List DIDs").operationId("listDids").addTagsItem("Decentralized Identifiers")
    }.json<Array<String>>("200")

    fun load(ctx: Context) {
        val resolved = DidService.loadOrResolveAnyDid(ctx.pathParam("id"))

        ctx.status(
            if (resolved != null) {
                ctx.json(resolved)
                200
            } else 404
        )
    }

    fun loadDocs() = document().operation {
        it.summary("Load DID").operationId("loadDid").addTagsItem("Decentralized Identifiers")
    }.json<String>("200")

    fun delete(ctx: Context) {
        DidService.deleteDid(ctx.pathParam("id"))
    }

    fun deleteDocs() = document().operation {
        it.summary("Delete DID by url").operationId("deleteDid").addTagsItem("Decentralized Identifiers")
    }.json<String>("200") { it.description("Http OK") }

    fun create(ctx: Context) {
        runCatching {
            KlaxonWithConverters().parse<CreateDidRequest>(ctx.bodyAsInputStream())!!
        }.onSuccess {
            ctx.status(HttpCode.CREATED)
            ctx.result(DidService.create(DidMethod.valueOf(it.method), it.keyAlias, getOptions(it)))
        }.onFailure {
            ctx.status(HttpCode.BAD_REQUEST)
            ctx.result(it.localizedMessage)
        }
    }

    private fun getOptions(request: CreateDidRequest) = when (request) {
        is WebCreateDidRequest -> DidWebCreateOptions(request.domain ?: "walt.id", request.path)
        is EbsiCreateDidRequest -> DidEbsiCreateOptions(request.version)
        is CheqdCreateDidRequest -> DidCheqdCreateOptions(request.network)
        is KeyCreateDidRequest -> DidKeyCreateOptions(request.useJwkJcsPub)
        else -> null
    }

    fun createDocs() = document().operation {
        it.summary("Create DID").operationId("createDid").addTagsItem("Decentralized Identifiers")
    }.body<CreateDidRequest> { it.description("Defines the DID method and optionally the key to be used") }
        .json<String>("200") { it.description("DID document of the resolved DID") }

    fun resolve(ctx: Context) {
        val did = ctx.bodyAsClass(ResolveDidRequest::class.java).did
        when {
            did.contains("ebsi") -> ctx.result(DidService.resolve(did, DidEbsiResolveOptions(true)).prettyPrint())
            else -> ctx.json(DidService.resolve(did))
        }
    }

    fun resolveDocs() = document().operation {
        it.summary("Resolve DID").operationId("resolveDid").addTagsItem("Decentralized Identifiers")
    }.body<ResolveDidRequest> { it.description("Identifier to be resolved") }
        .json<String>("200") { it.description("DID document of the resolved DID") }

    fun import(ctx: Context) {
        val did = ctx.body()
        DidService.importDid(did)

        val keyId = ctx.queryParam("keyId")

        when {
            !keyId.isNullOrEmpty() -> DidService.setKeyIdForDid(did, keyId)
            else -> DidService.importKeys(did)
        }

        ctx.status(201)
    }

    fun importDocs() = document().operation {
        it.summary("Import DID").operationId("importDid").addTagsItem("Decentralized Identifiers")
    }
        .body<String> { it.description("Resolves and imports a DID (e.g. did:key:z6MkiFniw3DEmvQ1AmF818vtFirrY1eJeYxtSoGCaGeqP5Mu) to the underlying data store") }
        .queryParam<String>("keyId") { it.allowEmptyValue(true) }
        .json<String>("201")
}
