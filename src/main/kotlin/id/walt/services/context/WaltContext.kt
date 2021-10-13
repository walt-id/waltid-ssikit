package id.walt.services.context

import id.walt.servicematrix.BaseService
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.hkvstore.HKVStoreService
import id.walt.services.keystore.KeyStoreService
import id.walt.services.vcstore.VcStoreService

const val DEFAULT_CONTEXT_ID = "__DEFAULT__"

abstract class WaltContext: BaseService() {
  override val implementation: WaltContext
    get() = ServiceRegistry.getService()
  abstract fun getKeyStore(): KeyStoreService
  abstract fun getVcStore(): VcStoreService
  abstract fun getHKVStore(): HKVStoreService

  companion object {
    val implementation: WaltContext
      get() = ServiceRegistry.getService()
    fun getService() = implementation
    val keyStore
      get() = implementation.getKeyStore()
    val vcStore
      get() = implementation.getVcStore()
    val hkvStore
      get() = implementation.getHKVStore()
  }
}