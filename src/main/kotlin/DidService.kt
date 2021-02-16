import model.*
import java.util.*


object DidService {

    var kms = KeyManagementService


    fun resolveDid(id: String): Did? {

        val didUrl: DidUrl = id.fromString()

        if ("key" == didUrl.method) {
            return resolveDidKey(didUrl)
        }

        return null
    }

    fun resolveDidKey(didUrl: DidUrl): Did? {

        val pubKey = ed25519PublicKeyFromMultibase58Btc(didUrl.identifier)

        val pubKey58 = pubKey.encodeBase58()

        val dhKey = convertPublicKeyEd25519ToCurve25519(pubKey)

        val dhKey58 = dhKey.encodeBase58()

        val dhKeyMb = x25519PublicKeyToMultiBase58Btc(dhKey)

        val keyId = didUrl.identifier + "#" + didUrl.identifier
        val dhKeyId = didUrl.identifier + "#" + dhKeyMb

        val verificationMethods = listOf(
            VerificationMethod(keyId, "Ed25519VerificationKey2018", didUrl.did, pubKey58),
            VerificationMethod(dhKeyId, "X25519KeyAgreementKey2019", didUrl.did, dhKey58)
        )

        val keyRef = listOf(keyId)

        return Did(DID_CONTEXT_URL, didUrl.did, verificationMethods, keyRef, keyRef, keyRef, keyRef, listOf(dhKeyId), null)
    }


    //TODO cleanup:


    fun createDidWeb(): DidWeb {

        val domain = "letstrust.org"
        val path = ":user:phil"
        val keyId = kms.generateKeyPair("Ed25519")
        val didUrl = DidUrl("web", "" + domain + path, keyId)
        val onwerDid = didUrl.did
        val keyDid = didUrl.url

        kms.addAlias(keyId, keyDid)
        val publicKeyBase58 = kms.getBase58PublicKey(keyDid)

        val keyRef = listOf(keyDid)

        val pubKey = DidWeb.PublicKey(keyDid, "Ed25519VerificationKey2018", onwerDid, publicKeyBase58)

        // TODO generate key-agreement key
//        val keyAgreement = DidWeb.KeyAgreement(
//            "did:web:did.actor:alice#zC8GybikEfyNaausDA4mkT4egP7SNLx2T1d1kujLQbcP6h",
//            "X25519KeyAgreementKey2019",
//            "Ed25519VerificationKey2018",
//            "CaSHXEvLKS6SfN9aBfkVGBpp15jSnaHazqHgLHp8KZ3Y"
//        )

        return DidWeb("https://w3id.org/did/v0.11", onwerDid, listOf(pubKey), null, keyRef, keyRef, keyRef, keyRef)

    }


    fun registerDid(): String {
        val keyId = kms.generateKeyPair("Ed25519")
        // should be fingerprintFromPublicKey
        var identifier = "did:key:z" + kms.getMultiBase58PublicKey(keyId)

        kms.addAlias(keyId, identifier)

        // TODO compute json did-key body
        var didBody = "{}"

        // TODO register did -> store to disk

        print("DID: " + identifier)

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


    fun createDidKey() {
        val keyId = kms.generateKeyPair("Ed25519")
        var identifier = "did:key:" + kms.getMultiBase58PublicKey(keyId)
        kms.addAlias(keyId, identifier)
    }


}
