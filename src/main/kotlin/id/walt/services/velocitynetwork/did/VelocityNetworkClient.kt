package id.walt.services.velocitynetwork.did

import id.walt.services.WaltIdServices
import java.io.File

object VelocityNetworkClient {
    val bearerTokenFile = File("${WaltIdServices.velocityDir}bearer-token.txt")
}