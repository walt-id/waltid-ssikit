package id.walt.deprecated

import com.beust.klaxon.Klaxon
import id.walt.servicematrix.ServiceMatrix
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import id.walt.model.Did
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.services.did.DidService
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@Deprecated(message = "New version in package id.walt.service.did")
class DidServiceTest : AnnotationSpec() {

    private val RESOURCES_PATH: String = "src/test/resources"

    init {
        ServiceMatrix("service-matrix.properties")
    }

    fun readExampleDid(fileName: String) =
        File("$RESOURCES_PATH/dids/${fileName}.json").readText(Charsets.UTF_8)

//    @Before
//    fun setup() {
//        Security.addProvider(BouncyCastleProvider())
//    }

    @Test
    fun parseDidUrlTest() {

        val did = Did("context")

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
    fun createResolveDidWebTest() {
        val ds = DidService
        val did = ds.create(DidMethod.web)
        did shouldNotBe null
        (30 < did.length) shouldBe true
        "did:web:" shouldBe did.substring(0, 8)
        print(did)

        val didWeb = ds.resolve(did)
        val encoded = Klaxon().toJsonString(didWeb)
        println(encoded)
    }

    @Test
    fun listDidTest() {
        Files.createDirectories(Path.of("data/did/created"))
        val ds = DidService
        val dids = ds.listDids()
        println(dids)
    }

//    @Test
//    fun didWebResolution() {
//        val identifier = "did:web:mattr.global"
//        val didUrl:  DidUrl= DidUrl.from(identifier)
//        val didWeb = DidService.resolveDidWeb(didUrl)
//        didWeb shouldNotBe null
//        // "https://w3id.org/did/v1" shouldBe didWeb.context
//        identifier shouldBe didWeb.id
//    }

//    // https://stackoverflow.com/questions/57178093/how-to-deserialize-json-with-dynamic-object
//    // https://stackoverflow.com/questions/12134231/jackson-dynamic-property-names
//    internal class Product() {
//        @JsonIgnore
//        var dynFields: LinkedHashMap<String, Any> = LinkedHashMap<String, Any>()
//
//        @JsonAnySetter
//        fun setDetail(key: String, value: Any) {
//            dynFields[key] = value
//        }
//
//        @JsonAnyGetter
//        fun any(): LinkedHashMap<String, Any> {
//            return dynFields
//        }
//    }

//    @Test
//    fun jsonDynObjectTest() {
//        val mapper = jacksonObjectMapper()
//        val json = readExampleDid("did-example2")
//        val product: Product = mapper.readValue(json, Product::class.java)
//        println(product)
//        val serialized = mapper.writeValueAsString(product)
//        print(serialized)
//    }
//
//    @Test
//    fun whenSerializeMap_thenSuccess() {
//        val mapper = jacksonObjectMapper()
//        val map = mapOf(1 to "one", 2 to "two")
//
//        val serialized = mapper.writeValueAsString(map)
//
//        val json = """{"1":"one","2":"two"}"""
//        serialized shouldBe json
//    }


//    JAXON TEST
//    @JsonInclude(JsonInclude.Include.NON_EMPTY)
//    data class Service(
//        val id: String,
//        val type: String,
//        val serviceEndpoint: String
//    )
//
//    @JsonInclude(JsonInclude.Include.NON_EMPTY)
//    data class PublicKey(
//        val id: String,
//        val controller: String,
//        val type: String,
//        val publicKeyBase58: String
//    )
//
//    @JsonInclude(JsonInclude.Include.NON_EMPTY)
//    data class did(
//        @JsonProperty("@context") val context: String,
//        val id: String,
////    val controller: List<PublicKey>,
//        val publicKey: List<PublicKey>? = null,
//        val verificationMethod: List<Map<String, Any>>? = null,
////    val authentication: List<String>?,
////    val assertionMethod: List<String>?,
////    val capabilityDelegation: List<String>?
//    ) {
//
//        @JsonIgnore
//        var dynFields: LinkedHashMap<String, Any> = LinkedHashMap<String, Any>()
//
//        @JsonAnySetter
//        fun setField(key: String, value: Any) {
//            dynFields[key] = value
//        }
//
//        @JsonAnyGetter
//        fun any():  LinkedHashMap<String, Any> {
//            return dynFields
//        }
//    }
//
//    @Test
//    fun jsonTest() {
//        val mapper = jacksonObjectMapper()
//
////        val state = did(
////            "https://w3id.org/did/v1",
////            "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH",
////            listOf(
////                PublicKey("id1", "contr1", "type1", "key1"),
////                PublicKey("id2", "contr2", "type2", "key2")
////            ),
////            null,
////            null,
////            null,
////            null
////        )
//        val did = did(
//            "https://w3id.org/did/v1",
//            "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH"
//        )
//        val writer = mapper.writer(DefaultPrettyPrinter())
//        val json = writer.writeValueAsString(did)
//        println(json)
//        val out = mapper.readValue<did>(json)
//        println(out)
//    }
//
//    @Test
//    fun parseDids() {
//        val mapper = jacksonObjectMapper()
//        var json = readExampleDid("did-test")
//        val did = mapper.readValue<did>(json)
//        println(did)
//        println(did.context)
//        println(did.id)
//        println(did.verificationMethod)
//        println(did.publicKey?.get(0)?.id)
//        println(did.dynFields.get("controller"))
//    }

}
