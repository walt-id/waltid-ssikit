package id.walt.services.keystore.azure

import com.microsoft.azure.keyvault.KeyVaultClient
import com.microsoft.azure.keyvault.webkey.JsonWebKey
import com.microsoft.azure.keyvault.webkey.JsonWebKeyCurveName
import id.walt.crypto.AzureKeyVaultConfig
import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.services.CryptoProvider
import id.walt.services.keystore.KeyStoreService
import id.walt.services.keystore.KeyType
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File

//azureKeyVaultConfig:
//  baseURL:
//  id:
//  secret:
open class AzureKeyStore(configurationPath: String) : KeyStoreService() {

    final override val configuration: AzureKeyVaultConfig = fromConfiguration(configurationPath)
    private val client = KeyVaultClient(TokenKeyVaultCredentials(configuration.id, configuration.secret))

    init {
        File(KEY_DIR_PATH).mkdirs()
    }

    override fun getKeyId(alias: String): String? = runCatching { File("$KEY_DIR_PATH/Alias-${alias.split(":").joinToString("-")}").readText() }.getOrNull()

    override fun listKeys(): List<Key> =
        TODO("Not yet implemented")

    override fun delete(alias: String) =
        TODO("Not yet implemented")

    override fun store(key: Key) =
        TODO("Not yet implemented")

    override fun load(alias: String, keyType: KeyType): Key {
        if (keyType == KeyType.PRIVATE)
            throw IllegalArgumentException("Cannot load private key part from HSM.")

        val keyName = getKeyId(alias) ?: alias
        val key = getKey(keyName)

        return when (key.crv()) {
            JsonWebKeyCurveName("SECP256K1"), JsonWebKeyCurveName.P_256K -> Key(
                KeyId(keyName),
                KeyAlgorithm.ECDSA_Secp256k1,
                CryptoProvider.CUSTOM,
                key.withCrv(JsonWebKeyCurveName.P_256K).toEC(false, BouncyCastleProvider())
            )
            else -> throw IllegalArgumentException("Curve not supported yet.")
        }
    }

    override fun addAlias(keyId: KeyId, alias: String) =
        File("$KEY_DIR_PATH/Alias-${alias.split(":").joinToString("-")}").writeText(keyId.id)

    private fun getKey(keyName: String): JsonWebKey =
        client.getKey(configuration.baseURL, keyName)?.key()
            ?: throw IllegalArgumentException("Key $keyName not found.")

    companion object {
        private const val KEY_DIR_PATH = "data/key"
    }
}
