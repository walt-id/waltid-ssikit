package id.walt.services.hkvstore

import id.walt.servicematrix.ServiceProvider
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.WaltIdService
import java.nio.charset.StandardCharsets
import java.nio.file.Path

abstract class HierarchicalKeyValueStoreService : WaltIdService() {
    override val implementation get() = ServiceRegistry.getService<HierarchicalKeyValueStoreService>()

    fun put(key: Path, value: String) {
        this.put(key, value.toByteArray(StandardCharsets.UTF_8))
    }

    abstract fun put(key: Path, value: ByteArray)

    fun getAsString(key: Path): String {
        return String(getAsByteArray(key), StandardCharsets.UTF_8)
    }

    abstract fun getAsByteArray(key: Path): ByteArray

    abstract fun listKeys(parent: Path, recursive: Boolean = false): Set<Path>

    abstract fun delete(key: Path, recursive: Boolean = false)

    companion object : ServiceProvider {
        val implementation get() = ServiceRegistry.getService<HierarchicalKeyValueStoreService>()
        override fun getService(): HierarchicalKeyValueStoreService {
            return implementation
        }
    }
}