package id.walt.crypto

import id.walt.Values
import java.security.Provider

class WaltIdInMemoryProvider : Provider(
    "Walt", Values.version,
    "Walt Security Provider, supporting signatures with following algorithms [ES256k, Ed25519]"
) {
    init {
        put("Signature.SHA1withECDSA", InMemoryLtSignature.SHA1withECDSA::class.java.name)
        put("Signature.SHA224withECDSA", InMemoryLtSignature.SHA224withECDSA::class.java.name)
        put("Signature.SHA256withECDSA", InMemoryLtSignature.SHA256withECDSA::class.java.name)
        put("Signature.SHA384withECDSA", InMemoryLtSignature.SHA384withECDSA::class.java.name)
        put("Signature.SHA512withECDSA", InMemoryLtSignature.SHA512withECDSA::class.java.name)
    }
}