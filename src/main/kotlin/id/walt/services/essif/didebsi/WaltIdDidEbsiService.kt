package id.walt.services.essif.didebsi

import com.beust.klaxon.Klaxon
import id.walt.crypto.canonicalize
import id.walt.services.WaltIdServices
import id.walt.services.context.WaltContext
import id.walt.services.crypto.CryptoService
import id.walt.services.did.DidService
import id.walt.services.essif.EssifClient
import id.walt.services.hkvstore.HKVKey
import id.walt.services.hkvstore.HKVStoreService
import id.walt.services.key.KeyService
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.web3j.crypto.Hash
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.TransactionEncoder
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import org.web3j.utils.Numeric
import java.math.BigInteger
import kotlin.random.Random

open class WaltIdDidEbsiService : DidEbsiService() {

    private val log = KotlinLogging.logger {}
    private val cryptoService = CryptoService.getService()
    private val keyService = KeyService.getService()

    @Serializable
    data class SignedTransactionResponse(val jsonrpc: String, val id: Int, val result: String) : DidResponse

    @Serializable
    data class JsonRpcRequest(val jsonrpc: String, val method: String, val params: List<JsonRpcParams>, val id: Int)

    @Serializable
    data class InsertDidDocumentResponse(val jsonrpc: String, val id: Int, val result: UnsignedTransaction) :
        DidResponse

    override fun registerDid(did: String, ethKeyAlias: String) = runBlocking {
        log.debug { "Running EBSI DID registration ... " }
        //TODO run auth-flow, if file is not present
        //TODO re-run auth-flow, if token is expired -> io.ktor.client.features.ClientRequestException: Client request(https://api.preprod.ebsi.eu/did-registry/v2/jsonrpc) invalid: 401 Unauthorized. Text: "{"title":"Unauthorized","status":401,"type":"about:blank","detail":"Invalid JWT: JWT has expired: exp: 1623244001 < now: 1623245358"}"
        // val token = readWhenContent(EssifClient.ebsiAccessTokenFile)
        val token = WaltContext.hkvStore.getAsString(HKVKey("ebsi", did.substringAfterLast(":"), EssifClient.ebsiAccessTokenFile))!!

        // Insert DID document request
        val insertDocumentParams = buildInsertDocumentParams(did, ethKeyAlias)
        log.debug { "Insert document request: $insertDocumentParams" }

        val unsignedTx = didRegistryJsonRpc<InsertDidDocumentResponse>(
            token, "insertDidDocument", insertDocumentParams
        ).result
        log.debug { "Unsigned transaction: $unsignedTx" }

        // Sign the raw transaction
        val signedTx = signTransaction(ethKeyAlias, unsignedTx)
        log.debug { "Singed transaction: $signedTx" }

        // Signed transaction request
        val signedTransactionParams = buildSignedTransactionParams(unsignedTx, signedTx)
        log.debug { "Signed transaction params: $signedTransactionParams" }

        val sendTransactionResponse = didRegistryJsonRpc<SignedTransactionResponse>(
            token, "signedTransaction", signedTransactionParams
        ).result
        log.debug { "Send transaction response: $sendTransactionResponse" }

        log.debug { "EBSI DID registration completed successfully" }
    }

    // TODO: Verify all params are properly defined according to EBSI expectations => https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=EBP&title=DID+Registry+Smart+Contract
    override fun buildInsertDocumentParams(did: String, ethKeyAlias: String?): List<InsertDidDocumentParams> {
        val didDocumentString = Klaxon().toJsonString(DidService.loadDidEbsi(did))

        val from = keyService.getEthereumAddress(ethKeyAlias ?: did)
        val identifier = Numeric.toHexString(did.toByteArray())
        val hashValue = Numeric.toHexString(Hash.sha256(canonicalize(didDocumentString).toByteArray()))
        val didVersionInfo = Numeric.toHexString(didDocumentString.toByteArray())
        val timestampData =
            Numeric.toHexString("{\"data\":\"test\"}".toByteArray()) // TODO: check what data needs to be put here
        val didVersionMetadata =
            Numeric.toHexString("{\"meta\":\"${Numeric.toHexStringNoPrefix(Random.Default.nextBytes(32))}\"}".toByteArray()) // TODO: check what data needs to be put here

        return listOf(
            InsertDidDocumentParams(from, identifier, 1, hashValue, didVersionInfo, timestampData, didVersionMetadata)
        )
    }

    override fun signTransaction(ethKeyAlias: String, unsignedTransaction: UnsignedTransaction): SignedTransaction {
        val chainId = BigInteger(Numeric.hexStringToByteArray(unsignedTransaction.chainId))
        val rawTransaction = RawTransaction.createTransaction(
            BigInteger(Numeric.hexStringToByteArray(unsignedTransaction.nonce)),
            BigInteger(Numeric.hexStringToByteArray(unsignedTransaction.gasPrice)),
            BigInteger(Numeric.hexStringToByteArray(unsignedTransaction.gasLimit)),
            unsignedTransaction.to,
            BigInteger(Numeric.hexStringToByteArray(unsignedTransaction.value)),
            unsignedTransaction.data
        )

        var signatureData = Sign.SignatureData(chainId.toByteArray(), ByteArray(0), ByteArray(0))
        var rlpList = RlpList(TransactionEncoder.asRlpValues(rawTransaction, signatureData))

        val encodedTx = RlpEncoder.encode(rlpList)
        val key = keyService.load(ethKeyAlias)
        val sig = cryptoService.signEthTransaction(key.keyId, encodedTx)
//        val sig = toECDSASignature(cs.sign(key.keyId, encodedTx), key.algorithm)
        val v = BigInteger
            .valueOf(keyService.getRecoveryId(ethKeyAlias, encodedTx, sig).toLong())
            .add(chainId.multiply(BigInteger.TWO))
            .add(BigInteger.valueOf(35L))

        signatureData = Sign.SignatureData(v.toByteArray(), sig.r.toByteArray(), sig.s.toByteArray())
        rlpList = RlpList(TransactionEncoder.asRlpValues(rawTransaction, signatureData))

        return SignedTransaction(
            Numeric.toHexString(signatureData.r),
            Numeric.toHexString(signatureData.s),
            Numeric.toHexString(signatureData.v),
            Numeric.toHexString(RlpEncoder.encode(rlpList))
        )
    }

    override fun buildSignedTransactionParams(
        unsignedTransaction: UnsignedTransaction,
        signedTransaction: SignedTransaction
    ):
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

    private suspend inline fun <reified T : DidResponse> didRegistryJsonRpc(
        bearerToken: String,
        method: String,
        params: List<JsonRpcParams>
        // TODO: make url configurable
    ): T = WaltIdServices.http.post("https://api.preprod.ebsi.eu/did-registry/v2/jsonrpc") {
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.Accept, "application/json")
            append(HttpHeaders.Authorization, "Bearer $bearerToken")
        }
        body = JsonRpcRequest(
            "2.0",
            method,
            params,
            (0..999).random()
        ) //TODO: consider ID value. is random the generation ok?
    }

}
