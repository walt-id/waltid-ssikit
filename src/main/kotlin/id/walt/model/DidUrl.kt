package id.walt.model

import id.walt.crypto.encodeBase58
import kotlin.random.Random


/**
scheme did:, a method identifier, and a unique, method-specific identifier
specified by the DID method. DIDs are resolvable to DID documents. A DID URL
extends the syntax of a basic DID to incorporate other standard URI components
such as path, query, and fragment in order to locate a particular resource
 */
data class DidUrl(
    val method: String,
    val identifier: String,
    val fragment: String? = null,
    val query: List<String>? = null // TODO: query-params are not supported yet
) {
    val did = "did:${method}:${identifier}"
    val url = did + if (fragment != null) "#${fragment}" else ""

    companion object {
        fun from(url: String): DidUrl {
            val didUrl = try {
                val matchResult = "^did:([a-z]+):(.+)".toRegex().find(url)!!
                val path = matchResult.groups[2]!!.value
                val fragmentStr = path.substringAfter('#')
                val identifierStr = path.substringBefore('#')
                return DidUrl(matchResult.groups[1]!!.value, identifierStr, fragmentStr)
            } catch (e: Exception) {
                throw IllegalArgumentException("DID has wrong format.")
            }
        }

        fun generateDidEbsiV2DidUrl() =
            DidUrl(DidMethod.ebsi.name, "2" + Random.nextBytes(47).encodeBase58().substring(0, 47))
    }
}





