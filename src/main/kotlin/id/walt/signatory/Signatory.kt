package id.walt.signatory

import com.beust.klaxon.Json
import id.walt.servicematrix.ServiceConfiguration
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.context.ContextManager
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.templates.VcTemplateManager
import mu.KotlinLogging
import net.pwall.yaml.YAMLSimple.log
import java.time.LocalDateTime
import java.util.*

private val log = KotlinLogging.logger {}

enum class ProofType {
    JWT,
    LD_PROOF
}

data class ProofConfig(
    val issuerDid: String,
    @Json(serializeNull = false) val subjectDid: String? = null,
    @Json(serializeNull = false) val verifierDid: String? = null,
    @Json(serializeNull = false) val issuerVerificationMethod: String? = null, // DID URL that defines key ID; if null the issuers' default key is used
    val proofType: ProofType = ProofType.LD_PROOF,
    @Json(serializeNull = false) val domain: String? = null,
    @Json(serializeNull = false) val nonce: String? = null,
    @Json(serializeNull = false) val proofPurpose: String? = null,
    @Json(serializeNull = false) val credentialId: String? = null,
    @Json(serializeNull = false) val issueDate: LocalDateTime? = null, // issue date from json-input or current system time if null
    @Json(serializeNull = false) val validDate: LocalDateTime? = null, // valid date from json-input or current system time if null
    @Json(serializeNull = false) val expirationDate: LocalDateTime? = null,
    @Json(serializeNull = false) val dataProviderIdentifier: String? = null // may be used for mapping data-sets from a custom data-provider
)

data class SignatoryConfig(
    val proofConfig: ProofConfig
) : ServiceConfiguration

abstract class Signatory : WaltIdService() {
    override val implementation: Signatory get() = serviceImplementation()

    companion object : ServiceProvider {
        override fun getService() = object : Signatory() {}
    }

    open fun issue(templateId: String, config: ProofConfig, dataProvider: SignatoryDataProvider? = null): String = implementation.issue(templateId, config, dataProvider)
    open fun listTemplates(): List<String> = implementation.listTemplates()
    open fun loadTemplate(templateId: String): VerifiableCredential = implementation.loadTemplate(templateId)
}

class WaltIdSignatory(configurationPath: String) : Signatory() {

    private val VC_GROUP = "signatory"
    override val configuration: SignatoryConfig = fromConfiguration(configurationPath)

    override fun issue(templateId: String, config: ProofConfig, dataProvider: SignatoryDataProvider?): String {

        // TODO: load proof-conf from signatory.conf and optionally substitute values on request basis

        val vcTemplate = kotlin.runCatching {
            VcTemplateManager.loadTemplate(templateId)
        }.getOrElse { throw Exception("Could not load template: $templateId") }

        val configDP = when (config.credentialId.isNullOrBlank()) {
            true -> ProofConfig(
                issuerDid = config.issuerDid,
                subjectDid = config.subjectDid,
                null,
                issuerVerificationMethod = config.issuerVerificationMethod,
                proofType = config.proofType,
                domain = config.domain,
                nonce = config.nonce,
                proofPurpose = config.proofPurpose,
                config.credentialId ?: "identity#${templateId}#${UUID.randomUUID()}",
                issueDate = config.issueDate ?: LocalDateTime.now(),
                validDate = config.validDate ?: LocalDateTime.now(),
                expirationDate = config.expirationDate,
                dataProviderIdentifier = config.dataProviderIdentifier
            )
            else -> config
        }

        val selectedDataProvider = dataProvider ?: DataProviderRegistry.getProvider(vcTemplate::class)
        val vcRequest = selectedDataProvider.populate(vcTemplate, configDP)

        log.info { "Signing credential with proof using ${config.proofType.name}..." }
        log.debug { "Signing credential with proof using ${config.proofType.name}, credential is: $vcRequest" }
        val signedVc = when (config.proofType) {
            ProofType.LD_PROOF -> JsonLdCredentialService.getService().sign(vcRequest.encode(), config)
            ProofType.JWT -> JwtCredentialService.getService().sign(vcRequest.encode(), config)
        }
        log.debug { "Signed VC is: $signedVc" }
        ContextManager.vcStore.storeCredential(configDP.credentialId!!, signedVc.toCredential(), VC_GROUP)
        return signedVc
    }

    override fun listTemplates(): List<String> = VcTemplateManager.getTemplateList()

    override fun loadTemplate(templateId: String): VerifiableCredential = VcTemplateManager.loadTemplate(templateId)

}
