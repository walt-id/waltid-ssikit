package id.walt.auditor.policies

import id.walt.auditor.OptionalParameterizedVerificationPolicy
import id.walt.auditor.VerificationPolicyResult
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.schema.SchemaValidatorFactory

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
        }
            ?: VerificationPolicyResult.failure(IllegalArgumentException("No \"argument.schema\" or \"credentialSchema.id\" supplied."))
    }

    override val applyToVP: Boolean
        get() = false
}
