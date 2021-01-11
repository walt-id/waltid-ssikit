import java.util.*

object DidService {

    var kms = KeyManagementService

    fun registerDid(): String {
        val keyId = kms.generateEd25519KeyPair()
        var identifier = "did:key:" + kms.getBase58PublicKey(keyId)

        kms.addAlias(keyId, identifier)

        // TODO compute json did-key body
        var didBody = "{}"

        // TODO register did -> store to disk

        return identifier
    }

    fun registerDid(keyId: String): String {

        // TODO register DID
        var identifier = "did:dummy:" + UUID.randomUUID().toString().replace("-", "")

        // TODO compute json did-key body
        var didBody = "{}"
        kms.addAlias(keyId, identifier)

        return identifier
    }
}
