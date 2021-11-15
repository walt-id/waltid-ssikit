package id.walt.context

import com.google.common.cache.CacheLoader
import id.walt.services.context.Context
import id.walt.services.hkvstore.FileSystemHKVStore
import id.walt.services.hkvstore.FilesystemStoreConfig
import id.walt.services.hkvstore.HKVStoreService
import id.walt.services.keystore.HKVKeyStoreService
import id.walt.services.keystore.KeyStoreService
import id.walt.services.vcstore.HKVVcStoreService
import id.walt.services.vcstore.VcStoreService

val TEST_CONTEXT_DATA_ROOT = "build/testdata/context"

class TestContext(val username: String): Context {
  override val keyStore = HKVKeyStoreService()
  override val vcStore = HKVVcStoreService()
  override val hkvStore = FileSystemHKVStore(FilesystemStoreConfig("${id.walt.context.TEST_CONTEXT_DATA_ROOT}/${username}"))
}
