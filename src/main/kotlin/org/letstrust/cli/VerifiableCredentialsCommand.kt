package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.letstrust.CliConfig
import org.letstrust.CredentialService
import org.letstrust.model.CredentialSchema
import org.letstrust.model.CredentialStatus
import org.letstrust.model.CredentialSubject
import org.letstrust.model.VerifiableCredential
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Timestamp
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

class VerifiableCredentialsCommand : CliktCommand(
    name = "vc",
    help = """Verifiable Credentials (VCs).

        VC related operations like issuing, verifying and revoking VCs.

        """
) {

    override fun run() {

    }
}

fun readCredOffer(templateName: String) =
    File("templates/${templateName}.json").readText(Charsets.UTF_8)

class IssueVcCommand : CliktCommand(
    name ="issue",
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
    ).default("did:key:z6MkrBJ2W4PLE5J1BtnEaSC7xSbx82whvpQRMcEWFwr2NnE4")

    val cs = CredentialService
    val subjectDid: String by option(
        "--subject-did",
        "-s",
        help = "Specific DID of the VC subject (receiver of VC)"
    ).default("did:key:z6MkrBJ2W4PLE5J1BtnEaSC7xSbx82whvpQRMcEWFwr2NnE4")

    override fun run() {
        echo("Issue & save cred ...")

        // Loading VC template
        val vcTemplateName = "templates/vc-template-ebsi-attestation.json"
        log.debug { "Loading credential template: $vcTemplateName" }
        val vcReq = Json.decodeFromString<VerifiableCredential>(File(vcTemplateName).readText())

        // Populating VC with data
        val vcId =  Timestamp.valueOf(LocalDateTime.now()).time
        vcReq.id = vcId.toString()
        vcReq.issuer = issuerDid
        vcReq.credentialSubject.id = subjectDid
        vcReq.issuanceDate = LocalDateTime.now()

        val vcReqEnc = Json{prettyPrint = true}.encodeToString(vcReq)

        log.debug { "Credential request:\n$vcReqEnc" }

        // Signing VC
        val vcStr = CredentialService.sign(issuerDid, vcReqEnc, CredentialService.SignatureType.Ed25519Signature2018)

        echo("Credential generated:\n$vcStr")

        // Saving VC to file
        Files.createDirectories(Path.of("data/vc"))
        val vcFileName = "data/vc/vc-" + vcId + vcTemplateName.substringAfterLast("vc-template")
        log.debug { "Writing VC to file $vcFileName" }
        File(vcFileName).writeText(vcStr)
        echo("\nSaving credential to $vcFileName")
    }
}

class PresentVcCommand : CliktCommand(
    name ="present",
    help = """Present VC.
        
        """
) {

    val src: File? by argument().file()
    val issuerDid: String by option(
        "--issuer-did",
        "-i",
        help = "Specific DID of the VC subject (receiver of VC)"
    ).default("did:key:z6MkrBJ2W4PLE5J1BtnEaSC7xSbx82whvpQRMcEWFwr2NnE4")

    override fun run() {
        echo("\nCreate VP form file $src ...")

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

        val vcStr = CredentialService.sign(issuerDid, vcOfferEnc, CredentialService.SignatureType.Ed25519Signature2018, domain, nonce)

        println("Credential generated:\n $vcStr")

        val vc = Json { prettyPrint = true }.decodeFromString<VerifiableCredential>(vcStr)

        val vcEnc = Json.encodeToString(vc)

        val vcVerified = CredentialService.verify(issuerDid, vcEnc, CredentialService.SignatureType.Ed25519Signature2018)

        println("Credential verified: $vcVerified")
    }
}

class VerifyVcCommand : CliktCommand(
    name ="verify",
    help = """Verify VC.
        
        """
) {

    val src: File? by argument().file()

    override fun run() {
        echo("\nVerify VC form file $src ...")
    }
}

class ListVcCommand : CliktCommand(
    name ="list",
    help = """List VC.
        
        """
) {

    val src: File? by argument().file()

    override fun run() {
        echo("\nList VCs ...")
    }
}

