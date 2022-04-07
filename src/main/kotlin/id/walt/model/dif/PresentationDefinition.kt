package id.walt.model.dif

data class PresentationDefinition (
    val id: String = "1",
    val input_descriptors: List<InputDescriptor>,
    val name: String? = null,
    val purpose: String? = null,
    val format: VCFormat? = null,
    val submission_requirements: List<SubmissionRequirement>? = null
)
