package id.walt.auditor

import id.walt.model.TrustedIssuer
import id.walt.services.did.DidService
import id.walt.services.essif.TrustedIssuerClient
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import id.walt.services.vc.VcUtils
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.VerifiablePresentation
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashMap

// the following validation policies can be applied
// - SIGNATURE
// - JSON_SCHEMA
// - TRUSTED_ISSUER_DID
// - TRUSTED_SUBJECT_DID
// - REVOCATION_STATUS
// - ISSUANCE_DATA_AFTER
// - EXPIRATION_DATE_BEFORE
// - REVOCATION_STATUS
// - SECURE_CRYPTO
// - HOLDER_BINDING (only for VPs)

private val log = KotlinLogging.logger {}

private val jsonLdCredentialService = JsonLdCredentialService.getService()
private val jwtCredentialService = JwtCredentialService.getService()
private val dateFormatter =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").also { it.timeZone = TimeZone.getTimeZone("UTC") }

@Serializable
data class VerificationPolicyMetadata(val description: String, val id: String)

interface VerificationPolicy {
    val id: String
        get() = this.javaClass.simpleName
    val description: String
    fun verify(vc: VerifiableCredential): Boolean
}

class SignaturePolicy : VerificationPolicy {
    override val description: String = "Verify by signature"
    override fun verify(vc: VerifiableCredential): Boolean {
        return try {
            log.debug { "is jwt: ${vc.jwt != null}" }

            DidService.importKey(VcUtils.getIssuer(vc)) && when (vc.jwt) {
                null -> jsonLdCredentialService.verify(vc.json!!).verified
                else -> jwtCredentialService.verify(vc.jwt!!).verified
            }
        } catch (e: Exception) {
            log.error(e.localizedMessage)
            false
        }
    }
}

class JsonSchemaPolicy : VerificationPolicy {
    override val description: String = "Verify by JSON schema"
    override fun verify(vc: VerifiableCredential): Boolean {
        return when (vc.jwt) {
            null -> jsonLdCredentialService.validateSchema(vc.encode()) // Schema already validated by json-ld?
            else -> jwtCredentialService.validateSchema(vc.encode())
        }
    }
}

class TrustedIssuerDidPolicy : VerificationPolicy {
    override val description: String = "Verify by trusted issuer did"
    override fun verify(vc: VerifiableCredential): Boolean {
        return DidService.loadOrResolveAnyDid(VcUtils.getIssuer(vc)) != null
    }
}

class TrustedIssuerRegistryPolicy : VerificationPolicy {
    override val description: String = "Verify by trusted EBSI Trusted Issuer Registry record"
    override fun verify(vc: VerifiableCredential): Boolean {

        // VPs are not considered
        if (vc is VerifiablePresentation) {
            return true
        }

        val issuerDid = VcUtils.getIssuer(vc)

        val resolvedIssuerDid =
            DidService.loadOrResolveAnyDid(issuerDid) ?: throw Exception("Could not resolve issuer DID $issuerDid")

        if (resolvedIssuerDid.id != issuerDid) {
            log.debug { "Resolved DID ${resolvedIssuerDid.id} does not match the issuer DID $issuerDid" }
            return false
        }

        val tirRecord = try {
            TrustedIssuerClient.getIssuer(issuerDid)
        } catch (e: Exception) {
            throw Exception("Could not resolve issuer TIR record of $issuerDid", e)
        }

        return validTrustedIssuerRecord(tirRecord)

    }

    private fun validTrustedIssuerRecord(tirRecord: TrustedIssuer): Boolean {
        var issuerRecordValid = true

        if (tirRecord.attributes[0].body != "eyJAY29udGV4dCI6Imh0dHBzOi8vZWJzaS5ldSIsInR5cGUiOiJhdHRyaWJ1dGUiLCJuYW1lIjoiaXNzdWVyIiwiZGF0YSI6IjVkNTBiM2ZhMThkZGUzMmIzODRkOGM2ZDA5Njg2OWRlIn0=") {
            issuerRecordValid = false
            log.debug { "Body of TIR record ${tirRecord} not valid." }
        }

        if (tirRecord.attributes[0].hash != "14f2d3c3320f65b6fd9413608e4c17f831e3c595ad61222ec12f899752348718") {
            issuerRecordValid = false
            log.debug { "Body of TIR record ${tirRecord} not valid." }
        }
        return issuerRecordValid
    }
}

