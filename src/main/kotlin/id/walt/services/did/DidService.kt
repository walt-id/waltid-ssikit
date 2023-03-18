package id.walt.services.did

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import id.walt.crypto.*
import id.walt.crypto.KeyAlgorithm.*
import id.walt.crypto.LdVerificationKeyType.*
import id.walt.model.*
import id.walt.model.did.DidEbsi
import id.walt.model.did.DidWeb
import id.walt.services.CryptoProvider
import id.walt.services.WaltIdServices
import id.walt.services.context.ContextManager
import id.walt.services.crypto.CryptoService
import id.walt.services.ecosystems.cheqd.CheqdService
import id.walt.services.ecosystems.iota.IotaService
import id.walt.services.hkvstore.HKVKey
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import id.walt.services.vc.JsonLdCredentialService
import id.walt.signatory.ProofConfig
import io.ipfs.multibase.Multibase
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.X509EncodedKeySpec
import java.util.*


/**
 * W3C Decentralized Identity Service
 */
object DidService {

    private val log = KotlinLogging.logger {}

    private val DEFAULT_KEY_ALGORITHM = EdDSA_Ed25519

    sealed class DidOptions
    data class DidWebOptions(val domain: String?, val path: String? = null) : DidOptions()
    data class DidEbsiOptions(val version: Int) : DidOptions()
    data class DidCheqdOptions(val network: String) : DidOptions()


    private val credentialService = JsonLdCredentialService.getService()
    private val cryptoService = CryptoService.getService()
    private val keyService = KeyService.getService()

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

    fun create(method: DidMethod, keyAlias: String? = null, options: DidOptions? = null): String {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        val didUrl = when (method) {
            DidMethod.key -> createDidKey(keyAlias)
            DidMethod.web -> createDidWeb(keyAlias,
                options?.let { it as DidWebOptions } ?: DidWebOptions("walt.id", UUID.randomUUID().toString()))

            DidMethod.ebsi -> createDidEbsi(keyAlias, options as? DidEbsiOptions)
            DidMethod.iota -> createDidIota(keyAlias)
            DidMethod.jwk -> createDidJwk(keyAlias)
            DidMethod.cheqd -> createDidCheqd(keyAlias, options as? DidCheqdOptions)
            else -> throw Exception("DID method $method not supported")
        }

        return didUrl
    }

    private fun createDidCheqd(keyAlias: String?, options: DidCheqdOptions?): String {
        val keyId = keyAlias?.let { KeyId(it) } ?: cryptoService.generateKey(EdDSA_Ed25519)
        val did = CheqdService.createDid(keyId.id, options?.network ?: "testnet")
        storeDid(did.id, did.encode())
        ContextManager.keyStore.addAlias(keyId, did.id)
        ContextManager.keyStore.addAlias(keyId, did.verificationMethod!![0].id)
        return did.id
    }

    fun resolve(did: String): Did = resolve(DidUrl.from(did))
    fun resolve(didUrl: DidUrl): Did {
        return when (didUrl.method) {
            DidMethod.key.name -> resolveDidKey(didUrl)
            DidMethod.web.name -> resolveDidWeb(didUrl)
            DidMethod.ebsi.name -> resolveDidEbsi(didUrl)
            DidMethod.jwk.name -> resolveDidJwk(didUrl)
            DidMethod.iota.name -> IotaService.resolveDid(didUrl.did) ?: throw Exception("Could not resolve $didUrl")
            DidMethod.cheqd.name -> CheqdService.resolveDid(didUrl.did)
            else -> TODO("did:${didUrl.method} not implemented yet")
        }
    }

    fun load(did: String): Did = load(DidUrl.from(did))
    fun load(didUrl: DidUrl): Did = Did.decode(
        loadDid(didUrl.did) ?: throw IllegalArgumentException("DID $didUrl not found.")
    ) ?: throw IllegalArgumentException("DID $didUrl not found.")

    fun resolveDidEbsiRaw(did: String): String = runBlocking {
        log.debug { "Resolving DID $did" }

        val didDoc = WaltIdServices.httpNoAuth.get("https://api-pilot.ebsi.eu/did-registry/v3/identifiers/$did").bodyAsText()

        log.debug { didDoc }

        return@runBlocking didDoc
    }

