package id.walt.services.velocitynetwork

import id.walt.model.DidUrl
import id.walt.services.WaltIdServices
import id.walt.services.velocitynetwork.did.DidVelocityService
import id.walt.services.velocitynetwork.issuer.IssuerVelocityService
import id.walt.services.velocitynetwork.models.responses.OfferResponse
import id.walt.services.velocitynetwork.verifier.VerifierVelocityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.pwall.json.schema.JSONSchema
import java.net.URLDecoder

object VelocityClient {

    data class OfferChoice(
        val accepted: List<String>,
        val rejected: List<String>,
    )

    private val log = KotlinLogging.logger {}
    private val issuerService = IssuerVelocityService.getService()
    private val didService = DidVelocityService.getService()
    private val verifierService = VerifierVelocityService.getService()

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
        val org = didService.onboard(data)
        WaltIdServices.clearBearerTokens()
        org
    }

    fun issue(issuerDid: String, credential: String, token: String): String = runBlocking {
        WaltIdServices.addBearerToken(token)
        val exchangeId = issuerService.initExchange(issuerDid).id
        issuerService.addOffer(issuerDid, exchangeId, credential)
        issuerService.completeOffer(issuerDid, exchangeId)
        val uri = issuerService.claimOffer(issuerDid, exchangeId)
        WaltIdServices.clearBearerTokens()
        withContext(Dispatchers.IO) {
            URLDecoder.decode(uri, "UTF-8")
        }
    }

    //TODO: holder data - accept email instead of credential
    fun issue(
        holderIdentity: String,
        issuerDid: String,
        vararg credentialTypes: String,
        offerSelection: suspend (offers: List<OfferResponse>) -> OfferChoice
    ): List<String> =
        runBlocking {
            // step 1: exchange id
            val exchangeId = issuerService.initExchange(issuerDid).id
            log.debug { "Using exchangeId $exchangeId" }
            // step 2: identification
            val token = issuerService.initDisclosure(exchangeId, holderIdentity, issuerDid).token
            WaltIdServices.addBearerToken(token)
            log.debug { "Using token $token" }
            // step 3: offers
            val offers = issuerService.generateOffers(exchangeId, issuerDid, credentialTypes.toList())
            log.debug { "Selecting offers from ${offers.map { it.id }}" }
            // step 3.1
            val selection = offerSelection(offers)
            log.debug { "Finalizing offers ${selection.accepted}" }
            // step 4: get credential
            val credentials = issuerService.finalizeOffers(exchangeId, issuerDid, selection.accepted, selection.rejected)
            log.debug { "Credentials $credentials" }
            WaltIdServices.clearBearerTokens()
            credentials
        }

    fun resolveDid(did: String) = runBlocking {
        val didUrl = DidUrl.from(did)
        log.debug { "Resolving DID ${didUrl.did}..." }
        WaltIdServices.addBearerToken(VelocityNetwork.agentBearerTokenFile.readText())
        val resolved = didService.resolveDid(didUrl)
        WaltIdServices.clearBearerTokens()
        resolved
    }

    fun verify(issuer: String, credentialId: String, credential: String, token: String) = runBlocking {
        WaltIdServices.addBearerToken(token)
        val inspectionResult = verifierService.check(issuer, credentialId, credential)
        WaltIdServices.clearBearerTokens()
        inspectionResult
    }

    private fun validate(data: String) =
        JSONSchema.parseFile("src/main/resources/velocitynetwork/schemas/organization-registration-reqSchema.json")
            .validateBasic(data).valid
}