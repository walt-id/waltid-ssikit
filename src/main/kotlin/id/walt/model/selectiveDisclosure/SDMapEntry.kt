package id.walt.model.selectiveDisclosure

data class SDMapEntry (
    val isSelectivelyDisclosable: Boolean,
    val nestedMap: SDMap
)
