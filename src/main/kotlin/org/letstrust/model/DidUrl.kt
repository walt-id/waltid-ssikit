package org.letstrust.model


// scheme did:, a method identifier, and a unique, method-specific identifier
// specified by the DID method. DIDs are resolvable to DID documents. A DID URL
// extends the syntax of a basic DID to incorporate other standard URI components
// such as path, query, and fragment in order to locate a particular resource
data class DidUrl(
    val method: String,
    val identifier: String,
    val fragment: String? = null,
// TODO: query-params are not supported yet
    val query: List<String>? = null
) {
    val did = "did:${method}:${identifier}"
    val url = did + if (fragment != null) "#${fragment}" else ""
}

fun String.fromString(): DidUrl {
    val matchResult = "^did:([a-z]+):(.+)".toRegex().find(this)!!
    var path = matchResult.groups[2]!!.value
    var fragmentStr = path.substringAfter('#')
    var identifierStr = path.substringBefore('#')
    return DidUrl(matchResult.groups[1]!!.value, identifierStr, fragmentStr)
}
