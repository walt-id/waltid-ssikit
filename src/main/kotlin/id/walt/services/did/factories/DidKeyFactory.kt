package id.walt.services.did.factories

import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.convertRawKeyToMultiBase58Btc
import id.walt.crypto.getMulticodecKeyCode
import id.walt.model.Did
import id.walt.services.did.DidOptions
import id.walt.services.did.DidService.resolve
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey

class DidKeyFactory : DidFactoryBase() {
    override fun create(key: Key, options: DidOptions?): Did = let {
        if (key.algorithm !in setOf(
                KeyAlgorithm.EdDSA_Ed25519, KeyAlgorithm.RSA, KeyAlgorithm.ECDSA_Secp256k1, KeyAlgorithm.ECDSA_Secp256r1
            )
        ) throw IllegalArgumentException("did:key can not be created with an ${key.algorithm} key.")

        val identifier = convertRawKeyToMultiBase58Btc(getPublicKeyBytesForDidKey(key), getMulticodecKeyCode(key.algorithm))
        resolve("did:key:$identifier")
    }

    private fun getPublicKeyBytesForDidKey(key: Key): ByteArray = when (key.algorithm) {
        KeyAlgorithm.ECDSA_Secp256k1, KeyAlgorithm.ECDSA_Secp256r1 -> (key.getPublicKey() as BCECPublicKey).q.getEncoded(
            true
        )

        KeyAlgorithm.RSA, KeyAlgorithm.EdDSA_Ed25519 -> key.getPublicKeyBytes()
    }
}
