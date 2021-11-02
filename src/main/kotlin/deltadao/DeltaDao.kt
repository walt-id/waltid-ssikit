package deltadao

import id.walt.signatory.DataProviderRegistry
import id.walt.vclib.VcLibManager

object DeltaDao {
    fun registerDeltaDaoCredentials() {
        VcLibManager.register<GaiaxCredential>(GaiaxCredential)
    }

    fun registerDeltaDaoDataProvider() {
        DataProviderRegistry.register(GaiaxCredential::class, DeltaDaoDataProvider())
    }
}
