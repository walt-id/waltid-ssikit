import org.junit.Test
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
    fun convertEd25519toX25519PublickeyTest() {

        val ed25519PublicKey = ed25519PublicKeyFromMultibase58Btc("z6Mkfriq1MqLBoPWecGoDLjguo1sB9brj6wT3qZ5BxkKpuP6")

        val publicKeyBase58 = ed25519PublicKey.encodeBase58()

        assertEquals("2QTnR7atrFu3Y7S6Xmmr4hTsMaL1KDh6Mpe9MgnJugbi", publicKeyBase58)

        val x25519PublicKey = convertPublicKeyEd25519ToCurve25519(ed25519PublicKey)

        val x25519CryptonymMultiBase = x25519PublicKeyToMultiBase58Btc(x25519PublicKey)

        assertEquals("z6LSbgq3GejX88eiAYWmZ9EiddS3GaXodvm8MJJyEH7bqXgz", x25519CryptonymMultiBase)
    }

}
