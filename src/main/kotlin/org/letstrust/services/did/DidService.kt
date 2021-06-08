package org.letstrust.services.did

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.bouncycastle.asn1.ASN1BitString
import org.bouncycastle.asn1.ASN1Sequence
import org.letstrust.LetsTrustServices
import org.letstrust.crypto.*
import org.letstrust.crypto.KeyAlgorithm.ECDSA_Secp256k1
import org.letstrust.crypto.KeyAlgorithm.EdDSA_Ed25519
import org.letstrust.crypto.keystore.KeyStore
import org.letstrust.model.*
import org.letstrust.services.key.KeyService
import org.letstrust.services.vc.CredentialService
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.streams.toList

private val log = KotlinLogging.logger {}

/**
 * W3C Decentralized Identity Service
 */
object DidService {

    private val cryptoService = LetsTrustServices.load<CryptoService>()
    private val keyStore = LetsTrustServices.load<KeyStore>()

    // Public methods

    fun create(method: DidMethod, keyAlias: String? = null): String {
        val didUrl = when (method) {
            DidMethod.key -> createDidKey(keyAlias)
            DidMethod.web -> createDidWeb(keyAlias)
            DidMethod.ebsi -> createDidEbsi(keyAlias)
            else -> throw Exception("DID method $method not supported")
        }

        return didUrl
    }

    fun resolve(did: String): Did = resolve(DidUrl.from(did))
    fun resolve(didUrl: DidUrl): Did {
        return when (didUrl.method) {
            DidMethod.key.name -> resolveDidKey(didUrl)
            DidMethod.ebsi.name -> resolveDidKey(didUrl)
            DidMethod.web.name -> resolveDidWebDummy(didUrl)
            else -> TODO("did:${didUrl.method} not implemented yet")
        }
    }

    fun resolveDidEbsi(did: String): DidEbsi = resolveDidEbsi(DidUrl.from(did))
    fun resolveDidEbsi(didUrl: DidUrl): DidEbsi {
        log.warn { "DID EBSI is not resolved correctly yet. It is read from directory." }
        return Json.decodeFromString<DidEbsi>(loadDid(didUrl.did))
    }

    fun updateDidEbsi(did: DidEbsi) = storeDid(did.id!!, Json.encodeToString(did))
    // Private methods

    private fun createDidEbsi(keyAlias: String?): String {
        val keyId = keyAlias?.let { KeyId(it) } ?: cryptoService.generateKey(EdDSA_Ed25519)
        val key = keyStore.load(keyId.id)

        // Created identifier
        val didUrlStr = DidUrl.generateDidEbsiV2DidUrl().did
        keyStore.addAlias(keyId, didUrlStr)

        val ebsiDid = if (key.algorithm == EdDSA_Ed25519) {
            val pubKeyBytes = key.getPublicKey().encoded
            val pubPrim = ASN1Sequence.fromByteArray(pubKeyBytes) as ASN1Sequence
            val edPublicKey = (pubPrim.getObjectAt(1) as ASN1BitString).octets
            // Create doc
            val ebsiDidBody = ebsiDid(DidUrl.from(didUrlStr), edPublicKey)

            val ebsiDidBodyStr = Json.encodeToString(ebsiDidBody)

            keyStore.addAlias(keyId, ebsiDidBody.verificationMethod!!.get(0)!!.id)

            // Create proof
            val verificationMethod = ebsiDidBody.verificationMethod?.get(0)?.id
            signDid(didUrlStr, verificationMethod!!, ebsiDidBodyStr)
        } else {
            val kid = "$didUrlStr#key-1"
            keyStore.addAlias(keyId, kid)
            val verificationMethods = mutableListOf(
                VerificationMethod(kid, "Secp256k1VerificationKey2018", didUrlStr, null, KeyService.toPem(kid)),
            )

            val did = DidEbsi(
                listOf("https://w3.org/ns/did/v1"), // TODO Context not working "https://ebsi.org/ns/did/v1"
                didUrlStr,
                verificationMethods,
                listOf("$didUrlStr#key-1")
            )
            Json.encodeToString(did)
        }

        // Store DID
        storeDid(didUrlStr, ebsiDid)

        return didUrlStr
    }

