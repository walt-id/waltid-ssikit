package id.walt.services.ecosystems.velocity

import com.beust.klaxon.Klaxon
import id.walt.common.parseResponse
import id.walt.common.validateForSchema
import id.walt.model.DidUrl
import id.walt.services.WaltIdServices
import id.walt.services.ecosystems.velocity.did.DidVelocityService
import id.walt.services.ecosystems.velocity.issuer.IssuerVelocityService
import id.walt.services.ecosystems.velocity.models.*
import id.walt.services.ecosystems.velocity.onboarding.OfferVelocityService
import id.walt.services.ecosystems.velocity.tenant.TenantVelocityService
import id.walt.services.ecosystems.velocity.verifier.VerifierVelocityService
import io.ktor.client.request.*
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
    private val offerService = OfferVelocityService.getService()

    fun register(data: String) = runBlocking {
        log.debug { "Registering organization on Velocity Network... " }
        if (!validateForSchema(
                "src/main/resources/velocity/schemas/organization-registration-requestSchema.json",
                data
            )
        ) throw Exception("Schema validation failed.")
        WaltIdServices.callWithToken(registrarTokenFile.readText()) {
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
        WaltIdServices.callWithToken(agentTokenFile.readText()) {
            tenantService.addDisclosure(did, data)
        }
    }

    fun offer(issuerDid: String, disclosureId: String, idMatcher: List<String>, offer: String) = runBlocking {
        runCatching {
            WaltIdServices.callWithToken(agentTokenFile.readText()) {
                val exchange =
                    parseResponse<IssuerExchangeResponse>(offerService.initExchange(issuerDid, disclosureId, idMatcher))
                offerService.addOffer(issuerDid, exchange.id, offer)
                offerService.completeOffer(issuerDid, exchange.id)
                offerService.claimOffer(issuerDid, exchange.id)
            }.getOrThrow()
        }
    }

    fun issue(
        holderIdentity: String,
        issuerDid: String,
        vararg credentialTypes: String,
        offerSelection: suspend (offers: List<OfferResponse>) -> OfferChoice
    ) = runBlocking {
        runCatching {
            // step 1: exchange id
            val exchange = parseResponse<HolderExchangeResponse>(issuerService.initExchange(issuerDid))
            log.debug { "Using exchangeId ${exchange.exchangeId}" }
            // step 2: identification
            val disclosure = parseResponse<DisclosureResponse>(
                issuerService.initDisclosure(
                    exchange.exchangeId,
                    holderIdentity,
                    issuerDid
                )
            )

            WaltIdServices.callWithToken(disclosure.token) {
                // step 3: offers
                val offers = parseResponse<List<OfferResponse>>(
                    issuerService.generateOffers(
                        exchange.exchangeId,
                        issuerDid,
                        credentialTypes.toList()
                    )
                )
                log.debug { "Selecting offers from ${offers.map { it.id }}" }
                // step 3.1
                val selection = offerSelection(offers)
                log.debug { "Finalizing offers ${selection.accepted}" }
                // step 4: get credential
                issuerService.finalizeOffers(exchange.exchangeId, issuerDid, selection.accepted, selection.rejected)
            }
        }
    }

    fun resolveDid(did: String) = runBlocking {
        val didUrl = DidUrl.from(did)
        log.debug { "Resolving DID ${didUrl.did}.." }
        WaltIdServices.callWithToken(agentTokenFile.readText()) {
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
