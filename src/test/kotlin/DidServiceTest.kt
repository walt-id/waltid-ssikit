
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import java.security.Security
import kotlin.test.*

class DidServiceTest {

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun registerDidTest() {

        val ds = DidService
        val identifier = ds.registerDid()
        assertNotNull(identifier)
        assertTrue(32 < identifier.length)
        assertEquals("did:", identifier.substring(0, 4))
        print(identifier)
    }
}
