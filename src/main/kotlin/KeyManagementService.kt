import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.subtle.Ed25519Sign
import io.ipfs.multibase.Multibase
import org.bitcoinj.core.ECKey
import org.bouncycastle.jce.ECNamedCurveTable
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.*
import kotlin.collections.ArrayList


object KeyManagementService {

    // TODO: keystore implementation should be configurable
    private val ks = FileSystemKeyStore

    private fun generateKeyId(): String = "LetsTrust-Key-${UUID.randomUUID().toString().replace("-", "")}"

    fun getSupportedCurveNames(): List<String> {
        var ecNames = ArrayList<String>()
        for (name in ECNamedCurveTable.getNames()) {
            ecNames.add(name.toString())
        }
        return ecNames;
    }

    fun generateEcKeyPair(ecCurveName: String): String {
        val generator = KeyPairGenerator.getInstance("ECDSA", "BC")
        generator.initialize(ECNamedCurveTable.getParameterSpec(ecCurveName), SecureRandom())

        val keys = Keys(generateKeyId(), generator.generateKeyPair())
        ks.saveKeyPair(keys)
        return keys.keyId
    }

    fun generateEd25519KeyPair(): String {
        TinkConfig.register();

        var keyPair = Ed25519Sign.KeyPair.newKeyPair()
        val keys = Keys(generateKeyId(), keyPair.privateKey, keyPair.publicKey)
        ks.saveKeyPair(keys)
        return keys.keyId
    }

    fun generateSecp256k1KeyPair(): String {
        var key = ECKey(SecureRandom())
        val keys = Keys(generateKeyId(), key.privKeyBytes, key.pubKey)
        ks.saveKeyPair(keys)
        return keys.keyId
    }

    fun generateRsaKeyPair(): String {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(1024)
        val keys = Keys(generateKeyId(), generator.generateKeyPair())
        ks.saveKeyPair(keys)
        return keys.keyId
    }

    fun loadKeys(keyId: String): Keys? {
        return ks.loadKeyPair(ks.getKeyId(keyId)!!)
    }

    fun deleteKeys(keyId: String) {
        ks.deleteKeyPair(ks.getKeyId(keyId)!!)
    }

    fun getMultiBase58PublicKey(keyId: String): String {
        return ks.loadKeyPair(keyId).let {
            Multibase.encode(Multibase.Base.Base58BTC, it!!.publicKey)
        }
    }

    fun addAlias(keyId: String, identifier: String) {
        ks.addAlias(keyId, identifier)
    }

}
