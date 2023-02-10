package id.walt.auditor

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentation
import id.walt.credentials.w3c.schema.SchemaValidatorFactory
import id.walt.model.AttributeInfo
import id.walt.model.TrustedIssuer
import id.walt.model.dif.PresentationDefinition
import id.walt.services.did.DidService
import id.walt.services.ecosystems.essif.TrustedIssuerClient
import id.walt.services.oidc.OIDCUtils
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import id.walt.signatory.RevocationClientService
import io.ktor.client.plugins.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.net.URI
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
data class VerificationPolicyMetadata(
    val id: String,
    val description: String?,
    val argumentType: String,
    val isMutable: Boolean
)

abstract class VerificationPolicy {
    open val id: String
        get() = this.javaClass.simpleName
    abstract val description: String
    protected abstract fun doVerify(vc: VerifiableCredential): Boolean
    open val applyToVC: Boolean = true
    open val applyToVP: Boolean = true

    fun verify(vc: VerifiableCredential) = when {
        vc is VerifiablePresentation && applyToVP
                || vc !is VerifiablePresentation && applyToVC -> doVerify(vc)

        else -> true
    }.also { log.debug { "VC ${vc.type} passes policy $id: $it" } }
}

abstract class SimpleVerificationPolicy : VerificationPolicy()

abstract class ParameterizedVerificationPolicy<T>(val argument: T) : VerificationPolicy()

abstract class OptionalParameterizedVerificationPolicy<T>(val argument: T?) : VerificationPolicy()

class SignaturePolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by signature"
    override fun doVerify(vc: VerifiableCredential) = runCatching {
        log.debug { "is jwt: ${vc.jwt != null}" }
        when (vc.jwt) {
            null -> jsonLdCredentialService.verify(vc.encode()).verified
            else -> jwtCredentialService.verify(vc.encode()).verified
        }
    }.onFailure {
        log.error(it.localizedMessage)
    }.getOrDefault(false)
}

class JsonSchemaPolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by JSON schema"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        return vc.credentialSchema?.id?.let { URI.create(it) }?.let {
            SchemaValidatorFactory.get(it).validate(vc.toJson())
        } ?: false
    }

    override val applyToVP: Boolean
        get() = false
}

class TrustedSchemaRegistryPolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by EBSI Trusted Schema Registry"
    override fun doVerify(vc: VerifiableCredential) = when (vc.jwt) {
        null -> jsonLdCredentialService.validateSchemaTsr(vc.encode()) // Schema already validated by json-ld?
        else -> jwtCredentialService.validateSchemaTsr(vc.encode())
    }
}

class TrustedIssuerDidPolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by trusted issuer did"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        return try {
            DidService.loadOrResolveAnyDid(vc.issuerId!!) != null
        } catch (e: ClientRequestException) {
            if (!e.message.contains("did must be a valid DID") && !e.message.contains("Identifier Not Found")) throw e
            false
        }
    }
}

class TrustedIssuerRegistryPolicy() : SimpleVerificationPolicy() {

    override val description: String = "Verify by an EBSI Trusted Issuers Registry compliant api."
    override fun doVerify(vc: VerifiableCredential): Boolean {

        return doVerifyByTrustedIssuersRegistry(vc, null)
    }

    override var applyToVP: Boolean = false
}

data class TrustedIssuerRegistryPolicyArg(val registryAddress: String)

class ParameterizedTrustedIssuerRegistryPolicy(registryArg: TrustedIssuerRegistryPolicyArg) :
    ParameterizedVerificationPolicy<TrustedIssuerRegistryPolicyArg>(registryArg) {

    constructor(registryAddress: String) : this(
        TrustedIssuerRegistryPolicyArg(registryAddress)
    )

    override val description: String = "Verify by an EBSI Trusted Issuers Registry compliant api."
    override fun doVerify(vc: VerifiableCredential): Boolean {

        return doVerifyByTrustedIssuersRegistry(vc, argument)
    }

    override var applyToVP: Boolean = false
}

