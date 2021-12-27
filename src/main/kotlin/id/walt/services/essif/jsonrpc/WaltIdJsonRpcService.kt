package id.walt.services.essif.jsonrpc

import id.walt.services.WaltIdServices
import id.walt.services.context.ContextManager
import id.walt.services.crypto.CryptoService
import id.walt.services.essif.EssifClient
import id.walt.services.hkvstore.HKVKey
import id.walt.services.key.KeyService
import io.ktor.client.request.*
import io.ktor.http.*
import mu.KotlinLogging
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.TransactionEncoder
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*

class WaltIdJsonRpcService : JsonRpcService() {

    private val log = KotlinLogging.logger {}
    private val cryptoService = CryptoService.getService()
    private val keyService = KeyService.getService()

    override suspend fun execute(
        did: String,
        ethKeyAlias: String,
        urlString: String,
        method: String,
        unsignedTransactionParams: List<JsonRpcParams>
    ): SignedTransactionResponse {
        //TODO run auth-flow, if file is not present
        //TODO re-run auth-flow, if token is expired -> io.ktor.client.features.ClientRequestException: Client request(https://api.preprod.ebsi.eu/did-registry/v2/jsonrpc) invalid: 401 Unauthorized. Text: "{"title":"Unauthorized","status":401,"type":"about:blank","detail":"Invalid JWT: JWT has expired: exp: 1623244001 < now: 1623245358"}"
        // val token = readWhenContent(EssifClient.ebsiAccessTokenFile)
        val token = ContextManager.hkvStore.getAsString(
            HKVKey("ebsi", did.substringAfterLast(":"), EssifClient.ebsiAccessTokenFile)
        ) ?: throw Exception("Could not load EBSI access token. Make sure that the ESSIF onboarding flow is performed correctly.")

        val unsignedTx = post<UnsignedTransactionResponse>(token, urlString, method, unsignedTransactionParams).result
        log.debug { "Unsigned transaction: $unsignedTx" }

        // Sign the raw transaction
        val signedTx = signTransaction(ethKeyAlias, unsignedTx)
        log.debug { "Signed transaction: $signedTx" }

        // Signed transaction request
        val signedTransactionParams = buildSignedTransactionParams(unsignedTx, signedTx)
        log.debug { "Signed transaction params: $signedTransactionParams" }

        val sendTransactionResponse =
            post<SignedTransactionResponse>(token, urlString, "signedTransaction", signedTransactionParams)
        log.debug { "Send transaction response: $sendTransactionResponse" }

        return sendTransactionResponse
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

        var sigR = sig.r.toByteArray()
        if (sigR.size == 33) {
            sigR = Arrays.copyOfRange(sigR, 1, 33)
        }

        signatureData = Sign.SignatureData(v.toByteArray(), sigR, sig.s.toByteArray())
        rlpList = RlpList(TransactionEncoder.asRlpValues(rawTransaction, signatureData))

        return SignedTransaction(
            Numeric.toHexString(signatureData.r),
            Numeric.toHexString(signatureData.s),
            Numeric.toHexString(signatureData.v),
            Numeric.toHexString(RlpEncoder.encode(rlpList))
        )
    }

    private fun buildSignedTransactionParams(
        unsignedTransaction: UnsignedTransaction,
        signedTransaction: SignedTransaction
    ): List<SignedTransactionParams> {
        val txParams = SignedTransactionParams(
            "eth",
            unsignedTransaction,
            signedTransaction.r,
            signedTransaction.s,
            signedTransaction.v,
            signedTransaction.signedRawTransaction
        )
        return listOf(txParams)
    }

    private suspend inline fun <reified T : JsonRpcResponse> post(
        bearerToken: String,
        urlString: String,
        method: String,
        params: List<JsonRpcParams>
    ): T = WaltIdServices.http.post(urlString) {
        contentType(ContentType.Application.Json)
        headers {
            append(HttpHeaders.Accept, "application/json")
            append(HttpHeaders.Authorization, "Bearer $bearerToken")
        }
        //TODO: consider ID value. is random the generation ok?
        body = JsonRpcRequest("2.0", method, params, (0..999).random())
    }
}
