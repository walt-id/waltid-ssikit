package id.walt.services.did.composers

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.jwk.JWK
import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.LdVerificationKeyType
import id.walt.model.Did
import id.walt.model.Jwk
import id.walt.model.VerificationMethod
import mu.KotlinLogging

abstract class DidDocumentComposerBase<T : Did> : DidDocumentComposer<T> {
    private val log = KotlinLogging.logger {}

    protected fun buildVerificationMethods(
        key: Key, kid: String, didUrlStr: String, jwk: JWK
    ): MutableList<VerificationMethod> {
        val keyType = when (key.algorithm) {
            KeyAlgorithm.EdDSA_Ed25519 -> LdVerificationKeyType.Ed25519VerificationKey2019
            KeyAlgorithm.ECDSA_Secp256k1 -> LdVerificationKeyType.EcdsaSecp256k1VerificationKey2019
            KeyAlgorithm.ECDSA_Secp256r1 -> LdVerificationKeyType.EcdsaSecp256r1VerificationKey2019
            KeyAlgorithm.RSA -> LdVerificationKeyType.RsaVerificationKey2018
        }
        log.debug { "Verification method JWK for kid: $jwk" }
        log.debug { "Verification method public JWK: ${jwk.toPublicJWK()}" }
        log.debug { "Verification method parsed public JWK: ${Klaxon().parse<Jwk>(jwk.toPublicJWK().toString())}" }
        val publicKeyJwk = Klaxon().parse<Jwk>(jwk.toPublicJWK().toString())

        return mutableListOf(
            VerificationMethod(kid, keyType.name, didUrlStr, null, null, publicKeyJwk),
        )
    }
}
