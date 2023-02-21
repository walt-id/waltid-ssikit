package id.walt.signatory

import com.beust.klaxon.Json
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.W3CIssuer
import id.walt.credentials.w3c.builder.AbstractW3CCredentialBuilder
import id.walt.credentials.w3c.templates.VcTemplate
import id.walt.crypto.LdSignatureType
import id.walt.servicematrix.ServiceConfiguration
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import java.time.Instant

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

