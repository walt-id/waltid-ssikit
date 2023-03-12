package id.walt.services.ecosystems.cheqd.models.job.didstates

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import id.walt.services.ecosystems.cheqd.models.job.didstates.action.ActionDidState
import id.walt.services.ecosystems.cheqd.models.job.didstates.finished.FinishedDidState
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
@TypeFor(field = "state", adapter = DidStateAdapter::class)
open class DidState(
    val state: String,
)

class DidStateAdapter : TypeAdapter<DidState> {
    override fun classFor(type: Any): KClass<out DidState> = when (type as String) {
        "action" -> ActionDidState::class
        "finished" -> FinishedDidState::class
        else -> throw IllegalArgumentException("Unknown did state: $type")
    }

}
