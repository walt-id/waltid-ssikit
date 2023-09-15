package id.walt.services.key.import

import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.crypto.newKeyId
import id.walt.services.CryptoProvider
import id.walt.services.key.deriver.PublicKeyDeriver
import id.walt.services.keystore.KeyStoreService
import mu.KotlinLogging
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import java.io.StringReader
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

class PemKeyImport(
    private val keyString: String,
    private val publicKeyDeriver: PublicKeyDeriver<AsymmetricKeyParameter>
) : KeyImportStrategy {

    private val log = KotlinLogging.logger {}

    override fun import(keyStore: KeyStoreService) = importPem(keyString, keyStore)

    /**
     * Imports the given PEM encoded key string
     * @param keyStr the key string
     * - for RSA keys: the PEM private key file
     * - for other key types: concatenated public and private key in PEM format
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
        keyStore.store(Key(kid, getKeyAlgorithm(keyPair), CryptoProvider.SUN, keyPair))

        return kid
    }

    private fun getKeyAlgorithm(keyPair: KeyPair): KeyAlgorithm = (keyPair.private ?: keyPair.public).let {
        KeyAlgorithm.fromString(it.algorithm)
    }

    /**
     * Parses a keypair out of a one or multiple objects
     */
    private fun getKeyPair(objs: List<Any>): KeyPair {
        var pubKey: SubjectPublicKeyInfo? = null
        var privKey: PrivateKeyInfo? = null

        objs.toList()

        log.debug { "Searching key pair in: $objs" }
        for (obj in objs) {
            if (obj is SubjectPublicKeyInfo) {
                pubKey = obj
            }
            if (obj is PrivateKeyInfo) {
                privKey = obj
            }
            if (obj is PEMKeyPair) {
                pubKey = obj.publicKeyInfo
                privKey = obj.privateKeyInfo
                break
            }
        }
        return KeyPair(
            getPublicKey(pubKey) ?: publicKeyDeriver.derive(PrivateKeyFactory.createKey(privKey)),
            getPrivateKey(privKey)
        )
    }

    private fun getPublicKey(key: SubjectPublicKeyInfo?): PublicKey? = key?.let {
        val kf = getKeyFactory(it.algorithm.algorithm)
        return kf.generatePublic(X509EncodedKeySpec(it.encoded))
    }

    private fun getPrivateKey(key: PrivateKeyInfo?): PrivateKey? = key?.let {
        val kf = getKeyFactory(it.privateKeyAlgorithm.algorithm)
        return kf.generatePrivate(PKCS8EncodedKeySpec(it.encoded))
    }

    private fun getKeyFactory(alg: ASN1ObjectIdentifier): KeyFactory = when (alg) {
        PKCSObjectIdentifiers.rsaEncryption -> KeyFactory.getInstance("RSA")
        ASN1ObjectIdentifier("1.3.101.112") -> KeyFactory.getInstance("Ed25519")
        ASN1ObjectIdentifier("1.2.840.10045.2.1") -> KeyFactory.getInstance("ECDSA")
        else -> throw IllegalArgumentException("Algorithm not supported")
    }
}
