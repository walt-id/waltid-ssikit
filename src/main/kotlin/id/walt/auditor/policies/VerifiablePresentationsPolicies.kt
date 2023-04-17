package id.walt.auditor.policies

import id.walt.auditor.ParameterizedVerificationPolicy
import id.walt.auditor.VerificationPolicyResult
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentation
import id.walt.model.dif.PresentationDefinition
import id.walt.services.oidc.OIDCUtils

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
