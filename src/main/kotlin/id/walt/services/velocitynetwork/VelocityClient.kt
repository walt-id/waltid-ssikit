package id.walt.services.velocitynetwork

import id.walt.model.DidUrl
import id.walt.services.WaltIdServices
import id.walt.services.velocitynetwork.did.DidVelocityService
import id.walt.services.velocitynetwork.issuer.IssuerVelocityService
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.pwall.json.schema.JSONSchema

object VelocityClient {

    private val log = KotlinLogging.logger {}
    private val issuerService = IssuerVelocityService.getService()
    private val didService = DidVelocityService.getService()

//    fun registerOrganization(data: String, token: String) = runBlocking {
//        log.debug { "Registering organization on Velocity Network... " }
//        if (!validate(data)) throw Exception("Schema validation failed.")
//        WaltIdServices.addBearerToken(token)
//        bodyAsText().let { response ->
//            Klaxon().parse<CreateOrganizationResponse>(response)?.let {
//                DidService.importDidAndDoc(it.id, it.didDoc.encodePretty())
//                File(WaltIdServices.velocityDir + it.id).writeText(response)
//                it.id
//            } ?: throw Exception("Error parsing response")
//        }
//    }
    
    fun register(data: String, token: String) = runBlocking {
        log.debug { "Registering organization on Velocity Network... " }
        if (!validate(data)) throw Exception("Schema validation failed.")
        WaltIdServices.addBearerToken(token)
        didService.onboard(data)
    }

    //TODO: holder data - accept email instead of credential
    fun issue(holderIdentity: String, issuerDid: String, vararg credentialTypes: String): List<String> =
        runBlocking {
            // step 1: exchange id
            val exchangeId = issuerService.initExchange(issuerDid).exchangeId
            log.debug { "Using exchangeId $exchangeId" }
            // step 2: identification
            val token = issuerService.initDisclosure(exchangeId, holderIdentity, issuerDid).token
            WaltIdServices.addBearerToken(token)
            log.debug { "Using token $token" }
            // step 3: offers
            val offers = issuerService.generateOffers(exchangeId, issuerDid, credentialTypes.toList())
            log.debug { "Finalizing offers ${offers.map { it.id }}" }
            // step 4: get credential
            val credentials = issuerService.finalizeOffers(exchangeId, issuerDid, offers.map { it.id })
            log.debug { "Credentials $credentials" }
            credentials
        }

    fun resolveDid(did: String) = runBlocking {
        val didUrl = DidUrl.from(did)
        log.debug { "Resolving DID ${didUrl.did}..." }
        WaltIdServices.addBearerToken(VelocityNetwork.agentBearerTokenFile.readText())
        didService.resolveDid(didUrl)
    }

    private fun validate(data: String) =
        JSONSchema.parseFile("src/main/resources/velocitynetwork/schemas/organization-registration-reqSchema.json")
            .validateBasic(data).valid
}