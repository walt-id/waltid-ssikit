package id.walt.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.SpecVersion.VersionFlag.*
import java.net.URI

object SchemaValidatorFactory {

    private val mapper = ObjectMapper()

    fun get(schema: URI): SchemaValidator = get(schema.toURL().readText())

    fun get(schema: String): SchemaValidator = with (mapper.readTree(schema).get("\$schema").asText()) {
        return when {
            contains("2019-09") -> NetworkntSchemaValidator(V201909, schema)
            contains("draft-07") -> NetworkntSchemaValidator(V7, schema)
            contains("draft-06") -> NetworkntSchemaValidator(V6, schema)
            contains("draft-04") -> NetworkntSchemaValidator(V4, schema)
            else -> PWallSchemaValidator(schema)
        }
    }
}