import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.junit.Test
import java.util.*


@Serializable
data class Project(
    val name: String,
    @Serializable(with = NestedObjectOrStringSerializer::class)
    val users: User
)

@Serializable
data class User(val name: String, val details: JsonObject)


object NestedObjectOrStringSerializer : KSerializer<User> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("User") {
        element<String>("name")
        element<JsonElement>("details")
    }

    override fun deserialize(decoder: Decoder): User {
        val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        val jsonElement = jsonInput.decodeJsonElement()
        println(jsonElement)
        var name = ""
        var details: MutableMap<String, JsonElement> = LinkedHashMap<String, JsonElement>()
        if (jsonElement is JsonObject) {
            val json = jsonElement.jsonObject
            name = json.getValue("name").jsonPrimitive.content
            details = json.toMutableMap()
            details.remove("name")
        } else {
            name = jsonElement.jsonPrimitive.content
        }

        return User(name, JsonObject(details))
    }

    override fun serialize(encoder: Encoder, value: User) {
        error("Serialization is not supported")
    }
}

@Serializable(with = ColorAsStringSerializer::class)
data class Color(val rgb: Int, val xyz: String)

object ColorAsStringSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        val string = value.rgb.toString(16).padStart(6, '0')
        encoder.encodeString(string + value.xyz)
    }

    override fun deserialize(decoder: Decoder): Color {
        val string = decoder.decodeString()
        return Color(string.toInt(16), "test")
    }
}

class JsonSerializationTest {

    val format = Json { prettyPrint = true }

    @Test
    fun unkownStructureTest() {

        println(
            Json.decodeFromString<Project>(
                """
        {"name":"kotlinx.serialization","users":{"name":"kotlin", "age":"21"}}
    """
            )
        )
        println(
            Json.decodeFromString<Project>(
                """
        {"name":"kotlinx.serialization","users":"kotlin"}
    """
            )
        )
    }

    @Test
    fun customSerializerTest() {

        val green = Color(0x00ff00, "asdf")
        val colorSerializer: KSerializer<Color> = Color.serializer()
        println(colorSerializer.descriptor)
        val intSerializer: KSerializer<Int> = Int.serializer()
        println(intSerializer.descriptor)
        val stringListSerializer: KSerializer<List<String>> = ListSerializer(String.serializer())
        println(stringListSerializer.descriptor)

        println(format.encodeToString(green))
        val color = Json.decodeFromString<Color>("\"00ff00\"")
        println(color)

    }
}
