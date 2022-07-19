package id.walt.rest.core

import id.walt.common.prettyPrint
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.services.did.DidService
import io.javalin.http.Context
import io.javalin.plugin.openapi.dsl.document
import kotlinx.serialization.Serializable

@Serializable
data class CreateDidRequest(
    val method: DidMethod,
    val keyAlias: String? = null,
    val didWebDomain: String? = null,
    val didWebPath: String? = null,

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
        val req = ctx.bodyAsClass(CreateDidRequest::class.java)

        ctx.result(
            DidService.create(
                req.method,
                req.keyAlias,
                DidService.DidWebOptions(req.didWebDomain ?: "walt.id", req.didWebPath)
            )
        )
    }

    fun createDocs() = document().operation {
        it.summary("Create DID").operationId("createDid").addTagsItem("Decentralized Identifiers")
    }.body<CreateDidRequest> { it.description("Defines the DID method and optionally the key to be used") }
        .json<String>("200") { it.description("DID document of the resolved DID") }

    fun resolve(ctx: Context) {
        val did = ctx.bodyAsClass(ResolveDidRequest::class.java).did
        when {
            did.contains("ebsi") -> ctx.result(DidService.resolveDidEbsiRaw(did).prettyPrint())
            else -> ctx.json(DidService.resolve(did))
        }
    }

    fun resolveDocs() = document().operation {
        it.summary("Resolve DID").operationId("resolveDid").addTagsItem("Decentralized Identifiers")
    }.body<ResolveDidRequest> { it.description("Identifier to be resolved") }
        .json<String>("200") { it.description("DID document of the resolved DID") }

    fun import(ctx: Context) {
        DidService.importDidAndKey(ctx.body())
        ctx.status(201)
    }

    fun importDocs() = document().operation {
        it.summary("Import DID").operationId("importDid").addTagsItem("Decentralized Identifiers")
    }.body<String> { it.description("Resolves and imports a DID (e.g. did:key:z6MkiFniw3DEmvQ1AmF818vtFirrY1eJeYxtSoGCaGeqP5Mu) to the underlying data store") }
        .json<String>("201")
}
