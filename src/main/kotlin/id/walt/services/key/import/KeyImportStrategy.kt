package id.walt.services.key.import

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyType
import id.walt.crypto.*
import id.walt.services.CryptoProvider
import id.walt.services.keystore.KeyStoreService
import mu.KotlinLogging
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import java.io.StringReader
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

interface KeyImportStrategy {
    fun import(keyStore: KeyStoreService): KeyId
}

abstract class KeyImportFactory {
    companion object {
        fun create(keyString: String) = when (isPEM(keyString)) {
            true -> PEMImportImpl(keyString)
            false -> JWKImportImpl(keyString)
        }

        private fun isPEM(keyString: String) = keyString.startsWith("-----")
    }
}

class PEMImportImpl(val keyString: String) : KeyImportStrategy {

    private val log = KotlinLogging.logger {}

    override fun import(keyStore: KeyStoreService) = importPem(keyString, keyStore)

    /**
     * Imports the given PEM encoded key string
     * @param keyStr the key string
     *
     *               - for RSA keys: the PEM private key file
     *               - for other key types: concatenated public and private key in PEM format
     * @return the imported key id
     */
    private fun importPem(keyStr: String, keyStore: KeyStoreService): KeyId {
        val parser = PEMParser(StringReader(keyStr))
        val parsedPemObject = mutableListOf<Any>()
        try {
            var currentPEMObject: Any?
            do {
                currentPEMObject = parser.readObject()
                log.debug { "PEM parser next object: $currentPEMObject" }
                if (currentPEMObject != null) {
                    parsedPemObject.add(currentPEMObject)
                }
            } while (currentPEMObject != null)
        } catch (e: Exception) {
            log.error(e) { "Error while importing PEM key!" }
        }

        val kid = newKeyId()
        val keyPair = getKeyPair(parsedPemObject)
        keyStore.store(Key(kid, KeyAlgorithm.fromString(keyPair.public.algorithm), CryptoProvider.SUN, keyPair))

        return kid
    }

    /**
     * Parses a keypair out of a one or multiple objects
     */
    private fun getKeyPair(objs: List<Any>): KeyPair {
        lateinit var pubKey: PublicKey
        lateinit var privKey: PrivateKey

        objs.toList()

        log.debug { "Searching key pair in: $objs" }
        for (obj in objs) {
            if (obj is SubjectPublicKeyInfo) {
                pubKey = getPublicKey(obj)
            }
            if (obj is PrivateKeyInfo) {
                privKey = getPrivateKey(obj)
            }
            if (obj is PEMKeyPair) {
                pubKey = getPublicKey(obj.publicKeyInfo)
                privKey = getPrivateKey(obj.privateKeyInfo)
                break
            }
        }
        return KeyPair(pubKey, privKey)
    }

    private fun getPublicKey(key: SubjectPublicKeyInfo): PublicKey {
        val kf = getKeyFactory(key.algorithm.algorithm)
        return kf.generatePublic(X509EncodedKeySpec(key.encoded))
    }

    private fun getPrivateKey(key: PrivateKeyInfo): PrivateKey {
        val kf = getKeyFactory(key.privateKeyAlgorithm.algorithm)
        return kf.generatePrivate(PKCS8EncodedKeySpec(key.encoded))
    }

    private fun getKeyFactory(alg: ASN1ObjectIdentifier): KeyFactory = when (alg) {
        PKCSObjectIdentifiers.rsaEncryption -> KeyFactory.getInstance("RSA")
        ASN1ObjectIdentifier("1.3.101.112") -> KeyFactory.getInstance("Ed25519")
        ASN1ObjectIdentifier("1.2.840.10045.2.1") -> KeyFactory.getInstance("ECDSA")
        else -> throw IllegalArgumentException("Algorithm not supported")
    }
}

class JWKImportImpl(val keyString: String) : KeyImportStrategy {

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
