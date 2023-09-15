package id.walt.services.key.deriver

import id.walt.services.key.AsymmetricPublicKeyConverter
import org.bouncycastle.crypto.params.*
import java.security.PublicKey

class AsymmetricPublicKeyDeriver(
    private val keyConverter: AsymmetricPublicKeyConverter
) : PublicKeyDeriver<AsymmetricKeyParameter> {

    override fun derive(key: AsymmetricKeyParameter): PublicKey? = when (key) {
        is RSAPrivateCrtKeyParameters -> {
            RSAKeyParameters(false, key.modulus, key.publicExponent)
        }
        is Ed25519PrivateKeyParameters -> {
            key.generatePublicKey()
        }
        is ECPrivateKeyParameters -> {
            val q = key.parameters.g.multiply(key.d)
            ECPublicKeyParameters(q, key.parameters)
        }
        else -> null
    }?.let{
        keyConverter.convert(it)
    }
}
