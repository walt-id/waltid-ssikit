package id.walt.credentials.selectiveDisclosure

data class SDField (
    val isSelectivelyDisclosable: Boolean,
    val isDisclosed: Boolean = true,
    val nestedMap: Map<String, SDField>? = null
)
