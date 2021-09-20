package id.walt.rest

import io.javalin.plugin.openapi.dsl.document

object OpenAPIUtils {
    fun documentedIgnored() = document().ignore(true)
}
