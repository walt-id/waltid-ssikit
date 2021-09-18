package id.walt.services.hkvstore

import id.walt.servicematrix.ServiceProvider
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.WaltIdService
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Hierarchical Kev Value Store Service
 */
abstract class HKVStoreService : WaltIdService() {
    override val implementation get() = ServiceRegistry.getService<HKVStoreService>()

    fun put(key: Path, value: String) = this.put(key, value.toByteArray(StandardCharsets.UTF_8))

    abstract fun put(key: Path, value: ByteArray)

    fun getAsString(key: Path): String = String(getAsByteArray(key), StandardCharsets.UTF_8)

    abstract fun getAsByteArray(key: Path): ByteArray

    abstract fun listChildKeys(parent: Path, recursive: Boolean = false): Set<Path>

    abstract fun delete(key: Path, recursive: Boolean = false): Boolean

    companion object : ServiceProvider {
        val implementation get() = ServiceRegistry.getService<HKVStoreService>()
        override fun getService(): HKVStoreService {
            return implementation
        }
    }
}
