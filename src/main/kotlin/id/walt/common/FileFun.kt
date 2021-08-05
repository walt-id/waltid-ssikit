package id.walt.common

import id.walt.services.essif.EssifFlowRunner
import java.io.File

fun readEssif(fileName: String) = ClassLoader.getSystemResource("essif/${fileName}.json").readText(Charsets.UTF_8)

fun readWhenContent(file: File, errorMessage: String? = null) = when {
    (file.length() > 0) -> file.readText()
    else -> throw Exception(errorMessage ?: "Expecting file with content at: ${file.absolutePath}")
}

fun readEssifBearerToken(): String = readWhenContent(
    EssifFlowRunner.bearerTokenFile,
    "The bearer token must be placed in file ${EssifFlowRunner.bearerTokenFile.absolutePath}. Visit https://app.preprod.ebsi.eu/users-onboarding for requesting a token."
).replace("\n", "")
