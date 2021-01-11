import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.subtle.Ed25519Sign
import org.bitcoinj.core.Base58
import org.bouncycastle.jce.ECNamedCurveTable
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.*
import kotlin.collections.ArrayList



object KeyManagementService {

    // TODO: keystore implementation should be configurable
    private val ks = FileSystemKeyStore

    private fun generateKeyId(): String = "LetsTrust-Key-${UUID.randomUUID().toString().replace("-", "")}"

    private val aliasMap = HashMap<String, String>()

    fun getSupportedCurveNames(): List<String> {
        var ecNames = ArrayList<String>()
        for (name in ECNamedCurveTable.getNames()) {
            println(name.toString())
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

    fun loadKeys(keyId: String): Keys? {
        return ks.loadKeyPair(keyId)
    }

    fun deleteKeys(keyId: String) {
        ks.deleteKeyPair(keyId)
    }

    fun getBase58PublicKey(keyId: String) : String{
        return ks.loadKeyPair(keyId).let {
            Base58.encode(it!!.publicKey)
        }
    }

    // TODO: Persist alias-map
    fun addAlias(keyId: String, alias: String) {
        aliasMap.put(alias, keyId)
    }

    fun getKeyId(alias: String): String? {
        return aliasMap.get(alias)
    }
}
