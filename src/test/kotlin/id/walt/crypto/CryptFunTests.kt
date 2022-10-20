package id.walt.crypto

import id.walt.services.WaltIdServices
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.util.encoders.Hex
import org.web3j.crypto.ECDSASignature
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.*
import java.util.*


class CryptFunTests : AnnotationSpec() {

    @Test
    fun publicKeyBase64EncodingTest() {
        val keypair = keyPairGeneratorEd25519().generateKeyPair()
        val pub = keypair.public.toBase64()
        val priv = keypair.private.toBase64()
        val decodedPubKey = decodePubKeyBase64(pub, KeyFactory.getInstance("Ed25519"))
        val decodedPrivKey = decodePrivKeyBase64(priv, KeyFactory.getInstance("Ed25519"))
        keypair.public shouldBe decodedPubKey
        keypair.private shouldBe decodedPrivKey
    }

    @Test
    fun publicKeyPemEncodingTest() {
        val keypair = keyPairGeneratorEd25519().generateKeyPair()
        val pub = keypair.public.toPEM()
        val priv = keypair.private.toPEM()
        val decodedPubKey = decodePubKeyPem(pub, KeyFactory.getInstance("Ed25519"))
        keypair.public shouldBe decodedPubKey
        val decodedPrivKey = decodePrivKeyPem(priv, KeyFactory.getInstance("Ed25519"))
        keypair.private shouldBe decodedPrivKey
    }

    @Test
    fun base64EncodingTest() {
        val input = "Hello World!"
        val encoded = encBase64Str(input)
        "SGVsbG8gV29ybGQh" shouldBe encoded
        val decoded = decBase64Str(encoded)
        input shouldBe decoded
    }

    @Test
    fun base58EncodingTest() {
        val input = "Hello World!"
        val encoded = input.toByteArray().encodeBase58()
        "2NEpo7TZRRrLZSi2U" shouldBe encoded
        val decoded = encoded.decodeBase58()
        input shouldBe String(decoded)
    }

    @Test
    fun multibaseEncodingTest() {
        val input = "Multibase is awesome! \\o/"
        val encoded = input.toByteArray().encodeMultiBase58Btc()
        "zYAjKoNbau5KiqmHPmSxYCvn66dA1vLmwbt" shouldBe encoded
        val decoded = encoded.decodeMultiBase58Btc()
        input shouldBe String(decoded)
    }

    @Test
    fun multibaseEd25519KeyTest() {
        val input =
            "AF CE E7 54 B5 D7 5A C9 4F A9 6E 5F E7 37 22 9F 09 CE D6 56 49 8B 88 99 3F A2 69 E2 13 79 B3 07".fromHexString()
        val encoded = convertRawKeyToMultiBase58Btc(input, getMulticodecKeyCode(KeyAlgorithm.EdDSA_Ed25519))
        encoded shouldBe "z6MkrHYNwvaGMF5KcpQFkunDGeZA4ovPvEVXe66jZKjC5DdY"
        val decoded = convertMultiBase58BtcToRawKey(encoded)
        decoded shouldBe input
    }

    // https://w3c-ccg.github.io/did-method-key/#rsa
    @Test
    fun multibaseRsaKeyTest() {
        val input =
            "30 82 01 0A 02 82 01 01 00 E7 F6 D4 31 48 B2 59 80 32 27 A9 22 01 51 CA C1 B8 F9 88 8A B3 84 3D FE 70 4C 9E F9 7E D0 31 ED 40 89 F6 28 A4 45 A0 E4 FF 21 19 B3 45 5F EA D4 1D 3B 61 81 ED E5 CF 1F 2F CB AA 55 26 22 11 93 23 30 D1 B5 60 07 02 D0 7F 4A F3 78 AF 9D 43 89 31 9B 5E BA 7C 4A A2 B7 46 40 8F 29 4D AB 00 EC 52 07 B3 E4 38 BA D9 E8 76 D8 62 31 EF 24 47 6F 83 77 52 05 D2 A7 70 2F 6F 47 D8 CB 1C F2 A5 64 08 A9 15 0D BF 1E 69 A7 EC 83 78 5A 84 EC 7D 1B 5D 31 CB 09 6B 6F 3E E6 6F BB 1C 2A 6E 72 B7 F3 E5 1E B1 60 7A 86 C1 27 C3 B1 E6 B4 FE 0A 2F 80 EF 2C 5D AE 6E AB 1D 34 F2 D2 D0 AE 62 74 23 9B AD 06 D6 B6 72 3C 97 60 4E 70 76 DE 1A 17 11 0F 10 E9 8B 4D EA EF C1 B4 ED 47 FB 1A 58 57 51 73 55 8D 45 25 F3 62 B8 BB E8 05 79 F7 BF 8E CE 88 6E 98 FD DF EF B0 C6 40 57 91 7B 80 11 DE A8 CD 02 03 01 00 01".fromHexString()
        val encoded = convertRawKeyToMultiBase58Btc(input, getMulticodecKeyCode(KeyAlgorithm.RSA))
        encoded shouldBe "z4MXj1wBzi9jUstyQbrdGy3KjQ7LwP8KRzQy1tvHyU8yhTDGYjK7PWPQW4U3bscyq1po8DnuSr9nNcCkBqQZmkEEeFKjdX54u9sf8cVVQcWmq5jhkyPrErVyjpQ6t8s5km91evQjKNH1j3JwUjJ7FdP5qbB2wCU9R5bV3Z1f2rbBRcnRnkGo2gAj8Qz72PLE2jGWZeNaq3XXfz3y4ojemRv3RvmeqVMLcJBbLaXSs6J4pCoey9ownLCD7hx8csxwW9x5QbGmgYN8sxB8bB4QMkghyLpMAHGYAyjsUp8pttCogu9oitEjFoK4X8WjNPj3N3VDEou66dqZ8rDgH8F2uTgbnBMohHuY3kwJGJNoNQgqZnprqWry6"
        val decoded = convertMultiBase58BtcToRawKey(encoded)
        decoded shouldBe input
    }

