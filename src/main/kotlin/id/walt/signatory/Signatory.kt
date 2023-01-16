package id.walt.signatory

import com.beust.klaxon.Json
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.W3CIssuer
import id.walt.credentials.w3c.builder.AbstractW3CCredentialBuilder
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.credentials.w3c.templates.VcTemplate
import id.walt.credentials.w3c.templates.VcTemplateManager
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.crypto.LdSignatureType
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.servicematrix.ServiceConfiguration
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.context.ContextManager
import id.walt.services.did.DidService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

private val log = KotlinLogging.logger {}

enum class ProofType {
    JWT, LD_PROOF
}

enum class Ecosystem {
    DEFAULT,
    ESSIF,
    GAIAX,
    IOTA
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
    @Json(serializeNull = false) val issueDate: Instant? = null, // issue date from json-input or current system time if null
    @Json(serializeNull = false) val validDate: Instant? = null, // valid date from json-input or current system time if null
    @Json(serializeNull = false) val expirationDate: Instant? = null,
    @Json(serializeNull = false) val dataProviderIdentifier: String? = null, // may be used for mapping data-sets from a custom data-provider
    @Json(serializeNull = false) val ldSignatureType: LdSignatureType? = null,
    @Json(serializeNull = false) val creator: String? = issuerDid,
    @Json(serializeNull = false) val ecosystem: Ecosystem = Ecosystem.DEFAULT
)

data class SignatoryConfig(
    val proofConfig: ProofConfig,
    val templatesFolder: String = "/vc-templates-runtime"
) : ServiceConfiguration

abstract class Signatory : WaltIdService() {
    override val implementation: Signatory get() = serviceImplementation()

    companion object : ServiceProvider {
        override fun getService() = object : Signatory() {}
    }

    open fun issue(
        templateIdOrFilename: String,
        config: ProofConfig,
        dataProvider: SignatoryDataProvider? = null,
        issuer: W3CIssuer? = null,
        storeCredential: Boolean = false
    ): String =
        implementation.issue(templateIdOrFilename, config, dataProvider, issuer, storeCredential)

    open fun issue(
        credentialBuilder: AbstractW3CCredentialBuilder<*, *>,
        config: ProofConfig,
        issuer: W3CIssuer? = null,
        storeCredential: Boolean = false
    ): String = implementation.issue(credentialBuilder, config, issuer)

    open fun listTemplates(): List<VcTemplate> = implementation.listTemplates()
    open fun listTemplateIds(): List<String> = implementation.listTemplateIds()
    open fun loadTemplate(templateId: String): VerifiableCredential = implementation.loadTemplate(templateId)

    open fun importTemplate(templateId: String, template: String): Unit = implementation.importTemplate(templateId, template)
    open fun removeTemplate(templateId: String): Unit = implementation.removeTemplate(templateId)
}

class WaltIdSignatory(configurationPath: String) : Signatory() {

    private val VC_GROUP = "signatory"
    override val configuration: SignatoryConfig = fromConfiguration(configurationPath)

    private fun defaultLdSignatureByDidMethod(did: String): LdSignatureType? {
        val didUrl = DidUrl.from(did)
        return when (didUrl.method) {
            DidMethod.iota.name -> LdSignatureType.JcsEd25519Signature2020
            else -> null
        }
    }

    private fun issuerVerificationMethodFor(config: ProofConfig): String? {
        val did = DidService.load(config.issuerDid)
        val proofPurpose = config.proofPurpose ?: "assertionMethod"
        return when (proofPurpose) {
            "assertionMethod" -> did.assertionMethod?.firstOrNull { (id) -> config.issuerVerificationMethod == null || id == config.issuerVerificationMethod }
            "authentication" -> did.authentication?.firstOrNull { (id) -> config.issuerVerificationMethod == null || id == config.issuerVerificationMethod }
            "capabilityDelegation" -> did.capabilityDelegation?.firstOrNull { (id) -> config.issuerVerificationMethod == null || id == config.issuerVerificationMethod }
            "capabilityInvocation" -> did.capabilityInvocation?.firstOrNull { (id) -> config.issuerVerificationMethod == null || id == config.issuerVerificationMethod }
            "keyAgreement" -> did.keyAgreement?.firstOrNull { (id) -> config.issuerVerificationMethod == null || id == config.issuerVerificationMethod }
            else -> did.verificationMethod?.firstOrNull { (id) -> config.issuerVerificationMethod == null || id == config.issuerVerificationMethod }
        }?.id ?: config.issuerVerificationMethod
    }

