package id.walt.signatory

import com.beust.klaxon.Klaxon
import id.walt.servicematrix.ServiceMatrix
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.vclist.VerifiableAttestation
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.File

class SignatoryApiTest : AnnotationSpec() {

    init {
        ServiceMatrix("service-matrix.properties")
    }

    val SIGNATORY_API_PORT = 7003
    val SIGNATORY_API_URL = "http://localhost:$SIGNATORY_API_PORT"

    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        expectSuccess = false
    }

    @BeforeClass
    fun startServer() {
        SignatoryRestAPI.start(SIGNATORY_API_PORT)
    }

    @AfterClass
    fun teardown() {
        SignatoryRestAPI.stop()
    }

    // @Test
    fun testHealth() = runBlocking {
        val response: HttpResponse = client.get("$SIGNATORY_API_URL/health")
        "OK" shouldBe response.readText()
    }

    //TODO fix @Test
    fun testListVcTemplates() = runBlocking {

        val templates = client.get<List<String>>("$SIGNATORY_API_URL/v1/vc/templates") {
            contentType(ContentType.Application.Json)
        }
        var foundDefaultTemplate = false
        templates.forEach { if (it == "default") foundDefaultTemplate = true }
        foundDefaultTemplate shouldBe true
    }

    // @Test
    fun testGetVcDefaultTemplate() = runBlocking {

        val defaultTemplate = client.get<String>("$SIGNATORY_API_URL/v1/vc/templates/default") {
            contentType(ContentType.Application.Json)
        }
        val input = File("templates/vc-template-default.json").readText().replace("\\s".toRegex(), "")

        val vc = input.toCredential()
        val enc = Klaxon().toJsonString(vc as VerifiableAttestation)
        input shouldEqualJson enc

    }

}