    private fun createDidKey(keyAlias: String?): String {
        val keyId = keyAlias?.let { KeyId(it) } ?: cryptoService.generateKey(EdDSA_Ed25519)
        val key = keyStore.load(keyId.id)

        if (key.algorithm != EdDSA_Ed25519)
            throw Exception("DID KEY can only be created with an EdDSA Ed25519 key.")

        val pubPrim = ASN1Sequence.fromByteArray(key.getPublicKey().encoded) as ASN1Sequence
        val x = (pubPrim.getObjectAt(1) as ASN1BitString).octets

        val identifier = convertEd25519PublicKeyToMultiBase58Btc(x)
        val didUrl = "did:key:$identifier"

        keyStore.addAlias(keyId, didUrl)

        resolveAndStore(didUrl)

        return didUrl
    }

    private fun createDidWeb(keyAlias: String?): String {
        val keyId = cryptoService.generateKey(ECDSA_Secp256k1)
        val key = keyStore.load(keyId.id)

        val domain = "letstrust.org"
        val username = UUID.randomUUID().toString().replace("-", "")
        val path = ":user:$username"

        val didUrl = DidUrl("web", "" + domain + path)

        keyStore.addAlias(keyId, didUrl.did)

        resolveAndStore(didUrl.did)

        return didUrl.did
    }

    private fun signDid(issuerDid: String, verificationMethod: String, edDidStr: String): String {
        val signedDid = CredentialService.sign(issuerDid, edDidStr, null, null, verificationMethod)
        return signedDid
    }

    private fun resolveDidKey(didUrl: DidUrl): Did {
        val pubKey = convertEd25519PublicKeyFromMultibase58Btc(didUrl.identifier)
        return ed25519Did(didUrl, pubKey)
    }

    private fun ebsiDid(didUrl: DidUrl, pubKey: ByteArray): DidEbsi {
        val (dhKeyId, verificationMethods, keyRef) = generateEdParams(pubKey, didUrl)

        // TODO Replace EIDAS dummy certificate with real one
//        {
//            "id": "did:ebsi:2b6a1ee5881158edf133421d63d4b9e5f3ac26d474c#key-3",
//            "type": "EidasVerificationKey2021",
//            "controller": "did:ebsi:2b6a1ee5881158edf133421d63d4b9e5f3ac26d472afcff8",
//            "publicKeyPem": "-----BEGIN.."
//        }

        val eidasKeyId = didUrl.identifier + "#" + UUID.randomUUID().toString().replace("-", "")
        verificationMethods.add(VerificationMethod(eidasKeyId, "EidasVerificationKey2021", "publicKeyPem", "-----BEGIN.."))

        return DidEbsi(
            listOf("https://w3.org/ns/did/v1"), // TODO Context not working "https://ebsi.org/ns/did/v1"
            didUrl.did,
            verificationMethods,
            keyRef,
            keyRef,
            keyRef,
            keyRef,
            listOf(dhKeyId),
            null
        )
    }

    private fun ed25519Did(didUrl: DidUrl, pubKey: ByteArray): Did {
        val (dhKeyId, verificationMethods, keyRef) = generateEdParams(pubKey, didUrl)

        return Did(
            DID_CONTEXT_URL,
            didUrl.did,
            verificationMethods,
            keyRef,
            keyRef,
            keyRef,
            keyRef,
            listOf(dhKeyId),
            null
        )
    }

