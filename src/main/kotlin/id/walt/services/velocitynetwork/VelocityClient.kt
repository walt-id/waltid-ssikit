package id.walt.services.velocitynetwork

import id.walt.common.readBearerToken
import id.walt.common.readWhenContent
import id.walt.crypto.buildKey
import id.walt.services.WaltIdServices
import id.walt.services.did.DidService
import id.walt.services.essif.EssifClient
import id.walt.services.keystore.KeyStoreService
import id.walt.services.velocitynetwork.did.DidVelocityService
import mu.KotlinLogging
import java.io.File

object VelocityClient {
    val registrarBearerTokenFile = File("${WaltIdServices.velocityDir}registrar-bearer-token.txt")
    val agentBearerTokenFile = File("${WaltIdServices.velocityDir}agent-bearer-token.txt")

    private val log = KotlinLogging.logger {}
    private val didService = DidVelocityService.getService()
    private val keyStore = KeyStoreService.getService()

    fun register(data: String?, token: String? = null) {
        val bearerToken = token ?: readBearerToken(
            registrarBearerTokenFile,
            "The bearer token must be placed in file ${EssifClient.bearerTokenFile.absolutePath}." +
                    "https://docs.velocitynetwork.foundation/docs/developers/developers-guide-getting-started#register-an-organization"
        )
        log.debug { "Using bearer token $bearerToken." }
        val json = data ?: readWhenContent(File("src/main/resources/velocitynetwork/samples/organization-registration-req.json"))
        didService.registerOrganization(json, bearerToken) { did, doc, keys, auth ->
            DidService.storeDid(did, doc)//TODO: prep did
            keys.forEach {
                keyStore.store(buildKey(it.id, it.algorithm, "", it.publicKey, it.key))
            }

        }
    }
}