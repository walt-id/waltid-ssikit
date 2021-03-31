package org.letstrust.crypto

import com.google.crypto.tink.KeysetHandle
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.util.Base64URL
import org.bouncycastle.asn1.ASN1BitString
import org.bouncycastle.asn1.ASN1Sequence
import org.letstrust.CryptoProvider
import org.letstrust.crypto.keystore.TinkKeyStore
import java.security.KeyPair
import java.security.PublicKey
import java.security.interfaces.ECPublicKey

inline class KeyId(val id: String) {}

data class Key(val keyId: KeyId, val algorithm: KeyAlgorithm, val cryptoProvider: CryptoProvider) {
    fun getPublicKey(): PublicKey {
        if (this.keyPair != null){
            return this.keyPair!!.public
        } else if (this.keysetHandle != null) {
            return TinkKeyStore.loadPublicKey(this) as ECPublicKey
        }
        throw Exception("No public key for $keyId")
    }

    constructor(keyId: KeyId, algorithm: KeyAlgorithm, cryptoProvider: CryptoProvider, keyPair: KeyPair) : this(keyId, algorithm, cryptoProvider) {
        this.keyPair = keyPair
    }

    constructor(keyId: KeyId, algorithm: KeyAlgorithm, cryptoProvider: CryptoProvider, keysetHandle: KeysetHandle) : this(keyId, algorithm, cryptoProvider) {
        this.keysetHandle = keysetHandle
    }

    var keyPair: KeyPair? = null
    var keysetHandle: KeysetHandle? = null

    fun toJwk(): OctetKeyPair {
        val keyUse = KeyUse.parse("sig")
        val keyAlg = JWSAlgorithm.parse("EdDSA")
        val keyCurve = Curve.parse("Ed25519")
        val pubPrim = ASN1Sequence.fromByteArray(this.getPublicKey().encoded) as ASN1Sequence
        val x = (pubPrim.getObjectAt(1) as ASN1BitString).octets
        return OctetKeyPair.Builder(keyCurve, Base64URL.encode(x))
            .keyUse(keyUse)
            .algorithm(keyAlg)
            .keyID(keyId.id)
            .build()
    }
}

data class KeyMetaData(val algorithm: KeyAlgorithm, val cryptoProvider: CryptoProvider)
