package id.walt.crypto

import java.security.interfaces.ECPublicKey
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint

class PublicKeyHandle(val keyId: KeyId, val publicKey: ECPublicKey) : ECPublicKey {
    override fun getAlgorithm(): String =
        publicKey.algorithm

    override fun getFormat(): String =
        publicKey.format

    override fun getEncoded(): ByteArray =
        publicKey.encoded

    override fun getParams(): ECParameterSpec =
        publicKey.params

    override fun getW(): ECPoint =
        publicKey.w
}
