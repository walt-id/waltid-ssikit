package id.walt.custodian

import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyStoreService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import id.walt.services.vcstore.VcStoreService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.VcLibManager
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.VerifiablePresentation
import mu.KotlinLogging
import java.util.*

private val log = KotlinLogging.logger {}

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

    open fun createPresentation(
        vcs: List<String>, holderDid: String, verifierDid: String?, domain: String?, challenge: String?
    ): String = implementation.createPresentation(vcs, holderDid, verifierDid, domain, challenge)

    companion object : ServiceProvider {
        override fun getService() = object : CustodianService() {}
    }
}

open class WaltCustodianService : CustodianService() {

    private val keyService = KeyService.getService()
    private val keystore = KeyStoreService.getService()
    private val vcStore = VcStoreService.getService()
    private val jwtCredentialService = JwtCredentialService.getService()
    private val jsonLdCredentialService = JsonLdCredentialService.getService()

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

    override fun createPresentation(
        vcs: List<String>, holderDid: String, verifierDid: String?, domain: String?, challenge: String?
    ): String = when {
        vcs.stream().allMatch { VcLibManager.isJWT(it) } -> jwtCredentialService.present(vcs, holderDid, verifierDid!!, challenge!!)
        vcs.stream().noneMatch { VcLibManager.isJWT(it) } -> jsonLdCredentialService.present(vcs, holderDid, domain, challenge)
        else -> throw IllegalStateException("All verifiable credentials must be of the same proof type.")
    }
}

