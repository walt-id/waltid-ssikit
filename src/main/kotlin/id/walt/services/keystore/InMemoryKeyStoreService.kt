package id.walt.services.keystore

import id.walt.services.hkvstore.InMemoryHKVStore

class InMemoryKeyStoreService : HKVKeyStoreService() {

    private val inMemoryHKVStore = InMemoryHKVStore()

    override val hkvStore get() = inMemoryHKVStore
}