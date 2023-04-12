package id.walt.auditor.policies

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import id.walt.auditor.ParameterizedVerificationPolicy
import id.walt.auditor.SimpleVerificationPolicy
import id.walt.auditor.VerificationPolicyResult
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentation
import id.walt.model.credential.status.CredentialStatus
import id.walt.model.credential.status.SimpleCredentialStatus2022
import id.walt.model.credential.status.StatusList2021EntryCredentialStatus
import id.walt.signatory.RevocationClientService
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

private val dateFormatter =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").also { it.timeZone = TimeZone.getTimeZone("UTC") }

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
    data class CredentialStatusCredential(
        @Json(serializeNull = false) var credentialStatus: CredentialStatus? = null
    )

    override val description: String = "Verify by credential status"
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult = runCatching {
        Klaxon().parse<CredentialStatusCredential>(vc.toJson())!!.credentialStatus!!.let {
            when (it) {
                is SimpleCredentialStatus2022 -> checkSimpleRevocation(it)
                is StatusList2021EntryCredentialStatus -> checkStatusListRevocation(it)
            }
        }
    }.getOrElse {
        //"CredentialStatus type \"${cs.type}\" is not yet supported."
        VerificationPolicyResult.failure(it)
    }

    private fun checkSimpleRevocation(cs: CredentialStatus) = let {
        fun revocationVerificationPolicy(revoked: Boolean, timeOfRevocation: Long?) =
            if (!revoked) VerificationPolicyResult.success()
            else VerificationPolicyResult.failure(
                IllegalArgumentException("CredentialStatus (type ${cs.type}) was REVOKED at timestamp $timeOfRevocation for id ${cs.id}.")
            )

        val rs = RevocationClientService.getService()
        val result = rs.checkRevoked(cs.id)
        revocationVerificationPolicy(result.isRevoked, result.timeOfRevocation)
    }

    private fun checkStatusListRevocation(cs: CredentialStatus) = let {
        //TODO: implement
        VerificationPolicyResult.success()
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
        return (vc.challenge?.let { argument.challenges.contains(it) } ?: false).takeIf { it }
            ?.let { VerificationPolicyResult.success() } ?: VerificationPolicyResult.failure()
    }

    override val applyToVC: Boolean
        get() = argument.applyToVC

    override val applyToVP: Boolean
        get() = argument.applyToVP
}

private fun parseDate(date: String?) = try {
    dateFormatter.parse(date)
} catch (e: Exception) {
    null
}
