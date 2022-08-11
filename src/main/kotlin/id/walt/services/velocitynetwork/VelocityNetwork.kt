package id.walt.services.velocitynetwork

object VelocityNetwork {
    const val AGENT_API_VERSION = "v0.8"
    const val REGISTRAR_API_VERSION = "v0.6"
    const val VELOCITY_NETWORK_REGISTRAR_ENDPOINT = "https://%sregistrar.velocitynetwork.foundation/"
    const val agentUrl = "http://127.0.0.1:8081"

    val VELOCITY_NETWORK_ENV = System.getenv().get("VN_ENV") ?: "staging"
    val VELOCITY_NETWORK_REGISTRAR_API = when (VELOCITY_NETWORK_ENV) {
        "prod" -> ""
        else -> VELOCITY_NETWORK_ENV
    }.let { String.format(VELOCITY_NETWORK_REGISTRAR_ENDPOINT, it) }
}