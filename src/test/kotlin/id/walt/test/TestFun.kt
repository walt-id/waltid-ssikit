package id.walt.test

import id.walt.vclib.VcLibManager
import id.walt.vclib.model.VerifiableCredential
import java.io.File

val RESOURCES_PATH: String = "src/test/resources"

fun readCredOffer(fileName: String) =
    File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

fun readVerifiableCredential(fileName: String) =
    File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

fun readDid(fileName: String) =
    File("$RESOURCES_PATH/dids/${fileName}.json").readText(Charsets.UTF_8)

fun getTemplate(name: String): VerifiableCredential =
    VcLibManager.getVerifiableCredential(File("templates/vc-template-$name.json").readText(Charsets.UTF_8))
