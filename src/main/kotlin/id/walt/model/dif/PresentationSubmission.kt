package id.walt.model.dif

data class PresentationSubmission (
    val descriptor_map: List<DescriptorMapping>,
    val id: String? = null,
    val definition_id: String? = null
)
