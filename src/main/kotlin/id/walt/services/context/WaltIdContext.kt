package id.walt.services.context

import com.google.common.cache.CacheLoader
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.hkvstore.HKVStoreService
import id.walt.services.keystore.KeyStoreService
import id.walt.services.vcstore.VcStoreService
import io.javalin.http.Context

object WaltIdContext : id.walt.services.context.Context {
  override val keyStore: KeyStoreService
    get() = ServiceRegistry.getService()

  override val vcStore: VcStoreService
    get() = ServiceRegistry.getService()

  override val hkvStore: HKVStoreService
    get() = ServiceRegistry.getService()
}
