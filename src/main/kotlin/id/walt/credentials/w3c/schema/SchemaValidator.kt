package id.walt.credentials.w3c.schema

interface SchemaValidator {
    fun validate(json: String): Boolean
}
