package id.walt.services.ecosystems.cheqd


import com.fasterxml.jackson.annotation.JsonProperty
import id.walt.model.did.DidCheqd

internal data class DidCheqdResolutionResponse(
    @JsonProperty("@context")
    var context: String? = null, // https://w3id.org/did-resolution/v1
    var didDocument: DidCheqd?,
    var didDocumentMetadata: DidDocumentMetadata,
    var didResolutionMetadata: DidResolutionMetadata
) {

    internal data class DidDocumentMetadata(
        var created: String? = null, // 2022-04-05T11:49:19Z
        var versionId: String? = null // EDEAD35C83E20A72872ACD3C36B7BA42300712FC8E3EEE1340E47E2F1B216B2D
    )

    internal data class DidResolutionMetadata(
        var contentType: String, // application/did+ld+json
        var did: Did,
        var error: String? = null,
        var retrieved: String // 2022-11-14T09:11:15Z
    ) {
        internal data class Did(
            var didString: String?, // did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY
            var method: String?, // cheqd
            var methodSpecificId: String? // zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY
        )
    }
}