    private fun fillProofConfig(config: ProofConfig): ProofConfig {
        return ProofConfig(
            issuerDid = config.issuerDid,
            subjectDid = config.subjectDid,
            null,
            issuerVerificationMethod = issuerVerificationMethodFor(config),
            proofType = config.proofType,
            domain = config.domain,
            nonce = config.nonce,
            proofPurpose = config.proofPurpose,
            credentialId = config.credentialId.orEmpty().ifEmpty { "urn:uuid:${UUID.randomUUID()}" },
            issueDate = config.issueDate ?: Instant.now(),
            validDate = config.validDate ?: Instant.now(),
            expirationDate = config.expirationDate,
            dataProviderIdentifier = config.dataProviderIdentifier,
            ldSignatureType = config.ldSignatureType ?: defaultLdSignatureByDidMethod(config.issuerDid),
            creator = config.creator
        )
    }

    override fun issue(
        templateIdOrFilename: String,
        config: ProofConfig,
        dataProvider: SignatoryDataProvider?,
        issuer: W3CIssuer?,
        storeCredential: Boolean
    ): String {

        val credentialBuilder = when (Files.exists(Path.of(templateIdOrFilename))) {
            true -> Files.readString(Path.of(templateIdOrFilename)).toVerifiableCredential()
            else -> VcTemplateManager.getTemplate(templateIdOrFilename, true, configuration.templatesFolder).template
        }?.let { W3CCredentialBuilder.fromPartial(it) } ?: throw Exception("Template not found")

        return issue(dataProvider?.populate(credentialBuilder, config) ?: credentialBuilder, config, issuer, storeCredential)
    }

    override fun issue(
        credentialBuilder: AbstractW3CCredentialBuilder<*, *>,
        config: ProofConfig,
        issuer: W3CIssuer?,
        storeCredential: Boolean
    ): String {
        val fullProofConfig = fillProofConfig(config)
        val vcRequest = credentialBuilder.apply {
            issuer?.let { setIssuer(it) }
            setIssuerId(fullProofConfig.issuerDid)
            setIssuanceDate(fullProofConfig.issueDate ?: Instant.now())
            setIssued(fullProofConfig.issueDate ?: Instant.now())
            fullProofConfig.subjectDid?.let { setSubjectId(it) }
            setId(fullProofConfig.credentialId.orEmpty().ifEmpty { "urn:uuid:${UUID.randomUUID()}" })
            setIssuanceDate(fullProofConfig.issueDate ?: Instant.now())
            setValidFrom(fullProofConfig.validDate ?: Instant.now())
            fullProofConfig.expirationDate?.let { setExpirationDate(it) }
        }.build()

        log.info { "Signing credential with proof using ${fullProofConfig.proofType.name}..." }
        log.debug { "Signing credential with proof using ${fullProofConfig.proofType.name}, credential is: $vcRequest" }
        val signedVc = when (fullProofConfig.proofType) {
            ProofType.LD_PROOF -> JsonLdCredentialService.getService().sign(vcRequest.toJson(), fullProofConfig)
            ProofType.JWT -> JwtCredentialService.getService().sign(vcRequest.toJson(), fullProofConfig)
        }
        log.debug { "Signed VC is: $signedVc" }

        if (storeCredential) {
            ContextManager.vcStore.storeCredential(fullProofConfig.credentialId!!, signedVc.toVerifiableCredential(), VC_GROUP)
        }

        return signedVc
    }

    override fun listTemplates(): List<VcTemplate> = VcTemplateManager.listTemplates(configuration.templatesFolder)
    override fun listTemplateIds() = VcTemplateManager.listTemplates(configuration.templatesFolder).map { it.name }
    override fun loadTemplate(templateId: String): VerifiableCredential =
        VcTemplateManager.getTemplate(templateId, true, configuration.templatesFolder).template!!

    override fun importTemplate(templateId: String, template: String) {
        val vc = VerifiableCredential.fromJson(template)
        // serialize parsed credential template and save
        VcTemplateManager.register(templateId, vc)
    }

    override fun removeTemplate(templateId: String) {
        val template = VcTemplateManager.getTemplate(templateId,true, configuration.templatesFolder)
        if (template.mutable) {
            VcTemplateManager.unregisterTemplate(templateId)
        } else {
            throw Exception("Template is immutable and cannot be removed. Use import to override existing templates.")
        }
    }

}
