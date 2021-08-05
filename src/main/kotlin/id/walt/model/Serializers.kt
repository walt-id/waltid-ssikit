package id.walt.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/*
object DateAsTimestampSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}*/


@Serializer(forClass = LocalDateTime::class)
object DateAsIso8601UtcStringSerializer : KSerializer<LocalDateTime> {
    override fun serialize(output: Encoder, dateTime: LocalDateTime) {
        val inDateEpochSeconds = Instant.ofEpochSecond(ZonedDateTime.of(dateTime, ZoneOffset.UTC).toEpochSecond())
        output.encodeString(DateTimeFormatter.ISO_INSTANT.format(inDateEpochSeconds))
    }

    override fun deserialize(input: Decoder): LocalDateTime {
        return LocalDateTime.parse(input.decodeString(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
    }
}

/*
@kotlinx.serialization.ExperimentalSerializationApi
@Serializer(forClass = String::class)
object ProofTypeSerializer :
    JsonTransformingSerializer<String>(String.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonArray) element[0] else element

    override fun transformSerialize(element: JsonElement): JsonElement = element
}

*/
