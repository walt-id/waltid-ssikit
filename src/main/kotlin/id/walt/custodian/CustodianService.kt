package id.walt.custodian

import id.walt.crypto.Key
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.keystore.KeyStoreService
import id.walt.services.vcstore.VcStoreService
import id.walt.vclib.model.VerifiableCredential

abstract class CustodianService : WaltIdService() {
    override val implementation get() = serviceImplementation<CustodianService>()

    open fun getKey(alias: String): Key = implementation.getKey(alias)
    open fun listKeys(): List<Key> = implementation.listKeys()
    open fun storeKey(key: Key): Unit = implementation.storeKey(key)

    open fun getCredential(id: String): VerifiableCredential = implementation.getCredential(id)
    open fun listCredentials(): List<VerifiableCredential> = implementation.listCredentials()
    open fun listCredentialIds(): List<String> = implementation.listCredentialIds()
    open fun storeCredential(alias: String, vc: VerifiableCredential): Unit = implementation.storeCredential(alias, vc)

    // fun createPresentation()

    companion object : ServiceProvider {
        override fun getService() = object : CustodianService() {}
    }
}

open class WaltCustodianService : CustodianService() {

    private val keystore = KeyStoreService.getService()
    private val vcstore = VcStoreService.getService()

    override fun getKey(alias: String): Key = keystore.load(alias)
    override fun listKeys(): List<Key> = keystore.listKeys()
    override fun storeKey(key: Key) = keystore.store(key)

    override fun getCredential(id: String) = vcstore.getCredential(id)
    override fun listCredentials(): List<VerifiableCredential> = vcstore.listCredentials()
    override fun listCredentialIds(): List<String> = vcstore.listCredentialIds()
    override fun storeCredential(alias: String, vc: VerifiableCredential) = vcstore.storeCredential(alias, vc)
}

