package id.walt.services.ecosystems.iota

import jnr.ffi.LibraryLoader
import jnr.ffi.Pointer

interface IotaWrapper {
    fun create_did(priv_key_ed25519_bytes: ByteArray?, key_len: Long): Pointer
    fun resolve_did(did: String): Pointer
    fun free_str(str: Pointer)

    companion object {
        fun createInstance(): IotaWrapper {
            return LibraryLoader.create(IotaWrapper::class.java).load("waltid_iota_identity_wrapper")
        }
    }
}
