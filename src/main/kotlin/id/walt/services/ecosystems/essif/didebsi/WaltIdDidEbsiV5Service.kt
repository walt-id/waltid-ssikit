package id.walt.services.ecosystems.essif.didebsi

import com.beust.klaxon.Klaxon
import id.walt.services.did.DidService
import id.walt.services.ecosystems.essif.jsonrpc.InsertDidDocumentV5Params
import id.walt.services.ecosystems.essif.jsonrpc.JsonRpcService
import id.walt.services.key.KeyService
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

open class WaltIdDidEbsiV5Service : DidEbsiService() {

    private val log = KotlinLogging.logger {}
    private val jsonRpcService = JsonRpcService.getService()
    private val keyService = KeyService.getService()
    private val klaxon = Klaxon()

    override fun apiVersion(): String = "v5"
    override fun registerDid(did: String, ethKeyAlias: String) = runBlocking {
        log.debug { "Running EBSI${apiVersion()} DID registration... " }

        val unsignedTransactionParams = buildUnsignedTransactionParams(did, ethKeyAlias)
        log.debug { "Insert document request: $unsignedTransactionParams" }

        jsonRpcService.execute(did, ethKeyAlias, rpcUrl(), "insertDidDocument", unsignedTransactionParams)
        log.debug { "EBSI${apiVersion()} DID registration completed successfully" }
    }

    override fun buildUnsignedTransactionParams(did: String, ethKeyAlias: String?): List<InsertDidDocumentV5Params> {
        val didDocument = DidService.load(did)
        val vMethod = didDocument.verificationMethod?.get(0)//TODO: fix hard-coded index
        val from = keyService.getEthereumAddress(ethKeyAlias ?: did)
        val baseDocument = klaxon.toJsonString(didDocument.context!!)
        val vMethodId = keyService.toJwk(vMethod?.publicKeyJwk?.kid!!).computeThumbprint().decodeToString()
        val publicKey = vMethod.ethereumAddress!!
        val isSecp256k1 = true
        val notBefore = System.currentTimeMillis()//TODO: inject value
        val notAfter = System.currentTimeMillis()//TODO: inject value

        return listOf(
            InsertDidDocumentV5Params(
                from, did, baseDocument, vMethodId, publicKey, isSecp256k1, notBefore, notAfter
            )
        )
    }
}
