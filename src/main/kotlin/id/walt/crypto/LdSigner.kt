package id.walt.crypto

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.util.Base64URL
import id.walt.services.crypto.CryptoService
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.canonicalizer.Canonicalizer
import info.weboftrust.ldsignatures.canonicalizer.Canonicalizers
import info.weboftrust.ldsignatures.signer.LdSigner
import info.weboftrust.ldsignatures.suites.*
import info.weboftrust.ldsignatures.util.JWSUtil
import io.ipfs.multibase.Base58
import java.security.InvalidKeyException
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.SignatureException

class LdSigner {

    // Calls Walt CryptoService via JCA WaltIdProvider
    // Currently not required since ECDSASigner is sufficient
    @Deprecated(message = "Only for testing - Use ECDSASigner instead")
    class JcaSigner(val privateKey: PrivateKey) : JWSSigner {

        private val jcaContext = JCAContext(WaltIdProvider(), SecureRandom())

        override fun getJCAContext(): JCAContext {
            return this.jcaContext
        }

        override fun supportedJWSAlgorithms(): MutableSet<JWSAlgorithm> {
            TODO("Not yet implemented")
        }

        override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
            val alg = header.algorithm
//            if (!supportedJWSAlgorithms().contains(alg)) {
//                throw JOSEException(AlgorithmSupportMessage.unsupportedJWSAlgorithm(alg, supportedJWSAlgorithms()))
//            }

