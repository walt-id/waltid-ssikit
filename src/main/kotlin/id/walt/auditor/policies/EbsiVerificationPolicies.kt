package id.walt.auditor.policies

import id.walt.auditor.ParameterizedVerificationPolicy
import id.walt.auditor.SimpleVerificationPolicy
import id.walt.auditor.VerificationPolicyResult
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentation
import id.walt.credentials.w3c.verifyByFormatType
import id.walt.model.AttributeInfo
import id.walt.model.TrustedIssuer
import id.walt.services.did.DidService
import id.walt.services.ecosystems.essif.TrustedIssuerClient
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import io.ktor.client.plugins.*
import mu.KotlinLogging

private const val TIR_TYPE_ATTRIBUTE = "attribute"
private const val TIR_NAME_ISSUER = "issuer"

private val log = KotlinLogging.logger {}
private val jsonLdCredentialService = JsonLdCredentialService.getService()
private val jwtCredentialService = JwtCredentialService.getService()

class EbsiTrustedSchemaRegistryPolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by EBSI Trusted Schema Registry"
    override fun doVerify(vc: VerifiableCredential) = vc.verifyByFormatType(
        { jwtCredentialService.validateSchemaTsr(it) },
        { jsonLdCredentialService.validateSchemaTsr(it) }
    )
}

class EbsiTrustedIssuerDidPolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by trusted issuer did"
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        return try {
            VerificationPolicyResult(DidService.loadOrResolveAnyDid(vc.issuerId!!) != null)
        } catch (e: ClientRequestException) {
            VerificationPolicyResult.failure(
                IllegalArgumentException(
                    when {
                        "did must be a valid DID" in e.message -> "did must be a valid DID"
                        "Identifier Not Found" in e.message -> "Identifier Not Found"
                        else -> throw e
                    }
                )
            )
        }
    }
}

data class EbsiTrustedIssuerRegistryPolicyArg(val registryAddress: String)

class EbsiTrustedIssuerRegistryPolicy(registryArg: EbsiTrustedIssuerRegistryPolicyArg) :
    ParameterizedVerificationPolicy<EbsiTrustedIssuerRegistryPolicyArg>(registryArg) {

    constructor(registryAddress: String) : this(
        EbsiTrustedIssuerRegistryPolicyArg(registryAddress)
    )

    constructor() : this(
        EbsiTrustedIssuerRegistryPolicyArg("https://api-pilot.ebsi.eu/trusted-issuers-registry/v2/issuers/")
    )

    override val description: String = "Verify by an EBSI Trusted Issuers Registry compliant api."
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {

        // VPs are not considered
        if (vc is VerifiablePresentation)
            return VerificationPolicyResult.success()

        val issuerDid = vc.issuerId!!

        val resolvedIssuerDid = DidService.loadOrResolveAnyDid(issuerDid)
            ?: throw IllegalArgumentException("Could not resolve issuer DID $issuerDid")

        if (resolvedIssuerDid.id != issuerDid) {
            return VerificationPolicyResult.failure(IllegalArgumentException("Resolved DID ${resolvedIssuerDid.id} does not match the issuer DID $issuerDid"))
        }
        var tirRecord: TrustedIssuer


        return VerificationPolicyResult(runCatching {
            tirRecord = TrustedIssuerClient.getIssuer(issuerDid, argument.registryAddress)
            isValidTrustedIssuerRecord(tirRecord)
        }.getOrElse {
            log.debug { it }
            log.warn { "Could not resolve issuer TIR record of $issuerDid" }
            false
        })
    }

    private fun isValidTrustedIssuerRecord(tirRecord: TrustedIssuer): Boolean {
        for (attribute in tirRecord.attributes) {
            val attributeInfo = AttributeInfo.from(attribute.body)
            log.warn { attributeInfo }
            if (TIR_TYPE_ATTRIBUTE == attributeInfo?.type && TIR_NAME_ISSUER == attributeInfo.name) {
                return true
            }
        }
        return false
    }

    override var applyToVP: Boolean = false
}


class EbsiTrustedSubjectDidPolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by trusted subject did"
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        return VerificationPolicyResult(vc.subjectId?.let {
            if (it.isEmpty()) true
            else try {
                DidService.loadOrResolveAnyDid(it) != null
            } catch (e: ClientRequestException) {
                if (!e.message.contains("did must be a valid DID") && !e.message.contains("Identifier Not Found")) throw e
                false
            }
        } ?: false)
    }
}
