package id.walt.services.did.resolvers

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.jwk.Curve
import id.walt.crypto.*
import id.walt.model.*
import id.walt.model.did.DidKey
import id.walt.services.CryptoProvider
import id.walt.services.did.DidOptions
import id.walt.services.did.DidService
import java.security.KeyPair

class DidKeyResolver : DidResolverBase<DidKey>() {

    override fun resolve(didUrl: DidUrl, options: DidOptions?) = resolveDidKey(didUrl)

    private fun resolveDidKey(didUrl: DidUrl): Did {
        val keyAlgorithm = getKeyAlgorithmFromMultibase(didUrl.identifier)
        val pubKey = convertMultiBase58BtcToRawKey(didUrl.identifier)
        return constructDidKey(didUrl, pubKey, keyAlgorithm)
    }

    private fun constructDidKey(didUrl: DidUrl, pubKey: ByteArray, keyAlgorithm: KeyAlgorithm): Did {

        val (keyAgreementKeys, verificationMethods, keyRef) = when (keyAlgorithm) {
            KeyAlgorithm.EdDSA_Ed25519 -> generateEdParams(pubKey, didUrl)
            KeyAlgorithm.ECDSA_Secp256r1, KeyAlgorithm.ECDSA_Secp256k1 -> generateEcKeyParams(
                pubKey,
                didUrl,
                keyAlgorithm
            )

            KeyAlgorithm.RSA -> generateRSAKeyParams(pubKey, didUrl)
        }

        return Did(
            context = DID_CONTEXT_URL,
            id = didUrl.did,
            verificationMethod = verificationMethods,
            authentication = keyRef,
            assertionMethod = keyRef,
            capabilityDelegation = keyRef,
            capabilityInvocation = keyRef,
            keyAgreement = keyAgreementKeys,
            serviceEndpoint = null
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
                pubKeyId,
                LdVerificationKeyType.Ed25519VerificationKey2019.name,
                didUrl.did,
                pubKey.encodeBase58()
            ),
            VerificationMethod(dhKeyId, "X25519KeyAgreementKey2019", didUrl.did, dhKey.encodeBase58())
        )

        return Triple(
            listOf(VerificationMethod.Reference(dhKeyId)),
            verificationMethods,
            listOf(VerificationMethod.Reference(pubKeyId))
        )
    }

    private fun generateEcKeyParams(
        pubKey: ByteArray, didUrl: DidUrl, algorithm: KeyAlgorithm
    ): Triple<List<VerificationMethod>?, MutableList<VerificationMethod>, List<VerificationMethod>> {
        val curve = if (algorithm == KeyAlgorithm.ECDSA_Secp256k1) Curve.SECP256K1 else Curve.P_256
        val vmType =
            if (algorithm == KeyAlgorithm.ECDSA_Secp256k1) LdVerificationKeyType.EcdsaSecp256k1VerificationKey2019.name else LdVerificationKeyType.EcdsaSecp256r1VerificationKey2019.name

        val uncompressedPubKey = uncompressSecp256k1(pubKey, curve) ?: throw IllegalArgumentException("Error uncompressing public key bytes")
        val key = Key(newKeyId(), algorithm, CryptoProvider.SUN, KeyPair(uncompressedPubKey.toECPublicKey(), null))
        val pubKeyId = didUrl.did + "#" + didUrl.identifier

        val verificationMethods = mutableListOf(
            VerificationMethod(
                pubKeyId,
                vmType,
                didUrl.did,
                publicKeyJwk = Klaxon().parse<Jwk>(DidService.keyService.toSecp256Jwk(key, curve, key.keyId.id).toJSONString())
            )
        )

        return Triple(
            null,
            verificationMethods,
            listOf(VerificationMethod.Reference(pubKeyId))
        )
    }

    private fun generateRSAKeyParams(
        pubKey: ByteArray, didUrl: DidUrl
    ): Triple<List<VerificationMethod>?, MutableList<VerificationMethod>, List<VerificationMethod>> {

        val pubKeyId = didUrl.did + "#" + didUrl.identifier

        val verificationMethods = mutableListOf(
            VerificationMethod(pubKeyId, LdVerificationKeyType.RsaVerificationKey2018.name, didUrl.did, pubKey.encodeBase58()),
        )

        val keyRef = listOf(VerificationMethod.Reference(pubKeyId))
        return Triple(null, verificationMethods, keyRef)
    }
}
