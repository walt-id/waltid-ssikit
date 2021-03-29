package org.letstrust.crypto

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.util.Base64URL
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.crypto.ByteSigner
import info.weboftrust.ldsignatures.crypto.adapter.JWSSignerAdapter
import info.weboftrust.ldsignatures.crypto.impl.Ed25519_EdDSA_PrivateKeySigner
import info.weboftrust.ldsignatures.signer.LdSigner
import info.weboftrust.ldsignatures.suites.EcdsaSecp256k1Signature2019SignatureSuite
import info.weboftrust.ldsignatures.suites.Ed25519Signature2018SignatureSuite
import info.weboftrust.ldsignatures.suites.SignatureSuites
import info.weboftrust.ldsignatures.util.JWSUtil
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.PrivateKey
import java.security.SignatureException
import java.security.interfaces.ECPrivateKey

class LdSigner {

    class JcaSigner : JWSSigner {

        val privateKey: PrivateKey

        private val jcaContext = JCAContext()

        constructor(privateKey: ECPrivateKey) {
            this.privateKey = privateKey
        }


        constructor(privateKey: PrivateKey, curve: Curve?) {
            require("EC".equals(privateKey.algorithm, ignoreCase = true)) { "The private key algorithm must be EC" }
            this.privateKey = privateKey
        }


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
            val jcaSignature: ByteArray
            jcaSignature = try {
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

    class EcdsaSecp256k1Signature2019(val keyId: KeyId) :
        LdSigner<EcdsaSecp256k1Signature2019SignatureSuite?>(SignatureSuites.SIGNATURE_SUITE_ECDSASECP256L1SIGNATURE2019, null) {

        override fun sign(ldProofBuilder: LdProof.Builder<*>, signingInput: ByteArray) {
            val jwsHeader = JWSHeader.Builder(JWSAlgorithm.ES256K).base64URLEncodePayload(false).criticalParams(setOf("b64")).build()
            val jwsSigningInput = JWSUtil.getJwsSigningInput(jwsHeader, signingInput)
            val jwsSigner = JcaSigner(PrivateKeyHandle(keyId), Curve.SECP256K1)
            jwsSigner.jcaContext.provider = LetsTrustProvider()
            val signature = jwsSigner.sign(jwsHeader, jwsSigningInput)
            val jws = JWSUtil.serializeDetachedJws(jwsHeader, signature)
            ldProofBuilder.jws(jws)
        }
    }

    class Ed25519Signature2018(val keyId: KeyId) :
        LdSigner<Ed25519Signature2018SignatureSuite?>(SignatureSuites.SIGNATURE_SUITE_ED25519SIGNATURE2018, null) {

        override fun sign(ldProofBuilder: LdProof.Builder<*>, signingInput: ByteArray) {

//            val jwsHeader = JWSHeader.Builder(JWSAlgorithm.EdDSA).base64URLEncodePayload(false).criticalParams(setOf("b64")).build()
//            val jwsSigningInput = JWSUtil.getJwsSigningInput(jwsHeader, signingInput)
//            val privateKey: OctetKeyPair = ...
//            val jwsSigner: JWSSigner = Ed25519Signer(privateKey)
//            val signature = jwsSigner.sign(jwsHeader, jwsSigningInput)
//            val jws = JWSUtil.serializeDetachedJws(jwsHeader, signature)
//
//            ldProofBuilder.jws(jws)
        }
    }

    class Ed25519Signature2018LdSigner(signer: ByteSigner? = null as ByteSigner?) :
        LdSigner<Ed25519Signature2018SignatureSuite?>(SignatureSuites.SIGNATURE_SUITE_ED25519SIGNATURE2018, signer) {
        constructor(privateKey: ByteArray?) : this(Ed25519_EdDSA_PrivateKeySigner(privateKey)) {}

        @Throws(GeneralSecurityException::class)
        override fun sign(ldProofBuilder: LdProof.Builder<*>, signingInput: ByteArray) {

            val jws: String
            jws = try {
                val jwsHeader = JWSHeader.Builder(JWSAlgorithm.EdDSA).base64URLEncodePayload(false).criticalParams(setOf("b64")).build()
                val jwsSigningInput = JWSUtil.getJwsSigningInput(jwsHeader, signingInput)
                val jwsSigner: JWSSigner = JWSSignerAdapter(signer, JWSAlgorithm.EdDSA)
                val signature = jwsSigner.sign(jwsHeader, jwsSigningInput)
                JWSUtil.serializeDetachedJws(jwsHeader, signature)
            } catch (ex: JOSEException) {
                throw GeneralSecurityException("JOSE signing problem: " + ex.message, ex)
            }

            // done
            ldProofBuilder.jws(jws)
        }
    }

}
