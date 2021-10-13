package id.walt.context

import id.walt.services.context.WaltContext
import id.walt.services.hkvstore.FileSystemHKVStore
import id.walt.services.hkvstore.FilesystemStoreConfig
import id.walt.services.hkvstore.HKVStoreService
import id.walt.services.keystore.HKVKeyStoreService
import id.walt.services.keystore.KeyStoreService
import id.walt.services.vcstore.HKVVcStoreService
import id.walt.services.vcstore.VcStoreService

val TEST_CONTEXT_DATA_ROOT = "build/testdata/context"

class TestContext(var currentContext: String): WaltContext() {

  val userContexts = mapOf(
    Pair("userA", FileSystemHKVStore(FilesystemStoreConfig(dataRoot = "${id.walt.context.TEST_CONTEXT_DATA_ROOT}/userA"))),
    Pair("userB",  FileSystemHKVStore(FilesystemStoreConfig(dataRoot = "${id.walt.context.TEST_CONTEXT_DATA_ROOT}/userB")))
  )

  val keyStore = HKVKeyStoreService()
  val vcStore = HKVVcStoreService()

  override fun getKeyStore(): KeyStoreService = keyStore

  override fun getVcStore(): VcStoreService = vcStore

  override fun getHKVStore(): HKVStoreService {
    return userContexts.get(currentContext)!!
  }

}