    @Test
    fun convertPublicKeyEd25519ToCurve25519Test() {
        val edPublicKey = "B12NYF8RrR3h41TDCTJojY59usg3mbtbjnFs7Eud1Y6u".decodeBase58()
        val x25519PublicKey = convertPublicKeyEd25519ToCurve25519(edPublicKey)
        "JhNWeSVLMYccCk7iopQW4guaSJTojqpMEELgSLhKwRr" shouldBe x25519PublicKey.encodeBase58()
    }

    @Test
    fun convertEd25519PublicKeyToMultiBase58Btc() {
        val mb =
            convertEd25519PublicKeyToMultiBase58Btc("B12NYF8RrR3h41TDCTJojY59usg3mbtbjnFs7Eud1Y6u".decodeBase58())
        "z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH" shouldBe mb
    }

    // @Test - Error in DID spec
    fun convertX25519PublicKeyToMultiBase58BtcTest_DIDKEYSPEC() { // https://w3c-ccg.github.io/did-method-key/
        val b58Key = "JhNWeSVLMYccCk7iopQW4guaSJTojqpMEELgSLhKwRr"
        val mb = convertX25519PublicKeyToMultiBase58Btc(b58Key.decodeBase58())
        "zBzoR5sqFgi6q3iFia8JPNfENCpi7RNSTKF7XNXX96SBY4" shouldBe mb // -> NOT OK

        val key = convertX25519PublicKeyFromMultibase58Btc(mb)
        b58Key shouldBe key.encodeBase58()
    }

    @Test
    fun convertX25519PublicKeyToMultiBase58BtcTest2() { // https://learn.mattr.global/tutorials/dids/did-key -> OK
        val b58Key = "64aFp2ntzaM9kBybNDRpRRfSQ4x7wfLusfJPfg9aKWt6"
        val mb = convertX25519PublicKeyToMultiBase58Btc(b58Key.decodeBase58())
        "z6LSgjkRLLbm634tqaMMtrwmk1svFDVEeGX4ke25A8o72ter" shouldBe mb

        val key = convertX25519PublicKeyFromMultibase58Btc(mb)
        b58Key shouldBe key.encodeBase58()
    }

    @Test
    fun convertEd25519toX25519PublickeyTest() {

        val ed25519PublicKey =
            convertEd25519PublicKeyFromMultibase58Btc("z6Mkfriq1MqLBoPWecGoDLjguo1sB9brj6wT3qZ5BxkKpuP6")

        val publicKeyBase58 = ed25519PublicKey.encodeBase58()

        "2QTnR7atrFu3Y7S6Xmmr4hTsMaL1KDh6Mpe9MgnJugbi" shouldBe publicKeyBase58

        val x25519PublicKey = convertPublicKeyEd25519ToCurve25519(ed25519PublicKey)

        val x25519PublicKeyBase58 = x25519PublicKey.encodeBase58()

        "1eskLvf2fvy5A912VimK3DZRRzgwKayUKbHjpU589vE" shouldBe x25519PublicKeyBase58

        val x25519CryptonymMultiBase = convertX25519PublicKeyToMultiBase58Btc(x25519PublicKey)

        "z6LSbgq3GejX88eiAYWmZ9EiddS3GaXodvm8MJJyEH7bqXgz" shouldBe x25519CryptonymMultiBase
    }

    // @Test - Error in DID spec
    fun convertEd25519toX25519PublicKeyTest_DIDKEYSPEC() { // https://w3c-ccg.github.io/did-method-key/  -> NOT OK

        val ed25519PublicKey =
            convertEd25519PublicKeyFromMultibase58Btc("z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH")

        val publicKeyBase58 = ed25519PublicKey.encodeBase58()

        "B12NYF8RrR3h41TDCTJojY59usg3mbtbjnFs7Eud1Y6u" shouldBe publicKeyBase58

        val x25519PublicKey = convertPublicKeyEd25519ToCurve25519(ed25519PublicKey)

        val x25519PublicKeyBase58 = x25519PublicKey.encodeBase58()

        "JhNWeSVLMYccCk7iopQW4guaSJTojqpMEELgSLhKwRr" shouldBe x25519PublicKeyBase58

        val x25519CryptonymMultiBase = convertX25519PublicKeyToMultiBase58Btc(x25519PublicKey)

        "zBzoR5sqFgi6q3iFia8JPNfENCpi7RNSTKF7XNXX96SBY4" shouldBe x25519CryptonymMultiBase
    }

