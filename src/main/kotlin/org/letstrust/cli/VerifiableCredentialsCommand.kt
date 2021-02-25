package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.letstrust.CliConfig
import org.letstrust.CredentialService
import org.letstrust.DidService
import org.letstrust.model.VerifiableCredential
import org.letstrust.model.VerifiablePresentation
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
    name = "issue",
    help = """Issues and save VC.
        
        """
) {
    val config: CliConfig by requireObject()
    val dest: File? by argument().file().optional()
    val template: String by option("-t", "--template", help = "VC template [templates/vc-template-default.json]").default("templates/vc-template-default.json")
    val issuerDid: String? by option("-i", "--issuer-did", help = "DID of the issuer (associated with signing key)")
    val subjectDid: String? by option("-s", "--subject-did", help = "DID of the VC subject (receiver of VC)")

    override fun run() {
        echo("Issuing & saving cred ...")

        // Loading VC template
        log.debug { "Loading credential template: $template" }
        if (!template.contains("vc-template")) {
            log.error { "Template-file name must start with \"vc-template\"" }
            return
        }
        val vcReq = Json.decodeFromString<VerifiableCredential>(File(template).readText())

        // Populating VC with data
        val vcId = Timestamp.valueOf(LocalDateTime.now()).time
        vcReq.id = vcId.toString()
        issuerDid?.let { vcReq.issuer = it }
        subjectDid?.let { vcReq.credentialSubject.id = it }

        // TODO: we could create a did:key for issuing a self-signed VC at this point

        vcReq.issuanceDate = LocalDateTime.now()

        val vcReqEnc = Json { prettyPrint = true }.encodeToString(vcReq)

        log.debug { "Credential request:\n$vcReqEnc" }

        // Signing VC
        val vcStr = CredentialService.sign(vcReq.issuer, vcReqEnc, CredentialService.SignatureType.Ed25519Signature2018)

        echo("Credential generated:\n$vcStr")

        // Saving VC to file
        Files.createDirectories(Path.of("data/vc"))
        val vcFileName = "data/vc/vc-" + vcId + template.substringAfterLast("vc-template")

        log.debug { "Writing VC to file $vcFileName" }
        File(vcFileName).writeText(vcStr)
        echo("\nSaving credential to credential store $vcFileName")

        dest?.run {
            log.debug { "Writing VC to DEST file $dest" }
            dest!!.writeText(vcStr)
            echo("\nSaving credential to DEST file: $dest")
        }
    }
}

class PresentVcCommand : CliktCommand(
    name = "present",
    help = """Present VC.
        
        """
) {

    val src: File by argument().file()
    val domain: String? by option("-d", "--domain", help = "Domain name to be used in the proof")
    val challenge: String? by option("-c", "--challenge", help = "Challenge to be used in the proof")
    // val holderDid: String? by option("-i", "--holder-did", help = "DID of the holder (owner of the VC)")

    override fun run() {
        echo("Create VP form file $src ...")

        if (!src.exists()) {
            log.error("Could not load VC $src")
            throw Exception("Could not load VC $src")
        }

        // Creating the Verifiable Presentation
        val vp = CredentialService.present(src.readText(), domain, challenge)

        log.debug { "Presentation created (ld-signature):\n$vp" }

        // FIX: This is required to filter out "type" : [ "Ed25519Signature2018" ] in the proof, which is s bug from signature.ld
        val vpStr = Json.decodeFromString<VerifiablePresentation>(vp).let { Json { prettyPrint = true }.encodeToString(it) }

        echo("Presentation created:\n$vpStr")

        // Storing VP
        Files.createDirectories(Path.of("data/vp"))
        val vpFileName = "data/vp/vp-${Timestamp.valueOf(LocalDateTime.now()).time}.json"
        log.debug { "Writing VP to file $vpFileName" }
        File(vpFileName).writeText(vpStr)
        echo("\nSaving presentation to: $vpFileName")
    }
}

class VerifyVcCommand : CliktCommand(
    name = "verify",
    help = """Verify VC.
        
        """
) {

    val src: File by argument().file()
    val isPresentation: Boolean by option("-p", "--is-presentation", help = "In case a VP is verified.").flag()

    override fun run() {
        echo("Verify VC form file $src ...\n")

        if (!src.exists()) {
            log.error("Could not load file $src")
            throw Exception("Could not load file $src")
        }

        if (isPresentation) {
            if (CredentialService.verifyVp(src.readText())) {
                echo("Presentation verified successfully")
            } else {
                echo("Presentation not valid")
            }
        }else {
            if (CredentialService.verify(src.readText())) {
                echo("Credential verified successfully")
            } else {
                echo("Credential not valid")
            }
        }
    }
}

class ListVcCommand : CliktCommand(
    name = "list",
    help = """List VC.
        
        """
) {

    override fun run() {
        echo("\nList VCs ...")

        CredentialService.listVCs()?.forEach { it -> echo(it) }
    }
}

