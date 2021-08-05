package id.walt.signatory

import id.walt.servicematrix.BaseService
import id.walt.servicematrix.ServiceConfiguration
import id.walt.servicematrix.ServiceMatrix
import id.walt.servicematrix.ServiceProvider
import id.walt.vclib.Helpers.encode
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.templates.VcTemplateManager
import org.bouncycastle.asn1.x500.style.RFC4519Style.name
import id.walt.services.vc.VCService

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
    val issuerVerificationMethod: String, // DID URL => defines key type
    val proofType: ProofType = ProofType.LD_PROOF,
    val domain: String? = null,
    val nonce: String? = null,
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

    override val configuration: SignatoryConfig = fromConfiguration(configurationPath)

    override fun issue(templateId: String, config: ProofConfig): String {
        val vcTemplate = VcTemplateManager.loadTemplate(listOf(templateId)) // TODO WALT 0508

        val dataProvider = DataProviderRegistry.getProvider(vcTemplate::class) // vclib.getUniqueId(vcTemplate)
        val vc = dataProvider.populate(vcTemplate)

        // TODO sign based on config
        // TODO encode
        return VCService.getService()
            .sign(config.issuerDid, vc.encode(), config.domain, config.nonce, config.issuerVerificationMethod)
    }

    override fun listTemplates(): List<String> = VcTemplateManager.listTemplateIds()

    override fun loadTemplate(templateId: String): VerifiableCredential = VcTemplateManager.loadTemplate(listOf(templateId)) // TODO WALT0508

}

fun main() {
    ServiceMatrix("service-matrix.properties")

    val signatory = Signatory.getService()

    val vc = signatory.issue(
        "templateId", ProofConfig(
            issuerDid = "did:ebsi:2A2Rgd4r9HL8KL2hgLC4RdyvZDd1tzoJMYrLaQrxogFTFQAv#5a6ab975123f477ba31b6a3bdd90b916",
            issuerVerificationMethod = "Ed25519Signature2018"
        )
    )

    println(vc)

    //SignatoryRestAPI.start(7004)

}
