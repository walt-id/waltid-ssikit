package id.walt.credentials.w3c.schema

import id.walt.auditor.VerificationPolicyResult
import mu.KotlinLogging
import net.pwall.json.schema.JSONSchema

class PWallSchemaValidator(schema: String) : SchemaValidator {
    private val log = KotlinLogging.logger {}

    private val jsonSchema = JSONSchema.parse(schema)

    override fun validate(json: String): VerificationPolicyResult {
        val errors = jsonSchema.validateBasic(json).errors ?: listOf()
        if (errors.isNotEmpty()) {
            log.debug { "Could not validate vc against schema. The validation errors are:" }
            errors.forEach { log.debug { it } }
        }
        return VerificationPolicyResult(errors.isEmpty(), errors)
    }
}
