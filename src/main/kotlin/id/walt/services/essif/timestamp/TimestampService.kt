package id.walt.services.essif.timestamp

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.essif.jsonrpc.TimestampHashesParams

open class TimestampService : WaltIdService() {

    override val implementation get() = serviceImplementation<TimestampService>()

    open fun timestampHashes(did: String, ethKeyAlias: String, data: String): Unit =
        implementation.timestampHashes(did, ethKeyAlias, data)

    open fun buildUnsignedTransactionParams(
        did: String,
        ethKeyAlias: String? = null,
        data: String
    ): List<TimestampHashesParams> =
        implementation.buildUnsignedTransactionParams(did, ethKeyAlias, data)

    companion object : ServiceProvider {
        override fun getService() = object : TimestampService() {}
    }
}
