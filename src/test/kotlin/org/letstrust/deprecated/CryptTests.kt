package org.letstrust.deprecated

import org.junit.Test
import org.letstrust.*
import kotlin.test.assertEquals


class CryptTests {
    @Test
    fun base58EncodingTest() {
        val input = "Hello World!"
        val encoded = input.toByteArray().encodeBase58()
        assertEquals("2NEpo7TZRRrLZSi2U", encoded)
        val decoded = encoded.decodeBase58()
        assertEquals(input, String(decoded))
    }

    @Test
    fun multibaseEncodingTest() {
        val input = "Multibase is awesome! \\o/"
        val encoded = input.toByteArray().encodeMultiBase58Btc()
        assertEquals("zYAjKoNbau5KiqmHPmSxYCvn66dA1vLmwbt", encoded)
        val decoded = encoded.decodeMultiBase58Btc()
        assertEquals(input, String(decoded))
    }

    @Test
    fun convertPublicKeyEd25519ToCurve25519Test() {
        val edPublicKey = "B12NYF8RrR3h41TDCTJojY59usg3mbtbjnFs7Eud1Y6u".decodeBase58()
        val x25519PublicKey = convertPublicKeyEd25519ToCurve25519(edPublicKey)
        assertEquals("JhNWeSVLMYccCk7iopQW4guaSJTojqpMEELgSLhKwRr", x25519PublicKey.encodeBase58())
    }

    @Test
    fun convertEd25519PublicKeyToMultiBase58Btc() {
        val mb =
            org.letstrust.convertEd25519PublicKeyToMultiBase58Btc("B12NYF8RrR3h41TDCTJojY59usg3mbtbjnFs7Eud1Y6u".decodeBase58())
        assertEquals("z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH", mb)
    }

    // @Test - Error in DID spec
    fun convertX25519PublicKeyToMultiBase58BtcTest_DIDKEYSPEC() { // https://w3c-ccg.github.io/did-method-key/
        val b58Key = "JhNWeSVLMYccCk7iopQW4guaSJTojqpMEELgSLhKwRr"
        val mb = convertX25519PublicKeyToMultiBase58Btc(b58Key.decodeBase58())
        assertEquals("zBzoR5sqFgi6q3iFia8JPNfENCpi7RNSTKF7XNXX96SBY4", mb) // -> NOT OK

        val key = convertX25519PublicKeyFromMultibase58Btc(mb)
        assertEquals(b58Key, key.encodeBase58())
    }

    @Test
    fun convertX25519PublicKeyToMultiBase58BtcTest2() { // https://learn.mattr.global/tutorials/dids/did-key -> OK
        val b58Key = "64aFp2ntzaM9kBybNDRpRRfSQ4x7wfLusfJPfg9aKWt6"
        val mb = convertX25519PublicKeyToMultiBase58Btc(b58Key.decodeBase58())
        assertEquals("z6LSgjkRLLbm634tqaMMtrwmk1svFDVEeGX4ke25A8o72ter", mb)

        val key = convertX25519PublicKeyFromMultibase58Btc(mb)
        assertEquals(b58Key, key.encodeBase58())
    }

    @Test
    fun convertEd25519toX25519PublickeyTest() {

        val ed25519PublicKey =
            convertEd25519PublicKeyFromMultibase58Btc("z6Mkfriq1MqLBoPWecGoDLjguo1sB9brj6wT3qZ5BxkKpuP6")

        val publicKeyBase58 = ed25519PublicKey.encodeBase58()

        assertEquals("2QTnR7atrFu3Y7S6Xmmr4hTsMaL1KDh6Mpe9MgnJugbi", publicKeyBase58)

        val x25519PublicKey = convertPublicKeyEd25519ToCurve25519(ed25519PublicKey)

        val x25519PublicKeyBase58 = x25519PublicKey.encodeBase58()

        assertEquals("1eskLvf2fvy5A912VimK3DZRRzgwKayUKbHjpU589vE", x25519PublicKeyBase58)

        val x25519CryptonymMultiBase = convertX25519PublicKeyToMultiBase58Btc(x25519PublicKey)

        assertEquals("z6LSbgq3GejX88eiAYWmZ9EiddS3GaXodvm8MJJyEH7bqXgz", x25519CryptonymMultiBase)
    }

    // @Test - Error in DID spec
    fun convertEd25519toX25519PublickeyTest_DIDKEYSPEC() { // https://w3c-ccg.github.io/did-method-key/  -> NOT OK

        val ed25519PublicKey =
            convertEd25519PublicKeyFromMultibase58Btc("z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH")

        val publicKeyBase58 = ed25519PublicKey.encodeBase58()

        assertEquals("B12NYF8RrR3h41TDCTJojY59usg3mbtbjnFs7Eud1Y6u", publicKeyBase58)

        val x25519PublicKey = convertPublicKeyEd25519ToCurve25519(ed25519PublicKey)

        val x25519PublicKeyBase58 = x25519PublicKey.encodeBase58()

        assertEquals("JhNWeSVLMYccCk7iopQW4guaSJTojqpMEELgSLhKwRr", x25519PublicKeyBase58)

        val x25519CryptonymMultiBase = convertX25519PublicKeyToMultiBase58Btc(x25519PublicKey)

        assertEquals("zBzoR5sqFgi6q3iFia8JPNfENCpi7RNSTKF7XNXX96SBY4", x25519CryptonymMultiBase)
    }

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
