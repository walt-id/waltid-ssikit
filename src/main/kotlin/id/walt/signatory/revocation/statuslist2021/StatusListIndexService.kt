package id.walt.signatory.revocation.statuslist2021

import com.beust.klaxon.Klaxon
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.WaltIdServices
import kotlinx.serialization.Serializable
import java.io.File


open class StatusListIndexService : WaltIdService() {
    override val implementation get() = serviceImplementation<StatusListIndexService>()

    open fun create(): StatusListIndex = implementation.create()
    open fun read(): StatusListIndex? = implementation.read()
    open fun update(index: StatusListIndex): Unit = implementation.update(index)
    open fun delete(): Unit = implementation.delete()

    companion object : ServiceProvider {
        override fun getService() = object : StatusListIndexService() {}
        override fun defaultImplementation() = WaltIdStatusListIndexService()
    }

}

@Serializable
data class StatusListIndex(
    val index: String
)

class WaltIdStatusListIndexService : StatusListIndexService() {
    private val indexPath = "${WaltIdServices.revocationDir}/status-list-index.json"

    override fun create(): StatusListIndex = StatusListIndex(index = "0").let {
        createAndUpdateIndex(it)
        it
    }

    override fun read(): StatusListIndex? = checkIndex()?.let {
        Klaxon().parse<StatusListIndex>(it.readText())
    }

    override fun update(index: StatusListIndex): Unit = createAndUpdateIndex(index)

    override fun delete(): Unit = run { checkIndex()?.delete() }

    private fun checkIndex() = File(indexPath).takeIf { it.exists() }

    private fun createAndUpdateIndex(index: StatusListIndex): Unit = run {
        File(indexPath).takeIf { it.exists() } ?: let {
            File(indexPath).createNewFile()
            File(indexPath)
        }
    }.run {
        this.writeText(Klaxon().toJsonString(index))
    }
}
