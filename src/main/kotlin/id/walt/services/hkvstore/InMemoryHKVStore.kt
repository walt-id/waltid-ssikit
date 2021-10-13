package id.walt.services.hkvstore

import kotlin.collections.HashMap

open class InMemoryHKVStore : HKVStoreService() {

    val store = HashMap<HKVKey, ByteArray>()

    override fun put(key: HKVKey, value: ByteArray) = store.set(key, value)

    override fun getAsByteArray(key: HKVKey): ByteArray? = store[key]

    private fun withParentKeys(key: HKVKey): Set<HKVKey> = when(key.parent) {
        null -> setOf(key)
        else -> setOf(key).plus(withParentKeys(key.parent!!))
    }

    private fun expandAllKeys() = store.keys.flatMap { withParentKeys(it) }.toSet()

    override fun listChildKeys(parent: HKVKey, recursive: Boolean): Set<HKVKey> = when {
        recursive -> store.keys.filter { it.startsWith(parent) }.toSet()
        else -> expandAllKeys().filter { it.parent == parent }.toSet()
    }

    override fun delete(key: HKVKey, recursive: Boolean): Boolean {
        if (recursive)
            listChildKeys(key, true).forEach { store.remove(it) }
        listKeyAliases(key).forEach { store.remove(it) }
        store.remove(key)
        return true
    }

    private fun listKeyAliases(key: HKVKey) = store.keys
        .filter { it.parent == HKVKey("keys", "alias") }.filter { getAsString(it) == key.name }.toSet()
}
