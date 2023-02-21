package id.walt.signatory

import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.W3CIssuer
import id.walt.credentials.w3c.builder.AbstractW3CCredentialBuilder
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.credentials.w3c.templates.VcTemplate
import id.walt.credentials.w3c.templates.VcTemplateService
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.crypto.LdSignatureType
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.services.context.ContextManager
import id.walt.services.did.DidService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

open class WaltIdSignatory(configurationPath: String) : Signatory() {
    private val log = KotlinLogging.logger {}

    private val VC_GROUP = "signatory"
    override val configuration: SignatoryConfig = fromConfiguration(configurationPath)
    
    private val templateService get() = VcTemplateService.getService() 

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
            else -> templateService.getTemplate(templateIdOrFilename, true, configuration.templatesFolder).template
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

    override fun listTemplates(): List<VcTemplate> = templateService.listTemplates(configuration.templatesFolder)
    override fun listTemplateIds() = templateService.listTemplates(configuration.templatesFolder).map { it.name }
    override fun loadTemplate(templateId: String): VerifiableCredential =
        templateService.getTemplate(templateId, true, configuration.templatesFolder).template!!

    override fun importTemplate(templateId: String, template: String) {
        val vc = VerifiableCredential.fromJson(template)
        // serialize parsed credential template and save
        templateService.register(templateId, vc)
    }

    override fun removeTemplate(templateId: String) {
        val template = templateService.getTemplate(templateId, true, configuration.templatesFolder)
        if (template.mutable) {
            templateService.unregisterTemplate(templateId)
        } else {
            throw Exception("Template is immutable and cannot be removed. Use import to override existing templates.")
        }
    }

}
