package id.walt.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion

class NetworkntSchemaValidator(versionFlag: SpecVersion.VersionFlag, schema: String): SchemaValidator {

    private val jsonSchema = JsonSchemaFactory.getInstance(versionFlag).getSchema(schema)
    private val mapper = ObjectMapper()

    override fun validate(json: String): Set<Any> {
        return jsonSchema.validate(mapper.readTree(json))
    }
}