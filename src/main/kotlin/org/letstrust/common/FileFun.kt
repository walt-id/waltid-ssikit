package org.letstrust.common

import org.letstrust.services.essif.EssifFlowRunner
import java.io.File

fun readEssif(fileName: String) = ClassLoader.getSystemResource("essif/${fileName}.json").readText(Charsets.UTF_8)

fun readWhenContent(file: File, errorMessage: String? = null) = when {
    (file.length() > 0) -> EssifFlowRunner.verifiableAuthorizationFile.readText()
    else -> throw Exception(errorMessage ?: "Expecting file with content at: ${EssifFlowRunner.verifiableAuthorizationFile.absolutePath}")
}

fun readEssifBearerToken(): String = readWhenContent(
    EssifFlowRunner.bearerTokenFile,
    "The bearer token must be placed in file ${EssifFlowRunner.bearerTokenFile.absolutePath}. Visit https://app.preprod.ebsi.eu/users-onboarding for requesting a token."
).replace("\n", "")