    fun resolveDidEbsi(did: String): DidEbsi {
        val didUrl = DidUrl.from(did)
        return resolveDidEbsi(didUrl)
    }

    fun resolveDidEbsi(didUrl: DidUrl): DidEbsi {
        val version = Multibase.decode(didUrl.identifier).first().toInt()
        return when (version) {
            1 -> resolveDidEbsiV1(didUrl)
            2 -> resolveDidEbsiV2(didUrl)
            else -> throw Exception("did:ebsi must have version 1 or 2")
        }
    }

    fun resolveDidEbsiV1(didUrl: DidUrl): DidEbsi = runBlocking {

        log.debug { "Resolving DID ${didUrl.did}..." }

        var didDoc: String
        var lastEx: ClientRequestException? = null

        for (i in 1..5) {
            try {
                log.debug { "Resolving did:ebsi at: https://api-pilot.ebsi.eu/did-registry/v3/identifiers/${didUrl.did}" }
                didDoc = WaltIdServices.httpNoAuth.get("https://api-pilot.ebsi.eu/did-registry/v3/identifiers/${didUrl.did}")
                    .bodyAsText()
                log.debug { "Result: $didDoc" }
                return@runBlocking Did.decode(didDoc)!! as DidEbsi
            } catch (e: ClientRequestException) {
                log.debug { "Resolving did ebsi failed: fail $i" }
                delay(1000)
                lastEx = e
            }
        }
        log.debug { "Could not resolve did ebsi!" }
        throw lastEx ?: Exception("Could not resolve did ebsi!")
    }

    fun resolveDidEbsiV2(didUrl: DidUrl): DidEbsi {
        val jwk = keyService.toJwk(didUrl.did)
        val vmId = "${didUrl.did}#${jwk.computeThumbprint()}"
        if (DidUrl.generateDidEbsiV2DidUrl(jwk.computeThumbprint().decode()).identifier != didUrl.identifier) {
            throw Exception("Public key doesn't match with DID identifier")
        }
        return DidEbsi(
            context = listOf("https://w3id.org/did/v1"),
            id = didUrl.did,
            verificationMethod = listOf(
                VerificationMethod(
                    id = vmId,
                    type = "JsonWebKey2020",
                    controller = didUrl.did,
                    publicKeyJwk = Klaxon().parse<Jwk>(jwk.toJSONString())
                )
            ),
            authentication = listOf(VerificationMethod.Reference(vmId)),
            assertionMethod = listOf(VerificationMethod.Reference(vmId))
        )
    }

    fun loadDidEbsi(did: String): DidEbsi = loadDidEbsi(DidUrl.from(did))
    fun loadDidEbsi(didUrl: DidUrl): DidEbsi = Did.decode(loadDid(didUrl.did)!!)!! as DidEbsi

    fun updateDidEbsi(did: DidEbsi) = storeDid(did.id, did.encode())

    private fun createDidEbsi(keyAlias: String?, didEbsiOptions: DidEbsiOptions?): String {
        val version = didEbsiOptions?.version ?: 1
        return when (version) {
            1 -> createDidEbsiV1(keyAlias)
            2 -> createDidEbsiV2(keyAlias)
            else -> throw Exception("Did ebsi version must be 1 or 2")
        }
    }

    private fun createDidEbsiV1(keyAlias: String?): String {
        val keyId = keyAlias?.let { KeyId(it) } ?: cryptoService.generateKey(DEFAULT_KEY_ALGORITHM)
        val key = ContextManager.keyStore.load(keyId.id)

        // Created identifier
        val didUrlStr = DidUrl.generateDidEbsiV1DidUrl().did

        ContextManager.keyStore.addAlias(keyId, didUrlStr)

        // Created DID doc
        val kid = didUrlStr + "#" + key.keyId
        ContextManager.keyStore.addAlias(keyId, kid)

        val verificationMethods = buildVerificationMethods(key, kid, didUrlStr)

        val did = DidEbsi(
            listOf(DID_CONTEXT_URL), // TODO Context not working "https://ebsi.org/ns/did/v1"
            didUrlStr,
            verificationMethods,
            listOf(VerificationMethod.Reference(kid)), listOf(VerificationMethod.Reference(kid))
        )
        val ebsiDid = did.encode()

        // Store DID
        storeDid(didUrlStr, ebsiDid)

        return didUrlStr
    }

