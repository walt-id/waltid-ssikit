package id.walt.services.context

import id.walt.servicematrix.ServiceRegistry
import id.walt.services.hkvstore.HKVStoreService
import id.walt.services.keystore.KeyStoreService
import id.walt.services.vcstore.VcStoreService

object WaltIdContext : Context {
  override val keyStore: KeyStoreService
    get() = ServiceRegistry.getService()

  override val vcStore: VcStoreService
    get() = ServiceRegistry.getService()

  override val hkvStore: HKVStoreService
    get() = ServiceRegistry.getService()
}
