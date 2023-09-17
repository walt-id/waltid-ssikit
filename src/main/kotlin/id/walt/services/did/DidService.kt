package id.walt.services.did

import com.beust.klaxon.Klaxon
import com.github.benmanes.caffeine.cache.Caffeine
import com.nimbusds.jose.jwk.JWK
import id.walt.crypto.*
import id.walt.crypto.KeyAlgorithm.*
import id.walt.crypto.LdVerificationKeyType.*
import id.walt.model.*
import id.walt.model.did.DidEbsi
import id.walt.services.CryptoProvider
import id.walt.services.WaltIdServices
import id.walt.services.context.ContextManager
import id.walt.services.crypto.CryptoService
import id.walt.services.did.composers.DidEbsiV2DocumentComposer
import id.walt.services.did.composers.DidJwkDocumentComposer
import id.walt.services.did.composers.DidKeyDocumentComposer
import id.walt.services.did.factories.DidFactoryBase
import id.walt.services.did.resolvers.DidResolverFactory
import id.walt.services.ecosystems.iota.IotaWrapper
import id.walt.services.hkvstore.HKVKey
import id.walt.services.key.KeyService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.signatory.ProofConfig
import io.ipfs.multibase.Multibase
import mu.KotlinLogging
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.X509EncodedKeySpec
import java.time.Duration
import java.util.*


/**
 * W3C Decentralized Identity Service
 */
object DidService {

    private val log = KotlinLogging.logger {}

    private val DEFAULT_KEY_ALGORITHM = EdDSA_Ed25519

