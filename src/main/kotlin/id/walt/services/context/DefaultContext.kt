package id.walt.services.context

import id.walt.servicematrix.ServiceRegistry
import id.walt.services.hkvstore.HKVStoreService
import id.walt.services.keystore.KeyStoreService
import id.walt.services.vcstore.VcStoreService

class DefaultContext : WaltContext() {
  override fun getKeyStore(): KeyStoreService = ServiceRegistry.getService()

  override fun getVcStore(): VcStoreService = ServiceRegistry.getService()

  override fun getHKVStore(): HKVStoreService = ServiceRegistry.getService()
}