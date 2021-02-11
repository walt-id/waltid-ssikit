import kotlinx.serialization.*
import kotlinx.serialization.json.*
import model.*
import org.junit.Test
import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.test.assertEquals


// for dynamic enc/decoding see  https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md#json-elements
@Serializable
class Box<T>(val contents: T)

@Serializable
class Project(val name: String, val owner: User)

@Serializable
class Car(val name: String, val owner: User)

@Serializable
class User(val name: String)

@Serializable
data class Data(
    val a: Box<Int>,
    val b: Box<Project>,
    val c: Box<Car>
)


class EbsiServicesTest {

    val format = Json { prettyPrint = true }

    @Test
    fun testJson() {
        val owner = User("kotlin")
        val project = Project("project", owner)
        val car = Car("car", owner)
        val data = Data(Box(42), Box(project), Box(car))

        val string = format.encodeToString(data)
        println(string)
        val obj = Json.decodeFromString<Data>(string)
        println(obj)
    }


    @Test
    fun testCredentialStatusList() {
        val string = File("src/test/resources/ebsi/verifiable-credential-status.json").readText()
        println(string)

        val obj = Json.decodeFromString<List<CredentialStatusListEntry>>(string)
        println(obj)
    }

    @Test
    fun testTrustedIssuerRegistry() {

        var did = listOf<String>("did:ebsi:00003333", "did:ebsi:00005555")

        var organizationInfo =
            OrganizationInfo("https://essif.europa.eu/tsr/53", "Great Company", "Great Company Street 1, Brussels, Belgium", "BE05555555XX", "https://great.company.be")
        val proof = Proof("EidasSeal2019", LocalDateTime.now(), "assertionMethod", VerificationMethod("EidasCertificate2019", "1088321447"), "BD21J4fdlnBvBA+y6D...fnC8Y=")
        val serviceEndpoints = listOf<ServiceEndpoint>(ServiceEndpoint("did:example:123456789abcdefghi#agent", "AgentService", "https://agent.example.com/8377464"))
        val eidasCertificate = EidasCertificate("123456", "123456", "blob")
        var issuer = Issuer("Brand Name", did, eidasCertificate, serviceEndpoints, organizationInfo)
        var accreditationCredentials = listOf<VerifiableCredential>(
            VerifiableCredential(
                listOf(
                    "https://www.w3.org/2018/credentials/v1",
                    "https://essif.europa.eu/schemas/vc/2020/v1"
                ),
                "https://essif.europa.eu/tsr/53",
                listOf<String>("VerifiableCredential", "VerifiableAttestation"),
                "",
                LocalDateTime.now(),
                CredentialSubject("did:ebsi:00001235", listOf("claim1", "claim2")),
                CredentialStatus("https://essif.europa.eu/status/45", "CredentialsStatusList2020"),
                CredentialSchema("https://essif.europa.eu/tsr/education/CSR1224.json", "JsonSchemaValidator2018"),
                proof
            )
        )

        var tir = Tir(issuer, accreditationCredentials)

        //val string = format.encodeToString(tir)
        val string = File("src/test/resources/ebsi/trusted-issuer-registry.json").readText()
        println(string)

        val obj = Json.decodeFromString<Tir>(string)
        println(obj)
    }

    @Test
    fun dateTest() {

        val inDateTime = ZonedDateTime.of(LocalDateTime.now(), ZoneOffset.UTC)

        val inDateEpochSeconds = Instant.ofEpochSecond(inDateTime.toEpochSecond())

        val dateStr = DateTimeFormatter.ISO_INSTANT.format(inDateEpochSeconds)

        println("STRING:  " + dateStr) // 2021-02-11T15:38:00Z

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val outDateTime = LocalDateTime.parse(dateStr, formatter)

        println("DATE TIME:  " + outDateTime) // 2021-02-11T15:41:01

    }
}
