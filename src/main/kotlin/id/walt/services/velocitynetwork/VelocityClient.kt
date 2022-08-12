package id.walt.services.velocitynetwork

import com.beust.klaxon.Klaxon
import id.walt.common.validateForSchema
import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.services.WaltIdServices
import id.walt.services.velocitynetwork.did.DidVelocityService
import id.walt.services.velocitynetwork.issuer.IssuerVelocityService
import id.walt.services.velocitynetwork.models.CredentialCheckType
import id.walt.services.velocitynetwork.models.CredentialCheckValue
import id.walt.services.velocitynetwork.models.VerificationResult
import id.walt.services.velocitynetwork.models.responses.InspectionResult
import id.walt.services.velocitynetwork.models.responses.OfferResponse
import id.walt.services.velocitynetwork.onboarding.TenantVelocityService
import id.walt.services.velocitynetwork.verifier.VerifierVelocityService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.net.URLDecoder

object VelocityClient {

    data class OfferChoice(
        val accepted: List<String>,
        val rejected: List<String>,
    )

    val agentTokenFile = File("${WaltIdServices.velocityDir}/agent-token.txt")
    val registrarTokenFile = File("${WaltIdServices.velocityDir}/registrar-token.txt")

    val config = WaltIdServices.loadVelocityConfig()

    private val log = KotlinLogging.logger {}
    private val issuerService = IssuerVelocityService.getService()
    private val didService = DidVelocityService.getService()
    private val verifierService = VerifierVelocityService.getService()
    private val tenantService = TenantVelocityService.getService()

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

    fun register(data: String) = runBlocking {
        log.debug { "Registering organization on Velocity Network... " }
        if (!validateForSchema(
                "src/main/resources/velocity/schemas/organization-registration-requestSchema.json",
                data
            )
        ) throw Exception("Schema validation failed.")
        WaltIdServices.callWithToken(registrarTokenFile.readText()){
            didService.onboard(data)
        }
    }

    fun addTenant(data: String) = runBlocking {
        log.debug { "Setting up new tenant with credential agent.. " }
        WaltIdServices.callWithToken(agentTokenFile.readText()) {
            tenantService.create(data)
        }
    }

    fun addDisclosure(did: String, data: String) = runBlocking {
        log.debug { "Adding disclosure.." }
        WaltIdServices.callWithToken(agentTokenFile.readText()){
            tenantService.addDisclosure(did, data)
        }
    }

    fun issue(issuerDid: String, credential: String, token: String): String = runBlocking {
        WaltIdServices.addBearerToken(agentTokenFile.readText())
        val exchangeId = issuerService.initExchange(issuerDid).id
        issuerService.addOffer(issuerDid, exchangeId, credential)
        issuerService.completeOffer(issuerDid, exchangeId)
        val uri = issuerService.claimOffer(issuerDid, exchangeId)
        WaltIdServices.clearBearerTokens()
        withContext(Dispatchers.IO) {
            URLDecoder.decode(uri, "UTF-8")
        }
    }

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
        log.debug { "Resolving DID ${didUrl.did}.." }
        WaltIdServices.callWithToken(agentTokenFile.readText()){
            didService.resolveDid(didUrl)
        }
    }

    fun verify(issuer: String, credential: String, checks: Map<CredentialCheckType, CredentialCheckValue>) =
        runBlocking {
            checkCredential(issuer, credential).fold(
                onSuccess = { checkResult ->
                    Result.success(validateResult(checkResult, checks))
                },
                onFailure = {
                    Result.failure(it)
                })
        }

    fun healthCheck() = runBlocking {
        runCatching { WaltIdServices.httpNoAuth.get(config.agentEndpoint).status == HttpStatusCode.OK }.getOrElse { false }
    }

    private suspend fun checkCredential(issuer: String, credential: String) =
        WaltIdServices.callWithToken(agentTokenFile.readText()) {
            verifierService.check(issuer, credential)
        }

    private fun validateInspection(
        credential: InspectionResult.Credential,
        checks: Map<CredentialCheckType, CredentialCheckValue>
    ) = credential.credentialChecks.filter {
        checks.containsKey(it.key)
    }.equals(checks)

    private fun validateResult(inspectionResult: String, checks: Map<CredentialCheckType, CredentialCheckValue>) =
        try {
            Klaxon().parse<InspectionResult>(inspectionResult)!!.let {
                it.credentials.map {
                    //TODO: specify failed check
                    VerificationResult(it.credentialChecks, validateInspection(it, checks))
                }
            }
        } catch (e: Exception) {
            log.error { e }
            throw e
        }
}