    private val credentialService = JsonLdCredentialService.getService()
    private val cryptoService = CryptoService.getService()
    val keyService = KeyService.getService()
    private val didResolverFactory = DidResolverFactory(
        httpNoAuth = WaltIdServices.httpNoAuth,
        keyService = keyService,
        iotaWrapper = IotaWrapper.createInstance(),
        didKeyDocumentComposer = DidKeyDocumentComposer(keyService),
        didJwkDocumentComposer = DidJwkDocumentComposer(),
        ebsiV2DocumentComposer = DidEbsiV2DocumentComposer(),
    )
    private val didCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofMinutes(10))
        .build<DidUrl, Did>()

    // Public methods

    fun getWebPathForDidWeb(didWebDomain: String, didWebPath: String?) = "https://$didWebDomain/.well-known" +
            (didWebPath?.replace(":", "/") ?: "").let {
                when {
                    it.endsWith("/") -> it
                    else -> "$it/"
                }
            }.let {
                when {
                    it.startsWith("/") -> it
                    else -> "/$it"
                }
            } +
            "did.json"

    // region did-create
    fun create(method: DidMethod, keyAlias: String? = null, options: DidOptions? = null): String =
        ensureKey(method, keyAlias).let {
            Pair(it.keyId, DidFactoryBase.new(method, keyService).create(it, options))
        }.also {
            addKeyAlias(it.first, it.second)
            storeDid(it.second)
        }.second.id
    //endregion

    //region did-load
    @Suppress("MemberVisibilityCanBePrivate")
    fun loadDid(didUrlStr: String): String? = ContextManager.hkvStore.getAsString(HKVKey("did", "created", didUrlStr))
    fun load(did: String): Did = load(DidUrl.from(did))
    fun load(didUrl: DidUrl): Did = didCache.get(didUrl) {
        Did.decode(
            loadDid(didUrl.did) ?: throw IllegalArgumentException("DID $didUrl could not be loaded/found.")
        ) ?: throw IllegalArgumentException("DID $didUrl could not be decoded.")
    }

    fun loadDidEbsi(did: String): DidEbsi = loadDidEbsi(DidUrl.from(did))
    private fun loadDidEbsi(didUrl: DidUrl): DidEbsi = load(didUrl.did) as DidEbsi
    //endregion

    //region did-delete
    fun deleteDid(didUrl: String) {
        loadOrResolveAnyDid(didUrl)?.let { did ->
            didCache.invalidate(did.url)
            ContextManager.hkvStore.delete(HKVKey("did", "created", didUrl), recursive = true)
            did.verificationMethod?.forEach {
                ContextManager.keyStore.delete(it.id)
            }
        }
    }
    //endregion

    //region did-update
    fun updateDidEbsi(did: DidEbsi) = storeDid(did)
    //endregion

    //region did-resolve
    fun resolve(did: String, options: DidOptions? = null): Did = resolve(DidUrl.from(did), options)
    fun resolve(didUrl: DidUrl, options: DidOptions? = null): Did =
        didResolverFactory.create(didUrl.method).resolve(didUrl, options)
    //endregion

    //region did-import
    fun importDid(did: String) {
        //val did2 = did.replace("-", ":") FIXME
        val did2 = did // FIXME
        resolveAndStore(did2)

        when {
            did.startsWith("did_web_") -> println("TODO(did:web implementation cannot yet load keys from web address (is dummy))")
            did.startsWith("did_ebsi_") -> println("TODO")
        }
    }

    fun importDidFromFile(file: File): String {
        if (!file.exists())
            throw Exception("DID doc file not found")
        val doc = file.readText(StandardCharsets.UTF_8)
        val did = Did.decode(doc)
        storeDid(did!!)
        return did.id
    }

    fun importDidAndKeys(did: String) {
        importDid(did)
        log.debug { "DID imported: $did" }

        importKeys(did)
        log.debug { "Key imported for: $did" }
    }
    //endregion

    fun listDids(): List<String> = ContextManager.hkvStore.listChildKeys(HKVKey("did", "created")).map { it.name }.toList()
    private fun resolveAndStore(didUrl: String) = storeDid(resolve(didUrl))

    @Suppress("MemberVisibilityCanBePrivate")
    fun storeDid(did: Did) {
        didCache.put(DidUrl.from(did.id), did)
        ContextManager.hkvStore.put(HKVKey("did", "created", did.id), did.encodePretty())
    }

    fun loadOrResolveAnyDid(didStr: String): Did? {
        log.debug { "Loading or resolving \"$didStr\"..." }
        val storedDid = loadDid(didStr)
        val url = runCatching { DidUrl.from(didStr) }.fold(onSuccess = { it }, onFailure = { null })
        log.debug { "loadOrResolve: url=${url ?: didStr}, length of stored=${storedDid?.length}" }
        return when (storedDid) {
            null -> resolve(didStr).also { did -> storeDid(did) }
            else -> Did.decode(storedDid)
        }
    }

    fun setKeyIdForDid(did: String, keyId: String) {
        val key = ContextManager.keyStore.load(keyId)
        log.debug { "Loaded key: $keyId" }

        ContextManager.keyStore.addAlias(key.keyId, did)

        val kid = did + "#" + key.keyId
        ContextManager.keyStore.addAlias(key.keyId, kid)
    }

    private fun signDid(issuerDid: String, verificationMethod: String, edDidStr: String): String {
        return credentialService.sign(
            edDidStr, ProofConfig(issuerDid = issuerDid, issuerVerificationMethod = verificationMethod)
        )
    }

    fun getAuthenticationMethods(did: String) = load(did).authentication

    //region key-import
    private fun tryImportVerificationKey(
        didUrl: String,
        verificationMethod: VerificationMethod,
        isMainMethod: Boolean
    ): Boolean {
        val keyService = KeyService.getService()
        if (!keyService.hasKey(verificationMethod.id)) {
            val keyId =
                tryImportJwk(didUrl, verificationMethod) ?: tryImportKeyBase58(didUrl, verificationMethod) ?: tryImportKeyPem(
                    didUrl,
                    verificationMethod
                ) ?: tryImportKeyMultibase(didUrl, verificationMethod) ?: return false
            ContextManager.keyStore.addAlias(keyId, verificationMethod.id)
            if (isMainMethod) {
                ContextManager.keyStore.addAlias(keyId, didUrl)
            }
        }
        return true
    }

    fun importKeys(didUrl: String): Boolean {
        val did = loadOrResolveAnyDid(didUrl) ?: throw IllegalArgumentException("Could not load or resolve $didUrl")

        return (did.verificationMethod ?: listOf())
            .asSequence()
            .plus(
                did.capabilityInvocation ?: listOf()
            ).plus(
                did.capabilityDelegation ?: listOf()
            ).plus(
                did.assertionMethod ?: listOf()
            ).plus(
                did.authentication ?: listOf()
            ).plus(
                did.keyAgreement ?: listOf()
            )
            .filter { vm -> !vm.isReference }
            .mapIndexed { idx, vm -> tryImportVerificationKey(didUrl, vm, idx == 0) }
            .reduce { acc, b -> acc || b }
    }

    private fun tryImportKeyPem(did: String, vm: VerificationMethod): KeyId? {

        vm.publicKeyPem ?: return null

        // TODO implement

        return null
    }

    private fun tryImportKeyBase58(did: String, vm: VerificationMethod): KeyId? {

        vm.publicKeyBase58 ?: return null

        if (vm.type !in setOf(
                Ed25519VerificationKey2018.name,
                Ed25519VerificationKey2019.name,
                Ed25519VerificationKey2020.name
            )
        ) {
            log.warn { "Key import does currently not support verification-key algorithm: ${vm.type}" }
            // TODO: support RSA and Secp256k1
            return null
        }

        val keyFactory = KeyFactory.getInstance("Ed25519")

        val pubKeyInfo =
            SubjectPublicKeyInfo(AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), vm.publicKeyBase58.decodeBase58())
        val x509KeySpec = X509EncodedKeySpec(pubKeyInfo.encoded)

        val pubKey = keyFactory.generatePublic(x509KeySpec)
        val keyId = KeyId(vm.id)
        ContextManager.keyStore.store(Key(keyId, EdDSA_Ed25519, CryptoProvider.SUN, KeyPair(pubKey, null)))

        return keyId
    }

    private fun tryImportKeyMultibase(did: String, vm: VerificationMethod): KeyId? {

        vm.publicKeyMultibase ?: return null

        if (vm.type !in setOf(
                Ed25519VerificationKey2018.name,
                Ed25519VerificationKey2019.name,
                Ed25519VerificationKey2020.name
            )
        ) {
            log.warn { "Key import does currently not support verification-key algorithm: ${vm.type}" }
            // TODO: support RSA and Secp256k1
            return null
        }

        val rawMultibaseDecoded = Multibase.decode(vm.publicKeyMultibase)

        val multibaseDecoded: ByteArray = when {
            rawMultibaseDecoded.size == 34 && did.startsWith("did:cheqd:") ->
                rawMultibaseDecoded.drop(2).toByteArray()

            else -> rawMultibaseDecoded
        }

        val keyFactory = KeyFactory.getInstance("Ed25519")

        val pubKeyInfo = SubjectPublicKeyInfo(AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), multibaseDecoded)

        val x509KeySpec = X509EncodedKeySpec(pubKeyInfo.encoded)

        val pubKey = keyFactory.generatePublic(x509KeySpec)
        val keyId = KeyId(vm.id)
        ContextManager.keyStore.store(Key(keyId, EdDSA_Ed25519, CryptoProvider.SUN, KeyPair(pubKey, null)))

        return keyId
    }

    private fun tryImportJwk(did: String, vm: VerificationMethod): KeyId? {

        vm.publicKeyJwk ?: return null

        log.debug { "Importing key: ${vm.id}" }
        val keyId = KeyService.getService().importKey(Klaxon().toJsonString(vm.publicKeyJwk))
        return keyId
    }

    fun importKeyForDidEbsiV2(did: String, key: JWK) {
        if (!isDidEbsiV2(did)) {
            throw IllegalArgumentException("Specified DID is not did:ebsi version 2")
        }
        val thumbprint = key.computeThumbprint()
        val generatedDid = DidUrl.generateDidEbsiV2DidUrl(thumbprint.decode())
        if (generatedDid.did != did) {
            throw IllegalArgumentException("did doesn't match specified key")
        }
        val vmId = "$did#$thumbprint"
        if (!keyService.hasKey(vmId)) {
            val keyId = keyService.importKey(key.toJSONString())
            keyService.addAlias(keyId, did)
            keyService.addAlias(keyId, "$did#$thumbprint")
        }
    }
    //endregion

    fun isDidEbsiV1(did: String): Boolean = checkIsDidEbsiAndVersion(did, 1)

    fun isDidEbsiV2(did: String): Boolean = checkIsDidEbsiAndVersion(did, 2)

    private fun checkIsDidEbsiAndVersion(did: String, version: Int): Boolean {
        return DidUrl.isDidUrl(did) &&
                DidUrl.from(did).let { didUrl ->
                    didUrl.method == DidMethod.ebsi.name && Multibase.decode(didUrl.identifier).first().toInt() == version
                }
    }

    private fun ensureKey(didMethod: DidMethod, keyAlias: String? = null): Key = when (didMethod) {
        DidMethod.iota -> EdDSA_Ed25519
//        DidMethod.key -> ECDSA_Secp256r1
        else -> DEFAULT_KEY_ALGORITHM
    }.let {
        keyAlias?.let { KeyId(it) } ?: cryptoService.generateKey(it)
    }.let {
        keyService.load(it.id)
    }

    private fun addKeyAlias(keyId: KeyId, did: Did) {
        runCatching { ContextManager.keyStore.load(keyId.id) }.onSuccess { _ ->
            log.debug { "A key with the id \"${keyId.id}\" exists." }
        }
        ContextManager.keyStore.addAlias(keyId, did.id)
        did.verificationMethod?.forEach { (id) ->
            ContextManager.keyStore.addAlias(keyId, id)
        }
    }

    // TODO: consider the methods below. They might be deprecated!

