package id.walt.services.key

import org.bouncycastle.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.RSAKeyParameters
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.RSAPublicKeySpec
import java.security.spec.X509EncodedKeySpec

class AsymmetricPublicKeyConverter {

    fun convert(key: AsymmetricKeyParameter) = when (key) {
        is ECPublicKeyParameters -> ecAsymmetricKeyParameterToPublicKey(key)
        is Ed25519PublicKeyParameters -> edAsymmetricKeyParameterToPublicKey(key)
        is RSAKeyParameters -> rsaAsymmetricKeyParameterToPublicKey(key)
        else -> null
    }

    private fun ecAsymmetricKeyParameterToPublicKey(key: ECPublicKeyParameters): PublicKey = let {
        val ecParameterSpec = EC5Util.convertToSpec(key.parameters)
        val ecPoint: ECPoint = EC5Util.convertPoint(key.q)
        val ecPublicKeySpec = ECPublicKeySpec(ecPoint, ecParameterSpec)
        KeyFactory.getInstance("ECDSA").generatePublic(ecPublicKeySpec)
    }

    private fun edAsymmetricKeyParameterToPublicKey(key: Ed25519PublicKeyParameters): PublicKey = let {
        val pubKeyInfo = SubjectPublicKeyInfo(AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), key.encoded)
        KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(pubKeyInfo.encoded))
    }

    private fun rsaAsymmetricKeyParameterToPublicKey(key: RSAKeyParameters): PublicKey = let {
        val rsaPublicKeySpec = RSAPublicKeySpec(key.modulus, key.exponent)
        KeyFactory.getInstance("RSA").generatePublic(rsaPublicKeySpec)
    }
}
