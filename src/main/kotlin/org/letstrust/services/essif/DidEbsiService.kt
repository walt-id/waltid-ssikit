package org.letstrust.services.essif

import com.nimbusds.jose.JWSAlgorithm
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.letstrust.LetsTrustServices
import org.letstrust.common.readWhenContent
import org.letstrust.crypto.CryptoService
import org.letstrust.crypto.canonicalize
import org.letstrust.crypto.toECDSASignature
import org.letstrust.crypto.toECDSASignatureAlt
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
    private val cs = LetsTrustServices.load<CryptoService>()

    fun registerDid(did: String) = runBlocking {
        log.debug { "Running EBSI DID registration ... " }
        //TODO run auth-flow, if file is not present
        //TODO re-run auth-flow, if token is expired -> io.ktor.client.features.ClientRequestException: Client request(https://api.preprod.ebsi.eu/did-registry/v2/jsonrpc) invalid: 401 Unauthorized. Text: "{"title":"Unauthorized","status":401,"type":"about:blank","detail":"Invalid JWT: JWT has expired: exp: 1623244001 < now: 1623245358"}"
        val token = readWhenContent(EssifFlowRunner.ebsiAccessTokenFile)

        // Insert DID document request
        val insertDocumentParams = buildInsertDocumentParams(did)
        log.debug { insertDocumentParams }

        val unsignedTx = didRegistryJsonRpc<InsertDidDocumentResponse>(
            token, "insertDidDocument", insertDocumentParams
        ).result
        log.debug { unsignedTx }

        // Sign the raw transaction
        val signedTx = signTransaction(did, unsignedTx)
        log.debug { signedTx }

        // Signed transaction request
        val signedTransactionParams = buildSignedTransactionParams(unsignedTx, signedTx)
        log.debug { signedTransactionParams }

        log.debug { "EBSI DID registration incomplete; this is a testing version " }

//        val sendTransactionResponse = didRegistryJsonRpc<SignedTransactionResponse>(
//            token, "signedTransaction", signedTransactionParams
//        ).result
//        log.debug { sendTransactionResponse }
//
//        log.debug { "EBSI DID registration completed successfully" }
    }

    // TODO: Verify all params are properly defined according to EBSI expectations => https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=EBP&title=DID+Registry+Smart+Contract
    fun buildInsertDocumentParams(did: String): List<InsertDidDocumentParams> {
        val didDocumentString = Json.encodeToString(DidService.loadDidEbsi(did))

        val from = KeyService.getEthereumAddress(did)
        val identifier = Numeric.toHexString(did.toByteArray())
        val hashValue = Numeric.toHexString(Hash.sha256(canonicalize(didDocumentString).toByteArray()))
        val didVersionInfo = Numeric.toHexString(didDocumentString.toByteArray())
        val timestampData = Numeric.toHexString("{\"data\":\"test\"}".toByteArray()) // TODO: check what data needs to be put here
        val didVersionMetadata =
            Numeric.toHexString("{\"meta\":\"${Numeric.toHexStringNoPrefix(Random.Default.nextBytes(32))}\"}".toByteArray()) // TODO: check what data needs to be put here

        return listOf(
            InsertDidDocumentParams(from, identifier, 1, hashValue, didVersionInfo, timestampData, didVersionMetadata)
        )
    }

    fun signTransaction(did: String, unsignedTransaction: UnsignedTransaction): SignedTransaction {
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
        var rlpList = RlpList(TransactionEncoder.asRlpValues(rawTransaction, signatureData))

        val hash = Hash.sha3(RlpEncoder.encode(rlpList))
//        val sig = ECKeyPair.create(KeyService.load(did, true).keyPair).sign(hash)
        val signature = cs.sign(KeyService.load(did).keyId, hash)
        val sig = toECDSASignature(signature, JWSAlgorithm.ES256K)
        val sigAlt = toECDSASignatureAlt(signature)

        println("r (nimbus) : ${sig.r}")
        println("r (decoded): ${sigAlt.r}")
        println("s (nimbus) : ${sig.s}")
        println("s (decoded): ${sigAlt.s}")

        val v = BigInteger
            .valueOf(getRecoveryId(did, hash, sig).toLong())
            .add(chainId.multiply(BigInteger.TWO))
            .add(BigInteger.valueOf(35L))

        signatureData = SignatureData(v.toByteArray(), sig.r.toByteArray(), sig.s.toByteArray())
        rlpList = RlpList(TransactionEncoder.asRlpValues(rawTransaction, signatureData))

        return SignedTransaction(
            Numeric.toHexString(signatureData.r),
            Numeric.toHexString(signatureData.s),
            Numeric.toHexString(signatureData.v),
            Numeric.toHexString(RlpEncoder.encode(rlpList))
        )
    }

    fun getRecoveryId(did: String, hash: ByteArray, sig: ECDSASignature): Int {
        val address = Numeric.prependHexPrefix(KeyService.getEthereumAddress(did))
        for (i in 0..3) {
            val k = Sign.recoverFromSignature(i, sig, hash)
            if (k != null && address == Keys.toChecksumAddress(Numeric.prependHexPrefix(Keys.getAddress(k))).toLowerCase())
                return i
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
