package id.walt.credentials.selectiveDisclosure

/**
 * @param sd    For issuance: field is selectively disclosable, for presentation: field is disclosed
 * @param nestedMap    Not null, if field is an object, contains SDField map for the properties of the object
 */
data class SDField (
    val sd: Boolean,
    val nestedMap: Map<String, SDField>? = null
) {
    companion object {
        fun prettyPrintSdMap(sdMap: Map<String, SDField>, indentBy: Int = 0) {
            val indentation = (0).rangeTo(indentBy).joinToString (" "){ "" }
            sdMap.keys.forEach { key ->
                println("${indentation}- $key: ${sdMap[key]?.sd == true}")
                sdMap[key]?.nestedMap?.also { prettyPrintSdMap(it, indentBy+2) }
            }
        }
    }
}
