package org.letstrust.services.did

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.bouncycastle.asn1.ASN1BitString
import org.bouncycastle.asn1.ASN1Sequence
import org.letstrust.LetsTrustServices
import org.letstrust.crypto.*
import org.letstrust.crypto.KeyAlgorithm.*
import org.letstrust.crypto.keystore.KeyStore
import org.letstrust.model.*
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
            else -> throw Exception("DID method $method not supported")
        }

        resolveAndStore(didUrl)

        return didUrl
    }

    private fun resolveAndStore(didUrl: String) {
        val destFile = File("data/did/created/${didUrl.replace(":","-")}.json")
        destFile.writeText(resolve(didUrl).encodePretty())
    }

    fun resolve(did: String): Did = resolve(toDidUrl(did))
    fun resolve(didUrl: DidUrl): Did {
        return when (didUrl.method) {
            DidMethod.key.name -> resolveDidKey(didUrl)
            DidMethod.web.name -> resolveDidWebDummy(didUrl)
            else -> TODO("did:${didUrl.method} not implemented yet")
        }
    }

    // Private methods

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

        return didUrl.did
    }

    private fun resolveDidKey(didUrl: DidUrl): Did {
        val pubKey = convertEd25519PublicKeyFromMultibase58Btc(didUrl.identifier)
        return ed25519Did(didUrl, pubKey)
    }

    private fun ed25519Did(didUrl: DidUrl, pubKey: ByteArray): Did {
        val dhKey = convertPublicKeyEd25519ToCurve25519(pubKey)

        val dhKeyMb = convertX25519PublicKeyToMultiBase58Btc(dhKey)

        val pubKeyId = didUrl.identifier + "#" + didUrl.identifier
        val dhKeyId = didUrl.identifier + "#" + dhKeyMb

        val verificationMethods = listOf(
            VerificationMethod(pubKeyId, "Ed25519VerificationKey2018", didUrl.did, pubKey.encodeBase58()),
            VerificationMethod(dhKeyId, "X25519KeyAgreementKey2019", didUrl.did, dhKey.encodeBase58())
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
            listOf(dhKeyId),
            null
        )
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


    // TODO: consider the methods below. They might be deprecated!

//    fun resolveDid(did: String): Did = resolveDid(did.toDidUrl())
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
