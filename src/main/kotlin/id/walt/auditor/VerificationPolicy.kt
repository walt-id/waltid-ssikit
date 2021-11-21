package id.walt.auditor

import id.walt.model.AttributeInfo
import id.walt.model.TrustedIssuer
import id.walt.services.did.DidService
import id.walt.services.essif.TrustedIssuerClient
import id.walt.services.key.KeyService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import id.walt.services.vc.VcUtils
import id.walt.vclib.Helpers.encode
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.vclist.GaiaxCredential
import id.walt.vclib.vclist.VerifiablePresentation
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.text.SimpleDateFormat
import java.util.*

private const val TIR_TYPE_ATTRIBUTE = "attribute"
private const val TIR_NAME_ISSUER = "issuer"
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

            val issuerDid = VcUtils.getIssuer(vc)

            try {
                // Check if key is already in keystore
                KeyService.getService().load(issuerDid)
            } catch (e: Exception) {
                // Resolve DID and import key
                DidService.importKey(issuerDid)
            }

            when (vc.jwt) {
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

        return isValidTrustedIssuerRecord(tirRecord)

    }

    private fun isValidTrustedIssuerRecord(tirRecord: TrustedIssuer): Boolean {
        for (attribute in tirRecord.attributes) {
            val attributeInfo = AttributeInfo.from(attribute.body)
            if(TIR_TYPE_ATTRIBUTE.equals(attributeInfo?.type) && TIR_NAME_ISSUER.equals(attributeInfo?.name)) {
                return true
            }
        }
        return false
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

class GaiaxTrustedPolicy : VerificationPolicy {
    override val description: String = "Verify Gaiax trusted fields"
    override fun verify(vc: VerifiableCredential): Boolean {
        // VPs are not considered
        if (vc is VerifiablePresentation) {
            return true
        }

        val gaiaxVc = vc as GaiaxCredential

        // TODO: validate trusted fields properly
        if (gaiaxVc.credentialSubject.DNSpublicKey.length < 0) {
            log.debug { "DNS Public key not valid." }
            return false
        }

        if (gaiaxVc.credentialSubject.ethereumAddress.id.length < 0) {
            log.debug { "ETH address not valid." }
            return false
        }

        return true
    }
}

private fun parseDate(date: String?) = try {
    dateFormatter.parse(date)
} catch (e: Exception) {
    null
}

data class VerificationResult(
    val overallStatus: Boolean = false,
    val policyResults: Map<String, Boolean>
) {
    override fun toString() =
        "VerificationResult(overallStatus=$overallStatus, policyResults={${policyResults.entries.joinToString { it.key + "=" + it.value }}})"
}
