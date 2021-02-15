import com.fasterxml.jackson.annotation.*
import io.ipfs.multibase.Base58
import io.ipfs.multibase.Multibase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.DidEbsi
import model.DidUrl
import model.fromString
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.Security
import java.util.*
import kotlin.test.*


class DidServiceTest {

    protected val RESOURCES_PATH: String = "src/test/resources"

    fun readExampleDid(fileName: String) =
        File("$RESOURCES_PATH/dids/${fileName}.json").readText(Charsets.UTF_8)

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun parseDidUrlTest() {

        val did = DidEbsi("context")

        val didUrl = DidUrl("method", "identifier", "key1")

        assertEquals("did:method:identifier#key1", didUrl.url)

        val obj: DidUrl = didUrl.url.fromString()

        assertEquals(didUrl, obj)
    }


    @Test
    fun creDidWebTest() {
        val format = Json { prettyPrint = true }
        val didWeb = DidService.createDidWeb()
        val encoded = format.encodeToString(didWeb)
        println("\n\n${didWeb.id}\n" + encoded)
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

    @Test
    fun didKeyTest() {
//        val ds = DidService
//        val didResolved = ds.resolveDidKey("did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH")
//        val exampleDid = readExampleDid("did-key-example1").replace("\\s+".toRegex(), "")
//        println("------------------")
//        println(exampleDid)
//        assertEquals(exampleDid, didResolved)
    }


    // https://stackoverflow.com/questions/57178093/how-to-deserialize-json-with-dynamic-object
    // https://stackoverflow.com/questions/12134231/jackson-dynamic-property-names
    internal class Product() {
        @JsonIgnore
        var dynFields: LinkedHashMap<String, Any> = LinkedHashMap<String, Any>()

        @JsonAnySetter
        fun setDetail(key: String, value: Any) {
            dynFields[key] = value
        }

        @JsonAnyGetter
        fun any(): LinkedHashMap<String, Any> {
            return dynFields
        }
    }

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
//        assertEquals(serialized, json)
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
//    data class Did(
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
////        val state = Did(
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
//        val did = Did(
//            "https://w3id.org/did/v1",
//            "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH"
//        )
//        val writer = mapper.writer(DefaultPrettyPrinter())
//        val json = writer.writeValueAsString(did)
//        println(json)
//        val out = mapper.readValue<Did>(json)
//        println(out)
//    }
//
//    @Test
//    fun parseDids() {
//        val mapper = jacksonObjectMapper()
//        var json = readExampleDid("did-test")
//        val did = mapper.readValue<Did>(json)
//        println(did)
//        println(did.context)
//        println(did.id)
//        println(did.verificationMethod)
//        println(did.publicKey?.get(0)?.id)
//        println(did.dynFields.get("controller"))
//    }

}
