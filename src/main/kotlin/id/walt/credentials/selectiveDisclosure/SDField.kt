package id.walt.credentials.selectiveDisclosure

/**
 * @param sd    For issuance: field is selectively disclosable, for presentation: field is disclosed
 * @param nesetedMap    Not null, if field is an object, contains SDField map for the properties of the object
 */
data class SDField (
    val sd: Boolean,
    val nestedMap: Map<String, SDField>? = null
)
