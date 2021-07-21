package org.letstrust.test

import org.letstrust.vclib.VcLibManager
import org.letstrust.vclib.vcs.VC
import java.io.File

val RESOURCES_PATH: String = "src/test/resources"

fun readCredOffer(fileName: String) =
    File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

fun readVerifiableCredential(fileName: String) =
    File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

fun readDid(fileName: String) =
    File("$RESOURCES_PATH/dids/${fileName}.json").readText(Charsets.UTF_8)

fun getTemplate(name: String): VC =
    VcLibManager.getVerifiableCredential(File("templates/vc-template-$name.json").readText(Charsets.UTF_8))
