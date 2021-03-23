package org.letstrust.crypto

import java.security.PrivateKey

class PrivateKeyHandle(val keyId: String) : PrivateKey {
    override fun getAlgorithm(): String {
        return "EC"
    }

    override fun getFormat(): String {
        TODO("Not yet implemented")
    }

    override fun getEncoded(): ByteArray {
        TODO("Not yet implemented")
    }
}
