import com.fasterxml.jackson.databind.ser.std.DateSerializer
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.junit.Test
import java.io.File
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

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

    object DateAsTimestampSerializer : KSerializer<Date> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
        override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
        override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
    }

    @Serializer(forClass = LocalDateTime::class)
    object DateAsIso8601UtcStringSerializer : KSerializer<LocalDateTime> {
        override fun serialize(output: Encoder, dateTime: LocalDateTime) {
            val timestampAsDateString = java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.ofEpochSecond(Timestamp.valueOf(dateTime).time))
            output.encodeString(timestampAsDateString)
        }

        override fun deserialize(input: Decoder): LocalDateTime {
            val secondApiFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            return LocalDateTime.parse(input.decodeString(), secondApiFormat)
        }
    }

    @Serializable
    data class Tir(val issuer: Issuer, val accreditationCredentials: List<VerifiableCredential>)

    @Serializable
    data class Issuer(
        val preferredName: String,
        val did: List<String>,
        val eidasCertificatePem: String,
        val serviceEndpoints: List<String>,
        val organizationInfo: OrganizationInfo
    )

    @Serializable
    data class OrganizationInfo(
        val id: String,
        var legalName: String,
        var currentAddress: String,
        val vatNumber: String,
        val domainName: String
    )

    @Serializable
    data class VerifiableCredential(

        @SerialName("@context") val context: List<String>,
        val id: String,
        val type: List<String>,
        val issuer: String,
        @Serializable(with = DateAsIso8601UtcStringSerializer::class) var issuanceDate: LocalDateTime,
        var credentialSubject: CredentialSubject,
        var credentialStatus: CredentialStatus,
        var credentialSchema: CredentialSchema,
        var proof: Proof
    )

    @Serializable
    data class CredentialSubject(
        val did: String,
        val authorizationClaims: List<String>
    )

    @Serializable
    data class CredentialStatus(
        val id: String,
        val type: String
    )

    @Serializable
    data class CredentialSchema(
        val id: String,
        val type: String
    )

    @Serializable
    data class Proof(
        val type: String,
        @Serializable(with = DateAsIso8601UtcStringSerializer::class) val created: LocalDateTime,
        val proofPurpose: String,
        val verificationMethod: VerificationMethod,
        val proofValue: String
    )

    @Serializable
    data class VerificationMethod(
        val type: String,
        val CertSerial: String
    )

    @Test
    fun testTrustedIssuerRegistry() {

        var did = listOf<String>("did:ebsi:00003333", "did:ebsi:00005555")
        var serviceEndpoints = listOf<String>("https: //example.one.com", "https: //example.two.com")
        var organizationInfo =
            OrganizationInfo("https://essif.europa.eu/tsr/53", "Great Company", "Great Company Street 1, Brussels, Belgium", "BE05555555XX", "https://great.company.be")
        val proof = Proof("EidasSeal2019", LocalDateTime.now(), "assertionMethod", VerificationMethod("EidasCertificate2019", "1088321447"), "BD21J4fdlnBvBA+y6D...fnC8Y=")
        var issuer = Issuer("Brand Name", did, "<PEM-ENC-CERT>", serviceEndpoints, organizationInfo)
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

        val string = format.encodeToString(tir)
        //val string = File("src/test/resources/ebsi/trusted-issuer-registry.json").readText()
        println(string)

        val obj = Json.decodeFromString<Tir>(string)
        println(obj)
    }

    @Test
    fun dateTest() {
        val secondApiFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val timestamp = 1565209665.toLong() // timestamp in Long


        val timestampAsDateString = java.time.format.DateTimeFormatter.ISO_INSTANT
            .format(java.time.Instant.ofEpochSecond(timestamp))

        println("parseTesting " + timestampAsDateString) // prints 2019-08-07T20:27:45Z


        val date = LocalDate.parse(timestampAsDateString, secondApiFormat)

        println(date)
    }
}
