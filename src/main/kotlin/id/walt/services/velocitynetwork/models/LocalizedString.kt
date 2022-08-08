package id.walt.services.velocitynetwork.models

import kotlinx.serialization.Serializable


@Serializable
data class LocalizedString(
    val localized: Map<Locale, String>
) {
    enum class Locale {
        en,
    }
}
