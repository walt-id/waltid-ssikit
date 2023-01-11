package id.walt.test

import id.walt.credentials.w3c.VerifiableCredential
import java.io.File

val RESOURCES_PATH: String = "src/test/resources"

fun readCredOffer(fileName: String) =
    File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

fun readVerifiableCredential(fileName: String) =
    File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

fun readVerifiablePresentation(fileName: String) =
    File("$RESOURCES_PATH/verifiable-presentations/${fileName}.json").readText(Charsets.UTF_8)

fun readDid(fileName: String) =
    File("$RESOURCES_PATH/dids/${fileName}.json").readText(Charsets.UTF_8)

fun getTemplate(name: String): VerifiableCredential =
    VerifiableCredential.fromString(File("$RESOURCES_PATH/verifiable-credentials/vc-template-default.json").readText(Charsets.UTF_8))
