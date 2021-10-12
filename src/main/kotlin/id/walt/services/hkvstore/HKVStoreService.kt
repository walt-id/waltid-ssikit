package id.walt.services.hkvstore

import id.walt.servicematrix.ServiceProvider
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.WaltIdService
import java.nio.charset.StandardCharsets

/**
 * Hierarchical Kev Value Store Service
 */
abstract class HKVStoreService : WaltIdService() {
    override val implementation get() = ServiceRegistry.getService<HKVStoreService>()

    fun put(key: HKVKey, value: String) = this.put(key, value.toByteArray(StandardCharsets.UTF_8))

    abstract fun put(key: HKVKey, value: ByteArray)

    fun getAsString(key: HKVKey): String? = getAsByteArray(key)?.let { String(it) }

    abstract fun getAsByteArray(key: HKVKey): ByteArray?

    abstract fun listChildKeys(parent: HKVKey, recursive: Boolean = false): Set<HKVKey>

    abstract fun delete(key: HKVKey, recursive: Boolean = false): Boolean

    fun search(search: String): List<HKVKey> = listChildKeys(HKVKey("root"), true).filter { getAsString(it)!!.contains(search) }
}
