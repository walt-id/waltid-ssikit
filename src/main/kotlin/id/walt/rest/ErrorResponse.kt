package id.walt.rest

import kotlinx.serialization.Serializable

// Same structure as Javalin is using https://javalin.io/documentation#default-responses
@Serializable
data class ErrorResponse(
    val title: String,
    val status: Int,
    val type: String? = null, //"https://walt.id/todo-point-to-error-doc",
    val details: Array<String> = emptyArray()
)
