import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.letstrust.model.DidWeb
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals


class JsonSerializeDidsTest {

    val format = Json { prettyPrint = true; ignoreUnknownKeys = true }


    fun serializeDidWeb(didWebFile: File) {
        val expected = didWebFile.readText()
        // println(expected)
        val obj = Json.decodeFromString<DidWeb>(expected)
        // println(obj)
        val encoded = format.encodeToString(obj)
        // println(encoded)
        assertEquals(expected.replace("\\s".toRegex(), ""), encoded.replace("\\s".toRegex(), ""))
    }

    @Test
    fun constructDidWebTest() {

        val keyRef = listOf("did:web:did.actor:alice#z6MkrmNwty5ajKtFqc1U48oL2MMLjWjartwc5sf2AihZwXDN")

        val pubKey = DidWeb.PublicKey(
            "did:web:did.actor:alice#z6MkrmNwty5ajKtFqc1U48oL2MMLjWjartwc5sf2AihZwXDN",
            "did:web:did.actor:alice",
            "Ed25519VerificationKey2018",
            "DK7uJiq9PnPnj7AmNZqVBFoLuwTjT1hFPrk6LSjZ2JRz"
        )

        val keyAgreement = DidWeb.KeyAgreement(
            "did:web:did.actor:alice#zC8GybikEfyNaausDA4mkT4egP7SNLx2T1d1kujLQbcP6h",
            "X25519KeyAgreementKey2019",
            "Ed25519VerificationKey2018",
            "CaSHXEvLKS6SfN9aBfkVGBpp15jSnaHazqHgLHp8KZ3Y"
        )

        val didWeb = DidWeb("https://w3id.org/did/v0.11", "did:web:did.actor:alice", listOf(pubKey), listOf(keyAgreement), keyRef, keyRef, keyRef, keyRef)

        val encoded = format.encodeToString(didWeb)
        // println(encoded)
        val obj = Json.decodeFromString<DidWeb>(encoded)
        // println(obj)

        assertEquals(didWeb, obj)
    }

    @Test
    fun serializeUniResDidWeb() {
        serializeDidWeb(File("src/test/resources/dids/web/did-web-unires.json"))
    }

    @Test
    fun serializeMattrDidWeb() {
        serializeDidWeb(File("src/test/resources/dids/web/did-web-mattr.json"))
    }

    // @Test
    fun serializeTransumuteDidWeb() {
        serializeDidWeb(File("src/test/resources/dids/web/did-web-transmute.json"))
    }

    // @Test
    fun serializeExample1DidWeb() {
        serializeDidWeb(File("src/test/resources/dids/web/did-web-example1.json"))
    }


    //TODO: NOT WORKING @Test
    fun serializeAllDidWebExamples() {
        File("src/test/resources/dids/web").walkTopDown()
            .filter { it -> it.toString().endsWith(".json") }
            .forEach { it ->
                println("serializing: $it")
                serializeDidWeb(it)
            }


    }
}
