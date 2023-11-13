package id.walt.auditor.policies

import com.beust.klaxon.Klaxon
import id.walt.auditor.ParameterizedVerificationPolicy
import id.walt.auditor.SimpleVerificationPolicy
import id.walt.auditor.VerificationPolicyResult
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentation
import id.walt.model.credential.status.CredentialStatus
import id.walt.signatory.revocation.CredentialStatusCredential
import id.walt.signatory.revocation.CredentialStatusClientService
import id.walt.signatory.revocation.RevocationStatus
import id.walt.signatory.revocation.TokenRevocationStatus
import java.text.SimpleDateFormat
import java.util.*

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

    override val description: String = "Verify by credential status"
    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        val maybeCredentialStatus = Klaxon().parse<CredentialStatusCredential>(vc.toJson())!!.credentialStatus
        return maybeCredentialStatus?.let { cs -> runCatching {
                CredentialStatusClientService.check(vc).let {
                    if (!it.isRevoked) VerificationPolicyResult.success()
                    else failResult(it, cs)
                }
            }.getOrElse { VerificationPolicyResult.failure(it) }
        } ?: VerificationPolicyResult.success()
    }

    private fun failResult(status: RevocationStatus, cs: CredentialStatus) = when (status) {
        is TokenRevocationStatus -> "CredentialStatus (type ${cs.type}) was REVOKED at timestamp ${status.timeOfRevocation} for id ${cs.id}."
        else -> "CredentialStatus ${cs.type} was REVOKED for id ${cs.id}"
    }.let {
        VerificationPolicyResult.failure(IllegalStateException(it))
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