    ///VALUE zBzoR5sqFgi6q3iFia8JPNfENCpi7RNSTKF7XNXX96SBY4 is calculated wrongly!!

//{
//    "@context": "https://w3id.org/did/v1",
//    "id": "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH",
//    "publicKey": [{
//    "id": "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH#z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH",
//    "type": "Ed25519VerificationKey2018",
//    "controller": "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH",
//    "publicKeyBase58": "B12NYF8RrR3h41TDCTJojY59usg3mbtbjnFs7Eud1Y6u"
//}],
//    "authentication": [ "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH#z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH" ],
//    "assertionMethod": [ "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH#z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH" ],
//    "capabilityDelegation": [ "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH#z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH" ],
//    "capabilityInvocation": [ "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH#z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH" ],
//    "keyAgreement": [{
//    "id": "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH#zBzoR5sqFgi6q3iFia8JPNfENCpi7RNSTKF7XNXX96SBY4",
//    "type": "X25519KeyAgreementKey2019",
//    "controller": "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH",
//    "publicKeyBase58": "JhNWeSVLMYccCk7iopQW4guaSJTojqpMEELgSLhKwRr"
//}]
//}


    // https://stackoverflow.com/questions/48832170/generate-ec-public-key-from-byte-array-private-key-in-native-java-7
    // Following will be used in CryptFun::keyPairGeneratorSecp256k1
    @Test
    fun generateECDSA_Secp256k1() {

        // val kg = KeyPairGenerator.getInstance("EC") // only working with Java 11  - not higher :-( -> https://bugs.openjdk.java.net/browse/JDK-8251547

        WaltIdServices // For loading Bouncy Castle
        val kg = KeyPairGenerator.getInstance("EC", "BC")
        kg.initialize(ECGenParameterSpec("secp256k1"), SecureRandom())
//OR
//        val kg = KeyPairGenerator.getInstance("ECDSA", "BC")
//        kg.initialize(ECNamedCurveTable.getParameterSpec("secp256k1"), SecureRandom())

        val p: ECParameterSpec = (kg.generateKeyPair().public as ECPublicKey).params
        println("p=(dec)" + (p.curve.field as ECFieldFp).p)
        val G: ECPoint = p.generator
        System.out.format("Gx=(hex)%032x%n", G.affineX)
        System.out.format("Gy=(hex)%032x%n", G.affineY)

//        val privatekey_enc: ByteArray = DatatypeConverter.parseHexBinary(
//            "303E020100301006072A8648CE3D020106052B8104000A042730250201010420" +
//                    "1184CD2CDD640CA42CFC3A091C51D549B2F016D454B2774019C2B2D2E08529FD"
//        ) // note fixed prefix for PKCS8-EC-secp256k1 plus your private value
//        val kf: KeyFactory = KeyFactory.getInstance("EC")
//        val k1: PrivateKey = kf.generatePrivate(PKCS8EncodedKeySpec(privatekey_enc))
//        val p2: ECParameterSpec = (k1 as ECPrivateKey).getParams()
//        println("again p=(dec)" + (p2.getCurve().getField() as ECFieldFp).p)
    }

    @Test
    fun toECDSASignature() {
        val actual = toECDSASignature(
            Hex.decode("3046022100c638bbfe76516c7e61a55c47f74ce93496119925b25c17c901c19aa5aa3a96770221008c47712a0291bbebe2bd82fce85d8ac4850546096503dabc28bf599ba4a435da"),
            KeyAlgorithm.ECDSA_Secp256k1
        )
        val expected = ECDSASignature(
            BigInteger("89658184941967983938858979071000288788759949065333276282917577796385005803127"),
            BigInteger("63450025973182083115333495879828484637440221913248412008596464222239711311322")
        ).toCanonicalised()
        expected.r shouldBe actual.r
        expected.s shouldBe actual.s
    }

    @Test
    fun constructJcaEd25519fromBytes() {
        // Both formatted as 32bit raw key values (x and d)
        // Both formatted as 32bit raw key values (x and d)
        val privateKeyBytes = Base64.getUrlDecoder().decode("nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A")
        val publicKeyBytes = Base64.getUrlDecoder().decode("11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo")

        val keyFactory = KeyFactory.getInstance("Ed25519")

        val pubKeyInfo: SubjectPublicKeyInfo =
            SubjectPublicKeyInfo(AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), publicKeyBytes)
        val x509KeySpec = X509EncodedKeySpec(pubKeyInfo.encoded)

        val jcaPublicKey = keyFactory.generatePublic(x509KeySpec)


        val privKeyInfo =
            PrivateKeyInfo(AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), DEROctetString(privateKeyBytes))
        val pkcs8KeySpec = PKCS8EncodedKeySpec(privKeyInfo.encoded)

        val jcaPrivateKey = keyFactory.generatePrivate(pkcs8KeySpec)

    }
}