            // DER-encoded signature, according to JCA spec
            // (sequence of two integers - R + S)
            val jcaSignature: ByteArray = try {
                val dsa = ECDSA.getSignerAndVerifier(alg, jcaContext.provider)
                dsa.initSign(privateKey, jcaContext.secureRandom)
                dsa.update(signingInput)
                dsa.sign()
            } catch (e: InvalidKeyException) {
                throw JOSEException(e.message, e)
            } catch (e: SignatureException) {
                throw JOSEException(e.message, e)
            }
            val rsByteArrayLength = ECDSA.getSignatureByteArrayLength(header.algorithm)
            val jwsSignature = ECDSA.transcodeSignatureToConcat(jcaSignature, rsByteArrayLength)
            return Base64URL.encode(jwsSignature)
        }
    }

    // Directly calls Walt CryptoService
    class JwsLtSigner(val keyId: KeyId) : JWSSigner {

        private val cryptoService = CryptoService.getService()

        override fun getJCAContext(): JCAContext {
            TODO("Not yet implemented")
        }

        override fun supportedJWSAlgorithms(): MutableSet<JWSAlgorithm> =
            HashSet(setOf(JWSAlgorithm.EdDSA, JWSAlgorithm.ES256K, JWSAlgorithm.RS256))

        override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {


            val jcaSignature = cryptoService.sign(keyId, signingInput)
            // ED sig seems not to be DER decoding
            // val rsByteArrayLength = 64  // ECDSA.getSignatureByteArrayLength(header.algorithm)
            //  val jwsSignature = CryptoUtilJava.transcodeSignatureToConcat(jcaSignature, rsByteArrayLength)
            return Base64URL.encode(jcaSignature)
        }

    }

    abstract class JwsLdSignature<S : SignatureSuite?>(val keyId: KeyId, signatureSuite: S, canonicalizer: Canonicalizer) :
        LdSigner<S>(signatureSuite, null, canonicalizer) {

        abstract fun getJwsAlgorithm(): JWSAlgorithm

        abstract fun getJwsSigner(): JWSSigner

        open fun createJwsHeader(): JWSHeader {
            val jwsAlg = getJwsAlgorithm()
            return JWSHeader.Builder(jwsAlg)
                .base64URLEncodePayload(false)
                .criticalParams(setOf("b64"))
                // .x509CertURL()
                .build()
        }

        override fun sign(ldProofBuilder: LdProof.Builder<*>, signingInput: ByteArray?) {
            val jwsHeader = createJwsHeader()
            val jwsSigningInput = JWSUtil.getJwsSigningInput(jwsHeader, signingInput)
            val jwsSigner = getJwsSigner()
            val signature = jwsSigner.sign(jwsHeader, jwsSigningInput)
            val jws = JWSUtil.serializeDetachedJws(jwsHeader, signature)
            ldProofBuilder.jws(jws)
        }

    }

    class EcdsaSecp256K1Signature2019(keyId: KeyId) : JwsLdSignature<EcdsaSecp256k1Signature2019SignatureSuite?>(
        keyId,
        SignatureSuites.SIGNATURE_SUITE_ECDSASECP256L1SIGNATURE2019, Canonicalizers.CANONICALIZER_URDNA2015CANONICALIZER
    ) {

        override fun getJwsAlgorithm(): JWSAlgorithm {
            return JWSAlgorithm.ES256K
        }

        override fun getJwsSigner(): JWSSigner {
            val jwsSigner = ECDSASigner(ECPrivateKeyHandle(keyId), Curve.SECP256K1)
            jwsSigner.jcaContext.provider = WaltIdProvider()
            return jwsSigner
        }
    }

    class EcdsaSecp256R1Signature2019(keyId: KeyId) : JwsLdSignature<EcdsaSecp256k1Signature2019SignatureSuite?>(
        keyId,
        SignatureSuites.SIGNATURE_SUITE_ECDSASECP256L1SIGNATURE2019, Canonicalizers.CANONICALIZER_URDNA2015CANONICALIZER
    ) {

        override fun getJwsAlgorithm(): JWSAlgorithm {
            return JWSAlgorithm.ES256
        }

        override fun getJwsSigner(): JWSSigner {
            val jwsSigner = ECDSASigner(ECPrivateKeyHandle(keyId), Curve.P_256)
            jwsSigner.jcaContext.provider = WaltIdProvider()
            return jwsSigner
        }
    }

    class Ed25519Signature2018(keyId: KeyId) : JwsLdSignature<Ed25519Signature2018SignatureSuite?>(
        keyId,
        SignatureSuites.SIGNATURE_SUITE_ED25519SIGNATURE2018, Canonicalizers.CANONICALIZER_URDNA2015CANONICALIZER
    ) {

        override fun getJwsAlgorithm(): JWSAlgorithm {
            return JWSAlgorithm.EdDSA
        }

        override fun getJwsSigner(): JWSSigner {
            return JwsLtSigner(keyId)
        }
    }

    class Ed25519Signature2020(keyId: KeyId) : JwsLdSignature<Ed25519Signature2020SignatureSuite?>(
        keyId,
        SignatureSuites.SIGNATURE_SUITE_ED25519SIGNATURE2020, Canonicalizers.CANONICALIZER_URDNA2015CANONICALIZER
    ) {

        override fun getJwsAlgorithm(): JWSAlgorithm {
            return JWSAlgorithm.EdDSA
        }

        override fun getJwsSigner(): JWSSigner {
            return JwsLtSigner(keyId)
        }
    }

    class RsaSignature2018(keyId: KeyId) : JwsLdSignature<RsaSignature2018SignatureSuite?>(
        keyId,
        SignatureSuites.SIGNATURE_SUITE_RSASIGNATURE2018, Canonicalizers.CANONICALIZER_URDNA2015CANONICALIZER
    ) {

        override fun getJwsAlgorithm(): JWSAlgorithm {
            return JWSAlgorithm.RS256
        }

        override fun getJwsSigner(): JWSSigner {
            val keyService = KeyService.getService()
            val jwk = keyService.toJwk(keyId.id, KeyType.PRIVATE)
            val rsaKey = RSAKey.Builder(jwk.toPublicJWK().toRSAKey()).privateKey(jwk.toRSAKey().toRSAPrivateKey()).build()
            return RSASSASigner(rsaKey.toRSAPrivateKey())
        }
    }

    class JsonWebSignature2020(keyId: KeyId) : JwsLdSignature<JsonWebSignature2020SignatureSuite?>(
        keyId,
        SignatureSuites.SIGNATURE_SUITE_JSONWEBSIGNATURE2020, Canonicalizers.CANONICALIZER_URDNA2015CANONICALIZER
    ) {

        override fun getJwsAlgorithm(): JWSAlgorithm {
            val keyService = KeyService.getService()
            val key = keyService.load(keyId.id)
            return when (key.algorithm) {
                KeyAlgorithm.RSA -> JWSAlgorithm.PS256
                KeyAlgorithm.EdDSA_Ed25519 -> JWSAlgorithm.EdDSA
                KeyAlgorithm.ECDSA_Secp256k1 -> JWSAlgorithm.ES256K
                KeyAlgorithm.ECDSA_Secp256r1 -> JWSAlgorithm.ES256
            }
        }

        override fun getJwsSigner(): JWSSigner {
            val keyService = KeyService.getService()
            val key = keyService.load(keyId.id)
            return when (key.algorithm) {
                KeyAlgorithm.RSA -> RsaSignature2018(keyId).getJwsSigner()
                KeyAlgorithm.EdDSA_Ed25519 -> Ed25519Signature2018(keyId).getJwsSigner()
                KeyAlgorithm.ECDSA_Secp256k1 -> EcdsaSecp256K1Signature2019(keyId).getJwsSigner()
                KeyAlgorithm.ECDSA_Secp256r1 -> EcdsaSecp256R1Signature2019(keyId).getJwsSigner()
            }
        }
    }

    class JcsEd25519Signature2020(val keyId: KeyId) : LdSigner<JcsEd25519Signature2020SignatureSuite?>(
        SignatureSuites.SIGNATURE_SUITE_JCSED25519SIGNATURE2020, null, Canonicalizers.CANONICALIZER_JCSCANONICALIZER
    ) {
        private val cryptoService = CryptoService.getService()

        override fun sign(ldProofBuilder: LdProof.Builder<*>, signingInput: ByteArray) {
            val signature = cryptoService.sign(keyId, signingInput)
            val signatureValue = Base58.encode(signature)
            ldProofBuilder.properties(mapOf("signatureValue" to signatureValue))
        }


    }

}