    private fun generateEdParams(
        pubKey: ByteArray,
        didUrl: DidUrl
    ): Triple<String, MutableList<VerificationMethod>, List<String>> {
        val dhKey = convertPublicKeyEd25519ToCurve25519(pubKey)

        val dhKeyMb = convertX25519PublicKeyToMultiBase58Btc(dhKey)

        val pubKeyId = didUrl.identifier + "#" + didUrl.identifier
        val dhKeyId = didUrl.identifier + "#" + dhKeyMb

        val verificationMethods = mutableListOf(
            VerificationMethod(pubKeyId, "Ed25519VerificationKey2018", didUrl.did, pubKey.encodeBase58()),
            VerificationMethod(dhKeyId, "X25519KeyAgreementKey2019", didUrl.did, dhKey.encodeBase58())
        )

        val keyRef = listOf(pubKeyId)
        return Triple(dhKeyId, verificationMethods, keyRef)
    }

    private fun resolveDidWebDummy(didUrl: DidUrl): Did {
        log.warn { "DID WEB implementation is not finalized yet. Use it only for demo purpose." }
        keyStore.getKeyId(didUrl.did).let {
            keyStore.load(it!!).let {
                val pubKeyId = didUrl.identifier + "#key-1"
                val verificationMethods = listOf(
                    VerificationMethod(
                        pubKeyId,
                        "ECDSASecp256k1VerificationKey2018",
                        didUrl.did,
                        "zMIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgPI4jGjTGPA3yFJp07jvx"
                    ), // TODO: key encoding
                )
                val keyRef = listOf(pubKeyId)
                return Did(
                    DID_CONTEXT_URL,
                    didUrl.did,
                    verificationMethods,
                    keyRef,
                    keyRef,
                    keyRef,
                    keyRef,
                    null
                )
            }
        }
    }

    private fun resolveAndStore(didUrl: String) = storeDid(didUrl, resolve(didUrl).encodePretty())

    private fun storeDid(didUrlStr: String, didDoc: String) =
        File(LetsTrustServices.dataDir + "/did/created/${didUrlStr.replace(":", "-")}.json").writeText(didDoc)

    private fun loadDid(didUrlStr: String) =
        File(LetsTrustServices.dataDir + "/did/created/${didUrlStr.replace(":", "-")}.json").readText(Charsets.UTF_8)


    // TODO: consider the methods below. They might be deprecated!

//    fun resolveDid(did: String): Did = resolveDid(did.DidUrl.toDidUrl())
//
//    fun resolveDid(didUrl: DidUrl): Did {
//        return when (didUrl.method) {
//            "key" -> resolveDidKey(didUrl)
//            "web" -> resolveDidWebDummy(didUrl)
//            else -> TODO("did:${didUrl.method} implemented yet")
//        }
//    }


    internal fun resolveDidWeb(didUrl: DidUrl): DidWeb {
        val domain = didUrl.identifier
        val didUrl = "https://${domain}/.well-known/did.json" // FIXME: didUrl argument is ignored?
        log.debug { "Resolving did:web for domain $domain at: $didUrl" }
        val didWebStr = URL(didUrl).readText()
        log.debug { "did:web resolved:\n$didWebStr" }
        print(didWebStr)
        val did = Json.decodeFromString<DidWeb>(didWebStr)
        log.debug { "did:web decoded:\n$did" }
        return did
    }

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
//        val domain = "letstrust.org"
//        val username = UUID.randomUUID().toString().replace("-", "")
//        val path = ":user:$username"
//
//        val didUrl = DidUrl("web", "" + domain + path, didKey.keyId)
//
//        KeyManagementService.addAlias(didKey.keyId, didUrl.did)
//
//        return didUrl.did
//    }

    fun listDids(): List<String> {

        // File("data").walkTopDown().filter {  it -> Files.isRegularFile(it)  }

        return Files.walk(Path.of("data/did/created"))
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().endsWith(".json") }
            .map { it.fileName.toString().substringBefore(".json").replace("-", ":") }.toList()
    }


}
