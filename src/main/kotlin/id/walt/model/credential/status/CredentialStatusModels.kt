package id.walt.model.credential.status

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
@TypeFor(field = "type", adapter = CredentialStatusTypeAdapter::class)
sealed class CredentialStatus(
    val type: String,
) {
    abstract val id: String
    @Serializable
    enum class Types{
        StatusList2021Entry,
        SimpleCredentialStatus2022,
    }
}

@Serializable
class SimpleCredentialStatus2022(
    override val id: String,
) : CredentialStatus(Types.SimpleCredentialStatus2022.name)

@Serializable
data class StatusList2021EntryCredentialStatus(
    override val id: String,
    val statusPurpose: String,
    val statusListIndex: String,
    val statusListCredential: String,
) : CredentialStatus(Types.StatusList2021Entry.name)

class CredentialStatusTypeAdapter : TypeAdapter<CredentialStatus> {
    override fun classFor(type: Any): KClass<out CredentialStatus> = when (type as String) {
        "SimpleCredentialStatus2022" -> SimpleCredentialStatus2022::class
        "StatusList2021Entry" -> StatusList2021EntryCredentialStatus::class
        else -> throw IllegalArgumentException("CredentialStatus type is not supported: $type")
    }
}
