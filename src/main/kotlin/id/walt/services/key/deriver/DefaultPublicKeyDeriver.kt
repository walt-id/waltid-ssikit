package id.walt.services.key.deriver

import id.walt.crypto.KeyAlgorithm
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.RSAPublicKeySpec


class DefaultPublicKeyDeriver: PublicKeyDeriver<PrivateKey> {
    override fun derive(key: PrivateKey): PublicKey? = when (KeyAlgorithm.fromString(key.algorithm)) {
        KeyAlgorithm.RSA -> (key as? RSAPrivateCrtKey)?.let {
            KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(it.modulus, it.publicExponent))
        }
        KeyAlgorithm.EdDSA_Ed25519 -> {

            null
        }
        //TODO: remove BC dependency, rely purely on java.security
        KeyAlgorithm.ECDSA_Secp256k1, KeyAlgorithm.ECDSA_Secp256r1 -> {
            val definingKey = key as BCECPrivateKey
            val d = definingKey.d
            val ecSpec = definingKey.parameters
            val q = definingKey.parameters.g.multiply(d)
            val pubSpec = org.bouncycastle.jce.spec.ECPublicKeySpec(q, ecSpec)
            KeyFactory.getInstance("ECDSA").generatePublic(pubSpec)
        }
    }
}
