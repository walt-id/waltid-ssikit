package id.walt.crypto

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.impl.AlgorithmSupportMessage
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.util.Base64URL
import id.walt.services.crypto.SunCryptoService


// Proxy for enabling secure key storage
// TODO: EdDSA not supported yet
// TODO: Merge with LdSigner.JwtSigner
class JwtSigner(val keyId: String) : JWSSigner {

    init {
        jcaContext.provider = WaltIdProvider()
    }

    val sunCryptoService = SunCryptoService()

    val nimbusSigner = com.nimbusds.jose.crypto.ECDSASigner(sunCryptoService.ecJWK)


    override fun getJCAContext(): JCAContext {
        return nimbusSigner.jcaContext
    }

    override fun supportedJWSAlgorithms(): MutableSet<JWSAlgorithm> {
        return nimbusSigner.supportedJWSAlgorithms()
    }

    override fun sign(header: JWSHeader?, signingInput: ByteArray?): Base64URL {
        // return nimbusSigner.sign(header, signingInput)

        val alg = header!!.algorithm

        if (!supportedJWSAlgorithms().contains(alg)) {
            throw JOSEException(AlgorithmSupportMessage.unsupportedJWSAlgorithm(alg, supportedJWSAlgorithms()))
        }

        // DER-encoded signature, according to JCA spec
        // (sequence of two integers - R + S)

        val jcaSignature = sunCryptoService.sign(KeyId(keyId), signingInput!!)

        // OR keyId to PrivateKey Handle + JCA Provider

        //       val jcaSignature: ByteArray

//        jcaSignature = try {
//            val dsa = ECDSA.getSignerAndVerifier(alg, jcaContext.provider)
//            dsa.initSign(privateKey, jcaContext.secureRandom)
//            dsa.update(signingInput)
//            dsa.sign()
//        } catch (e: InvalidKeyException) {
//            throw JOSEException(e.message, e)
//        } catch (e: SignatureException) {
//            throw JOSEException(e.message, e)
//        }

        val rsByteArrayLength = ECDSA.getSignatureByteArrayLength(header.algorithm)
        val jwsSignature = ECDSA.transcodeSignatureToConcat(jcaSignature, rsByteArrayLength)
        return Base64URL.encode(jwsSignature)

    }
    // ECDSASigner(ecJWK)
}
