package org.letstrust.services.essif

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.letstrust.LetsTrustServices
import org.letstrust.common.readWhenContent
import org.letstrust.crypto.canonicalize
import org.letstrust.model.DidEbsi
import org.letstrust.services.did.DidService
import org.letstrust.services.key.KeyService
import org.web3j.crypto.*
import org.web3j.crypto.Sign.SignatureData
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import org.web3j.utils.Numeric
import java.math.BigInteger
import kotlin.random.Random

@Serializable
sealed class JsonRpcParams

@Serializable
data class JsonRpcRequest(val jsonrpc: String, val method: String, val params: List<JsonRpcParams>, val id: Int)

@Serializable
data class InsertDidDocumentParams(
    val from: String,
    val identifier: String,
    val hashAlgorithmId: Int,
    val hashValue: String,
    val didVersionInfo: String,
    val timestampData: String,
    val didVersionMetadata: String
) : JsonRpcParams()

@Serializable
data class InsertDidDocumentResponse(val jsonrpc: String, val id: Int, val result: UnsignedTransaction)

@Serializable
data class SignedTransactionParams(
    val protocol: String,
    val unsignedTransaction: UnsignedTransaction,
    val r: String,
    val s: String,
    val v: String,
    val signedRawTransaction: String
) : JsonRpcParams()

@Serializable
data class SignedTransactionResponse(val jsonrpc: String, val id: Int, val result: String)

@Serializable
data class UnsignedTransaction(
    val from: String,
    val to: String,
    val data: String,
    val nonce: String,
    val chainId: String,
    val gasLimit: String,
    val gasPrice: String,
    val value: String
)

@Serializable
data class SignedTransaction(val r: String, val s: String, val v: String, val signedRawTransaction: String)


object DidEbsiService {

    private val log = KotlinLogging.logger {}


    fun registerDid(did: String) = runBlocking {
        log.debug { "Running EBSI DID registration ... " }

        // Generate once and set below:
        //        val keyId = KeyManagementService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        //        log.debug{keyId)
        //        val did = DidService.create(DidMethod.ebsi, keyId.id)
        //        log.debug{did)
        //        val token = // TODO: implement EBSI on-boarding
//        val keyId = "96c128ca032e4ea88aef41c0d90ff4cf"
//        val did = "did:ebsi:2VrSjVYT81DMDijubiWDr8ATAzADqyZGDu41i1QPJvKemvdJ"
//        val token =
//            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksiLCJraWQiOiJodHRwczovL2FwaS5wcmVwcm9kLmVic2kuZXUvdHJ1c3RlZC1hcHBzLXJlZ2lzdHJ5L3YyL2FwcHMvMHgwOGMyNTg1NmZiY2JkZDA3NmM5YzM5NTEyYWJlZjYzMDk3NDk5MTBhMTEwZDlkMWE5YzlhN2QyYjI3N2I2ZDIwIn0.eyJpYXQiOjE2MjIxOTU5NDgsImV4cCI6MTYyMjE5Njg0OCwic3ViIjoiZGlkOmVic2k6QzdZOUw0RzNpeGVORUw1UkFoTUdTQkxnZlRQdm02b1ZObm00Q21tWU1admoiLCJhdWQiOiJlYnNpLWNvcmUtc2VydmljZXMiLCJub25jZSI6IjJmOGZlMjQ2LTJkOGYtNDdhNS04ZWE5LTE3MGNiMTdhYjM2MCIsImxvZ2luX2hpbnQiOiJkaWRfc2lvcCIsImlzcyI6ImRpZDplYnNpOkhDOXRtaWl0VzRTOWZZQWFqNlJZc29xYlQzczdUY3d5aHlyc2JTaXpuc2RaIn0.fi3HCy_jXpTOwkuiefPc9W01X7e_s1rqxa0guNPCJ7AZzfeSQOe_0jKDjmrNRxcD3Wd6wqadWZ3VBQM4AZa4kA"

        //TODO run auth-flow, if file is not present
        //TODO re-run auth-flow, if token is expired -> io.ktor.client.features.ClientRequestException: Client request(https://api.preprod.ebsi.eu/did-registry/v2/jsonrpc) invalid: 401 Unauthorized. Text: "{"title":"Unauthorized","status":401,"type":"about:blank","detail":"Invalid JWT: JWT has expired: exp: 1623244001 < now: 1623245358"}"
        val token = readWhenContent(EssifFlowRunner.ebsiAccessTokenFile)

        val ecKeyPair = ECKeyPair.create(KeyService.load(did, true).keyPair)

        // Insert DID document request
        val address = Numeric.prependHexPrefix(Keys.getAddress(ecKeyPair))
        val didDocument = DidService.resolveDidEbsi(did)
        val insertDocumentParams = buildInsertDocumentParams(address, didDocument)
        log.debug { insertDocumentParams }

        val unsignedTx = didRegistryJsonRpc<InsertDidDocumentResponse>(
            token, "insertDidDocument", insertDocumentParams
        ).result
        log.debug { unsignedTx }

        // Sign the raw transaction
        val signedTx = signTransaction(ecKeyPair, unsignedTx)
        log.debug { signedTx }

        // Signed transaction request
        val signedTransactionParams = buildSignedTransactionParams(unsignedTx, signedTx)
        log.debug { signedTransactionParams }

        val sendTransactionResponse = didRegistryJsonRpc<SignedTransactionResponse>(
            token, "signedTransaction", signedTransactionParams
        ).result
        log.debug { sendTransactionResponse }

        log.debug { "EBSI DID registration completed successfully" }
    }

