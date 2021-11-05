package id.walt.services.key

import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.util.Base64URL
import org.bouncycastle.asn1.ASN1BitString
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Sequence
import java.security.KeyPair

object KeyUtil {


    // Thx to jrichter for providing this code withing lib: json-webkey-generator
    fun make(keyPair: KeyPair, keyCurve: Curve, keyUse: KeyUse?, keyAlg: Algorithm?, kid: String): OctetKeyPair {

        // Java only gives us the keys in ASN.1 format, so we need to parse them back out to get the raw numbers

        /*
              * Public key is:
              *
              * SEQUENCE (2 elem)
              *   SEQUENCE (1 elem)
              *     OBJECT IDENTIFIER
              *   BIT STRING (n bit) <-- x value
              *
              */
        val pubPrim = ASN1Sequence.fromByteArray(keyPair.public.encoded) as ASN1Sequence
        val x = (pubPrim.getObjectAt(1) as ASN1BitString).octets

        /*
              * Private key is:
              *
              * SEQUENCE (4 elem)
              *   INTEGER
              *   SEQUENCE (1 elem)
              *     OBJECT IDENTIFIER
              *   OCTET STRING (1 elem)
              *     OCTET STRING (n byte) <-- d value
              *   OCTET STRING (n byte) <-- (x value)
              *
              */
        val privPrim = ASN1Sequence.fromByteArray(keyPair.private.encoded) as ASN1Sequence
        var d = (privPrim.getObjectAt(2) as ASN1OctetString).octets

        // Both the public and private keys should be the same length.
        // For some reason, sometimes the private key is double-wrapped in OctetStrings, and we need to unpack that.
        if (x.size < d.size) {
            d = (ASN1OctetString.fromByteArray(d) as ASN1OctetString).octets
        }

        // Now that we have the raw numbers, export them as a JWK
        return OctetKeyPair.Builder(keyCurve, Base64URL.encode(x))
            .d(Base64URL.encode(d))
            .keyUse(keyUse)
            .algorithm(keyAlg)
            .keyID(kid)
            .build()
    }
}
