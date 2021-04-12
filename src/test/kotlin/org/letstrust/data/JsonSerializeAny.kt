package org.letstrust.services.data//import kotlinx.serialization.*
//import kotlinx.serialization.descriptors.SerialDescriptor
//import kotlinx.serialization.encoding.CompositeDecoder
//import kotlinx.serialization.encoding.Decoder
//import kotlinx.serialization.encoding.Encoder
//import kotlinx.serialization.json.*
//import org.junit.Test
//import kotlin.reflect.KType
//import kotlin.reflect.full.isSubtypeOf
//import kotlin.reflect.full.starProjectedType
//
//// FROM https://gist.github.com/3v1n0/ecbc5e825e2921bd0022611d7046690b
//@Serializable
//data class AnyValueSurrogate(
//    val type : String,
//    @Contextual
//    val value : Any?
//)
//
//@Serializable
//object NoneType
//
//object AnyValueSerializer : KSerializer<Any?> {
//    override val descriptor : SerialDescriptor = AnyValueSurrogate.serializer().descriptor
//
//    override fun serialize(encoder: Encoder, value: Any?) {
//        if (value != null) {
//            val valueClass = value::class
//            val valueType = valueClass.starProjectedType
//            val valueSerializer = serializer(valueType)
//
//            if (encoder is JsonEncoder && isTypePrimitive(valueType)) {
//                encoder.encodeJsonElement(Json.encodeToJsonElement(valueSerializer, value))
//            } else {
//                /* Would be nice to use valueSerializer.descriptor.serialName,
//                 * but how to deserialize that to a type? */
//                val composite = encoder.beginCollection(descriptor, 2)
//                composite.encodeSerializableElement(descriptor, 0, serializer(), valueClass.java.name)
//                composite.encodeSerializableElement(descriptor, 1, valueSerializer, value)
//                composite.endStructure(descriptor)
//            }
//        } else {
//            if (encoder is JsonEncoder) {
//                encoder.encodeJsonElement(JsonNull)
//            } else {
//                val composite = encoder.beginCollection(descriptor, 2)
//                composite.encodeSerializableElement(descriptor, 1, serializer<NoneType?>(), null)
//                composite.endStructure(descriptor)
//            }
//        }
//    }
//
//    private fun isTypePrimitive(type : KType) : Boolean {
//        /* This can be replaced when using experimental API (via @ExperimentalSerializationApi) with:
//         *  valueSerializer.descriptor.kind is PrimitiveKind */
//        if (type.isSubtypeOf(Number::class.starProjectedType))
//            return true
//
//        if (type.isSubtypeOf(String::class.starProjectedType))
//            return true
//
//        if (type.isSubtypeOf(Boolean::class.starProjectedType))
//            return true
//
//        return false
//    }
//
//    private fun getSerializerForTypeName(strType : String) : KSerializer<*> {
//        return try {
//            serializer(Class.forName(strType).kotlin.starProjectedType)
//        } catch (e: ClassNotFoundException) {
//            throw SerializationException(e.message)
//        }
//    }
//
//    override fun deserialize(decoder: Decoder): Any? {
//        if (decoder is JsonDecoder) {
//            val element = decoder.decodeJsonElement()
//            if (element is JsonNull)
//                return null
//
//            if (element is JsonPrimitive) {
//                if (element.isString)
//                    return element.content
//
//                return try {
//                    element.boolean
//                } catch (e: Throwable) {
//                    try {
//                        element.long
//                    } catch (e: Throwable) {
//                        element.double
//                    }
//                }
////                else if (element.content == "true" || element.content == "false")
////                    return element.boolean
////                else if (element.content.contains('.'))
////                    return element.double
////                else
////                    return element.long
//            } else if (element is JsonObject && "type" in element && "value" in element) {
//                element["type"].also { type ->
//                    if (type is JsonPrimitive && type.isString) {
//                        val valueSerializer = getSerializerForTypeName(type.content)
//                        element["value"].also { value ->
//                            if (value is JsonObject)
//                                return Json.decodeFromJsonElement(valueSerializer, value)
//                        }
//                    }
//                }
//            }
//            throw SerializationException("Invalid Json element $element")
//        } else {
//            val composite = decoder.beginStructure(descriptor)
//            var index = composite.decodeElementIndex(descriptor)
//            if (index == CompositeDecoder.DECODE_DONE)
//                return null
//
//            val strType = composite.decodeStringElement(descriptor, index)
//            if (strType.isEmpty())
//                throw SerializationException("Unknown serialization type")
//
//            index = composite.decodeElementIndex(descriptor).also {
//                if (it != index + 1)
//                    throw SerializationException("Unexpected element index!")
//            }
//
//            getSerializerForTypeName(strType).also { serializer ->
//                composite.decodeSerializableElement(descriptor, index, serializer).also {
//                    composite.endStructure(descriptor)
//                    return it
//                }
//            }
//        }
//    }
//}
//
//@Serializable
//data class MySerializableData(
//    val mapValues : Map<String, @Serializable(with = AnyValueSerializer::class) Any?>)
//
//@Serializable
//@ExperimentalSerializationApi
//data class OtherDataClass(
//    val intVal : Int,
//    val strVal : String,
//    @Serializable(with = AnyValueSerializer::class)
//    val anyVal : Any? = null,
//)
//
///*
//This is another option not to use MySerializable data as is, and so not making "mapValues" to be
//the root of the serialized data
//*/
//
//@Serializable(with = AnySerializableValueSerializer::class)
//data class AnySerializableValue(val value : Any?)
//
//object AnySerializableValueSerializer : KSerializer<AnySerializableValue> {
//    override val descriptor: SerialDescriptor = AnyValueSurrogate.serializer().descriptor
//    override fun serialize(encoder: Encoder, value: AnySerializableValue) =
//        AnyValueSerializer.serialize(encoder, value.value)
//    override fun deserialize(decoder: Decoder): AnySerializableValue =
//        AnySerializableValue(AnyValueSerializer.deserialize(decoder))
//}
//
//typealias SerializableMapType = MutableMap<String, AnySerializableValue>
//@Serializable
//abstract class SerializableMap : SerializableMapType
//
//
//object MapStringAnySerializer : KSerializer<MapStringAny> {
//    override val descriptor : SerialDescriptor = SerializableMap.serializer().descriptor
//    override fun serialize(encoder: Encoder, value: MapStringAny) {
//        val entries : SerializableMapType = mutableMapOf()
//        value.entries.forEach { entries[it.key] = AnySerializableValue(it.value) }
//        encoder.encodeSerializableValue(serializer(), entries)
//    }
//    override fun deserialize(decoder: Decoder): MapStringAny {
//        val map = mutableMapOf<String, Any?>()
//        decoder.decodeSerializableValue(serializer<SerializableMapType>()).forEach {
//            map[it.key] = it.value.value
//        }
//        return MapStringAny(map)
//    }
//}
//
//@Serializable(with = MapStringAnySerializer::class)
//data class MapStringAny(val entries : Map<String, Any?> = emptyMap())
//
//class JsonSerializeAny() {
//
//
//    @Test()
//    fun serializeAnyTest() {
//        val map = mapOf<String, Any?>("Fooo" to "bar",
//            "One" to 1,
//            "Two dot zero" to 2.0,
//            "True" to true,
//            "None" to null,
//            "Data" to OtherDataClass(5, "Cinque"))
//        val mapData = MySerializableData(map)
//
//        val encoded = Json.encodeToString(mapData)
//        println(encoded)
//
//        val decoded = Json.decodeFromString<MySerializableData>(encoded)
//        println(decoded)
//
//        // require(mapData == decoded)
//
//        val mapStringAny = MapStringAny(map)
//        val encoded2 = Json.encodeToString(mapStringAny)
//        println(encoded2)
//
//        val decoded2 = Json.decodeFromString<MapStringAny>(encoded2)
//        println(decoded2)
//
//        // require(mapStringAny == decoded2)
//    }
//
//}
//
//
//
////fun List<*>.toJsonElement(): JsonElement {
////    val list: MutableList<JsonElement> = mutableListOf()
////    this.forEach {
////        val value = it as? Any ?: return@forEach
////        when(value) {
////            is Map<*, *> -> list.add((value).toJsonElement())
////            is List<*> -> list.add(value.toJsonElement())
////            else -> list.add(JsonPrimitive(value.toString()))
////        }
////    }
////    return JsonArray(list)
////}
////
////fun Map<*, *>.toJsonElement(): JsonElement {
////    val map: MutableMap<String, JsonElement> = mutableMapOf()
////    this.forEach {
////        val key = it.key as? String ?: return@forEach
////        val value = it.value ?: return@forEach
////        when(value) {
////            is Map<*, *> -> map[key] = (value).toJsonElement()
////            is List<*> -> map[key] = value.toJsonElement()
////            else -> map[key] = JsonPrimitive(value.toString())
////        }
////    }
////    return JsonObject(map)
////}
