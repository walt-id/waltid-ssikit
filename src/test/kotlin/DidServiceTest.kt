import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import java.security.Security
import kotlin.test.*
import io.ipfs.multibase.Multibase
import java.io.File
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.annotation.JsonAnySetter

import java.util.LinkedHashMap
import DidServiceTest.Product
import java.util.Collections

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonIgnore


class DidServiceTest {

    protected val RESOURCES_PATH: String = "src/test/resources"

    fun readExampleDid(fileName: String) =
        File("$RESOURCES_PATH/dids/${fileName}.json").readText(Charsets.UTF_8)

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

    @Test
    fun didKeyTest() {
        val ds = DidService
        val didResolved = ds.resolveDidKey("did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH")
        val exampleDid = readExampleDid("did-key-example1").replace("\n", "").replace("\r", "").replace(" ", "");
        println("------------------")
        println(exampleDid)
        assertEquals(exampleDid, didResolved)
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
        fun any():  LinkedHashMap<String, Any> {
            return dynFields
        }
    }

    @Test
    fun jsonDynObjectTest() {
        val mapper = jacksonObjectMapper()
        val json = readExampleDid("product")
        val product: Product = mapper.readValue(json, Product::class.java)
        println(product)
        val serialized = mapper.writeValueAsString(product)
        print(serialized)
    }

    @Test
    fun whenSerializeMap_thenSuccess() {
        val mapper = jacksonObjectMapper()
        val map = mapOf(1 to "one", 2 to "two")

        val serialized = mapper.writeValueAsString(map)

        val json = """{"1":"one","2":"two"}"""
        assertEquals(serialized, json)
    }

    @Test
    fun jsonTest() {
        val mapper = jacksonObjectMapper()

//        val state = Did(
//            "https://w3id.org/did/v1",
//            "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH",
//            listOf(
//                PublicKey("id1", "contr1", "type1", "key1"),
//                PublicKey("id2", "contr2", "type2", "key2")
//            ),
//            null,
//            null,
//            null,
//            null
//        )
        val did = Did(
            "https://w3id.org/did/v1",
            "did:key:z6MkpTHR8VNsBxYAAWHut2Geadd9jSwuBV8xRoAnwWsdvktH"
        )
        val writer = mapper.writer(DefaultPrettyPrinter())
        val json = writer.writeValueAsString(did)
        println(json)
        val out = mapper.readValue<Did>(json)
        println(out)
    }

    @Test
    fun parseDids() {
        val mapper = jacksonObjectMapper()
        var json = readExampleDid("did-test")
        val did = mapper.readValue<Did>(json)
        println(did)
        println(did.context)
        println(did.id)
        println(did.verificationMethod)
        println(did.publicKey?.get(0)?.id)
        println(did.dynFields.get("controller"))
    }

}
