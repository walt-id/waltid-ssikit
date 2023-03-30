package id.walt.auditor

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import com.fasterxml.jackson.annotation.JsonIgnore
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

class VerificationPolicyResult private constructor(
    private val result: Boolean,
    @JsonIgnore
    private val errorList: List<Throwable> = emptyList()
) {

    companion object {
        fun success() = VerificationPolicyResult(true)
        fun failure(error: Throwable): VerificationPolicyResult {
            log.debug { "VerificationPolicy failed: ${error.stackTraceToString()}" }
            return failure(listOf(error))
        }
        fun failure(errors: List<Throwable> = emptyList()) = VerificationPolicyResult(false, errors)
    }

    val isSuccess = result
    val isFailure = !result
    @JsonIgnore
    val errors = errorList

    private fun getErrorString() = errorList.mapIndexed { index, throwable ->
        "#${index + 1}: ${throwable::class.simpleName ?: "Error"} - ${throwable.message}"
    }.joinToString()

    override fun toString(): String {
        return when (result) {
            true -> "passed"
            false -> "failed: ${getErrorString()}"
        }
    }
}

abstract class VerificationPolicy {
    open val id: String
        get() = this.javaClass.simpleName
    abstract val description: String
    protected abstract fun doVerify(vc: VerifiableCredential): VerificationPolicyResult
    open val applyToVC = true
    open val applyToVP = true

    fun verify(vc: VerifiableCredential): VerificationPolicyResult {
        val verifyPresentation = vc is VerifiablePresentation && applyToVP
        val verifyCredential = vc !is VerifiablePresentation && applyToVC
        return when {
            verifyPresentation || verifyCredential -> doVerify(vc)
            else -> VerificationPolicyResult.success()
        }.also { log.debug { "VC ${vc.type} passes policy $id: $it" } }
    }
}

abstract class SimpleVerificationPolicy : VerificationPolicy()

abstract class ParameterizedVerificationPolicy<T>(val argument: T) : VerificationPolicy()

abstract class OptionalParameterizedVerificationPolicy<T>(argument: T?) : ParameterizedVerificationPolicy<T?>(argument)

class SignaturePolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by signature"
    override fun doVerify(vc: VerifiableCredential) = runCatching {
        log.debug { "is jwt: ${vc.jwt != null}" }
        when (vc.jwt) {
            null -> jsonLdCredentialService.verify(vc.encode()).verified
            else -> jwtCredentialService.verify(vc.encode()).verified
        }.takeIf { it }?.let { VerificationPolicyResult.success() } ?: VerificationPolicyResult.failure()
    }.getOrElse {
        VerificationPolicyResult.failure(it)
    }
}

/**
 * @param schema    URL, file path or content of JSON schema to validate against
 */
data class JsonSchemaPolicyArg(val schema: String)

class JsonSchemaPolicy(schemaPolicyArg: JsonSchemaPolicyArg?) :
    OptionalParameterizedVerificationPolicy<JsonSchemaPolicyArg>(schemaPolicyArg) {
    constructor() : this(null)

    override val description: String = "Verify by JSON schema"
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        return (argument?.schema ?: vc.credentialSchema?.id)?.let {
            SchemaValidatorFactory.get(it).validate(vc.toJson())
        } ?: VerificationPolicyResult.failure(IllegalArgumentException("No \"argument.schema\" or \"credentialSchema.id\" supplied."))
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
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        return try {
            DidService.loadOrResolveAnyDid(vc.issuerId!!)?.let {
                VerificationPolicyResult.success()
            } ?: VerificationPolicyResult.failure()
        } catch (e: ClientRequestException) {
            VerificationPolicyResult.failure(IllegalArgumentException(when {
                "did must be a valid DID" in e.message -> "did must be a valid DID"
                "Identifier Not Found" in e.message -> "Identifier Not Found"
                else -> throw e
            }))
        }
    }
}


data class TrustedIssuerRegistryPolicyArg(val registryAddress: String)

