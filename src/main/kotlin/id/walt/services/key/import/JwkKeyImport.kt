package id.walt.services.key.import

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyType
import id.walt.crypto.*
import id.walt.services.CryptoProvider
import id.walt.services.keystore.KeyStoreService

class JwkKeyImport(private val keyString: String) : KeyImportStrategy {

    override fun import(keyStore: KeyStoreService): KeyId {
        val key = parseJwkKey(keyString)
        keyStore.store(key)
        return key.keyId
    }

    private fun parseJwkKey(jwkKeyStr: String): Key {
        val jwk = JWK.parse(jwkKeyStr)

        val key = when (jwk.keyType) {
            KeyType.RSA -> Key(
                keyId = KeyId(jwk.keyID ?: newKeyId().id),
                algorithm = KeyAlgorithm.RSA,
                cryptoProvider = CryptoProvider.SUN,
                keyPair = jwk.toRSAKey().toKeyPair()
            )

            KeyType.EC -> {
                val alg = when (jwk.toECKey().curve) {
                    Curve.P_256 -> KeyAlgorithm.ECDSA_Secp256r1
                    Curve.SECP256K1 -> KeyAlgorithm.ECDSA_Secp256k1
                    else -> throw IllegalArgumentException("EC key with curve ${jwk.toECKey().curve} not suppoerted")
                }
                Key(
                    keyId = KeyId(jwk.keyID ?: newKeyId().id),
                    algorithm = alg,
                    cryptoProvider = CryptoProvider.SUN,
                    keyPair = jwk.toECKey().toKeyPair()
                )
            }

            KeyType.OKP -> {
                val alg = when (jwk.toOctetKeyPair().curve) {
                    Curve.Ed25519 -> KeyAlgorithm.EdDSA_Ed25519
                    else -> throw IllegalArgumentException("OKP key with curve ${jwk.toOctetKeyPair().curve} not supported")
                }
                buildKey(
                    keyId = jwk.keyID ?: newKeyId().id,
                    algorithm = alg.name,
                    provider = CryptoProvider.SUN.name,
                    publicPart = jwk.toOctetKeyPair().x.toString(),
                    privatePart = jwk.toOctetKeyPair().d?.let { jwk.toOctetKeyPair().d.toString() },
                    format = KeyFormat.BASE64_RAW
                )
            }

            else -> throw IllegalArgumentException("KeyType ${jwk.keyType} / Algorithm ${jwk.algorithm} not supported")
        }
        return key
    }

}
