package id.walt.services.ecosystems.essif.didebsi

import id.walt.crypto.canonicalize
import id.walt.services.did.DidService
import id.walt.services.ecosystems.essif.jsonrpc.InsertDidDocumentV3Params
import id.walt.services.ecosystems.essif.jsonrpc.JsonRpcService
import id.walt.services.key.KeyService
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import kotlin.random.Random

open class WaltIdDidEbsiV3Service : DidEbsiService() {

    private val log = KotlinLogging.logger {}
    private val jsonRpcService = JsonRpcService.getService()
    private val keyService = KeyService.getService()

    override fun apiVersion(): String = "v3"

    override fun registerDid(did: String, ethKeyAlias: String) = runBlocking {
        log.debug { "Running EBSI${apiVersion()} DID registration... " }

        val unsignedTransactionParams = buildUnsignedTransactionParams(did, ethKeyAlias)
        log.debug { "Insert document request: $unsignedTransactionParams" }

        jsonRpcService.execute(did, ethKeyAlias, rpcUrl(), "insertDidDocument", unsignedTransactionParams)
        log.debug { "EBSI${apiVersion()} DID registration completed successfully" }
    }

    // TODO: Verify all params are properly defined according to EBSI expectations => https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=EBP&title=DID+Registry+Smart+Contract
    override fun buildUnsignedTransactionParams(did: String, ethKeyAlias: String?): List<InsertDidDocumentV3Params> {
        val didDocumentString = DidService.load(did).encode()

        val from = keyService.getEthereumAddress(ethKeyAlias ?: did)
        val identifier = Numeric.toHexString(did.toByteArray())
        val hashValue = Numeric.toHexString(Hash.sha256(canonicalize(didDocumentString).toByteArray()))
        val didVersionInfo = Numeric.toHexString(didDocumentString.toByteArray())
        val timestampData =
            Numeric.toHexString("{\"data\":\"test\"}".toByteArray()) // TODO: check what data needs to be put here
        val didVersionMetadata =
            Numeric.toHexString("{\"meta\":\"${Numeric.toHexStringNoPrefix(Random.Default.nextBytes(32))}\"}".toByteArray()) // TODO: check what data needs to be put here

        return listOf(
            InsertDidDocumentV3Params(from, identifier, 1, hashValue, didVersionInfo, timestampData, didVersionMetadata)
        )
    }
}
