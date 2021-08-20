package id.walt.services.vcstore

import id.walt.vclib.model.VerifiableCredential

open class InMemoryVcStoreService : VcStoreService() {

    val store = HashMap<String, VerifiableCredential>()

    override fun getCredential(id: String): VerifiableCredential = store[id]!!

    override fun listCredentials(): List<VerifiableCredential> = store.values.toList()

    override fun listCredentialIds(): List<String> = store.keys.toList()

    override fun storeCredential(alias: String, vc: VerifiableCredential) {
        store[alias] = vc
    }

    override fun deleteCredential(alias: String) = store.remove(alias) != null
}
