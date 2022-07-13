package id.walt.custodian

import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.context.ContextManager
import id.walt.services.key.KeyFormat
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import id.walt.vclib.model.VerifiableCredential
import java.time.Instant

abstract class Custodian : WaltIdService() {
    override val implementation get() = serviceImplementation<Custodian>()

    open fun generateKey(keyAlgorithm: KeyAlgorithm): Key = implementation.generateKey(keyAlgorithm)
    open fun getKey(alias: String): Key = implementation.getKey(alias)
    open fun listKeys(): List<Key> = implementation.listKeys()
    open fun importKey(keyStr: String): KeyId = implementation.importKey(keyStr)
    open fun deleteKey(id: String): Unit = implementation.deleteKey(id)
    open fun exportKey(keyAlias: String, format: KeyFormat = KeyFormat.JWK, type: KeyType = KeyType.PUBLIC): String =
        implementation.exportKey(keyAlias, format, type)

    open fun getCredential(id: String): VerifiableCredential? = implementation.getCredential(id)
    open fun listCredentials(): List<VerifiableCredential> = implementation.listCredentials()
    open fun listCredentialIds(): List<String> = implementation.listCredentialIds()
    open fun storeCredential(alias: String, vc: VerifiableCredential): Unit = implementation.storeCredential(alias, vc)
    open fun deleteCredential(alias: String): Boolean = implementation.deleteCredential(alias)

    open fun createPresentation(
        vcs: List<String>,
        holderDid: String,
        verifierDid: String? = null,
        domain: String? = null,
        challenge: String? = null
        , expirationDate: Instant?
    ): String = implementation.createPresentation(vcs, holderDid, verifierDid, domain, challenge, expirationDate)

    companion object : ServiceProvider {
        override fun getService() = object : Custodian() {}
        override fun defaultImplementation() = WaltIdCustodian()
    }
}

open class WaltIdCustodian : Custodian() {
    private val VC_GROUP = "custodian"
    private val keyService = KeyService.getService()
    private val jwtCredentialService = JwtCredentialService.getService()
    private val jsonLdCredentialService = JsonLdCredentialService.getService()

    override fun generateKey(keyAlgorithm: KeyAlgorithm): Key =
        ContextManager.keyStore.load(keyService.generate(keyAlgorithm).id)

    override fun getKey(alias: String): Key = ContextManager.keyStore.load(alias)
    override fun listKeys(): List<Key> = ContextManager.keyStore.listKeys()
    override fun importKey(keyStr: String) = keyService.importKey(keyStr)
    override fun deleteKey(id: String) = ContextManager.keyStore.delete(id)
    override fun exportKey(keyAlias: String, format: KeyFormat, type: KeyType) =
        keyService.export(keyAlias, format, type)

    override fun getCredential(id: String) = ContextManager.vcStore.getCredential(id, VC_GROUP)
    override fun listCredentials(): List<VerifiableCredential> = ContextManager.vcStore.listCredentials(VC_GROUP)
    override fun listCredentialIds(): List<String> = ContextManager.vcStore.listCredentialIds(VC_GROUP)
    override fun storeCredential(alias: String, vc: VerifiableCredential) =
        ContextManager.vcStore.storeCredential(alias, vc, VC_GROUP)

    override fun deleteCredential(alias: String) = ContextManager.vcStore.deleteCredential(alias, VC_GROUP)

    override fun createPresentation(
        vcs: List<String>,
        holderDid: String,
        verifierDid: String?,
        domain: String?,
        challenge: String?,
        expirationDate: Instant?
    ) = when {
        vcs.stream().allMatch { VerifiableCredential.isJWT(it) } -> jwtCredentialService.present(vcs, holderDid, verifierDid, challenge, expirationDate)
        vcs.stream().noneMatch { VerifiableCredential.isJWT(it) } -> jsonLdCredentialService.present(vcs, holderDid, domain, challenge, expirationDate)
        else -> throw IllegalStateException("All verifiable credentials must be of the same proof type.")
    }
}

