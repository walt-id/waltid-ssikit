package id.walt.services.hkvstore

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.crypto.SunCryptoService
import java.nio.charset.StandardCharsets

/**
 * Hierarchical Kev Value Store Service
 */
abstract class HKVStoreService : WaltIdService() {
    override val implementation get() = serviceImplementation<HKVStoreService>()

    open fun put(key: HKVKey, value: String) = this.put(key, value.toByteArray(StandardCharsets.UTF_8))

    /*abstract*/ open fun put(key: HKVKey, value: ByteArray): Unit = implementation.put(key, value)

    open fun getAsString(key: HKVKey): String? = getAsByteArray(key)?.let { String(it) }

    /*abstract*/ open fun getAsByteArray(key: HKVKey): ByteArray? = implementation.getAsByteArray(key)

    /*abstract*/ open fun listChildKeys(parent: HKVKey, recursive: Boolean = false): Set<HKVKey> =
        implementation.listChildKeys(parent, recursive)

    /*abstract*/ open fun delete(key: HKVKey, recursive: Boolean = false): Boolean = implementation.delete(key, recursive)

    open fun search(search: String): List<HKVKey> =
        listChildKeys(HKVKey("root"), true).filter { getAsString(it)!!.contains(search) }

    companion object : ServiceProvider {
        override fun getService() = object : HKVStoreService() {}
        override fun defaultImplementation() = FileSystemHKVStore(FilesystemStoreConfig("data"))
    }
}
