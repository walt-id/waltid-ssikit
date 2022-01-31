package id.walt.json

interface SchemaValidator {
    fun validate(json: String): Set<Any>
}