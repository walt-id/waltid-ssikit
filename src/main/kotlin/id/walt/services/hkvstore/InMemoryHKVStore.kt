package id.walt.services.hkvstore

import java.nio.file.Path

open class InMemoryHKVStore : HierarchicalKeyValueStoreService() {
    val store = HashMap<Path, ByteArray>()

    override fun put(key: Path, value: ByteArray) = store.set(key, value)

    override fun getAsByteArray(key: Path): ByteArray = store[key]!!

    override fun getChildKeys(parent: Path, recursive: Boolean): Set<Path> = when {
        recursive -> store.keys.filter { it.count() > parent.count() && it.subpath(0, parent.count()).equals(parent) }.toSet()
        else -> store.keys.filter { it.parent == parent }.toSet()
    }

    override fun delete(key: Path, recursive: Boolean): Boolean {
        if (recursive)
            getChildKeys(key, true).forEach { delete(it, true) }
        store.remove(key)
        return true
    }
}
