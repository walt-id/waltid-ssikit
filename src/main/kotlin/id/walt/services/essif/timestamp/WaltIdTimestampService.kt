package id.walt.services.essif.timestamp

import id.walt.crypto.canonicalize
import id.walt.services.essif.jsonrpc.JsonRpcService
import id.walt.services.essif.jsonrpc.TimestampHashesParams
import id.walt.services.key.KeyService
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric

open class WaltIdTimestampService : TimestampService() {

    companion object {
        private const val TIMESTAMP_JSONRPC =
            "https://api.preprod.ebsi.eu/timestamp/v2/jsonrpc" // TODO: make url configurable
    }

    private val log = KotlinLogging.logger {}
    private val jsonRpcService = JsonRpcService.getService()
    private val keyService = KeyService.getService()

    override fun timestampHashes(did: String, ethKeyAlias: String, data: String) = runBlocking {
        log.debug { "Running EBSI timestamp hashes... " }

        // Insert timestamp request
        val unsignedTransactionParams = buildUnsignedTransactionParams(did, ethKeyAlias, data)
        log.debug { "TimestampHashes request: $unsignedTransactionParams" }

        jsonRpcService.execute(did, ethKeyAlias, TIMESTAMP_JSONRPC, "timestampHashes", unsignedTransactionParams)
        log.debug { "EBSI timestamp hashes completed successfully" }
    }

    // TODO: Verify all params are properly defined according to EBSI expectations => https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=EBP&title=DID+Registry+Smart+Contract
    override fun buildUnsignedTransactionParams(did: String, ethKeyAlias: String?, data: String): List<TimestampHashesParams> {
        return listOf(
            TimestampHashesParams(
                keyService.getEthereumAddress(ethKeyAlias ?: did),
                listOf(0),
                listOf(Numeric.toHexString(Hash.sha256(canonicalize(data).toByteArray()))),
                listOf(Numeric.toHexString(data.toByteArray()))
            )
        )
    }
}
