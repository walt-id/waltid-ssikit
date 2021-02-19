package org.letstrust

import org.letstrust.model.*

object DidService {

    var kms = KeyManagementService

    fun resolveDid(did: String): Did? = resolveDid(did.fromString())

    fun resolveDid(didUrl: DidUrl): Did? {
        return when (didUrl.method) {
            "key" -> resolveDidKey(didUrl)
            "web" -> resolveDidWeb(didUrl)
            else -> TODO("did:${didUrl.method} implemented yet")
        }
    }

    private fun resolveDidKey(didUrl: DidUrl): Did? {
        val pubKey = convertEd25519PublicKeyFromMultibase58Btc(didUrl.identifier)
        return ed25519Did(didUrl, pubKey)
    }

    private fun resolveDidWeb(didUrl: DidUrl): Did {
        val keys = KeyManagementService.loadKeys(didUrl.did)!!
        return ed25519Did(didUrl, keys.getPubKey())
    }

    fun createDid(didMethod: String): String {
        return when (didMethod) {
            "key" -> createDidKey()
            "web" -> createDidWeb()
            else -> TODO("Did creation by method ${didMethod} not supported yet")
        }
    }

    internal fun createDidKey(): String {
        val keyId = KeyManagementService.generateKeyPair("Ed25519")
        val keys = KeyManagementService.loadKeys(keyId)!!

        val identifier = convertEd25519PublicKeyToMultiBase58Btc(keys.getPubKey())

        var did = "did:key:" + identifier

        KeyManagementService.addAlias(keyId, did)

        return did
    }

    internal fun createDidWeb(): String {
        val domain = "letstrust.org"
        val path = ":user:phil"
        val keyId = KeyManagementService.generateKeyPair("Ed25519")
        val didUrl = DidUrl("web", "" + domain + path, keyId)

        KeyManagementService.addAlias(keyId, didUrl.did)

        return didUrl.did
    }

    private fun ed25519Did(didUrl: DidUrl, pubKey: ByteArray): Did {

        val dhKey = convertPublicKeyEd25519ToCurve25519(pubKey)

        val dhKeyMb = convertX25519PublicKeyToMultiBase58Btc(dhKey)

        val pubKeyId = didUrl.identifier + "#" + didUrl.identifier
        val dhKeyId = didUrl.identifier + "#" + dhKeyMb

        val verificationMethods = listOf(
            VerificationMethod(pubKeyId, "Ed25519VerificationKey2018", didUrl.did, pubKey.encodeBase58()),
            VerificationMethod(dhKeyId, "X25519KeyAgreementKey2019", didUrl.did, dhKey.encodeBase58())
        )

        val keyRef = listOf(pubKeyId)

        return Did(DID_CONTEXT_URL, didUrl.did, verificationMethods, keyRef, keyRef, keyRef, keyRef, listOf(dhKeyId), null)
    }

}
