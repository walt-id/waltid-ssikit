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
        val input = "Hello World!"
        val encoded = input.toByteArray().encodeMultiBase58()
        assertEquals("z2NEpo7TZRRrLZSi2U", encoded)
        val decoded = encoded.decodeMultiBase58()
        assertEquals(input, String(decoded))
    }
}
