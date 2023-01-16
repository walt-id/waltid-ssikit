package id.walt.credentials.w3c.schema

import mu.KotlinLogging
import net.pwall.json.schema.JSONSchema

class PWallSchemaValidator(schema: String) : SchemaValidator {
    private val log = KotlinLogging.logger {}

    private val jsonSchema = JSONSchema.parse(schema)

    override fun validate(json: String) = jsonSchema.validateBasic(json).errors?.also {
        if (it.isNotEmpty()) {
            log.debug { "Could not validate vc against schema . The validation errors are:" }
            it.forEach { log.debug { it } }
        }
    }?.isEmpty() ?: true
}
