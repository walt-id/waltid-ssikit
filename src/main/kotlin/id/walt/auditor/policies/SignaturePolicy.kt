package id.walt.auditor.policies

import id.walt.auditor.SimpleVerificationPolicy
import id.walt.auditor.VerificationPolicyResult
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.verifyByFormatType
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import mu.KotlinLogging

class SignaturePolicy : SimpleVerificationPolicy() {
    override val description: String = "Verify by signature"
    private val log = KotlinLogging.logger {}
    private val jsonLdCredentialService = JsonLdCredentialService.getService()
    private val jwtCredentialService = JwtCredentialService.getService()

    override fun doVerify(vc: VerifiableCredential) = runCatching {
        log.debug { "is jwt: ${vc.sdJwt != null}" }
        vc.verifyByFormatType(
            { jwtCredentialService.verify(it) },
            { jsonLdCredentialService.verify(it) }
        ).verified.takeIf { it }?.let { VerificationPolicyResult.success() } ?: VerificationPolicyResult.failure()
    }.getOrElse {
        VerificationPolicyResult.failure(it)
    }
}