class TrustedIssuerRegistryPolicy(registryArg: TrustedIssuerRegistryPolicyArg) :
    ParameterizedVerificationPolicy<TrustedIssuerRegistryPolicyArg>(registryArg) {

    constructor(registryAddress: String) : this(
        TrustedIssuerRegistryPolicyArg(registryAddress)
    )

    constructor() : this(
        TrustedIssuerRegistryPolicyArg("https://api-pilot.ebsi.eu/trusted-issuers-registry/v2/issuers/")
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


        return runCatching {
            tirRecord = TrustedIssuerClient.getIssuer(issuerDid, argument.registryAddress)
            isValidTrustedIssuerRecord(tirRecord)
        }.getOrElse {
            log.debug { it }
            log.warn { "Could not resolve issuer TIR record of $issuerDid" }
            false
        }.takeIf {
            it
        }?.let { VerificationPolicyResult.success() } ?: VerificationPolicyResult.failure()
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


class TrustedSubjectDidPolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by trusted subject did"
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        return (vc.subjectId?.let {
            if (it.isEmpty()) true
            else try {
                DidService.loadOrResolveAnyDid(it) != null
            } catch (e: ClientRequestException) {
                if (!e.message.contains("did must be a valid DID") && !e.message.contains("Identifier Not Found")) throw e
                false
            }
        } ?: false).takeIf { it }?.let {
            VerificationPolicyResult.success()
        } ?: VerificationPolicyResult.failure()
    }
}

class IssuedDateBeforePolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by issuance date"
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        return when (vc) {
            is VerifiablePresentation -> true
            else -> parseDate(vc.issued).let { it != null && it.before(Date()) }
        }.takeIf { it }?.let { VerificationPolicyResult.success() } ?: VerificationPolicyResult.failure()
    }
}

class ValidFromBeforePolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by valid from"
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        return when (vc) {
            is VerifiablePresentation -> true
            else -> parseDate(vc.validFrom).let { it != null && it.before(Date()) }
        }.takeIf { it }?.let { VerificationPolicyResult.success() } ?: VerificationPolicyResult.failure()
    }
}

class ExpirationDateAfterPolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by expiration date"
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        return when (vc) {
            is VerifiablePresentation -> true
            else -> parseDate(vc.expirationDate).let { it == null || it.after(Date()) }
        }.takeIf { it }?.let { VerificationPolicyResult.success() } ?: VerificationPolicyResult.failure()
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
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        val cs = Klaxon().parse<CredentialStatusCredential>(vc.toJson())!!.credentialStatus!!

        fun revocationVerificationPolicy(revoked: Boolean, timeOfRevocation: Long?) =
            if (!revoked) VerificationPolicyResult.success() else VerificationPolicyResult.failure(IllegalArgumentException("CredentialStatus (type ${cs.type}) was REVOKED at timestamp $timeOfRevocation for id ${cs.id}."))

        return when (cs.type) {
            "SimpleCredentialStatus2022" -> {
                val rs = RevocationClientService.getService()
                val result = rs.checkRevoked(cs.id)
                revocationVerificationPolicy(result.isRevoked, result.timeOfRevocation)
            }
            else -> VerificationPolicyResult.failure(UnsupportedOperationException("CredentialStatus type \"${cs.type}\" is not yet supported."))
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
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        return (vc.challenge?.let { argument.challenges.contains(it) } ?: false).takeIf { it }?.let {
            VerificationPolicyResult.success()
        } ?: VerificationPolicyResult.failure()
    }

    override val applyToVC: Boolean
        get() = argument.applyToVC

    override val applyToVP: Boolean
        get() = argument.applyToVP
}

class PresentationDefinitionPolicy(presentationDefinition: PresentationDefinition) :
    ParameterizedVerificationPolicy<PresentationDefinition>(presentationDefinition) {
    override val description: String = "Verify that verifiable presentation complies with presentation definition"
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        return (if (vc is VerifiablePresentation) {
            argument.input_descriptors.all { desc ->
                vc.verifiableCredential?.any { cred -> OIDCUtils.matchesInputDescriptor(cred, desc) } ?: false
            }
        } else false).takeIf { it }?.let { VerificationPolicyResult.success() } ?: VerificationPolicyResult.failure()
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
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        return VerificationPolicyResult.success()
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
    val result: Boolean = false,
    val policyResults: Map<String, VerificationPolicyResult>
) {
    @Deprecated("Deprecated in favour of: result")
    val valid: Boolean = result

    override fun toString() =
        "VerificationResult(result=$result, policyResults={${policyResults.entries.joinToString { it.key + "=" + it.value }}})"
}
