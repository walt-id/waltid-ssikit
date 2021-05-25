package org.letstrust.vclib

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.letstrust.vclib.vcs.*
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.jvm.jvmName

object VcLibManager {

    @OptIn(InternalSerializationApi::class)
    fun getVerifiableCredential(json: String): VC {
        val vcTypeClass = getCredentialType(json)

        println("Got type: ${vcTypeClass.qualifiedName} (is ${vcTypeClass.jvmName})")

        val metadata = vcTypeClass.companionObjectInstance as VCMetadata
        println("Meta: ${metadata.metadataContext} ${metadata.metadataType}")

        val serializer = vcTypeClass.serializer()

        // println("Serializer descriptor: " + serializer.descriptor.serialName)
        // serializer.descriptor.elementNames.forEach { println("Element: $it") }

        val decodedVC = Json /*{ ignoreUnknownKeys = true }*/.decodeFromString(serializer, json)

        return decodedVC
    }


    fun getCredentialType(json: String): KClass<out VC> {
        val minVc = Json { ignoreUnknownKeys = true }.decodeFromString<MinVC>(json)

        val contexts = minVc.context
        val types = minVc.type

        return getCredentialType(contexts, types)
    }

    private val defaultContexts = listOf("https://www.w3.org/2018/credentials/v1")
    private val defaultTypes = listOf("VerifiableCredential")

    @OptIn(InternalSerializationApi::class)
    fun getCredentialType(contexts: List<String>, types: List<String>): KClass<out VC> {
        val vcTypes = listOf(PermanentResidentCard::class, Europass::class, EbsiVerifiableAttestation::class)

        val searchedContexts = contexts.minus(defaultContexts)
        val searchedTypes = types.minus(defaultTypes)

        if (searchedContexts.isEmpty() && searchedTypes.isEmpty())
            throw IllegalArgumentException("Too broadly specified credential")

        vcTypes.forEach {
            val metadata = it.companionObjectInstance as VCMetadata

            if (metadata.metadataContext in searchedContexts || metadata.metadataType in searchedTypes) {
                return it
            }
        }

        throw IllegalArgumentException("No mapping found")
    }
}
