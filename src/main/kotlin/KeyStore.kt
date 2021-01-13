import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec


interface KeyStore {
    fun saveKeyPair(keys: Keys)
    fun loadKeyPair(keyId: String): Keys?
}

object FileSystemKeyStore : KeyStore {

    private const val KEY_DIR_PATH = "keys"

    private var keyFactory = KeyFactory.getInstance("ECDSA", "BC")

    init {
        File(KEY_DIR_PATH).mkdirs()
    }

    public fun updateProvider(keyFactory: KeyFactory) {
        this.keyFactory = keyFactory
    }

    override fun saveKeyPair(keys: Keys) {
        keys.pair?.let { saveEncPublicKey(keys.keyId, it.public) }
        keys.pair?.let { saveEncPrivateKey(keys.keyId, it.private) }
        keys.publicKey?.let { saveRawPublicKey(keys.keyId, it) }
        keys.privateKey?.let { saveRawPrivateKey(keys.keyId, it) }
    }

    override fun loadKeyPair(keyId: String): Keys? {
        if (keyFileExists(keyId, "enc-pubkey") && keyFileExists(keyId, "enc-privkey")) {
            return Keys(keyId, KeyPair(loadEncPublicKey(keyId), loadEncPrivateKey(keyId)))
        }

        if (keyFileExists(keyId, "raw-pubkey") && keyFileExists(keyId, "raw-privkey")) {
            return Keys(keyId, loadRawPrivateKey(keyId), loadRawPublicKey(keyId))
        }
        return null;
    }

    private fun saveEncPublicKey(keyId: String, encodedPublicKey: PublicKey) =
        saveKeyFile(keyId, "enc-pubkey", X509EncodedKeySpec(encodedPublicKey.encoded).encoded)

    private fun saveEncPrivateKey(keyId: String, encodedPrivateKey: PrivateKey) =
        saveKeyFile(keyId, "enc-privkey", PKCS8EncodedKeySpec(encodedPrivateKey.encoded).encoded)

    private fun saveRawPublicKey(keyId: String, rawPublicKey: ByteArray) =
        saveKeyFile(keyId, "raw-pubkey", rawPublicKey)

    private fun saveRawPrivateKey(keyId: String, rawPrivateKey: ByteArray) =
        saveKeyFile(keyId, "raw-privkey", rawPrivateKey)

    private fun saveKeyFile(keyId: String, suffix: String, data: ByteArray): Unit =
        FileOutputStream("$KEY_DIR_PATH/$keyId.$suffix").use { it.write(data) }

    private fun loadKeyFile(keyId: String, suffix: String): ByteArray =
        IOUtils.toByteArray(FileInputStream("$KEY_DIR_PATH/$keyId.$suffix"))

    private fun deleteKeyFile(keyId: String, suffix: String) = File("$KEY_DIR_PATH/$keyId.$suffix").delete()

    fun getKeyIdList() = File(KEY_DIR_PATH).listFiles()!!.map { it.nameWithoutExtension }.distinct()

    private fun keyFileExists(keyId: String, suffix: String) = File("$KEY_DIR_PATH/$keyId.$suffix").exists()

    private fun loadRawPublicKey(keyId: String): ByteArray = loadKeyFile(keyId, "raw-pubkey");

    private fun loadRawPrivateKey(keyId: String): ByteArray = loadKeyFile(keyId, "raw-privkey");

    private fun loadEncPublicKey(keyId: String): PublicKey {
        return keyFactory.generatePublic(
            X509EncodedKeySpec(
                loadKeyFile(keyId, "enc-pubkey")
            )
        )
    }

    private fun loadEncPrivateKey(keyId: String): PrivateKey {
        return keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(
                loadKeyFile(keyId, "enc-privkey")
            )
        )
    }

    fun deleteKeyPair(keyId: String) {
        deleteKeyFile(keyId, "enc-pubkey")
        deleteKeyFile(keyId, "enc-privkey")
        deleteKeyFile(keyId, "raw-pubkey")
        deleteKeyFile(keyId, "raw-privkey")
    }
}
