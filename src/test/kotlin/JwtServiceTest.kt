import org.junit.Test
import org.letstrust.JwtService
import org.letstrust.KeyManagementService
import kotlin.test.assertTrue

class JwtServiceTest {

    @Test
    fun genJwtSecp256k1() {
        val keyId = KeyManagementService.generateSecp256k1KeyPairSun()

        val jwt = JwtService.sign(keyId, "")

        val res1 = JwtService.verify(jwt)
        assertTrue(res1, "JWT verification failed")
    }

    @Test
    fun genJwtEd25519() {
        val keyId = KeyManagementService.generateEd25519KeyPairNimbus()

        val jwt = JwtService.sign(keyId, "")

        val res1 = JwtService.verify(jwt)
        assertTrue(res1, "JWT verification failed")
    }
}
