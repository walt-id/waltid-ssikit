package id.walt.custodian

import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyStoreService
import id.walt.services.vcstore.VcStoreService
import id.walt.vclib.model.VerifiableCredential

abstract class CustodianService : WaltIdService() {
    override val implementation get() = serviceImplementation<CustodianService>()

    open fun generateKey(keyAlgorithm: KeyAlgorithm): Key = implementation.generateKey(keyAlgorithm)
    open fun getKey(alias: String): Key = implementation.getKey(alias)
    open fun listKeys(): List<Key> = implementation.listKeys()
    open fun storeKey(key: Key): Unit = implementation.storeKey(key)
    open fun deleteKey(id: String): Unit = implementation.deleteKey(id)

    open fun getCredential(id: String): VerifiableCredential = implementation.getCredential(id)
    open fun listCredentials(): List<VerifiableCredential> = implementation.listCredentials()
    open fun listCredentialIds(): List<String> = implementation.listCredentialIds()
    open fun storeCredential(alias: String, vc: VerifiableCredential): Unit = implementation.storeCredential(alias, vc)
    open fun deleteCredential(alias: String): Boolean = implementation.deleteCredential(alias)

    // fun createPresentation()

    companion object : ServiceProvider {
        override fun getService() = object : CustodianService() {}
    }
}

open class WaltCustodianService : CustodianService() {

    private val keyService = KeyService.getService()
    private val keystore = KeyStoreService.getService()
    private val vcStore = VcStoreService.getService()

    override fun generateKey(keyAlgorithm: KeyAlgorithm): Key = keystore.load(keyService.generate(keyAlgorithm).id)
    override fun getKey(alias: String): Key = keystore.load(alias)
    override fun listKeys(): List<Key> = keystore.listKeys()
    override fun storeKey(key: Key) = keystore.store(key)
    override fun deleteKey(id: String) = keystore.delete(id)

    override fun getCredential(id: String) = vcStore.getCredential(id)
    override fun listCredentials(): List<VerifiableCredential> = vcStore.listCredentials()
    override fun listCredentialIds(): List<String> = vcStore.listCredentialIds()
    override fun storeCredential(alias: String, vc: VerifiableCredential) = vcStore.storeCredential(alias, vc)
    override fun deleteCredential(alias: String) = vcStore.deleteCredential(alias)
}

