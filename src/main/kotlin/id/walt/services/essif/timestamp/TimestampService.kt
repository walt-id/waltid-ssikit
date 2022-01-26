package id.walt.services.essif.timestamp

import id.walt.servicematrix.BaseService
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.essif.jsonrpc.TimestampHashesParams

open class TimestampService : WaltIdService() {

    override val implementation get() = serviceImplementation<TimestampService>()

    open fun getByTimestampId(timestampId: String): Timestamp? = implementation.getByTimestampId(timestampId)
    open fun getByTransactionHash(transactionHash: String): Timestamp? = implementation.getByTransactionHash(transactionHash)

    open fun createTimestamp(did: String, ethKeyAlias: String, data: String): String =
        implementation.createTimestamp(did, ethKeyAlias, data)

    open fun buildUnsignedTransactionParams(
        did: String,
        ethKeyAlias: String? = null,
        data: String
    ): List<TimestampHashesParams> =
        implementation.buildUnsignedTransactionParams(did, ethKeyAlias, data)

    companion object : ServiceProvider {
        override fun getService() = object : TimestampService() {}
        override fun defaultImplementation() = WaltIdTimestampService()
    }
}
