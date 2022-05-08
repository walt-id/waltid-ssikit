package id.walt.services.essif.timestamp

import id.walt.crypto.canonicalize
import id.walt.services.WaltIdServices
import id.walt.services.essif.jsonrpc.JsonRpcService
import id.walt.services.essif.jsonrpc.TimestampHashesParams
import id.walt.services.key.KeyService
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric

@Serializable
data class Timestamps(
    val self: String,
    val items: List<TimestampItem>,
    val total: Int,
    val pageSize: Int,
    val links: Links
)

@Serializable
data class TimestampItem(
    val timestampId: String,
    val href: String
)

@Serializable
data class Links(
    val first: String,
    val prev: String,
    val next: String,
    val last: String
)

@Serializable
data class Timestamp(
    var timestampId: String? = null,
    var href: String? = null,
    val hash: String,
    val timestampedBy: String,
    val blockNumber: Int,
    val timestamp: String,
    val data: String,
    val transactionHash: String
)

open class WaltIdTimestampService : TimestampService() {

    companion object {
        private const val TIMESTAMP_JSONRPC =
            "https://api.preprod.ebsi.eu/timestamp/v2/jsonrpc" // TODO: make url configurable
        private const val TIMESTAMPS =
            "https://api.preprod.ebsi.eu/timestamp/v2/timestamps" // TODO: make url configurable
    }

    private val log = KotlinLogging.logger {}
    private val jsonRpcService = JsonRpcService.getService()
    private val keyService = KeyService.getService()

    override fun getByTimestampId(timestampId: String): Timestamp? = runBlocking {
        val href = TIMESTAMPS + "/$timestampId"




        return@runBlocking runBlocking {
            WaltIdServices.http.get(href).body<Timestamp>().also {
                it.timestampId = timestampId
                it.href = href
            }
        }
    }

    override fun getByTransactionHash(transactionHash: String): Timestamp? = runBlocking {
        var timestamps = WaltIdServices.http.get(
            WaltIdServices.http.get(TIMESTAMPS).body<Timestamps>().links.last
        ).body<Timestamps>()

        while (timestamps.self != timestamps.links.prev) {
            val timestampsIterator = timestamps.items.listIterator(timestamps.items.size)
            while (timestampsIterator.hasPrevious()) {
                val timestampItem = timestampsIterator.previous()
                val timestamp = runBlocking { WaltIdServices.http.get(timestampItem.href).body<Timestamp>() }
                if (timestamp.transactionHash == transactionHash) {
                    timestamp.timestampId = timestampItem.timestampId
                    timestamp.href = timestampItem.href
                    return@runBlocking timestamp
                }
            }
            timestamps = WaltIdServices.http.get(timestamps.links.prev).body()
        }

        return@runBlocking null
    }

    override fun createTimestamp(did: String, ethKeyAlias: String, data: String): String = runBlocking {
        log.debug { "Running EBSI timestamp hashes... " }

        // Insert timestamp request
        val unsignedTransactionParams = buildUnsignedTransactionParams(did, ethKeyAlias, data)
        log.debug { "TimestampHashes request: $unsignedTransactionParams" }

        val response =
            jsonRpcService.execute(did, ethKeyAlias, TIMESTAMP_JSONRPC, "timestampHashes", unsignedTransactionParams)
        log.debug { "EBSI timestamp hashes completed successfully in transaction $response" }

        response.result
    }

    // TODO: Verify all params are properly defined according to EBSI expectations => https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=EBP&title=DID+Registry+Smart+Contract
    override fun buildUnsignedTransactionParams(
        did: String,
        ethKeyAlias: String?,
        data: String
    ): List<TimestampHashesParams> {
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
