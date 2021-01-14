
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import java.security.Security
import kotlin.test.*
import io.ipfs.multibase.Multibase




class DidServiceTest {

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun multibaseEncodingTest() {
        val input = "Hello World!"
        val data = input.toByteArray()
        val encoded = Multibase.encode(Multibase.Base.Base58BTC, data)
        assertEquals("z2NEpo7TZRRrLZSi2U", encoded)
        val decoded = Multibase.decode(encoded)
        assertEquals(input, String(decoded))
    }

    @Test
    fun registerDidTest() {

        val ds = DidService
        val identifier = ds.registerDid()
        assertNotNull(identifier)
        assertTrue(32 < identifier.length)
        assertEquals("did:key", identifier.substring(0, 7))
        print(identifier)
    }
}
