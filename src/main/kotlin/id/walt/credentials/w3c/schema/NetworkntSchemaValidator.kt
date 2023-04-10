package id.walt.credentials.w3c.schema

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import id.walt.auditor.VerificationPolicyResult
import mu.KotlinLogging
import javax.naming.directory.SchemaViolationException

class NetworkntSchemaValidator(versionFlag: SpecVersion.VersionFlag, schema: String) : SchemaValidator {
    private val log = KotlinLogging.logger {}

    private val jsonSchema = JsonSchemaFactory.getInstance(versionFlag).getSchema(schema)
    private val mapper = ObjectMapper()

    override fun validate(json: String): VerificationPolicyResult {
        val errors = jsonSchema.validate(mapper.readTree(json)).toList()
        if (errors.isNotEmpty()) {
            log.debug { "Could not validate vc against schema. The validation errors are:" }
            errors.forEach { log.debug { it } }
        }
        return errors.takeIf { it.isEmpty() }?.let {
            VerificationPolicyResult.success()
        } ?: VerificationPolicyResult.failure(*errors.map { SchemaViolationException(it.toString()) }.toTypedArray())
    }
}
