package org.letstrust.vclib

import org.letstrust.vclib.vcs.Europass
import org.letstrust.vclib.vcs.PermanentResidentCard
import java.io.File

/*
@OptIn(InternalSerializationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
fun main() {
    val residentCard = File("PermanentResidentCard.json").readText()
    println(residentCard)
    println("^^^^ Starting decode...")

    //println(VcLibManager.getCredentialType(residentCard))

    //val residentCardCredential = Json.decodeFromString<PermanentResidentCard>(residentCard)

    //println(residentCardCredential)


    val vcTypes = listOf(PermanentResidentCard::class, Europass::class)

    val it = vcTypes.first()
    println("Got type: ${it.qualifiedName} (is ${it.jvmName})")

    val metadata = it.companionObjectInstance as VCMetadata
    println("Meta: ${metadata.metadataContext} ${metadata.metadataType}")

    val serializer = it.serializer()
    println("Serializer descriptor: " + serializer.descriptor.serialName)

    serializer.descriptor.elementNames.forEach { println("Element: $it") }

    val i = Json { ignoreUnknownKeys = true }.decodeFromString(serializer, residentCard)

    println(i)

}
*/

fun main() {
    val residentCard = File("src/test/resources/verifiable-credentials/PermanentResidentCard.json").readText()
    println(residentCard)

    println("Getting VC:")
    val vc = VcLibManager.getVerifiableCredential(residentCard)

    if (vc is Europass) {
        println("! VC should not be Europass?")
    } else {
        println("VC at least isn't a Europass")
    }

    if (vc is PermanentResidentCard) {
        println("VC is indeed a PermanentResidentCard")
    } else {
        println("VC is not a PermanentResidentCard? => ${vc.javaClass.name}")
    }
}
