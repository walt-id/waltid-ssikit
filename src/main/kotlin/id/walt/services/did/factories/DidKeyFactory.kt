package id.walt.services.did.factories

import com.beust.klaxon.Klaxon
import id.walt.common.convertToRequiredMembersJsonString
import id.walt.crypto.*
import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.did.DidKey
import id.walt.services.did.DidKeyCreateOptions
import id.walt.services.did.DidOptions
import id.walt.services.did.composers.DidDocumentComposer
import id.walt.services.did.composers.models.DocumentComposerBaseParameter
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.erdtman.jcs.JsonCanonicalizer

class DidKeyFactory(
    private val keyService: KeyService,
    private val documentComposer: DidDocumentComposer<DidKey>,
) : DidFactory {
    override fun create(key: Key, options: DidOptions?): Did = let {
        if (key.algorithm !in setOf(
                KeyAlgorithm.EdDSA_Ed25519, KeyAlgorithm.RSA, KeyAlgorithm.ECDSA_Secp256k1, KeyAlgorithm.ECDSA_Secp256r1
            )
        ) throw IllegalArgumentException("did:key can not be created with an ${key.algorithm} key.")
        val identifierComponents = getIdentifierComponents(key, options as? DidKeyCreateOptions)
        val identifier = convertRawKeyToMultiBase58Btc(identifierComponents.pubKeyBytes, identifierComponents.multiCodecKeyCode)
        documentComposer.make(DocumentComposerBaseParameter(DidUrl.from("did:key:$identifier")))
    }

    private fun getIdentifierComponents(key: Key, options: DidKeyCreateOptions?): IdentifierComponents =
        options?.takeIf { it.useJwkJcsPub }?.let {
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