    // TODO: Verify all params are properly defined according to EBSI expectations => https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=EBP&title=DID+Registry+Smart+Contract
    fun buildInsertDocumentParams(from: String, didDocument: DidEbsi): List<InsertDidDocumentParams> {
        val didDocumentString = Json.encodeToString(didDocument)

        val identifier = Numeric.toHexString(didDocument.id!!.toByteArray())
        val hashValue = Numeric.toHexString(Hash.sha256(canonicalize(didDocumentString).toByteArray()))
        val didVersionInfo = Numeric.toHexString(didDocumentString.toByteArray())
        val timestampData = Numeric.toHexString("{\"data\":\"test\"}".toByteArray()) // TODO: check what data needs to be put here
        val didVersionMetadata =
            Numeric.toHexString("{\"meta\":\"${Numeric.toHexStringNoPrefix(Random.Default.nextBytes(32))}\"}".toByteArray()) // TODO: check what data needs to be put here

        return listOf(
            InsertDidDocumentParams(from, identifier, 1, hashValue, didVersionInfo, timestampData, didVersionMetadata)
        )
    }

    fun signTransaction(ecKeyPair: ECKeyPair, unsignedTransaction: UnsignedTransaction): SignedTransaction {
        val chainId = BigInteger(Numeric.hexStringToByteArray(unsignedTransaction.chainId))
        val rawTransaction = RawTransaction.createTransaction(
            BigInteger(Numeric.hexStringToByteArray(unsignedTransaction.nonce)),
            BigInteger(Numeric.hexStringToByteArray(unsignedTransaction.gasPrice)),
            BigInteger(Numeric.hexStringToByteArray(unsignedTransaction.gasLimit)),
            unsignedTransaction.to,
            BigInteger(Numeric.hexStringToByteArray(unsignedTransaction.value)),
            unsignedTransaction.data
        )

        var signatureData = SignatureData(chainId.toByteArray(), ByteArray(0), ByteArray(0))
        var values = TransactionEncoder.asRlpValues(rawTransaction, signatureData)
        var rlpList = RlpList(values)

        val hash = Hash.sha3(RlpEncoder.encode(rlpList))
        val sig = ecKeyPair.sign(hash)
//TODO:
//        val sig = LetsTrustServices.load<CryptoService>().sign(
//            KeyId("keyId"), hash
//        )
        val recId = getRecoveryId(ecKeyPair, hash, sig)
        val v = BigInteger
            .valueOf(recId.toLong())
            .add(chainId.multiply(BigInteger.TWO))
            .add(BigInteger.valueOf(35L))

        signatureData = SignatureData(v.toByteArray(), sig.r.toByteArray(), sig.s.toByteArray())
        values = TransactionEncoder.asRlpValues(rawTransaction, signatureData)
        rlpList = RlpList(values)

        return SignedTransaction(
            Numeric.toHexString(signatureData.r),
            Numeric.toHexString(signatureData.s),
            Numeric.toHexString(signatureData.v),
            Numeric.toHexString(RlpEncoder.encode(rlpList))
        )
    }

    fun getRecoveryId(ecKeyPair: ECKeyPair, hash: ByteArray, sig: ECDSASignature): Int {
        for (i in 0..3) {
            val k = Sign.recoverFromSignature(i, sig, hash)
            if (k != null) {
                val addressFromPublicKey = Keys
                    .toChecksumAddress(Numeric.prependHexPrefix(Keys.getAddress(k)))
                    .toLowerCase()
                val address = Numeric.prependHexPrefix(Keys.getAddress(ecKeyPair))
                if (addressFromPublicKey == address) return i
            }
        }
        throw RuntimeException("Could not construct a recoverable key. This should never happen.")
    }

    fun buildSignedTransactionParams(unsignedTransaction: UnsignedTransaction, signedTransaction: SignedTransaction):
            List<SignedTransactionParams> =
        listOf(
            SignedTransactionParams(
                "eth",
                unsignedTransaction,
                signedTransaction.r,
                signedTransaction.s,
                signedTransaction.v,
                signedTransaction.signedRawTransaction
            )
        )

    suspend inline fun <reified T> didRegistryJsonRpc(
        bearerToken: String,
        method: String,
        params: List<JsonRpcParams>
        //TODO parameterize URL
    ) = LetsTrustServices.http.post<T>("https://api.preprod.ebsi.eu/did-registry/v2/jsonrpc") {
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.Accept, "application/json")
            append(HttpHeaders.Authorization, "Bearer $bearerToken")
        }
        body = JsonRpcRequest("2.0", method, params, (0..999).random()) //TODO: consider ID value. is random the generation ok?
    }
}
