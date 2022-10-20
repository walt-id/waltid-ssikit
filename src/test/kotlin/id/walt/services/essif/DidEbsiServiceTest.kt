package id.walt.services.essif

import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.crypto.buildKey
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.context.ContextManager
import id.walt.services.ecosystems.essif.didebsi.DidEbsiService
import id.walt.services.hkvstore.HKVKey
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

class DidEbsiServiceTest : AnnotationSpec() {

    companion object {
        private val KEY_ID = KeyId("DidEbsiServiceTest_key")
        private const val DID = "did:ebsi:23R3YwWEc7J1chejmwjh5JDaRjqvvf6ogHnxJNHUvaep4f98"
        private val DID_FILENAME = DID.replace(":", "-") + ".json"
    }

    init {
        println("Running ServiceMatrix")
        // TODO replace with thest config
        //ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
        ServiceMatrix("service-matrix.properties")
        println("Done running the ServiceMatrix")
    }

    private val didEbsiService = DidEbsiService.getService()
    private val keyStore
        get() = ContextManager.keyStore
    private val key = buildKey(
        KEY_ID.id,
        KeyAlgorithm.ECDSA_Secp256k1.name,
        "SUN",
        "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEnCOwfFKyIsS4Tu/BrEUac9XskbPVHY+gyvROBtd/mAFA3XJ8zhmLyLhVCZb1x5r+cE/ukHB9xk2tUrx8Gc2RQA==",
        "MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgQlg6QdheWjyyF1OSE8qjYkBQqS3NTji0t5RY5WFXd4+gBwYFK4EEAAqhRANCAAScI7B8UrIixLhO78GsRRpz1eyRs9Udj6DK9E4G13+YAUDdcnzOGYvIuFUJlvXHmv5wT+6QcH3GTa1SvHwZzZFA"
    )

    @Before
    fun setup() {
        ContextManager.hkvStore.put(
            HKVKey("did", "created", DID),
            Path.of("src", "test", "resources", "ebsi", DID_FILENAME).toFile().readText()
        )
        keyStore.store(key)
        keyStore.addAlias(KEY_ID, DID)
        keyStore.addAlias(KEY_ID, "$DID#key-1")
    }

    @After
    fun clean() {
        ContextManager.hkvStore.delete(HKVKey("did", "created", DID))
        keyStore.delete(KEY_ID.id)
    }

    @Test
    fun testBuildInsertDocumentParams() {
        println("NEEDING DID EBSI 1")
        val params = didEbsiService.buildUnsignedTransactionParams(DID)[0]

        params.from shouldBe "0x7bfA7efe33fD22aaE73bE80eC9901755F55065c2"
        params.identifier shouldBe "0x6469643a656273693a323352335977574563374a316368656a6d776a68354a4461526a71767666366f67486e784a4e48557661657034663938"
        params.hashAlgorithmId shouldBe 1
        // params.hashValue shouldBe "0x1761d3a6bc3eca2d4460e04ec65e5b4fdd490114ba690f0832e0d245ac7a5612" // TODO FIXME
        // params.didVersionInfo shouldBe "0x7b2240636f6e74657874223a5b2268747470733a2f2f77332e6f72672f6e732f6469642f7631225d2c226964223a226469643a656273693a323352335977574563374a316368656a6d776a68354a4461526a71767666366f67486e784a4e48557661657034663938222c22766572696669636174696f6e4d6574686f64223a5b7b226964223a226469643a656273693a323352335977574563374a316368656a6d776a68354a4461526a71767666366f67486e784a4e48557661657034663938236b65792d31222c2274797065223a22536563703235366b31566572696669636174696f6e4b657932303138222c22636f6e74726f6c6c6572223a226469643a656273693a323352335977574563374a316368656a6d776a68354a4461526a71767666366f67486e784a4e48557661657034663938222c227075626c69634b65794a776b223a7b226b6964223a224469644562736953657276696365546573745f6b6579222c226b7479223a224543222c22616c67223a2245533235364b222c22637276223a22736563703235366b31222c22757365223a22736967222c2278223a226e434f7766464b794973533454755f4272455561633958736b62505648592d677976524f4274645f6d4145222c2279223a22514e3179664d345a6938693456516d57396365615f6e42503770427766635a4e72564b3866426e4e6b5541227d7d5d2c2261757468656e7469636174696f6e223a5b226469643a656273693a323352335977574563374a316368656a6d776a68354a4461526a71767666366f67486e784a4e48557661657034663938236b65792d31225d7d"
        params.timestampData shouldBe "0x7b2264617461223a2274657374227d"
        params.didVersionMetadata.length shouldBe 152
    }
}
