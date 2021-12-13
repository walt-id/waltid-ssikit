package id.walt.crypto

import java.security.GeneralSecurityException
import java.security.PrivateKey

class ECPrivateKeyHandle(val keyId: KeyId) : PrivateKey {
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

class RSAPrivateKeyHandle(val keyId: KeyId) : PrivateKey {
    override fun getAlgorithm(): String {
        return "RSA"
    }

    override fun getFormat(): String {
        throw GeneralSecurityException("Not supported, as private keys will not be exposed.")
    }

    override fun getEncoded(): ByteArray {
        throw GeneralSecurityException("Not supported, as private keys will not be exposed.")
    }
}