    private fun createDidEbsiV2(keyAlias: String?): String {
        val keyId = keyAlias?.let { KeyId(it) } ?: cryptoService.generateKey(DEFAULT_KEY_ALGORITHM)
        val publicKeyJwk = keyService.toJwk(keyId.id, KeyType.PUBLIC)
        val publicKeyThumbprint = publicKeyJwk.computeThumbprint()
        val didUrlStr = DidUrl.generateDidEbsiV2DidUrl(publicKeyThumbprint.decode()).did
        val vmId = "$didUrlStr#$publicKeyThumbprint"
        ContextManager.keyStore.addAlias(keyId, didUrlStr)
        ContextManager.keyStore.addAlias(keyId, vmId)
        val didDoc = resolveDidEbsi(didUrlStr)
        storeDid(didUrlStr, didDoc.encode())
        return didUrlStr
    }

    private fun getPublicKeyBytesForDidKey(key: Key): ByteArray {
        return when (key.algorithm) {
            ECDSA_Secp256k1, ECDSA_Secp256r1 -> (key.getPublicKey() as BCECPublicKey).q.getEncoded(true)
            RSA, EdDSA_Ed25519 -> key.getPublicKeyBytes()
        }
    }

    private fun createDidKey(keyAlias: String?): String {
        val keyId = keyAlias?.let { KeyId(it) } ?: cryptoService.generateKey(DEFAULT_KEY_ALGORITHM)
        val key = ContextManager.keyStore.load(keyId.id)

        if (key.algorithm !in setOf(EdDSA_Ed25519, RSA, ECDSA_Secp256k1, ECDSA_Secp256r1))
            throw IllegalArgumentException("did:key can not be created with an ${key.algorithm} key.")

        val identifier = convertRawKeyToMultiBase58Btc(getPublicKeyBytesForDidKey(key), getMulticodecKeyCode(key.algorithm))

        val didUrl = "did:key:$identifier"

        runCatching {
            ContextManager.keyStore.load(keyId.id)
        }.onSuccess {
            if (it != null) {
                log.debug { "A key with the id \"${keyId.id}\" exists." }
                //throw IllegalArgumentException("A key with the id \"${keyId.id}\" already exists.")
            }
        }
        ContextManager.keyStore.addAlias(keyId, didUrl)

        val didDoc = resolve(didUrl)
        didDoc.verificationMethod?.forEach { (id) ->
            ContextManager.keyStore.addAlias(keyId, id)
        }
        storeDid(didUrl, didDoc.encodePretty())

        return didUrl
    }

    private fun createDidJwk(keyAlias: String?): String {
        val keyId = keyAlias?.let { KeyId(it) } ?: cryptoService.generateKey(DEFAULT_KEY_ALGORITHM)

        val identifier = Base64URL.encode(keyService.toJwk(keyId.id).toString())

        val didUrl = "did:jwk:$identifier"

        runCatching {
            ContextManager.keyStore.load(keyId.id)
        }.onSuccess {
            if (it != null) {
                log.debug { "A key with the id \"${keyId.id}\" exists." }
                //throw IllegalArgumentException("A key with the id \"${keyId.id}\" already exists.")
            }
        }
        ContextManager.keyStore.addAlias(keyId, didUrl)

        val didDoc = resolve(didUrl)
        didDoc.verificationMethod?.forEach { (id) ->
            ContextManager.keyStore.addAlias(keyId, id)
        }
        storeDid(didUrl, didDoc.encodePretty())

        return didUrl
    }

    private fun createDidWeb(keyAlias: String?, options: DidWebOptions?): String {

        options ?: throw Exception("DidWebOptions are mandatory")
        if (options.domain.isNullOrEmpty())
            throw IllegalArgumentException("Missing 'domain' parameter for creating did:web")

        val key = keyAlias?.let { ContextManager.keyStore.load(it) } ?: cryptoService.generateKey(DEFAULT_KEY_ALGORITHM)
            .let { ContextManager.keyStore.load(it.id) }

        val domain = URLEncoder.encode(options.domain, StandardCharsets.UTF_8)

        val path = when {
            options.path.isNullOrEmpty() -> ""
            else -> ":${
                options.path.split("/").joinToString(":") { part -> URLEncoder.encode(part, StandardCharsets.UTF_8) }
            }"
        }

        val didUrlStr = DidUrl("web", "$domain$path").did

