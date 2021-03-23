package org.letstrust.crypto

import java.security.Provider

class LetsTrustProvider : Provider(
    "LT", "1.0",
    "LetsTrust Security Provider, supporting signatures with following algorithms [ES256k]"
) {
    init {
        put("Signature.SHA256withECDSA", "org.letstrust.LtSignature\$SHA1")
        put("Signature.SHA256withECDSA", "org.letstrust.LtSignature\$SHA224")
        put("Signature.SHA256withECDSA", "org.letstrust.LtSignature\$SHA256")
        put("Signature.SHA256withECDSA", "org.letstrust.LtSignature\$SHA384")
        put("Signature.SHA256withECDSA", "org.letstrust.LtSignature\$SHA512")
        println(LetsTrustKeyStore::class.java)
        put("KeyStore.PKCS11", "org.letstrust.crypto.KeyStoreProvider")
    }
}

