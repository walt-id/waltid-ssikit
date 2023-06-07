package id.walt.auditor

import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentation
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import mu.KotlinLogging
import java.util.concurrent.atomic.*

private val log = KotlinLogging.logger {}

abstract class Auditor : WaltIdService() {
    override val implementation: Auditor get() = serviceImplementation()

    protected fun allAccepted(policyResults: Map<String, VerificationPolicyResult>) = policyResults.values.all { it.isSuccess }

    open fun verify(vc: VerifiableCredential, policies: List<VerificationPolicy>): VerificationResult =
        implementation.verify(vc, policies)

    open fun verify(vcJson: String, policies: List<VerificationPolicy>): VerificationResult =
        implementation.verify(vcJson, policies)

    companion object : ServiceProvider {
        override fun getService() = object : Auditor() {}
        override fun defaultImplementation() = WaltIdAuditor()
    }
}

class WaltIdAuditor : Auditor() {
    override fun verify(vcJson: String, policies: List<VerificationPolicy>): VerificationResult {
        val vc = vcJson.toVerifiableCredential()
        return verify(vc, policies)
    }

    override fun verify(vc: VerifiableCredential, policies: List<VerificationPolicy>): VerificationResult {
        val policyResults = policies.associateBy(keySelector = VerificationPolicy::id) { policy ->
            log.debug { "Verifying vc with ${policy.id} ..." }

            val vcResult = policy.verify(vc)
            val success = AtomicBoolean(vcResult.isSuccess)
            val allErrors = vcResult.errors.toMutableList()
            if (allErrors.isEmpty() && vc is VerifiablePresentation) {
                vc.verifiableCredential?.forEach { cred ->
                    log.debug { "Verifying ${cred.type.last()} in VP with ${policy.id}..." }
                    val vpResult = policy.verify(cred)
                    allErrors.addAll(vpResult.errors)
                    success.compareAndSet(true, vpResult.isSuccess)
                }
            }
            allErrors.forEach { log.error { "${policy.id}: $it" } }
            success.get().takeIf { it }?.let { VerificationPolicyResult.success() } ?: VerificationPolicyResult.failure(*allErrors.toTypedArray())
        }

        return VerificationResult(allAccepted(policyResults), policyResults)
    }
}
