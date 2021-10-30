package id.walt.crypto

import com.microsoft.azure.keyvault.KeyVaultClient
import com.microsoft.azure.keyvault.models.KeyBundle
import com.microsoft.azure.keyvault.webkey.JsonWebKey
import com.microsoft.azure.keyvault.webkey.JsonWebKeyCurveName
import com.sksamuel.hoplite.PropertySource
import id.walt.services.crypto.CryptoService
import id.walt.services.key.KeyService
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.AnnotationSpec
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec

@Ignored // FIXME: mockk KeyVaultClient.sign and verify methods
class AzureCryptoTest : AnnotationSpec() {

    private val cryptoService = CryptoService.getService()
    private val keyService = KeyService.getService()

    private val keyPair = KeyPairGenerator.getInstance("EC", BouncyCastleProvider()).let {
        it.initialize(ECGenParameterSpec("secp256k1"), SecureRandom())
        it.generateKeyPair()
    }

    @Before
    fun setup() {
        mockkObject(PropertySource)
        every { PropertySource.file(File("walt.yaml"), optional = true) } returns
                PropertySource.file(File("src/test/resources/walt-azure.yaml"))

        mockkConstructor(KeyVaultClient::class)
        every { anyConstructed<KeyVaultClient>().getKey(any(), "106") } returns getKeyBundle(keyPair)
    }

    @After
    fun cleanup() {
        val aliasFile = Path.of("data", "key", "Alias-ForTesting")
        if (Files.exists(aliasFile))
            Files.delete(aliasFile)
    }

    @Test
    fun testSignAndVerify() {
        val keyAlias = KeyId("106")
        val data = "Test data".toByteArray()
        val signature = cryptoService.sign(keyAlias, data)
        assert(cryptoService.verify(keyAlias, signature, data))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSignWithECDSAWithNonExistingKey() {
        val keyAlias = KeyId("ForTesting")
        keyService.addAlias(KeyId("ShouldNotExist"), keyAlias.id)
        cryptoService.signEthTransaction(keyAlias, "Test data".toByteArray())
    }

    @Test
    fun testSignWithECDSA() {
        val keyAlias = KeyId("106")
        val data = "Test data".toByteArray()
        val signature = cryptoService.signEthTransaction(keyAlias, data)
        assert(keyService.getRecoveryId(keyAlias.id, data, signature) != -1)
    }

    private fun getKeyBundle(keyPair: KeyPair): KeyBundle = KeyBundle().withKey(
        JsonWebKey
            .fromEC(keyPair, BouncyCastleProvider())
            .withCrv(JsonWebKeyCurveName("SECP256K1"))
            .withKid("http://domain.com/keys/keyIdentifier")
    )
}
