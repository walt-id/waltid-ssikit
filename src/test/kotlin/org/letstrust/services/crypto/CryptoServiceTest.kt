package org.letstrust.services.crypto

import org.junit.Test
import org.letstrust.LetsTrustServices
import org.letstrust.crypto.*
import org.letstrust.services.key.KeyService
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import java.security.KeyStore
import java.security.Provider
import java.security.Security
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CryptoServiceTest {

    @Test
    fun testGetEthereumAddress() {
        val keyId = KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val calculatedAddress = KeyService.getEthereumAddress(keyId.id)
        val addressFromKeyPair =
            Keys.toChecksumAddress(Keys.getAddress(ECKeyPair.create(KeyService.load(keyId.id, true).keyPair)))
        assertEquals(addressFromKeyPair, calculatedAddress)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetEthereumAddressWithBadKeyAlgorithm() {
        val keyId = KeyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        KeyService.getEthereumAddress(keyId.id)
    }

    @Test
    fun testGenSecp256k1Sun() {
        val keyId = SunCryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
        assertNotNull(keyId.id)
    }

    @Test
    fun testGenEd255191Sun() {
        val keyId = SunCryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        assertNotNull(keyId.id)
    }

    @Test
    fun testGenEd25519Tink() {
        val keyId = TinkCryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        assertNotNull(keyId.id)
    }

    // @Test TODO: not supported yet https://github.com/google/tink/issues/146
    fun testGenSecp256k1Tink() {
        val keyId = TinkCryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
        assertNotNull(keyId.id)
    }

    @Test
    fun testAddLetsTrustProviders() {

        LetsTrustServices

        Security.addProvider(LetsTrustProvider());

        println("SupportedCurves")
        println(
            "SupportedCurves: -> SunEC: " + Security.getProvider("SunEC")
                .getProperty("AlgorithmParameters.EC SupportedCurves")
        )


        var letsTrustProviderFound = false

        val mysig = Signature.getInstance("SHA256withECDSA", "LetsTrust")
        println(mysig)

        val myks = KeyStore.getInstance("PKCS11", "LetsTrust")
        println(myks)

        val mycipher = Cipher.getInstance("AES/GCM/NoPadding", "LetsTrust")
        println(mycipher)

        val providers: Array<Provider> = Security.getProviders()

        for (p in providers) {
            val info: String = p.getInfo()
            println(p.toString() + " - " + info)

            if (p.toString().contains("LetsTrust version 1.0", true)) {
                letsTrustProviderFound = true
            }

            p.services.forEach { s ->
                if (s.toString().contains("LetsTrust", true)) {
                    println("\t -> " + s)
                }
            }
        }
        assertTrue(letsTrustProviderFound, "LetsTrust provider not registered correctly")
    }

    @Test
    fun testEncWithAes() {
        val data = "Encrypt Me!"
        val encKey = KeyGenerator.getInstance("AES").generateKey()
        val encCipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        encCipher.init(Cipher.ENCRYPT_MODE, encKey)
        val enc = encCipher.doFinal(data.toByteArray())

        val encodedKey = encBase64(encKey.encoded)
        //        val ap = encCipher.parameters // IV
        val decKey = SecretKeySpec(decBase64(encodedKey), "AES")
        val decCipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        decCipher.init(Cipher.DECRYPT_MODE, decKey)
        val decData = String(decCipher.doFinal(enc))

        assertEquals(data, decData)
    }

}
