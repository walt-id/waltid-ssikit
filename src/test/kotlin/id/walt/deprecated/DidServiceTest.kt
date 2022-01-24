package id.walt.deprecated

import com.beust.klaxon.Klaxon
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@Deprecated(message = "New version in package id.walt.service.did")
class DidServiceTest : AnnotationSpec() {

    private val RESOURCES_PATH: String = "src/test/resources"

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    fun readExampleDid(fileName: String) =
        File("$RESOURCES_PATH/dids/${fileName}.json").readText(Charsets.UTF_8)

    @Test
    fun parseDidUrlTest() {

        val didUrl = DidUrl("method", "identifier", "key1")

        "did:method:identifier#key1" shouldBe didUrl.url

        val obj: DidUrl = DidUrl.from(didUrl.url)

        didUrl shouldBe obj
    }

    @Test
    fun createResolveDidKeyTest() {
        val ds = DidService
        val did = ds.create(DidMethod.key)
        did shouldNotBe null
        (32 < did.length) shouldBe true
        "did:key:" shouldBe did.substring(0, 8)
        print(did)
        val didKey = ds.resolve("did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH")
        val encoded = Klaxon().toJsonString(didKey)
        println(encoded)
    }

    @Test
    fun createDidWebTest() {

        val did = DidService.create(DidMethod.web, null, DidService.DidWebOptions("example.com", "asdf"))
        did shouldBe "did:web:example.com:asdf"
        print(did)
    }

    @Test
    fun listDidTest() {
        Files.createDirectories(Path.of("data/did/created"))
        val ds = DidService
        val dids = ds.listDids()
        println(dids)
    }

}
