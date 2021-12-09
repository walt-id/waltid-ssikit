package id.walt.services.essif.jsonrpc

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import kotlinx.serialization.Serializable

@Serializable
data class JsonRpcRequest(val jsonrpc: String, val method: String, val params: List<JsonRpcParams>, val id: Int)

@Serializable
sealed class JsonRpcParams

interface JsonRpcResponse

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
data class TimestampHashesParams(
    val from: String,
    val hashAlgorithmIds: List<Int>,
    val hashValues: List<String>,
    val timestampData: List<String?>
) : JsonRpcParams()

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
data class UnsignedTransactionResponse(val jsonrpc: String, val id: Int, val result: UnsignedTransaction) :
    JsonRpcResponse

@Serializable
data class SignedTransactionResponse(val jsonrpc: String, val id: Int, val result: String) : JsonRpcResponse

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

open class JsonRpcService : WaltIdService() {

    override val implementation get() = serviceImplementation<JsonRpcService>()

    open suspend fun execute(
        did: String,
        ethKeyAlias: String,
        urlString: String,
        method: String,
        unsignedTransactionParams: List<JsonRpcParams>
    ): SignedTransactionResponse =
        implementation.execute(did, ethKeyAlias, urlString, method, unsignedTransactionParams)

    open fun signTransaction(ethKeyAlias: String, unsignedTransaction: UnsignedTransaction): SignedTransaction =
        implementation.signTransaction(ethKeyAlias, unsignedTransaction)

    companion object : ServiceProvider {
        override fun getService() = object : JsonRpcService() {}
    }
}