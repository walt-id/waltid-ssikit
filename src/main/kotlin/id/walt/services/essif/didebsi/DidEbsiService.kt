package id.walt.services.essif.didebsi

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import kotlinx.serialization.Serializable

@Serializable
sealed class JsonRpcParams

interface DidResponse

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
data class SignedTransactionParams(
    val protocol: String,
    val unsignedTransaction: UnsignedTransaction,
    val r: String,
    val s: String,
    val v: String,
    val signedRawTransaction: String
) : JsonRpcParams()


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


open class DidEbsiService : WaltIdService() {

    override val implementation get() = serviceImplementation<DidEbsiService>()

    open fun registerDid(did: String, ethKeyAlias: String): Unit = implementation.registerDid(did, ethKeyAlias)

    // TODO: Verify all params are properly defined according to EBSI expectations => https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=EBP&title=DID+Registry+Smart+Contract
    open fun buildInsertDocumentParams(did: String, ethKeyAlias: String? = null): List<InsertDidDocumentParams> =
        implementation.buildInsertDocumentParams(did, ethKeyAlias)

    open fun signTransaction(ethKeyAlias: String, unsignedTransaction: UnsignedTransaction): SignedTransaction =
        implementation.signTransaction(ethKeyAlias, unsignedTransaction)

    open fun buildSignedTransactionParams(
        unsignedTransaction: UnsignedTransaction,
        signedTransaction: SignedTransaction
    ): List<SignedTransactionParams> =
        implementation.buildSignedTransactionParams(unsignedTransaction, signedTransaction)

    companion object : ServiceProvider {
        override fun getService() = object : DidEbsiService() {}
    }
}
