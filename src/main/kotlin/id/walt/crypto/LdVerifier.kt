package id.walt.crypto

import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.Ed25519Verifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import id.walt.services.key.KeyService
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.canonicalizer.Canonicalizers
import info.weboftrust.ldsignatures.suites.*
import info.weboftrust.ldsignatures.util.JWSUtil
import info.weboftrust.ldsignatures.verifier.LdVerifier
import io.ipfs.multibase.Base58
import java.security.GeneralSecurityException
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey

class LdVerifier {

    class EcdsaSecp256k1Signature2019(val publicKey: Key) : LdVerifier<EcdsaSecp256k1Signature2019SignatureSuite?>(
        SignatureSuites.SIGNATURE_SUITE_ECDSASECP256L1SIGNATURE2019, null, Canonicalizers.CANONICALIZER_URDNA2015CANONICALIZER
    ) {

        override fun verify(signingInput: ByteArray, ldProof: LdProof): Boolean {
            val detachedJwsObject = JWSObject.parse(ldProof.jws)
            val jwsSigningInput = JWSUtil.getJwsSigningInput(detachedJwsObject.header, signingInput)
            val jwsVerifier = ECDSAVerifier(publicKey.getPublicKey() as ECPublicKey)
            return jwsVerifier.verify(detachedJwsObject.header, jwsSigningInput, detachedJwsObject.signature)
        }
    }

    class EcdsaSecp256r1Signature2019(val publicKey: Key) : LdVerifier<EcdsaSecp256k1Signature2019SignatureSuite?>(
        SignatureSuites.SIGNATURE_SUITE_ECDSASECP256L1SIGNATURE2019, null, Canonicalizers.CANONICALIZER_URDNA2015CANONICALIZER
    ) {

        override fun verify(signingInput: ByteArray, ldProof: LdProof): Boolean {
            val detachedJwsObject = JWSObject.parse(ldProof.jws)
            val jwsSigningInput = JWSUtil.getJwsSigningInput(detachedJwsObject.header, signingInput)
            val jwsVerifier = ECDSAVerifier(publicKey.getPublicKey() as ECPublicKey)
            return jwsVerifier.verify(detachedJwsObject.header, jwsSigningInput, detachedJwsObject.signature)
        }
    }

    class Ed25519Signature2018(val publicKey: Key) : LdVerifier<Ed25519Signature2018SignatureSuite?>(
        SignatureSuites.SIGNATURE_SUITE_ED25519SIGNATURE2018,
        null,
        Canonicalizers.CANONICALIZER_URDNA2015CANONICALIZER
    ) {

        private val keyService = KeyService.getService()

        override fun verify(signingInput: ByteArray, ldProof: LdProof): Boolean {
            val detachedJwsObject = JWSObject.parse(ldProof.jws)
            val jwsSigningInput = JWSUtil.getJwsSigningInput(detachedJwsObject.header, signingInput)
            val jwsVerifier = Ed25519Verifier(keyService.toEd25519Jwk(publicKey))
            return jwsVerifier.verify(detachedJwsObject.header, jwsSigningInput, detachedJwsObject.signature)
        }
    }

    class Ed25519Signature2020(val publicKey: Key) : LdVerifier<Ed25519Signature2020SignatureSuite?>(
        SignatureSuites.SIGNATURE_SUITE_ED25519SIGNATURE2020,
        null,
        Canonicalizers.CANONICALIZER_URDNA2015CANONICALIZER
    ) {

        private val keyService = KeyService.getService()

        override fun verify(signingInput: ByteArray, ldProof: LdProof): Boolean {
            val detachedJwsObject = JWSObject.parse(ldProof.jws)
            val jwsSigningInput = JWSUtil.getJwsSigningInput(detachedJwsObject.header, signingInput)
            val jwsVerifier = Ed25519Verifier(keyService.toEd25519Jwk(publicKey))
            return jwsVerifier.verify(detachedJwsObject.header, jwsSigningInput, detachedJwsObject.signature)
        }
    }

    class RsaSignature2018(val publicKey: Key) : LdVerifier<RsaSignature2018SignatureSuite?>(
        SignatureSuites.SIGNATURE_SUITE_RSASIGNATURE2018,
        null,
        Canonicalizers.CANONICALIZER_URDNA2015CANONICALIZER
    ) {

        override fun verify(signingInput: ByteArray, ldProof: LdProof): Boolean {
            val detachedJwsObject = JWSObject.parse(ldProof.jws)
            val jwsSigningInput = JWSUtil.getJwsSigningInput(detachedJwsObject.header, signingInput)
            val jwsVerifier = RSASSAVerifier(publicKey.getPublicKey() as RSAPublicKey)
            return jwsVerifier.verify(detachedJwsObject.header, jwsSigningInput, detachedJwsObject.signature)
        }
    }

    class JsonWebSignature2020(val publicKey: Key) : LdVerifier<JsonWebSignature2020SignatureSuite?>(
        SignatureSuites.SIGNATURE_SUITE_JSONWEBSIGNATURE2020, null, Canonicalizers.CANONICALIZER_URDNA2015CANONICALIZER
    ) {
        override fun verify(signingInput: ByteArray, ldProof: LdProof): Boolean {
            return when (publicKey.algorithm) {
                KeyAlgorithm.RSA -> RsaSignature2018(publicKey).verify(signingInput, ldProof)
                KeyAlgorithm.ECDSA_Secp256k1 -> EcdsaSecp256k1Signature2019(publicKey).verify(signingInput, ldProof)
                KeyAlgorithm.ECDSA_Secp256r1 -> EcdsaSecp256r1Signature2019(publicKey).verify(signingInput, ldProof)
                KeyAlgorithm.EdDSA_Ed25519 -> Ed25519Signature2018(publicKey).verify(signingInput, ldProof)
            }
        }
    }

    class JcsEd25519Signature2020(val publicKey: Key) : LdVerifier<JcsEd25519Signature2020SignatureSuite?>(
        SignatureSuites.SIGNATURE_SUITE_JCSED25519SIGNATURE2020, null, Canonicalizers.CANONICALIZER_JCSCANONICALIZER
    ) {

        override fun verify(signingInput: ByteArray, ldProof: LdProof): Boolean {
            val signatureVerifier = Signature.getInstance("Ed25519")
            val signatureValue = ldProof.jsonObject["signatureValue"] as String?
                ?: throw GeneralSecurityException("No signatureValue found in ld proof.")
            val signature = Base58.decode(signatureValue)
            signatureVerifier.initVerify(publicKey.getPublicKey())
            signatureVerifier.update(signingInput)
            return signatureVerifier.verify(signature)
        }
    }
}
