package id.walt.signatory

import id.walt.custodian.CustodianService
import id.walt.servicematrix.BaseService
import id.walt.servicematrix.ServiceConfiguration
import id.walt.servicematrix.ServiceProvider
import id.walt.services.vc.JwtCredentialService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vcstore.VcStoreService
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.templates.VcTemplateManager
import java.util.*

// JWT are using the IANA types of signatures: alg=EdDSA oder ES256 oder ES256K oder RS256
//enum class ProofType {
//    JWT_EdDSA,
//    JWT_ES256,
//    JWT_ES256K,
//    LD_PROOF_Ed25519Signature2018,
//    LD_PROOF_EcdsaSecp256k1Signature2019
//}

// Assuming the detailed algorithm will be derived from the issuer key algorithm
enum class ProofType {
    JWT,
    LD_PROOF
}

data class ProofConfig(
    val issuerDid: String,
    val subjectDid: String? = null, // if null and ProofType.LD_PROOF -> subject DID from json-input
    val verifierDid: String? = null,
    val issuerVerificationMethod: String? = null, // DID URL => defines key type; if null and ProofType.LD_PROOF -> issuerDid default key
    val proofType: ProofType = ProofType.LD_PROOF,
    val domain: String? = null,
    val nonce: String? = null,
    val proofPurpose: String? = null,
    val id: String? = null, // if null and ProofType.LD_PROOF -> generated with UUID random value
    val issueDate: Date? = null, // if null and ProofType.LD_PROOF -> issue date from json-input or now if null as well
    val validDate: Date? = null, // if null and ProofType.LD_PROOF -> valid date from json-input or now if null as well
    val expirationDate: Date? = null
)

data class SignatoryConfig(
    val proofConfig: ProofConfig
) : ServiceConfiguration

interface ISignatory {

    fun issue(templateId: String, config: ProofConfig): String

}

abstract class Signatory : BaseService(), ISignatory {
    override val implementation: Signatory get() = serviceImplementation()

    companion object : ServiceProvider {
        override fun getService() = object : Signatory() {}
    }

    override fun issue(templateId: String, config: ProofConfig): String = implementation.issue(templateId, config)
    open fun listTemplates(): List<String> = implementation.listTemplates()
    open fun loadTemplate(templateId: String): VerifiableCredential = implementation.loadTemplate(templateId)
}

class WaltSignatory(configurationPath: String) : Signatory() {

    private val vcStore = VcStoreService.getService()
    private val VC_GROUP = "signatory"
    override val configuration: SignatoryConfig = fromConfiguration(configurationPath)

    override fun issue(templateId: String, config: ProofConfig): String {

        // TODO: load proof-conf from signatory.conf and optionally substitute values on request basis
        val vcTemplate = VcTemplateManager.loadTemplate(templateId)
        val configDP = when (config.id.isNullOrBlank()) {
            true -> ProofConfig(
                config.issuerDid, config.subjectDid, null, config.issuerVerificationMethod, config.proofType,
                config.domain, config.nonce, config.proofPurpose,
                "identity#${templateId}#${UUID.randomUUID()}",
                config.issueDate, config.expirationDate
            )
            else -> config
        }
        val dataProvider = DataProviderRegistry.getProvider(vcTemplate::class) // vclib.getUniqueId(vcTemplate)
        val vcRequest = dataProvider.populate(vcTemplate, configDP)

        val signedVc = when (config.proofType) {
            ProofType.LD_PROOF -> JsonLdCredentialService.getService().sign(vcRequest.encode(), config)
            ProofType.JWT -> JwtCredentialService.getService().sign(vcRequest.encode(), config)
        }
        vcStore.storeCredential(configDP.id!!, signedVc.toCredential(), VC_GROUP)
        return signedVc
    }

    override fun listTemplates(): List<String> = VcTemplateManager.getTemplateList()

    override fun loadTemplate(templateId: String): VerifiableCredential = VcTemplateManager.loadTemplate(templateId)

}
