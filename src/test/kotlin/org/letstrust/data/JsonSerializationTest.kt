package org.letstrust.services.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.junit.Test
import kotlin.test.assertEquals

/**
 * HINTS
 * https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md
 * https://stackoverflow.com/questions/58173422/how-to-iterate-a-jsonelement-with-unknown-keys-on-kotlin-using-kotlinx
 * https://github.com/Kotlin/kotlinx.serialization/issues/296
 *
 * TODO: check out https://github.com/filip26/titanium-json-ld
 */

@Serializable(with = ColorAsStringSerializer::class)
data class Color(val rgb: Int, val xyz: String)

object ColorAsStringSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("org.letstrust.services.data.Color", PrimitiveKind.STRING)

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

    interface Stuff {
        val prop: String
    }

    @Serializable
    class StuffImpl(override val prop: String) : Stuff

    object StuffSerializer : JsonContentPolymorphicSerializer<Stuff>(Stuff::class) {
        override fun selectDeserializer(element: JsonElement) = StuffImpl.serializer()
    }

    object StuffListSerializer : KSerializer<List<Stuff>> {
        private val builtIn: KSerializer<List<Stuff>> = ListSerializer(StuffSerializer)

        override fun deserialize(decoder: Decoder): List<Stuff> {
            return builtIn.deserialize(decoder)
        }

        override val descriptor: SerialDescriptor = builtIn.descriptor

        override fun serialize(encoder: Encoder, value: List<Stuff>) {
            builtIn.serialize(encoder, value)
        }

    }

    @Serializable
    class Foo(
        @Serializable(with = StuffListSerializer::class)
        val list: List<Stuff>
    )

    object ActorSerializer : JsonTransformingSerializer<Actor>(Actor.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement =
            if (element !is JsonObject) JsonObject(mapOf("name" to element)) else element

        override fun transformSerialize(element: JsonElement): JsonElement =
            if (element.jsonObject["age"] == null) element.jsonObject["name"]!! else element
    }

    @Serializable
    data class Movie(
        val title: String,
        @Serializable(with = ActorSerializer::class)
        val actor: Actor,
    )

    @Serializable
    data class Actor(
        val name: String,
        val age: Int? = null
    )

    @Test
    fun unkownStructureTest() {
        val json1 = """{"title":"Matrix","actor":{"name":"Keanu","age":56}}"""
        val movie1 = Json.decodeFromString<Movie>(json1)
        println(movie1) // Movie(title=Matrix, actor=Actor(name=Keanu, age=56))
        assertEquals(json1, Json.encodeToString(movie1))

        val json2 = """{"title":"Matrix","actor":"Keanu"}"""
        val movie2 = Json.decodeFromString<Movie>(json2) // FAILING
        println(movie2) // Should be Movie(title=Matrix, actor=Actor(name=Keanu, null))
        assertEquals(json2, Json.encodeToString(movie2))
    }

    @Test
    fun listOfInterfaceImplentationsTest() {
        val json = """{"list":[{"prop":"3"},{"prop":"4"},{"prop":"5"}]}"""
        val data = Foo(listOf(StuffImpl("3"), StuffImpl("4"), StuffImpl("5")))
        assertEquals(json, Json.encodeToString(data))
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
