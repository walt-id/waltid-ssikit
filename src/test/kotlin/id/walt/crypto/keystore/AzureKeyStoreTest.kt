package id.walt.crypto.keystore

import com.microsoft.azure.keyvault.KeyVaultClient
import com.microsoft.azure.keyvault.models.KeyBundle
import com.microsoft.azure.keyvault.webkey.JsonWebKey
import com.microsoft.azure.keyvault.webkey.JsonWebKeyCurveName
import com.sksamuel.hoplite.PropertySource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.AnnotationSpec
import io.mockk.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import id.walt.model.DidMethod
import id.walt.services.context.WaltContext
import id.walt.services.did.DidService
import id.walt.services.keystore.KeyStoreService
import id.walt.services.keystore.KeyType
import id.walt.test.RESOURCES_PATH
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec

@Ignored // FIXME: mockk does not work with @After
class AzureKeyStoreTest : AnnotationSpec() {

    private lateinit var did: String
    private val keyStore
        get() =  WaltContext.keyStore

    private val keyPair = KeyPairGenerator.getInstance("EC", BouncyCastleProvider()).let {
        it.initialize(ECGenParameterSpec("secp256k1"), SecureRandom())
        it.generateKeyPair()
    }

    @Before
    fun setup() {
        mockkObject(PropertySource)
        every { PropertySource.file(File("walt.yaml"), optional = true) } returns
                PropertySource.file(File("$RESOURCES_PATH/walt-azure.yaml"))

        mockkConstructor(KeyVaultClient::class)
        every { anyConstructed<KeyVaultClient>().getKey(any(), "106") } returns getKeyBundle(keyPair)

        did = DidService.create(DidMethod.ebsi, "106")
    }

    @After
    fun cleanup() {
        Files.delete(Path.of("data", "key", "Alias-${did.replace(":", "-")}"))
        Files.delete(Path.of("data", "key", "Alias-${did.replace(":", "-")}#106"))

        unmockkConstructor(KeyVaultClient::class)
        unmockkObject(PropertySource)
    }

    @Test
    fun testGetKeyId() {
        assert(keyStore.getKeyId(did) == "106")
    }

    @Test
    fun testGetKeyIdThatDoesNotExist() {
        assert(keyStore.getKeyId("IDoNotExist") == null)
    }

    @Test
    fun testLoadWhenKeyDoesNotExist() {
        val alias = "IDoNotExist"
        shouldThrow<IllegalArgumentException> {
            keyStore.load(alias)
        }
    }

    @Test
    fun testLoadWithPrivatePart() {
        shouldThrow<IllegalArgumentException> {
            keyStore.load(
                "106",
                KeyType.PRIVATE
            )
        }
    }

    @Test
    fun testLoad() {
        assert(keyStore.load("106").keyId.id == "106")
    }

    private fun getKeyBundle(keyPair: KeyPair) = KeyBundle().withKey(
        JsonWebKey
            .fromEC(keyPair, BouncyCastleProvider())
            .withCrv(JsonWebKeyCurveName("SECP256K1"))
    )
}
