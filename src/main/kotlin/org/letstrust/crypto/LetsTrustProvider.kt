package org.letstrust.crypto

import java.security.Provider

class LetsTrustProvider : Provider(
    "LetsTrust", "1.0",
    "LetsTrust Security Provider, supporting signatures with following algorithms [ES256k, Ed25519]"
) {
    init {
        put("Signature.SHA1withECDSA", LetsTrustSignature.SHA1withECDSA::class.java.name)
        put("Signature.SHA224withECDSA", LetsTrustSignature.SHA224withECDSA::class.java.name)
        put("Signature.SHA256withECDSA", LetsTrustSignature.SHA256withECDSA::class.java.name)
        put("Signature.SHA384withECDSA", LetsTrustSignature.SHA384withECDSA::class.java.name)
        put("Signature.SHA512withECDSA", LetsTrustSignature.SHA512withECDSA::class.java.name)
        println(LetsTrustKeyStore::class.java)
        put("KeyStore.PKCS11", LetsTrustKeyStore::class.java.name)
    }
}
