package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.letstrust.model.VerifiableCredential
import org.letstrust.model.VerifiablePresentation
import org.letstrust.model.encodePretty
import org.letstrust.services.vc.CredentialService
import org.letstrust.services.vc.CredentialService.VerificationType
import org.letstrust.vclib.VcLibManager
import org.letstrust.vclib.vcs.Europass
import org.letstrust.vclib.vcs.PermanentResidentCard
import java.io.File
import java.lang.IllegalArgumentException
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
    val template: File by option(
        "-t",
        "--template",
        help = "VC template [data/vc/templates/vc-template-default.json]"
    ).file().default(File("data/vc/templates/vc-template-default.json"))
    val issuerDid: String by option(
        "-i",
        "--issuer-did",
        help = "DID of the issuer (associated with signing key)"
    ).required()
    val subjectDid: String by option("-s", "--subject-did", help = "DID of the VC subject (receiver of VC)").required()

    override fun run() {
        echo("Issuing and saving verifiable credential (using template ${template.absolutePath})...")

        // Loading VC template
        log.debug { "Loading credential template: ${template.absolutePath}" }
        if (!template.name.contains("vc-template")) {
            log.error { "Template-file name must start with \"vc-template\"" }
            return
        }

        if (!template.exists()) {
            template.writeText(CredentialService.defaultVcTemplate().encodePretty())
        }

        // Populating VC with data
        val vcId = Timestamp.valueOf(LocalDateTime.now()).time

        val vcReqEnc = try {
            val vcReq = VcLibManager.getVerifiableCredential(template.readText())
            when (vcReq) {
                is Europass -> {
                    val vcEuropass: Europass = vcReq
                    vcEuropass.id = vcId.toString()
                    vcEuropass.issuer = issuerDid
                    vcEuropass.credentialSubject?.id = subjectDid
                    vcEuropass.issuanceDate = LocalDateTime.now().toString()
                    Json { prettyPrint = true }.encodeToString(vcEuropass)
                }
                is PermanentResidentCard -> "todo"
                else -> throw IllegalArgumentException()
            }
        } catch (e: IllegalArgumentException) {
            // TODO: get rid of legacy code
            val vcReq = Json.decodeFromString<VerifiableCredential>(template.readText())
            vcReq.id = vcId.toString()
            vcReq.issuer = issuerDid
            vcReq.credentialSubject.id = subjectDid
            vcReq.issuanceDate = LocalDateTime.now()
            Json { prettyPrint = true }.encodeToString(vcReq)
        }

        log.debug { "Credential request:\n$vcReqEnc" }

        echo("\nResults:\n")

        // Signing VC
        val vcStr = CredentialService.sign(issuerDid, vcReqEnc)


        echo("Generated Credential:\n\n$vcStr")

        // Saving VC to file
        val vcFileName = "data/vc/created/vc-" + vcId + template.name.substringAfterLast("vc-template")

        log.debug { "Writing VC to file $vcFileName" }
        File(vcFileName).writeText(vcStr)
        echo("\nSaved credential to credential store \"$vcFileName\".")

        dest?.run {
            log.debug { "Writing VC to DEST file $dest" }
            dest!!.writeText(vcStr)
            echo("\nSaved credential to file: $dest")
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
        echo("Creating verifiable presentation form file \"$src\"...")

        if (!src.exists()) {
            log.error("Could not load VC $src")
            throw Exception("Could not load VC $src")
        }

        // Creating the Verifiable Presentation
        val vp = CredentialService.present(src.readText(), domain, challenge)

        log.debug { "Presentation created (ld-signature):\n$vp" }

        echo("\nResults:\n")

        // FIX: This is required to filter out "type" : [ "Ed25519Signature2018" ] in the proof, which is s bug from signature.ld
        val vpStr =
            Json.decodeFromString<VerifiablePresentation>(vp).let { Json { prettyPrint = true }.encodeToString(it) }

        echo("Presentation created:\n")
        echo(vpStr)

        // Storing VP
        val vpFileName = "data/vc/presented/vp-${Timestamp.valueOf(LocalDateTime.now()).time}.json"
        log.debug { "Writing VP to file $vpFileName" }
        File(vpFileName).writeText(vpStr)
        echo("\nSaved verifiable presentation to: \"$vpFileName\"")
    }
}

class VerifyVcCommand : CliktCommand(
    name = "verify",
    help = """Verify VC or VP.
        
        """
) {

    val src: File by argument().file()
    //val isPresentation: Boolean by option("-p", "--is-presentation", help = "In case a VP is verified.").flag()

    override fun run() {
        echo("Verifying form file $src ...\n")

        if (!src.exists()) {
            log.error("Could not load file $src")
            throw Exception("Could not load file $src")
        }

        val verificationResult = CredentialService.verify(src.readText())

        echo("\nResults:\n")

        val type = when (verificationResult.verificationType) {
            VerificationType.VERIFIABLE_PRESENTATION -> "verifiable presentation"
            VerificationType.VERIFIABLE_CREDENTIAL -> "verifiable credential"
        }

        echo(
            when (verificationResult.verified) {
                true -> "The $type was verified successfully."
                false -> "The $type is not valid or could not be verified."
            }
        )
    }
}

class ListVcCommand : CliktCommand(
    name = "list",
    help = """List VC.
        
        """
) {

    override fun run() {
        echo("\nListing verifiable credentials...")

        echo("\nResults:\n")

        CredentialService.listVCs().forEachIndexed { index, vc -> echo("- ${index + 1}: $vc") }
    }
}

class VcTemplatesCommand : CliktCommand(
    name = "templates",
    help = """VC Templates.

        VC templates related operations e.g.: list & export.

        """
) {

    override fun run() {

    }
}

class ListVcTemplateCommand : CliktCommand(
    name = "list",
    help = """List VC Templates.

        """
) {

    override fun run() {
        echo("\nListing VC templates ...")

        echo("\nResults:\n")

        CredentialService.listTemplates().forEachIndexed { index, vc -> echo("- ${index + 1}: $vc") }
    }
}

class ExportVcTemplateCommand : CliktCommand(
    name = "export",
    help = """Export VC Template.

        """
) {

    val templateName: String by option("-n", "--template-name", help = "Name of the template to being exported").required()

    override fun run() {
        echo("\nExporting VC template ...")

        echo(CredentialService.loadTemplate(templateName))
    }
}
