package id.walt.auditor

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.toCredential
import mu.KotlinLogging


private val log = KotlinLogging.logger {}


abstract class Auditor : WaltIdService() {
    override val implementation: Auditor get() = serviceImplementation()

    protected fun allAccepted(policyResults: Map<String, Boolean>) = policyResults.values.all { it }

    open fun verify(vcJson: String, policies: Map<VerificationPolicy, Any>): VerificationResult =
        implementation.verify(vcJson, policies)

    fun verify(vcJson: String, policies: List<VerificationPolicy>): VerificationResult =
        implementation.verify(vcJson, policies.associateWith {})

    companion object : ServiceProvider {
        override fun getService() = object : Auditor() {}
        override fun defaultImplementation() = WaltIdAuditor()
    }
}

class WaltIdAuditor : Auditor() {
    override fun verify(vcJson: String, policies: Map<VerificationPolicy, Any>): VerificationResult {
        val vc = vcJson.toCredential()
        val policyResults = policies.keys
            .associateBy(keySelector = VerificationPolicy::id) { policy ->
                log.debug { "Verifying vc with ${policy.id} ..." }

                policy.arguments = policies[policy]!!

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
