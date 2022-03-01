package id.walt.auditor

import com.beust.klaxon.Klaxon
import id.walt.model.AttributeInfo
import id.walt.model.TrustedIssuer
import id.walt.model.oidc.VpTokenClaim
import id.walt.services.did.DidService
import id.walt.services.essif.TrustedIssuerClient
import id.walt.services.key.KeyService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import id.walt.signatory.RevocationClientService
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.credentials.gaiax.GaiaxCredential
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.schema.SchemaService
import io.ktor.client.features.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.text.SimpleDateFormat
import java.util.*
import id.walt.vclib.credentials.CredentialStatusCredential

private const val TIR_TYPE_ATTRIBUTE = "attribute"
private const val TIR_NAME_ISSUER = "issuer"
private val log = KotlinLogging.logger {}
private val jsonLdCredentialService = JsonLdCredentialService.getService()
private val jwtCredentialService = JwtCredentialService.getService()
private val dateFormatter =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").also { it.timeZone = TimeZone.getTimeZone("UTC") }

@Serializable
data class VerificationPolicyMetadata(val description: String, val id: String, val applyToVC: Boolean, val applyToVP: Boolean)

abstract class VerificationPolicy {
    val id: String
        get() = this.javaClass.simpleName
    abstract val description: String
    protected abstract fun doVerify(vc: VerifiableCredential): Boolean
    open var applyToVC: Boolean = true
    open var applyToVP: Boolean = true
    fun verify(vc: VerifiableCredential) = when {
        vc is VerifiablePresentation && applyToVP
                || vc !is VerifiablePresentation && applyToVC -> doVerify(vc)
        else -> true
    }
}

class SignaturePolicy : VerificationPolicy() {
    override val description: String = "Verify by signature"
    override fun doVerify(vc: VerifiableCredential) = runCatching {
        log.debug { "is jwt: ${vc.jwt != null}" }

        val issuerDid = vc.issuer!!

        if (!KeyService.getService().hasKey(issuerDid))
            DidService.importKey(issuerDid)

        when (vc.jwt) {
            null -> jsonLdCredentialService.verify(vc.json!!).verified
            else -> jwtCredentialService.verify(vc.jwt!!).verified
        }
    }.onFailure {
        log.error(it.localizedMessage)
    }.getOrDefault(false)
}

class JsonSchemaPolicy : VerificationPolicy() {
    override val description: String = "Verify by JSON schema"
    override fun doVerify(vc: VerifiableCredential): Boolean = SchemaService.validateSchema(vc.json!!).run {
        return if (valid)
            true
        else {
            log.error { "Credential not valid according the json-schema of type ${vc.type}. The validation errors are:" }
            errors?.forEach { error -> log.error { error } }
            false
        }
    }
}

class TrustedSchemaRegistryPolicy : VerificationPolicy() {
    override val description: String = "Verify by EBSI Trusted Schema Registry"
    override fun doVerify(vc: VerifiableCredential) = when (vc.jwt) {
        null -> jsonLdCredentialService.validateSchemaTsr(vc.encode()) // Schema already validated by json-ld?
        else -> jwtCredentialService.validateSchemaTsr(vc.encode())
    }
}

class TrustedIssuerDidPolicy : VerificationPolicy() {
    override val description: String = "Verify by trusted issuer did"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        return try {
            DidService.loadOrResolveAnyDid(vc.issuer!!) != null
        } catch (e: ClientRequestException) {
            if (!e.message.contains("did must be a valid DID") && !e.message.contains("Identifier Not Found")) throw e
            false
        }
    }
}

class TrustedIssuerRegistryPolicy : VerificationPolicy() {
    override val description: String = "Verify by trusted EBSI Trusted Issuer Registry record"
    override fun doVerify(vc: VerifiableCredential): Boolean {

        // VPs are not considered
        if (vc is VerifiablePresentation)
            return true

        val issuerDid = vc.issuer!!

        val resolvedIssuerDid = DidService.loadOrResolveAnyDid(issuerDid)
            ?: throw Exception("Could not resolve issuer DID $issuerDid")

        if (resolvedIssuerDid.id != issuerDid) {
            log.debug { "Resolved DID ${resolvedIssuerDid.id} does not match the issuer DID $issuerDid" }
            return false
        }

        val tirRecord = runCatching {
            TrustedIssuerClient.getIssuer(issuerDid)
        }.getOrElse { throw Exception("Could not resolve issuer TIR record of $issuerDid", it) }

        return isValidTrustedIssuerRecord(tirRecord)
    }

