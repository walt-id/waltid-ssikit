package id.walt.services.velocitynetwork

import id.walt.services.WaltIdServices
import java.io.File

object VelocityNetwork {
    const val VELOCITY_NETWORK_REGISTRAR_ENDPOINT = "https://%sregistrar.velocitynetwork.foundation/"
    const val agentUrl = "https://devagent.velocitycareerlabs.io"
    //    const val agentUrl = "http://localhost:8080"

    val VELOCITY_NETWORK_ENV = System.getenv().get("VN_ENV") ?: "staging"
    val VELOCITY_NETWORK_REGISTRAR_API = when (VELOCITY_NETWORK_ENV) {
        "prod" -> ""
        else -> VELOCITY_NETWORK_ENV
    }.let { String.format(VELOCITY_NETWORK_REGISTRAR_ENDPOINT, it) }

    val registrarBearerTokenFile = File("${WaltIdServices.velocityDir}registrar-bearer-token.txt")
    val agentBearerTokenFile = File("${WaltIdServices.velocityDir}agent-bearer-token.txt")
}