private fun doVerifyByTrustedIssuersRegistry(vc: VerifiableCredential, argument: TrustedIssuerRegistryPolicyArg?): Boolean {

    // VPs are not considered
    if (vc is VerifiablePresentation)
        return true

    val issuerDid = vc.issuerId!!

    val resolvedIssuerDid = DidService.loadOrResolveAnyDid(issuerDid)
        ?: throw Exception("Could not resolve issuer DID $issuerDid")

    if (resolvedIssuerDid.id != issuerDid) {
        log.debug { "Resolved DID ${resolvedIssuerDid.id} does not match the issuer DID $issuerDid" }
        return false
    }
    var tirRecord: TrustedIssuer

    argument?.let {
        tirRecord = runCatching {
            TrustedIssuerClient.getIssuer(issuerDid, it.registryAddress)
        }.getOrElse { throw Exception("Could not resolve issuer TIR record of $issuerDid", it) }
        return isValidTrustedIssuerRecord(tirRecord)
    } ?: run {
        tirRecord = runCatching {
            TrustedIssuerClient.getIssuer(issuerDid)
        }.getOrElse { throw Exception("Could not resolve issuer TIR record of $issuerDid", it) }
        return isValidTrustedIssuerRecord(tirRecord)
    }

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

class TrustedSubjectDidPolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by trusted subject did"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        return vc.subjectId?.let {
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

class IssuedDateBeforePolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by issuance date"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        return when (vc) {
            is VerifiablePresentation -> true
            else -> parseDate(vc.issued).let { it != null && it.before(Date()) }
        }
    }
}

class ValidFromBeforePolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by valid from"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        return when (vc) {
            is VerifiablePresentation -> true
            else -> parseDate(vc.validFrom).let { it != null && it.before(Date()) }
        }
    }
}

class ExpirationDateAfterPolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by expiration date"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        return when (vc) {
            is VerifiablePresentation -> true
            else -> parseDate(vc.expirationDate).let { it == null || it.after(Date()) }
        }
    }
}

class CredentialStatusPolicy : SimpleVerificationPolicy() {

    @Serializable
    data class CredentialStatus(
        val id: String,
        var type: String
    )

    @Serializable
    data class CredentialStatusCredential(
        @Json(serializeNull = false) var credentialStatus: CredentialStatus? = null
    )

    override val description: String = "Verify by credential status"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        val cs = Klaxon().parse<CredentialStatusCredential>(vc.toJson())!!.credentialStatus!!

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

data class ChallengePolicyArg(val challenges: Set<String>, val applyToVC: Boolean = true, val applyToVP: Boolean = true)

class ChallengePolicy(challengeArg: ChallengePolicyArg) :
    ParameterizedVerificationPolicy<ChallengePolicyArg>(challengeArg) {
    constructor(challenge: String, applyToVC: Boolean = true, applyToVP: Boolean = true) : this(
        ChallengePolicyArg(
            setOf(
                challenge
            ), applyToVC, applyToVP
        )
    )

    override val description: String = "Verify challenge"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        return vc.challenge?.let { argument.challenges.contains(it) } ?: false
    }

    override val applyToVC: Boolean
        get() = argument.applyToVC

    override val applyToVP: Boolean
        get() = argument.applyToVP
}

class PresentationDefinitionPolicy(presentationDefinition: PresentationDefinition) :
    ParameterizedVerificationPolicy<PresentationDefinition>(presentationDefinition) {
    override val description: String = "Verify that verifiable presentation complies with presentation definition"
    override fun doVerify(vc: VerifiableCredential): Boolean {
        if (vc is VerifiablePresentation) {
            return argument.input_descriptors.all { desc ->
                vc.verifiableCredential?.any { cred -> OIDCUtils.matchesInputDescriptor(cred, desc) } ?: false
            }
        }
        // else: nothing to check
        return false
    }

    override var applyToVC: Boolean = false
}

/*class GaiaxTrustedPolicy : SimpleVerificationPolicy() {
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
        if (gaiaxVc.credentialSubject!!.DNSpublicKey.isEmpty()) {
            log.debug { "DNS Public key not valid." }
            return false
        }

        if (gaiaxVc.credentialSubject!!.ethereumAddress.id.isEmpty()) {
            log.debug { "ETH address not valid." }
            return false
        }

        return true
    }
}*/

class GaiaxSDPolicy : SimpleVerificationPolicy() {
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
