package id.walt.services.did.composers

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.JWK
import id.walt.crypto.*
import id.walt.model.*
import id.walt.model.did.DidKey
import id.walt.services.CryptoProvider
import id.walt.services.did.composers.models.DocumentComposerBaseParameter
import id.walt.services.key.KeyService
import java.security.KeyPair

class DidKeyDocumentComposer(
    private val keyService: KeyService,
) : DidDocumentComposerBase<DidKey>() {

    override fun make(parameter: DocumentComposerBaseParameter): DidKey = parameter.didUrl.let {
        val pubKey = convertMultiBase58BtcToRawKey(it.identifier)
        val keyCode = getMultiCodecKeyCode(it.identifier)
        constructDidKey(it, pubKey, keyCode)
    }

    /**
     * Top level did:key composer
     * @param [didUrl] - the did-url to build the did document for
     * @param [pubKey] - the public key byte array
     * @param [keyCode] - the multi-codec key code
     * @return the [Did] document
     */
    private fun constructDidKey(didUrl: DidUrl, pubKey: ByteArray, keyCode: UInt): DidKey =
        keyCode.takeIf { it == JwkJcsPubMultiCodecKeyCode }?.let {
            constructDidKey(didUrl, pubKey)
        } ?: constructDidKey(didUrl, pubKey, getKeyAlgorithmFromKeyCode(keyCode))

    /**
     * jwk_jcs-pub did:key
     * @param [didUrl] - the did-url to build the did document for
     * @param [pubKey] - the public key byte array
     * @return the [Did] document
     */
    private fun constructDidKey(didUrl: DidUrl, pubKey: ByteArray): DidKey = "${didUrl.did}#${didUrl.identifier}".let {
        DidKey(
            context = listOf(
                "https://www.w3.org/ns/did/v1",
                "https://w3id.org/security/suites/jws-2020/v1"
            ),
            id = didUrl.did,
            verificationMethod = listOf(
                VerificationMethod(
                    id = it,
                    type = "JsonWebKey2020",
                    controller = didUrl.did,
                    publicKeyJwk = Klaxon().parse(JWK.parse(String(pubKey)).toJSONString())
                )
            ),
            assertionMethod = listOf(VerificationMethod.Reference(it)),
            authentication = listOf(VerificationMethod.Reference(it)),
            capabilityInvocation = listOf(VerificationMethod.Reference(it)),
            capabilityDelegation = listOf(VerificationMethod.Reference(it)),
        )
    }

    /**
     * other types of did:key
     * @param [didUrl] - the did-url to build the did document for
     * @param [pubKey] - the public key byte array
     * @param [keyAlgorithm] - the key algorithm
     * @return the [Did] document
     */
    private fun constructDidKey(didUrl: DidUrl, pubKey: ByteArray, keyAlgorithm: KeyAlgorithm): DidKey {

        val (keyAgreementKeys, verificationMethods, keyRef) = when (keyAlgorithm) {
            KeyAlgorithm.EdDSA_Ed25519 -> generateEdParams(pubKey, didUrl)
            KeyAlgorithm.ECDSA_Secp256r1, KeyAlgorithm.ECDSA_Secp256k1 -> generateEcKeyParams(
                pubKey, didUrl, keyAlgorithm
            )

            KeyAlgorithm.RSA -> generateRSAKeyParams(pubKey, didUrl)
        }

        return DidKey(
            context = DID_CONTEXT_URL,
            id = didUrl.did,
            verificationMethod = verificationMethods,
            authentication = keyRef,
            assertionMethod = keyRef,
            capabilityDelegation = keyRef,
            capabilityInvocation = keyRef,
            keyAgreement = keyAgreementKeys,
        )
    }

    private fun generateEdParams(
        pubKey: ByteArray, didUrl: DidUrl
    ): Triple<List<VerificationMethod>?, MutableList<VerificationMethod>, List<VerificationMethod>> {
        val dhKey = convertPublicKeyEd25519ToCurve25519(pubKey)

        val dhKeyMb = convertX25519PublicKeyToMultiBase58Btc(dhKey)

        val pubKeyId = didUrl.did + "#" + didUrl.identifier
        val dhKeyId = didUrl.did + "#" + dhKeyMb

        val verificationMethods = mutableListOf(
            VerificationMethod(
                pubKeyId, LdVerificationKeyType.Ed25519VerificationKey2019.name, didUrl.did, pubKey.encodeBase58()
            ), VerificationMethod(dhKeyId, "X25519KeyAgreementKey2019", didUrl.did, dhKey.encodeBase58())
        )

        return Triple(
            listOf(VerificationMethod.Reference(dhKeyId)), verificationMethods, listOf(
                VerificationMethod.Reference(
                    pubKeyId
                )
            )
        )
    }

    private fun generateEcKeyParams(
        pubKey: ByteArray, didUrl: DidUrl, algorithm: KeyAlgorithm
    ): Triple<List<VerificationMethod>?, MutableList<VerificationMethod>, List<VerificationMethod>> {
        val curve = if (algorithm == KeyAlgorithm.ECDSA_Secp256k1) Curve.SECP256K1 else Curve.P_256
        val vmType =
            if (algorithm == KeyAlgorithm.ECDSA_Secp256k1) LdVerificationKeyType.EcdsaSecp256k1VerificationKey2019.name else LdVerificationKeyType.EcdsaSecp256r1VerificationKey2019.name

        val uncompressedPubKey =
            uncompressSecp256k1(pubKey, curve) ?: throw IllegalArgumentException("Error uncompressing public key bytes")
        val pubKeyId = didUrl.did + "#" + didUrl.identifier
        val key = Key(newKeyId(), algorithm, CryptoProvider.SUN, KeyPair(uncompressedPubKey.toECPublicKey(), null))

        val verificationMethods = mutableListOf(
            VerificationMethod(
                id = pubKeyId,
                type = vmType,
                controller = didUrl.did,
                publicKeyJwk = Klaxon().parse<Jwk>(keyService.toSecp256Jwk(key, curve, key.keyId.id).toJSONString())
            )
        )

        return Triple(
            null, verificationMethods, listOf(VerificationMethod.Reference(pubKeyId))
        )
    }

    private fun generateRSAKeyParams(
        pubKey: ByteArray, didUrl: DidUrl
    ): Triple<List<VerificationMethod>?, MutableList<VerificationMethod>, List<VerificationMethod>> {

        val pubKeyId = didUrl.did + "#" + didUrl.identifier

        val verificationMethods = mutableListOf(
            VerificationMethod(
                pubKeyId, LdVerificationKeyType.RsaVerificationKey2018.name, didUrl.did, pubKey.encodeBase58()
            ),
        )

        val keyRef = listOf(VerificationMethod.Reference(pubKeyId))
        return Triple(null, verificationMethods, keyRef)
    }
}
