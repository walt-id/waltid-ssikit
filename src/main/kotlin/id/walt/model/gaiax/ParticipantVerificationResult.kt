package id.walt.model.gaiax


import kotlinx.serialization.Serializable

@Serializable
data class ParticipantVerificationResult(
    val conforms: Boolean, // true
    val content: Content,
    val isValidSignature: Boolean, // true
    val shape: Shape
) {
    @Serializable
    data class Content(
        val conforms: Boolean, // false
        val results: List<String>
    )

    @Serializable
    data class Shape(
        val conforms: Boolean, // true
        val results: List<String>
    )

    override fun toString() =
        """
            Conforms: $conforms
            Shape conforms: ${shape.conforms}, results: ${shape.results}
            Content conforms: ${content.conforms}, results: ${content.results}
            Valid signature: $isValidSignature
        """.trimIndent()
}
