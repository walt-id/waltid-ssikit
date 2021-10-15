package id.walt.services.did

import com.beust.klaxon.Klaxon
import id.walt.servicematrix.ServiceMatrix
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import id.walt.common.prettyPrint
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import java.io.File

class DidServiceTest : AnnotationSpec() {

    @Before
    fun setup() {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    private val keyService = KeyService.getService()

    fun readExampleDid(fileName: String) =
        File("$RESOURCES_PATH/dids/${fileName}.json").readText(Charsets.UTF_8)

    val ds = DidService

    @Test
    fun parseDidUrlTest() {

        val didUrl = DidUrl("method", "identifier", "key1")
        "did:method:identifier#key1" shouldBe didUrl.url

        val obj: DidUrl = DidUrl.from(didUrl.url)
        didUrl shouldBe obj
    }

    @Test
    fun createResolveDidKeyTest() {

        // Create
        val did = ds.create(DidMethod.key)
        val didUrl = DidUrl.from(did)
        did shouldBe didUrl.did
        "key" shouldBe didUrl.method
        print(did)

        // Resolve
        val resolvedDid = ds.resolve(did)
        val encoded = Klaxon().toJsonString(resolvedDid)
        println(encoded)
    }

    @Test
    fun createResolveDidWebTest() {

        // Create
        val did = ds.create(DidMethod.web)
        val didUrl = DidUrl.from(did)
        did shouldBe didUrl.did
        "web" shouldBe didUrl.method
        print(did)

        // Resolve
        val resolvedDid = ds.resolve(did)
        val encoded = Klaxon().toJsonString(resolvedDid)
        println(encoded)
    }

    @Test
    fun createDidEbsiV2Identifier() {
        val didUrl = DidUrl.generateDidEbsiV2DidUrl()
        val did = didUrl.did
        "did:ebsi:" shouldBe did.substring(0, 9)
        17 shouldBe didUrl.identifier.length
    }

    @Test
    fun createDidEbsiTest() {

        // Create
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val did = ds.create(DidMethod.ebsi, keyId.id)
        println(did)
        val didUrl = DidUrl.from(did)
        did shouldBe didUrl.did
        "ebsi" shouldBe didUrl.method
        print(did)

        // Load
        val resolvedDid = ds.loadDidEbsi(did)
        val encoded = Klaxon().toJsonString(resolvedDid)
        println(encoded)

        // Update
        resolvedDid.assertionMethod = listOf(resolvedDid.verificationMethod!!.get(0).id)
        ds.updateDidEbsi(resolvedDid)
        val encodedUpd = Klaxon().toJsonString(resolvedDid)
        println(encodedUpd)
    }

    @Test
    @Ignore // TODO: ESSIF backend issue
    fun resolveDidEbsiTest() {
        val did = "did:ebsi:22S7TBCJxzPS2Vv1UniBSdzFD2ZDFjZeYvQuFQWSeAQN5nTG"
        val didDoc = DidService.resolveDidEbsi(did)
        val encDidEbsi = Klaxon().toJsonString(didDoc)
        println(encDidEbsi)
    }

    @Test
    @Ignore // TODO: ESSIF backend issue
    fun resolveDidEbsiRawTest() {
        val did = "did:ebsi:22S7TBCJxzPS2Vv1UniBSdzFD2ZDFjZeYvQuFQWSeAQN5nTG"
        val didDoc = DidService.resolveDidEbsiRaw(did)
        println(didDoc.prettyPrint())
    }

    @Test
    fun listDidsTest() {

        ds.create(DidMethod.key)

        val dids = ds.listDids()

        dids.isNotEmpty() shouldBe true

        dids.forEach { s -> s shouldBe DidUrl.from(s).did }
    }

}