        ContextManager.keyStore.addAlias(key.keyId, didUrlStr)

        // Created DID doc
        val kid = didUrlStr + "#" + key.keyId
        ContextManager.keyStore.addAlias(key.keyId, kid)

        val verificationMethods = buildVerificationMethods(key, kid, didUrlStr)


        val keyRef = listOf(VerificationMethod.Reference(kid))

        val didDoc = DidWeb(DID_CONTEXT_URL, didUrlStr, verificationMethods, keyRef, keyRef)

        storeDid(didUrlStr, didDoc.encode())

        return didUrlStr
    }

    private fun createDidIota(keyAlias: String?): String {
        // did:iota requires key of type EdDSA_Ed25519
        val keyId = keyAlias?.let { KeyId(it) } ?: cryptoService.generateKey(EdDSA_Ed25519)
        val didIota = IotaService.createDid(keyId.id)
        storeDid(didIota.id, didIota.encode())
        ContextManager.keyStore.addAlias(keyId, didIota.id)
        ContextManager.keyStore.addAlias(keyId, didIota.verificationMethod!![0].id)
        return didIota.id
    }

    private fun buildVerificationMethods(
        key: Key,
        kid: String,
        didUrlStr: String
    ): MutableList<VerificationMethod> {
        val keyType = when (key.algorithm) {
            EdDSA_Ed25519 -> Ed25519VerificationKey2019
            ECDSA_Secp256k1 -> EcdsaSecp256k1VerificationKey2019
            ECDSA_Secp256r1 -> EcdsaSecp256r1VerificationKey2019
            RSA -> RsaVerificationKey2018
        }
        log.debug { "Verification method JWK for kid: ${keyService.toJwk(kid)}" }
        log.debug { "Verification method public JWK: ${keyService.toJwk(kid).toPublicJWK()}" }
        log.debug {
            "Verification method parsed public JWK: ${
                Klaxon().parse<Jwk>(
                    keyService.toJwk(kid).toPublicJWK().toString()
                )
            }"
        }
        val publicKeyJwk = Klaxon().parse<Jwk>(keyService.toJwk(kid).toPublicJWK().toString())

        val verificationMethods = mutableListOf(
            VerificationMethod(kid, keyType.name, didUrlStr, null, null, publicKeyJwk),
        )
        return verificationMethods
    }

    fun importDid(did: String) {
        val did2 = did.replace("-", ":")
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
        storeDid(did!!.id, doc)
        return did.id
    }

    fun importDidAndKeys(did: String) {
        importDid(did)
        log.debug { "DID imported: $did" }

        importKeys(did)
        log.debug { "Key imported for: $did" }
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

    private fun resolveDidWeb(didUrl: DidUrl): Did = runBlocking {
        log.debug { "Resolving DID $didUrl" }

        val didDocUri = DidWeb.getDidDocUri(didUrl)

        log.debug { "Fetching DID from $didDocUri" }

        val didDoc = WaltIdServices.httpNoAuth.get(didDocUri.toString()).bodyAsText()

        log.debug { didDoc }

        return@runBlocking Did.decode(didDoc)!!
    }

    private fun resolveDidKey(didUrl: DidUrl): Did {
        val keyAlgorithm = getKeyAlgorithmFromMultibase(didUrl.identifier)

        val pubKey = convertMultiBase58BtcToRawKey(didUrl.identifier)

        return constructDidKey(didUrl, pubKey, keyAlgorithm)
    }

    private fun resolveDidJwk(didUrl: DidUrl): Did {
        return Did(
            context = listOf("https://www.w3.org/ns/did/v1", "https://w3id.org/security/suites/jws-2020/v1"),
            id = didUrl.did,
            verificationMethod = listOf(
                VerificationMethod(
                    id = "${didUrl.did}#0",
                    type = "JsonWebKey2020",
                    controller = didUrl.did,
                    publicKeyJwk = Klaxon().parse<Jwk>(Base64URL.from(didUrl.identifier).decodeToString())
                )
            ),
            assertionMethod = listOf(VerificationMethod.Reference("${didUrl.did}#0")),
            authentication = listOf(VerificationMethod.Reference("${didUrl.did}#0")),
            capabilityInvocation = listOf(VerificationMethod.Reference("${didUrl.did}#0")),
            capabilityDelegation = listOf(VerificationMethod.Reference("${didUrl.did}#0")),
            keyAgreement = listOf(VerificationMethod.Reference("${didUrl.did}#0"))
        )
    }

    private fun ebsiDid(didUrl: DidUrl, pubKey: ByteArray): DidEbsi {
        val (keyAgreementKeys, verificationMethods, keyRef) = generateEdParams(pubKey, didUrl)

        // TODO Replace EIDAS dummy certificate with real one
//        {
//            "id": "did:ebsi:2b6a1ee5881158edf133421d63d4b9e5f3ac26d474c#key-3",
//            "type": "EidasVerificationKey2021",
//            "controller": "did:ebsi:2b6a1ee5881158edf133421d63d4b9e5f3ac26d472afcff8",
//            "publicKeyPem": "-----BEGIN.."
//        }

        val eidasKeyId = didUrl.identifier + "#" + UUID.randomUUID().toString().replace("-", "")
        verificationMethods.add(
            VerificationMethod(
                eidasKeyId, "EidasVerificationKey2021", "publicKeyPem", "-----BEGIN.."
            )
        )

        return DidEbsi(
            listOf(DID_CONTEXT_URL), // TODO Context not working "https://ebsi.org/ns/did/v1"
            didUrl.did, verificationMethods, keyRef, keyRef, keyRef, verificationMethods, keyAgreementKeys, null
        )
    }

    private fun constructDidKey(didUrl: DidUrl, pubKey: ByteArray, keyAlgorithm: KeyAlgorithm): Did {

        val (keyAgreementKeys, verificationMethods, keyRef) = when (keyAlgorithm) {
            EdDSA_Ed25519 -> generateEdParams(pubKey, didUrl)
            ECDSA_Secp256r1, ECDSA_Secp256k1 -> generateEcKeyParams(pubKey, didUrl, keyAlgorithm)
            RSA -> generateRSAKeyParams(pubKey, didUrl)
        }

        return Did(
            context = DID_CONTEXT_URL,
            id = didUrl.did,
            verificationMethod = verificationMethods,
            authentication = keyRef,
            assertionMethod = keyRef,
            capabilityDelegation = keyRef,
            capabilityInvocation = keyRef,
            keyAgreement = keyAgreementKeys,
            serviceEndpoint = null
        )
    }

    private fun generateRSAKeyParams(
        pubKey: ByteArray, didUrl: DidUrl
    ): Triple<List<VerificationMethod>?, MutableList<VerificationMethod>, List<VerificationMethod>> {

        val pubKeyId = didUrl.did + "#" + didUrl.identifier

        val verificationMethods = mutableListOf(
            VerificationMethod(pubKeyId, RsaVerificationKey2018.name, didUrl.did, pubKey.encodeBase58()),
        )

        val keyRef = listOf(VerificationMethod.Reference(pubKeyId))
        return Triple(null, verificationMethods, keyRef)
    }

    private fun generateEdParams(
        pubKey: ByteArray, didUrl: DidUrl
    ): Triple<List<VerificationMethod>?, MutableList<VerificationMethod>, List<VerificationMethod>> {
        val dhKey = convertPublicKeyEd25519ToCurve25519(pubKey)

        val dhKeyMb = convertX25519PublicKeyToMultiBase58Btc(dhKey)

        val pubKeyId = didUrl.did + "#" + didUrl.identifier
        val dhKeyId = didUrl.did + "#" + dhKeyMb

        val verificationMethods = mutableListOf(
            VerificationMethod(pubKeyId, Ed25519VerificationKey2019.name, didUrl.did, pubKey.encodeBase58()),
            VerificationMethod(dhKeyId, "X25519KeyAgreementKey2019", didUrl.did, dhKey.encodeBase58())
        )

        return Triple(
            listOf(VerificationMethod.Reference(dhKeyId)),
            verificationMethods,
            listOf(VerificationMethod.Reference(pubKeyId))
        )
    }

    private fun generateEcKeyParams(
        pubKey: ByteArray, didUrl: DidUrl, algorithm: KeyAlgorithm
    ): Triple<List<VerificationMethod>?, MutableList<VerificationMethod>, List<VerificationMethod>> {
        val curve = if (algorithm == ECDSA_Secp256k1) Curve.SECP256K1 else Curve.P_256
        val vmType =
            if (algorithm == ECDSA_Secp256k1) EcdsaSecp256k1VerificationKey2019.name else EcdsaSecp256r1VerificationKey2019.name

        val uncompressedPubKey = uncompressSecp256k1(pubKey, curve) ?: throw Exception("Error uncompressing public key bytes")
        val key = Key(newKeyId(), algorithm, CryptoProvider.SUN, KeyPair(uncompressedPubKey.toECPublicKey(), null))
        val pubKeyId = didUrl.did + "#" + didUrl.identifier

        val verificationMethods = mutableListOf(
            VerificationMethod(
                pubKeyId,
                vmType,
                didUrl.did,
                publicKeyJwk = Klaxon().parse<Jwk>(keyService.toSecp256Jwk(key, curve, key.keyId.id).toJSONString())
            )
        )

        return Triple(
            null,
            verificationMethods,
            listOf(VerificationMethod.Reference(pubKeyId))
        )
    }

    fun getAuthenticationMethods(did: String) = load(did).authentication

    private fun resolveAndStore(didUrl: String) = storeDid(didUrl, resolve(didUrl).encodePretty())

    fun storeDid(didUrlStr: String, didDoc: String) = ContextManager.hkvStore.put(HKVKey("did", "created", didUrlStr), didDoc)

    private fun loadDid(didUrlStr: String): String? = ContextManager.hkvStore.getAsString(HKVKey("did", "created", didUrlStr))


    fun listDids(): List<String> = ContextManager.hkvStore.listChildKeys(HKVKey("did", "created")).map { it.name }.toList()

    fun loadOrResolveAnyDid(didStr: String): Did? {
        log.debug { "Loading or resolving \"$didStr\"..." }
        val url = DidUrl.from(didStr)
        val storedDid = loadDid(didStr)

        log.debug { "loadOrResolve: url=$url, length of stored=${storedDid?.length}" }
        return when (storedDid) {
            null -> resolve(didStr).also { did ->
                storeDid(didStr, did.encodePretty())
            }

            else -> Did.decode(storedDid)
        }
    }

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
        val did = loadOrResolveAnyDid(didUrl) ?: throw Exception("Could not load or resolve $didUrl")

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

    fun deleteDid(didUrl: String) {
        loadOrResolveAnyDid(didUrl)?.let { did ->
            ContextManager.hkvStore.delete(HKVKey("did", "created", didUrl), recursive = true)
            did.verificationMethod?.forEach {
                ContextManager.keyStore.delete(it.id)
            }
        }
    }

    private fun tryImportKeyPem(did: String, vm: VerificationMethod): KeyId? {

        vm.publicKeyPem ?: return null

        // TODO implement

        return null
    }

    private fun tryImportKeyBase58(did: String, vm: VerificationMethod): KeyId? {

        vm.publicKeyBase58 ?: return null

        if (!setOf(Ed25519VerificationKey2018.name, Ed25519VerificationKey2019.name, Ed25519VerificationKey2020.name).contains(
                vm.type
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

        if (!setOf(Ed25519VerificationKey2018.name, Ed25519VerificationKey2019.name, Ed25519VerificationKey2020.name).contains(
                vm.type
            )
        ) {
            log.warn { "Key import does currently not support verification-key algorithm: ${vm.type}" }
            // TODO: support RSA and Secp256k1
            return null
        }

        val keyFactory = KeyFactory.getInstance("Ed25519")

        val pubKeyInfo =
            SubjectPublicKeyInfo(AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), Multibase.decode(vm.publicKeyMultibase))
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
            throw Exception("Specified DID is not did:ebsi version 2")
        }
        val thumbprint = key.computeThumbprint()
        val generatedDid = DidUrl.generateDidEbsiV2DidUrl(thumbprint.decode())
        if (generatedDid.did != did) {
            throw Exception("did doesn't match specified key")
        }
        val vmId = "$did#$thumbprint"
        if (!keyService.hasKey(vmId)) {
            val keyId = keyService.importKey(key.toJSONString())
            keyService.addAlias(keyId, did)
            keyService.addAlias(keyId, "$did#$thumbprint")
        }
    }

    fun isDidEbsiV2(did: String): Boolean {
        return DidUrl.isDidUrl(did) &&
                DidUrl.from(did).let { didUrl ->
                    didUrl.method == DidMethod.ebsi.name && Multibase.decode(didUrl.identifier).first().toInt() == 2
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