class TrustedSubjectDidPolicy : VerificationPolicy {
    override val description: String = "Verify by trusted subject did"
    override fun verify(vc: VerifiableCredential): Boolean {
        return VcUtils.getSubject(vc).let {
            if (it.isEmpty()) true
            else DidService.loadOrResolveAnyDid(it) != null
        }
    }
}

class IssuanceDateBeforePolicy : VerificationPolicy {
    override val description: String = "Verify by issuance date"
    override fun verify(vc: VerifiableCredential): Boolean {
        return when (vc) {
            is VerifiablePresentation -> true
            else -> parseDate(VcUtils.getIssuanceDate(vc)).let { it != null && it.before(Date()) }
        }
    }
}

class ValidFromBeforePolicy : VerificationPolicy {
    override val description: String = "Verify by valid from"
    override fun verify(vc: VerifiableCredential): Boolean {
        return when (vc) {
            is VerifiablePresentation -> true
            else -> parseDate(VcUtils.getValidFrom(vc)).let { it != null && it.before(Date()) }
        }
    }
}

class ExpirationDateAfterPolicy : VerificationPolicy {
    override val description: String = "Verify by expiration date"
    override fun verify(vc: VerifiableCredential): Boolean {
        return when (vc) {
            is VerifiablePresentation -> true
            else -> parseDate(VcUtils.getExpirationDate(vc)).let { it == null || it.after(Date()) }
        }
    }
}

private fun parseDate(date: String?) = try {
    dateFormatter.parse(date)
} catch (e: Exception) {
    null
}

object PolicyRegistry {
    private val policies = LinkedHashMap<String, VerificationPolicy>()
    val defaultPolicyId: String

    fun register(policy: VerificationPolicy) = policies.put(policy.id, policy)
    fun getPolicy(id: String) = policies[id]!!
    fun contains(id: String) = policies.containsKey(id)
    fun listPolicies() = policies.values

    init {
        val sigPol = SignaturePolicy()
        defaultPolicyId = sigPol.id
        register(sigPol)
        register(JsonSchemaPolicy())
        register(TrustedIssuerDidPolicy())
        register(TrustedIssuerRegistryPolicy())
        register(TrustedSubjectDidPolicy())
        register(IssuanceDateBeforePolicy())
        register(ValidFromBeforePolicy())
        register(ExpirationDateAfterPolicy())
    }
}

data class VerificationResult(
    val overallStatus: Boolean = false,
    val policyResults: Map<String, Boolean>
) {
    override fun toString() =
        "VerificationResult(overallStatus=$overallStatus, policyResults={${policyResults.entries.joinToString { it.key + "=" + it.value }}})"
}

interface IAuditor {

    fun verify(vcJson: String, policies: List<VerificationPolicy>): VerificationResult

    //    fun verifyVc(vc: String, config: AuditorConfig) = VerificationStatus(true)
    //    fun verifyVp(vp: String, config: AuditorConfig) = VerificationStatus(true)
}

object Auditor : IAuditor {

    private fun allAccepted(policyResults: Map<String, Boolean>) = policyResults.values.all { it }

    override fun verify(vcJson: String, policies: List<VerificationPolicy>): VerificationResult {
        val vc = vcJson.toCredential()
        val policyResults = policies
            .associateBy(keySelector = VerificationPolicy::id) { policy ->
                log.debug { "Verifying vc with ${policy.id}..." }
                policy.verify(vc) && when (vc) {
                    is VerifiablePresentation -> vc.verifiableCredential.all { cred ->
                        log.debug { "Verifying ${cred.type.last()} in VP with ${policy.id}..." }
                        policy.verify(cred)
                    }
                    else -> true
                }
            }

        return VerificationResult(allAccepted(policyResults), policyResults)
    }
}
