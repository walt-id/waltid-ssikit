package deltadao

import id.walt.vclib.VcLibManager

object DeltaDao {
    fun registerDeltaDaoCredentials() {
        VcLibManager.register<GaiaxCredential>(GaiaxCredential)
    }
}
