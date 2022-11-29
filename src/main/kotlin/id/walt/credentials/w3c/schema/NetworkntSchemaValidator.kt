package id.walt.credentials.w3c.schema

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import mu.KotlinLogging

class NetworkntSchemaValidator(versionFlag: SpecVersion.VersionFlag, schema: String): SchemaValidator {
    private val log = KotlinLogging.logger {}
    
    private val jsonSchema = JsonSchemaFactory.getInstance(versionFlag).getSchema(schema)
    private val mapper = ObjectMapper()

    override fun validate(json: String): Boolean {
        return jsonSchema.validate(mapper.readTree(json)).also {
            if(it.isNotEmpty()) {
                log.debug { "Could not validate vc against schema . The validation errors are:" }
                it.forEach { log.debug { it } }
            }
        }.isEmpty()
    }
}
