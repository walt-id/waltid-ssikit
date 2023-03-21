package id.walt.credentials.w3c.schema

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.SpecVersion.VersionFlag.*
import id.walt.common.resolveContent
import java.net.URI

object SchemaValidatorFactory {

    private val mapper = ObjectMapper()

    fun get(schema: URI): SchemaValidator = get(schema.toURL().readText())

    fun get(schemaUrlFileOrContent: String): SchemaValidator {
        val schema = resolveContent(schemaUrlFileOrContent).trim()
        require(schema.startsWith('{') && schema.endsWith('}')) { "Invalid schema content: $schema" }
        with(mapper.readTree(schema).get("\$schema").asText()) {
            return when {
                contains("2020-12") -> NetworkntSchemaValidator(V202012, schema)
                contains("2019-09") -> NetworkntSchemaValidator(V201909, schema)
                contains("draft-07") -> NetworkntSchemaValidator(V7, schema)
                contains("draft-06") -> NetworkntSchemaValidator(V6, schema)
                contains("draft-04") -> NetworkntSchemaValidator(V4, schema)
                else -> PWallSchemaValidator(schema)
            }
        }
    }
}
