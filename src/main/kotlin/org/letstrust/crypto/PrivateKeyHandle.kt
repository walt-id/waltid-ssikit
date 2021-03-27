package org.letstrust.crypto

import java.security.GeneralSecurityException
import java.security.PrivateKey

class PrivateKeyHandle(val keyId: KeyId) : PrivateKey {
    override fun getAlgorithm(): String {
        return "EC"
    }

    override fun getFormat(): String {
        throw GeneralSecurityException("Not supported, as private keys will not be exposed.")
    }

    override fun getEncoded(): ByteArray {
        throw GeneralSecurityException("Not supported, as private keys will not be exposed.")
    }
}
