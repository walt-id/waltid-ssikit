package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.letstrust.CliConfig
import org.letstrust.CredentialService
import org.letstrust.DidService
import org.letstrust.model.CredentialSchema
import org.letstrust.model.CredentialStatus
import org.letstrust.model.CredentialSubject
import org.letstrust.model.VerifiableCredential
import java.io.File
import java.time.LocalDateTime

class VerifiableCredentialsCommand : CliktCommand(
    name = "vc",
    help = """Verifiable Credentials (VCs).

        VC related operations like issuing, verifying and revoking VCs

        """
) {

    override fun run() {

        echo("Do some VC magic here ..\n")

    }
}

fun readCredOffer(templateName: String) =
    File("templates/${templateName}.json").readText(Charsets.UTF_8)

class IssueCommand : CliktCommand(
    help = """Issues and distributes VC.
        
        """
) {
    val config: CliConfig by requireObject()
    val template: File? by argument().file().optional()
    val dest: File? by argument().file().optional()
    val issuerDid: String by option(
        "--issuer-did",
        "-i",
        help = "Specific DID of the VC subject (receiver of VC)"
    ).prompt()

    val cs = CredentialService
    val subjectDid: String by option(
        "--subject-did",
        "-s",
        help = "Specific DID of the VC subject (receiver of VC)"
    ).prompt()

    override fun run() {
        echo("\nIssue & send cred ...")

        // val credOffer = readCredOffer("WorkHistory")


        val issuerDid = DidService.createDid("key")
        val domain = "example.com"
        val nonce: String? = null

        val vcOffer = VerifiableCredential(
            listOf(
                "https://www.w3.org/2018/credentials/v1"
            ),
            "https://essif.europa.eu/tsr/53",
            listOf("VerifiableCredential", "VerifiableAttestation"),
            "did:ebsi:000098765",
            LocalDateTime.now().withNano(0),
            CredentialSubject("did:ebsi:00001235", null, listOf("claim1", "claim2")),
            CredentialStatus("https://essif.europa.eu/status/45", "CredentialsStatusList2020"),
            CredentialSchema("https://essif.europa.eu/tsr/education/CSR1224.json", "JsonSchemaValidator2018")
        )


        val vcOfferEnc = Json.encodeToString(vcOffer)

        val vcStr = CredentialService.sign(issuerDid, domain, nonce, vcOfferEnc, CredentialService.SignatureType.Ed25519Signature2018)

        println("Credential generated:\n $vcStr")

        val vc = Json { prettyPrint = true }.decodeFromString<VerifiableCredential>(vcStr)

        val vcEnc = Json.encodeToString(vc)

        val vcVerified = CredentialService.verify(issuerDid, vcEnc, CredentialService.SignatureType.Ed25519Signature2018)

        println("Credential verified: $vcVerified")

    }
}

class VerifyCommand : CliktCommand(
    help = """Verifies VC.
        
        """
) {

    val src: File? by argument().file()

    override fun run() {
        echo("\nVerify VC form file $src ...")
    }
}
