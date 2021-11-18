package id.walt.services.vcstore

import id.walt.services.context.ContextManager
import id.walt.services.hkvstore.HKVKey
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.model.VerifiableCredential


class HKVVcStoreService : VcStoreService() {

    private val hkvStore
        get() =  ContextManager.hkvStore // lazy load!

    private val vcRoot = "vc"

    private fun getGroupRoot(group: String): HKVKey = HKVKey(vcRoot, group)

    private fun getStoreKeyFor(id: String, group: String): HKVKey =
        HKVKey(vcRoot, group, id)

    override fun getCredential(id: String, group: String): VerifiableCredential? =
        hkvStore.getAsString(getStoreKeyFor(id, group))?.let { it.toCredential() }

    override fun listCredentials(group: String): List<VerifiableCredential> =
        listCredentialIds(group).map { hkvStore.getAsString(getStoreKeyFor(it, group))!!.toCredential() }

    override fun listCredentialIds(group: String): List<String> =
        hkvStore.listChildKeys(getGroupRoot(group)).map { it.name }

    override fun storeCredential(alias: String, vc: VerifiableCredential, group: String) =
        hkvStore.put(getStoreKeyFor(alias, group), vc.encode())

    override fun deleteCredential(alias: String, group: String): Boolean = hkvStore.delete(getStoreKeyFor(alias, group))
}
