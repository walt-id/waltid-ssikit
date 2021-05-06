package org.letstrust.test

import java.io.File

val RESOURCES_PATH: String = "src/test/resources"

fun readCredOffer(fileName: String) =
    File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

fun readVerifiableCredential(fileName: String) =
    File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)
