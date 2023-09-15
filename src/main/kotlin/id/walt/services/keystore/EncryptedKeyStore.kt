package id.walt.services.keystore


import id.walt.crypto.*
import id.walt.servicematrix.ServiceConfiguration
import id.walt.services.hkvstore.enc.EncryptedHKVStore
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.name

class EncryptedKeyStore(configurationPath: String) : KeyStoreService() {

    // private val log = KotlinLogging.logger {}

    data class EncryptionConfiguration(
        val encryptionAtRestKey: String,
        val keyFormat: String = "PEM",
        val keysRoot: String = "keys",
        val aliasRoot: String = "alias"
    ) : ServiceConfiguration

    override val configuration: EncryptionConfiguration = fromConfiguration(configurationPath)

    private val hkvs = EncryptedHKVStore("keystore", configuration.encryptionAtRestKey.toByteArray())

    private val keyFormat = KeyFormat.valueOf(configuration.keyFormat)
    private val keysRoot = Path(configuration.keysRoot)
    private val aliasRoot = Path(configuration.aliasRoot)

    override fun listKeys(): List<Key> = hkvs.listDocuments(keysRoot)
        .filter { k -> k.name == "meta" }
        .map {
            load(it.parent!!.name)
        }

    override fun load(alias: String, keyType: KeyType): Key {
        // log.debug { "Loading key \"${alias}\"." }

        val keyId = getKeyId(alias) ?: alias

        val metaData = loadKey(keyId, "meta").decodeToString()
        val algorithm = metaData.substringBefore(delimiter = ";")
        val provider = metaData.substringAfter(delimiter = ";")

        val publicPart = loadKey(keyId, "enc-pubkey").decodeToString()
        val privatePart = if (keyType == KeyType.PRIVATE) loadKey(keyId, "enc-privkey").decodeToString() else null


        return buildKey(keyId, algorithm, provider, publicPart, privatePart, keyFormat)
    }

    override fun addAlias(keyId: KeyId, alias: String) {
        hkvs.storeDocument(Path(aliasRoot.name, alias), keyId.id)

        val aliasListPath = Path(keysRoot.name, keyId.id, "aliases")

        if (!hkvs.exists(aliasListPath)) hkvs.storeDocument(aliasListPath, "")

        val aliases = hkvs.loadDocument(aliasListPath).toString()
            .split("\n").plus(alias)
        hkvs.storeDocument(Path(keysRoot.name, keyId.id, "aliases"), aliases.joinToString("\n"))
    }

    override fun store(key: Key) {
        // log.debug { "Storing key \"${key.keyId}\"." }
        addAlias(key.keyId, key.keyId.id)
        storeKeyMetaData(key)
        storeAvailableKeys(key)
    }

    override fun getKeyId(alias: String) =
        runCatching { hkvs.loadDocument(Path(aliasRoot.name, alias)).toString() }.getOrNull()

    override fun delete(alias: String) {
        val keyId = getKeyId(alias)
        if (keyId.isNullOrEmpty())
            return
        val aliases = hkvs.loadDocument(Path(keysRoot.name, keyId, "aliases")).toString()
        aliases.split("\n").filterNot { it.isBlank() }.forEach { a -> hkvs.deleteDocument(Path(aliasRoot.name, a)) }
        hkvs.deleteDocument(Path(keysRoot.name, keyId))
    }

    private fun storeAvailableKeys(key: Key) = run {
        key.keyPair?.run {
            this.private?.run { saveKey(key.keyId, this) }
//            this.public?.run { saveKey(key.keyId, this) }
        }
        runCatching { key.getPublicKey() }.onSuccess { saveKey(key.keyId, it) }
    }

    private fun saveKey(keyId: KeyId, key: java.security.Key) = when (key) {
        is PrivateKey -> "enc-privkey"
        is PublicKey -> "enc-pubkey"
        else -> throw IllegalArgumentException()
    }.run {
        saveKeyData(
            keyId, this, when (keyFormat) {
                KeyFormat.PEM -> key.toPEM()
                else -> key.toBase64()
            }.encodeToByteArray()
        )
    }

    private fun storeKeyMetaData(key: Key) {
        saveKeyData(key.keyId, "meta", (key.algorithm.name + ";" + key.cryptoProvider.name).encodeToByteArray())
    }

    private fun saveKeyData(keyId: KeyId, suffix: String, data: ByteArray): Unit =
        hkvs.storeDocument(
            path = Path(keysRoot.name, keyId.id, suffix),
            text = Base64.getEncoder().encodeToString(data)
        )

    private fun loadKey(keyId: String, suffix: String): ByteArray =
        Base64.getDecoder().decode(hkvs.loadDocument(Path(keysRoot.name, keyId, suffix)).toBytes())
}
