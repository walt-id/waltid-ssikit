package id.walt.crypto

import id.walt.services.crypto.SunCryptoService
import id.walt.services.keystore.InMemoryKeyStoreService

open class InMemoryLtSignature(algorithm: String) : LtSignature(algorithm) {

    class SHA1withECDSA : InMemoryLtSignature("SHA1withECDSA")
    class SHA224withECDSA : InMemoryLtSignature("SHA224withECDSA")
    class SHA256withECDSA : InMemoryLtSignature("SHA256withECDSA")
    class SHA384withECDSA : InMemoryLtSignature("SHA384withECDSAS")
    class SHA512withECDSA : InMemoryLtSignature("SHA512withECDSA")

    override val cryptoService = SunCryptoService().let { it.setKeyStore(InMemoryKeyStoreService()) ; it }
}