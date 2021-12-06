package id.walt.services.essif.timestamp

import com.beust.klaxon.Klaxon
import id.walt.crypto.canonicalize
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.context.ContextManager
import id.walt.services.did.DidService
import id.walt.services.essif.EssifClient
import id.walt.services.essif.didebsi.WaltIdDidEbsiService
import id.walt.services.essif.jsonrpc.*
import id.walt.services.hkvstore.HKVKey
import id.walt.services.key.KeyService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import kotlin.random.Random

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
        return listOf(TimestampHashesParams(
            keyService.getEthereumAddress(ethKeyAlias ?: did),
            listOf(0),
            listOf(Numeric.toHexString(Hash.sha256(canonicalize(data).toByteArray()))),
            listOf(Numeric.toHexString(data.toByteArray())))
        )
    }
}
