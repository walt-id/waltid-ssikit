package id.walt.services.did.factories

import com.beust.klaxon.Klaxon
import id.walt.common.convertToRequiredMembersJsonString
import id.walt.crypto.*
import id.walt.model.Did
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.crypto.CryptoService
import id.walt.services.did.DidKeyCreateOptions
import id.walt.services.did.DidOptions
import id.walt.services.did.DidService.resolve
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.erdtman.jcs.JsonCanonicalizer

class DidKeyFactory(
    private val keyService: KeyService,
) : DidFactoryBase() {
    override fun create(key: Key, options: DidOptions?): Did = let {
        if (key.algorithm !in setOf(
                KeyAlgorithm.EdDSA_Ed25519, KeyAlgorithm.RSA, KeyAlgorithm.ECDSA_Secp256k1, KeyAlgorithm.ECDSA_Secp256r1
            )
        ) throw IllegalArgumentException("did:key can not be created with an ${key.algorithm} key.")
        val identifierComponents = getIdentifierComponents(key, options as? DidKeyCreateOptions)
        val identifier = convertRawKeyToMultiBase58Btc(identifierComponents.pubKeyBytes, identifierComponents.multiCodecKeyCode)
        resolve("did:key:$identifier")
    }

    private fun getIdentifierComponents(key: Key, options: DidKeyCreateOptions?): IdentifierComponents =
        options?.takeIf { it.isJwk }?.let {
            IdentifierComponents(JwkJcsPubMultiCodecKeyCode, getJwkPubKeyRequiredMembersBytes(key))
        } ?: IdentifierComponents(getMulticodecKeyCode(key.algorithm), getPublicKeyBytesForDidKey(key))

    private fun getJwkPubKeyRequiredMembersBytes(key: Key) = JsonCanonicalizer(
        Klaxon().toJsonString(
            convertToRequiredMembersJsonString(
                keyService.toJwk(
                    key.keyId.id,
                    KeyType.PUBLIC
                )
            )
        )
    ).encodedUTF8

    private fun getPublicKeyBytesForDidKey(key: Key): ByteArray = when (key.algorithm) {
        KeyAlgorithm.ECDSA_Secp256k1, KeyAlgorithm.ECDSA_Secp256r1 -> (key.getPublicKey() as BCECPublicKey).q.getEncoded(
            true
        )

        KeyAlgorithm.RSA, KeyAlgorithm.EdDSA_Ed25519 -> key.getPublicKeyBytes()
    }

    data class IdentifierComponents(
        val multiCodecKeyCode: UInt,
        val pubKeyBytes: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as IdentifierComponents

            if (multiCodecKeyCode != other.multiCodecKeyCode) return false
            if (!pubKeyBytes.contentEquals(other.pubKeyBytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = multiCodecKeyCode.hashCode()
            result = 31 * result + pubKeyBytes.contentHashCode()
            return result
        }
    }
}

fun main(){
    ServiceMatrix("service-matrix.properties")
    val key = CryptoService.getService().generateKey(KeyAlgorithm.ECDSA_Secp256r1)
    val keyService = KeyService.getService()
    val did = DidKeyFactory(keyService).create(keyService.load(key.id), DidKeyCreateOptions(true))
    println("did: ${did.id}")
}
