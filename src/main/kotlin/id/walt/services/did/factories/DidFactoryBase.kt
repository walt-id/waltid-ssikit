package id.walt.services.did.factories

import com.beust.klaxon.Klaxon
import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.LdVerificationKeyType
import id.walt.model.DidMethod
import id.walt.model.Jwk
import id.walt.model.VerificationMethod
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import mu.KotlinLogging

abstract class DidFactoryBase : DidFactory {
    private val log = KotlinLogging.logger {}
    protected fun buildVerificationMethods(
        key: Key, kid: String, didUrlStr: String
    ): MutableList<VerificationMethod> {
        val keyType = when (key.algorithm) {
            KeyAlgorithm.EdDSA_Ed25519 -> LdVerificationKeyType.Ed25519VerificationKey2019
            KeyAlgorithm.ECDSA_Secp256k1 -> LdVerificationKeyType.EcdsaSecp256k1VerificationKey2019
            KeyAlgorithm.ECDSA_Secp256r1 -> LdVerificationKeyType.EcdsaSecp256r1VerificationKey2019
            KeyAlgorithm.RSA -> LdVerificationKeyType.RsaVerificationKey2018
        }
        log.debug { "Verification method JWK for kid: ${DidService.keyService.toJwk(kid)}" }
        log.debug { "Verification method public JWK: ${DidService.keyService.toJwk(kid).toPublicJWK()}" }
        log.debug {
            "Verification method parsed public JWK: ${
                Klaxon().parse<Jwk>(
                    DidService.keyService.toJwk(kid).toPublicJWK().toString()
                )
            }"
        }
        //TODO: inject keyService
        val publicKeyJwk = Klaxon().parse<Jwk>(DidService.keyService.toJwk(kid).toPublicJWK().toString())

        return mutableListOf(
            VerificationMethod(kid, keyType.name, didUrlStr, null, null, publicKeyJwk),
        )
    }

    companion object {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        fun new(method: DidMethod, keyService: KeyService): DidFactory = when (method) {
            DidMethod.key -> DidKeyFactory()
            DidMethod.web -> DidWebFactory()
            DidMethod.iota -> DidIotaFactory()
            DidMethod.cheqd -> DidCheqdFactory()
            DidMethod.ebsi -> DidEbsiFactory(keyService)
            DidMethod.jwk -> DidJwkFactory(keyService)
            else -> throw UnsupportedOperationException("DID method $method not supported")
        }
    }
}
