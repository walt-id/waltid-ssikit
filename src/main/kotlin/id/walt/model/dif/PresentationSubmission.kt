package id.walt.model.dif

data class PresentationSubmission (
    val id: String,
    val definition_id: String,
    val descriptor_map: List<DescriptorMapping>
)
