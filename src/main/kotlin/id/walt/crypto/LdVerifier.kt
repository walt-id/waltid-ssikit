package id.walt.crypto

import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.Ed25519Verifier
import id.walt.services.key.KeyService
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.canonicalizer.Canonicalizers
import info.weboftrust.ldsignatures.suites.EcdsaSecp256k1Signature2019SignatureSuite
import info.weboftrust.ldsignatures.suites.Ed25519Signature2018SignatureSuite
import info.weboftrust.ldsignatures.suites.SignatureSuites
import info.weboftrust.ldsignatures.util.JWSUtil
import info.weboftrust.ldsignatures.verifier.LdVerifier
import java.security.PublicKey
import java.security.interfaces.ECPublicKey

class LdVerifier {

    class EcdsaSecp256k1Signature2019(val publicKey: PublicKey) :
        LdVerifier<EcdsaSecp256k1Signature2019SignatureSuite?>(
            SignatureSuites.SIGNATURE_SUITE_ECDSASECP256L1SIGNATURE2019,
            null,
            Canonicalizers.CANONICALIZER_JCSCANONICALIZER
        ) {

        override fun verify(signingInput: ByteArray, ldProof: LdProof): Boolean {
            val detachedJwsObject = JWSObject.parse(ldProof.jws)
            val jwsSigningInput = JWSUtil.getJwsSigningInput(detachedJwsObject.header, signingInput)
            val jwsVerifier = ECDSAVerifier(publicKey as ECPublicKey)
            return jwsVerifier.verify(detachedJwsObject.header, jwsSigningInput, detachedJwsObject.signature)
        }
    }

    class Ed25519Signature2018(val publicKey: Key) :
        LdVerifier<Ed25519Signature2018SignatureSuite?>(SignatureSuites.SIGNATURE_SUITE_ED25519SIGNATURE2018, null, Canonicalizers.CANONICALIZER_JCSCANONICALIZER) {

        private val keyService = KeyService.getService()

        override fun verify(signingInput: ByteArray, ldProof: LdProof): Boolean {
            val detachedJwsObject = JWSObject.parse(ldProof.jws)
            val jwsSigningInput = JWSUtil.getJwsSigningInput(detachedJwsObject.header, signingInput)
            val jwsVerifier = Ed25519Verifier(keyService.toEd25519Jwk(publicKey))
            return jwsVerifier.verify(detachedJwsObject.header, jwsSigningInput, detachedJwsObject.signature)
        }
    }
}
