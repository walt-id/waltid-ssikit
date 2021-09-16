package id.walt.services.hkvstore

import java.nio.file.Path

open class InMemoryHKVStore : HierarchicalKeyValueStoreService() {
    val store = HashMap<Path, ByteArray>()

    override fun put(key: Path, value: ByteArray) {
        store[key] = value
    }

    override fun getAsByteArray(key: Path): ByteArray {
        return store[key]!!
    }

    override fun listKeys(parent: Path, recursive: Boolean): Set<Path> {
        if(recursive) {
            return store.keys.filter { it.count() > parent.count() && it.subpath(0, parent.count()).equals(parent) }.toSet()
        }
        return store.keys.filter { it.parent.equals(parent) }.toSet()
    }

    override fun delete(key: Path, recursive: Boolean) {
        if(recursive) {
            val children = listKeys(key, true)
            if(children.isNotEmpty()) {
                children.forEach { delete(it, true) }
            }
        }
        store.remove(key)
    }


}