//    fun resolveDid(did: String): Did = resolveDid(did.DidUrl.toDidUrl())
//
//    fun resolveDid(didUrl: DidUrl): Did {
//        return when (didUrl.method) {
//            "key" -> resolveDidKey(didUrl)
//            "web" -> resolveDidWebDummy(didUrl)
//            else -> TODO("did:${didUrl.method} implemented yet")
//        }
//    }String

// TODO: include once working
//    internal fun resolveDidWeb(didUrl: DidUrl): DidWeb {
//        val domain = didUrl.identifier
//        val didUrl = "https://${domain}/.well-known/did.json" // FIXME: didUrl argument is ignored?
//        log.debug { "Resolving did:web for domain $domain at: $didUrl" }
//        val didWebStr = URL(didUrl).readText()
//        log.debug { "did:web resolved:\n$didWebStr" }
//        print(didWebStr)
//        val did = Klaxon().parse<DidWeb>(didWebStr)
//        log.debug { "did:web decoded:\n$did" }
//        return did
//    }

//    @Deprecated(message ="use create()")
//    fun createDid(didMethod: String, keys: Keys? = null): String {
//
//        val didKey = if (keys != null) keys else {
//            val keyId = KeyManagementService.generateEd25519KeyPairNimbus() // .generateKeyPair("Ed25519")
//            KeyManagementService.loadKeys(keyId)
//        }!!
//
//        return when (didMethod) {
//            "key" -> createDidKey(didKey)
//            "web" -> createDidWeb(didKey)
//            else -> T//ODO("did creation by method $didMethod not supported yet")
//        }
//    }

//    internal fun createDidKey(didKey: Keys): String {
//
//        val identifier = convertEd25519PublicKeyToMultiBase58Btc(didKey.getPubKey())
//
//        val did = "did:key:$identifier"
//
//        KeyManagementService.addAlias(didKey.keyId, did)
//
//        return did
//    }
//
//    internal fun createDidWeb(didKey: Keys): String {
//        val domain = "walt.id"
//        val username = UUID.randomUUID().toString().replace("-", "")
//        val path = ":user:$username"
//
//        val didUrl = DidUrl("web", "" + domain + path, didKey.keyId)
//
//        KeyManagementService.addAlias(didKey.keyId, didUrl.did)
//
//        return didUrl.did
//    }

}
