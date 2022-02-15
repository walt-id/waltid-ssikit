package id.walt.services.essif.didebsi

import com.beust.klaxon.Klaxon
import id.walt.crypto.canonicalize
import id.walt.services.did.DidService
import id.walt.services.essif.jsonrpc.InsertDidDocumentParams
import id.walt.services.essif.jsonrpc.JsonRpcService
import id.walt.services.key.KeyService
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import kotlin.random.Random

var EBSI_ENV_URL = System.getenv().get("EBSI_ENV_URL") ?: "https://api.preprod.ebsi.eu"

open class WaltIdDidEbsiService : DidEbsiService() {

    companion object {
        private val DID_REGISTRY_JSONRPC =
            "${EBSI_ENV_URL}/did-registry/v2/jsonrpc" // TODO: make url configurable
    }

    private val log = KotlinLogging.logger {}
    private val jsonRpcService = JsonRpcService.getService()
    private val keyService = KeyService.getService()

    override fun registerDid(did: String, ethKeyAlias: String) = runBlocking {
        log.debug { "Running EBSI DID registration... " }

        val unsignedTransactionParams = buildUnsignedTransactionParams(did, ethKeyAlias)
        log.debug { "Insert document request: $unsignedTransactionParams" }

        jsonRpcService.execute(did, ethKeyAlias, DID_REGISTRY_JSONRPC, "insertDidDocument", unsignedTransactionParams)
        log.debug { "EBSI DID registration completed successfully" }
    }

    // TODO: Verify all params are properly defined according to EBSI expectations => https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=EBP&title=DID+Registry+Smart+Contract
    override fun buildUnsignedTransactionParams(did: String, ethKeyAlias: String?): List<InsertDidDocumentParams> {
        val didDocumentString = Klaxon().toJsonString(DidService.load(did))

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
}
