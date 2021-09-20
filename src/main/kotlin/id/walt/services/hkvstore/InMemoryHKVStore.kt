package id.walt.services.hkvstore

import java.nio.file.Path

open class InMemoryHKVStore : HKVStoreService() {
    val store = HashMap<HKVKey, ByteArray>()

    override fun put(key: HKVKey, value: ByteArray) = store.set(key, value)

    override fun getAsByteArray(key: HKVKey): ByteArray = store[key]!!

    override fun listChildKeys(parent: HKVKey, recursive: Boolean): Set<HKVKey> = when {
        recursive -> store.keys.filter { it.startsWith(parent) }.toSet()
        else -> store.keys.filter { it.parent == parent }.toSet()
    }

    override fun delete(key: HKVKey, recursive: Boolean): Boolean {
        if (recursive)
            listChildKeys(key, true).forEach { store.remove(it) }
        store.remove(key)
        return true
    }
}
