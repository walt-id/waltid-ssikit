package org.letstrust.crypto

import java.security.Provider

class LetsTrustProvider : Provider(
    "LetsTrust", "1.0",
    "LetsTrust Security Provider, supporting signatures with following algorithms [ES256k, Ed25519]"
) {
    init {
        put("Signature.SHA1withECDSA", LtSignature.SHA1withECDSA::class.java.name)
        put("Signature.SHA224withECDSA", LtSignature.SHA224withECDSA::class.java.name)
        put("Signature.SHA256withECDSA", LtSignature.SHA256withECDSA::class.java.name)
        put("Signature.SHA384withECDSA", LtSignature.SHA384withECDSA::class.java.name)
        put("Signature.SHA512withECDSA", LtSignature.SHA512withECDSA::class.java.name)

        put("MessageDigest.SHA-256", LtMessageDigestSpi.SHA256::class.java.name)

        put("KeyStore.PKCS11", LtKeyStore::class.java.name)

        put("Cipher.AES/GCM/NoPadding", LtCipherSpi.AES256_GCM_NoPadding::class.java.name)

    }
}