    private fun isValidTrustedIssuerRecord(tirRecord: TrustedIssuer): Boolean {
        for (attribute in tirRecord.attributes) {
            val attributeInfo = AttributeInfo.from(attribute.body)
            if (TIR_TYPE_ATTRIBUTE == attributeInfo?.type && TIR_NAME_ISSUER == attributeInfo.name) {
                return true
            }
        }
        return false
    }

    override var applyToVP: Boolean = false
}

class TrustedSubjectDidPolicy : VerificationPolicy() {
    override val description: String = "Verify by trusted subject did"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        return vc.subject?.let {
            if (it.isEmpty()) true
            else try {
                DidService.loadOrResolveAnyDid(it) != null
            } catch (e: ClientRequestException) {
                if (!e.message.contains("did must be a valid DID") && !e.message.contains("Identifier Not Found")) throw e
                false
            }
        } ?: false
    }
}

class IssuedDateBeforePolicy : VerificationPolicy() {
    override val description: String = "Verify by issuance date"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        return when (vc) {
            is VerifiablePresentation -> true
            else -> parseDate(vc.issued).let { it != null && it.before(Date()) }
        }
    }
}

class ValidFromBeforePolicy : VerificationPolicy() {
    override val description: String = "Verify by valid from"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        return when (vc) {
            is VerifiablePresentation -> true
            else -> parseDate(vc.validFrom).let { it != null && it.before(Date()) }
        }
    }
}

class ExpirationDateAfterPolicy : VerificationPolicy() {
    override val description: String = "Verify by expiration date"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        return when (vc) {
            is VerifiablePresentation -> true
            else -> parseDate(vc.expirationDate).let { it == null || it.after(Date()) }
        }
    }
}

class CredentialStatusPolicy : VerificationPolicy() {
    override val description: String = "Verify by credential status"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        val cs = Klaxon().parse<CredentialStatusCredential>(vc.json!!)!!.credentialStatus!!

        when (cs.type) {
            "SimpleCredentialStatus2022" -> {
                val rs = RevocationClientService.getService()

                val result = rs.checkRevoked(cs.id)

                return !result.isRevoked
            }
            else -> {
                throw IllegalArgumentException("CredentialStatus type \"\"")
            }
        }
    }
}

class ChallengePolicy(val verifyChallenge: (c: String) -> Boolean) : VerificationPolicy() {
    constructor(challenge: String) : this({ c -> c == challenge })
    override val description: String = "Verify challenge"
    override fun doVerify(vc: VerifiableCredential): Boolean = vc.challenge?.let { verifyChallenge(it) } ?: false
}

class VpTokenClaimPolicy(val vpTokenClaim: VpTokenClaim?) : VerificationPolicy() {
    override val description: String = "Verify verifiable presentation by OIDC/SIOPv2 VP token claim"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        if (vpTokenClaim != null && vc is VerifiablePresentation) {
            return vpTokenClaim.presentation_definition.input_descriptors.all { desc ->
                vc.verifiableCredential.any { cred -> desc.schema.uri == cred.credentialSchema?.id }
            }
        }
        // else: nothing to check
        return true
    }

    override var applyToVC: Boolean = false
}

class GaiaxTrustedPolicy : VerificationPolicy() {
    override val description: String = "Verify Gaiax trusted fields"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        // VPs are not considered
        if (vc is VerifiablePresentation) {
            return true
        }

        val gaiaxVc = vc as GaiaxCredential
        if (gaiaxVc.credentialSubject == null) {
            return false
        }
        // TODO: validate trusted fields properly
        if (gaiaxVc.credentialSubject!!.DNSpublicKey.length < 1) {
            log.debug { "DNS Public key not valid." }
            return false
        }

        if (gaiaxVc.credentialSubject!!.ethereumAddress.id.length < 1) {
            log.debug { "ETH address not valid." }
            return false
        }

        return true
    }
}

class GaiaxSDPolicy : VerificationPolicy() {
    override val description: String = "Verify Gaiax SD fields"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        return true
    }
}

private fun parseDate(date: String?) = try {
    dateFormatter.parse(date)
} catch (e: Exception) {
    null
}

data class VerificationResult(
    /***
     * Validation status over all policy results.
     */
    val valid: Boolean = false,
    val policyResults: Map<String, Boolean>
) {
    override fun toString() =
        "VerificationResult(valid=$valid, policyResults={${policyResults.entries.joinToString { it.key + "=" + it.value }}})"
}
