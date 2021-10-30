package id.walt.rest

import kotlinx.serialization.Serializable

// Same structure as Javalin is using https://javalin.io/documentation#default-responses
@Serializable
data class ErrorResponse(
    val title: String,
    val status: Int,
    val type: String? = null, //"https://walt.id/todo-point-to-error-doc",
    val details: Array<String> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ErrorResponse

        if (title != other.title) return false
        if (status != other.status) return false
        if (type != other.type) return false
        if (!details.contentEquals(other.details)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + status
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + details.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "ErrorResponse(title='$title', status=$status, type=$type, details=${details.contentToString()})"
    }
}
