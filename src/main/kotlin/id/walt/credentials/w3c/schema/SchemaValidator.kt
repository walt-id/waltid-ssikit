package id.walt.credentials.w3c.schema

import id.walt.auditor.VerificationPolicyResult

interface SchemaValidator {
    fun validate(json: String): VerificationPolicyResult
}
