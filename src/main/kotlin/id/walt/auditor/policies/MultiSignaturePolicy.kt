package id.walt.auditor.policies

import id.walt.auditor.SimpleVerificationPolicy
import id.walt.auditor.VerificationPolicyResult
import id.walt.credentials.w3c.VerifiableCredential

class JwtHelper(val credential: String) {
    val header get() = credential.substringBefore(".")
    val payload get() = credential.substringAfter(".").substringBefore(".")
    val signature get() = credential.substringAfterLast(".")
    val jwsSignaturePart get() = mapOf(
        "protected" to header,
        "signature" to signature
    )

    companion object {
        fun fromJWS(payload: String, sig: Map<String, String>): JwtHelper {
            val h = sig["protected"] ?: throw Exception("No header found")
            val s = sig["signature"] ?: throw Exception("No sig found")
            return JwtHelper("$h.$payload.$s")
        }
    }
}

class MultiSignaturePolicy: SimpleVerificationPolicy() {
    override val description: String
        get() = "JWS Multi Signature Verification Policy"

    override fun doVerify(vc: VerifiableCredential): VerificationPolicyResult {
        val payload = (vc.credentialSubject?.properties?.get("payload") as? String) ?: return VerificationPolicyResult.failure()
        val signatures = (vc.credentialSubject?.properties?.get("signatures") as? List<Map<String, String>>) ?: return VerificationPolicyResult.failure()
        val credentials = signatures.map { JwtHelper.fromJWS(payload, it).credential }
        return if(credentials.all { SignaturePolicy().verify(VerifiableCredential.fromString(it)).isSuccess }) {
            VerificationPolicyResult.success()
        } else VerificationPolicyResult.failure()
    }
}
