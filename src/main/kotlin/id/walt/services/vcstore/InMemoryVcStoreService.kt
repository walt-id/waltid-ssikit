package id.walt.services.vcstore

import id.walt.vclib.model.VerifiableCredential

open class InMemoryVcStoreService : VcStoreService() {

    val store = HashMap<String, VerifiableCredential>()

    private fun getKey(id: String, group: String) = "${group}/${id}"

    override fun getCredential(id: String, group: String): VerifiableCredential = store[getKey(id, group)]!!

    override fun listCredentials(group: String): List<VerifiableCredential> = listCredentialIds(group).map { store[it]!! }.toList()

    override fun listCredentialIds(group: String): List<String> = store.keys.filter { it.startsWith("${group}/") }.toList()

    override fun storeCredential(alias: String, vc: VerifiableCredential, group: String) {
        store[getKey(alias, group)] = vc
    }

    override fun deleteCredential(alias: String, group: String) = store.remove(getKey(alias, group)) != null
}
