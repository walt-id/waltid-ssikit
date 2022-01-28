package id.walt.json

import net.pwall.json.schema.JSONSchema

class PWallSchemaValidator(schema: String): SchemaValidator {

    private val jsonSchema = JSONSchema.parse(schema)

    override fun validate(json: String) = jsonSchema.validateBasic(json).errors?.toSet() ?: emptySet()
}