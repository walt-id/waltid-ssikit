package id.walt.services.keystore

import id.walt.crypto.Key
import id.walt.crypto.KeyId
import id.walt.custodian.Custodian

class CustodianKeyStoreService : KeyStoreService() {

    private val custodian = Custodian.getService()

    override fun store(key: Key): Unit = custodian.storeKey(key)
    override fun load(alias: String, keyType: KeyType): Key = custodian.getKey(alias)
    override fun addAlias(keyId: KeyId, alias: String): Unit = TODO("Not implemented")
    override fun delete(alias: String): Unit = custodian.deleteKey(alias)
    override fun listKeys(): List<Key> = custodian.listKeys()

}
