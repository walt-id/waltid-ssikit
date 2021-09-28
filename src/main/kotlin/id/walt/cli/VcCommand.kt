package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import id.walt.auditor.AuditorService
import id.walt.auditor.PolicyRegistry
import id.walt.common.prettyPrint
import id.walt.custodian.CustodianService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.signatory.*
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.templates.VcTemplateManager
import io.ktor.util.date.*
import mu.KotlinLogging
import java.io.File
import java.nio.file.Path
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors
import kotlin.io.path.readText

private val log = KotlinLogging.logger {}

private val credentialService = JsonLdCredentialService.getService()

class VcCommand : CliktCommand(
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

class VcIssueCommand : CliktCommand(
    name = "issue",
    help = """Issues and save VC.
        
        """
) {
    val config: CliConfig by requireObject()
    val dest: File? by argument().file().optional()
    val template: String by option("-t", "--template", help = "VC template [VerifiableDiploma]").default("VerifiableDiploma")
    val issuerDid: String by option("-i", "--issuer-did", help = "DID of the issuer (associated with signing key)").required()
    val subjectDid: String by option("-s", "--subject-did", help = "DID of the VC subject (receiver of VC)").required()
    val proofType: ProofType by option("-p", "--proof-type", help = "Proof type to be used [LD_PROOF]").enum<ProofType>().default(ProofType.LD_PROOF)
    val interactive: Boolean by option("--interactive", help = "Interactively prompt for VC data to fill in").flag(default = false)

    private val signatory = Signatory.getService()

    override fun run() {
        if (interactive) {
            val cliDataProvider = CLIDataProviders.getCLIDataProviderFor(template)
            if (cliDataProvider == null) {
                echo("No interactive data provider available for template: $template")
                return
            }
            val templ = VcTemplateManager.loadTemplate(template)
            DataProviderRegistry.register(templ::class, cliDataProvider)
        }
        echo("Issuing a verifiable credential (using template ${template})")

        // Loading VC template
        log.debug { "Loading credential template: ${template}" }

        val vcStr = signatory.issue(template, ProofConfig(issuerDid, subjectDid, "Ed25519Signature2018", null, proofType))

        echo("Generated Credential:\n\n$vcStr")

        dest?.run {
            log.debug { "Writing VC to DEST file $dest" }
            dest!!.writeText(vcStr)
            echo("\nSaved credential to file: $dest")
        }
    }
}

class VcImportCommand : CliktCommand(
    name = "import",
    help = "Import VC to custodian store"
) {

    val src: File by argument().file()

    override fun run() {
        if (src.exists()) {
            val cred = src.readText().toCredential()
            val storeId = cred.id ?: "custodian#${UUID.randomUUID()}"
            CustodianService.getService().storeCredential(storeId, cred)
            println("Credential stored as $storeId")
        }
    }
}

class PresentVcCommand : CliktCommand(
    name = "present",
    help = """Present VC.
        
        """
) {
    val src: List<Path> by argument().path(mustExist = true).multiple()
    val holderDid: String by option("-i", "--holder-did", help = "DID of the holder (owner of the VC)").required()
    val verifierDid: String? by option("-v", "--verifier-did", help = "DID of the verifier (recipient of the VP)")
    val domain: String? by option("-d", "--domain", help = "Domain name to be used in the LD proof")
    val challenge: String? by option("-c", "--challenge", help = "Challenge to be used in the LD proof")

    override fun run() {
        echo("Creating verifiable presentation from files:")
        src.forEach { vc -> echo("- $vc") }

        val vcStrList = src.stream().map { vc -> vc.readText() }.collect(Collectors.toList())

        // Creating the Verifiable Presentation
        val vp = CustodianService.getService().createPresentation(vcStrList, holderDid, verifierDid, domain, challenge)

        log.debug { "Presentation created:\n$vp" }

        // Storing VP
        val vpFileName = "data/vc/presented/vp-${Timestamp.valueOf(LocalDateTime.now()).time}.json"
        log.debug { "Writing VP to file $vpFileName" }
        File(vpFileName).writeText(vp)
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
    val policies: List<String> by option(
        "-p",
        "--policy",
        help = "Verification policy. Can be specified multiple times. By default, ${PolicyRegistry.defaultPolicyId} is used."
    ).multiple(default = listOf(PolicyRegistry.defaultPolicyId))

    override fun run() {
        echo("Verifying from file $src ...\n")

        if (!src.exists()) {
            log.error("Could not load file $src")
            throw Exception("Could not load file $src")
        }
        if (policies.any { !PolicyRegistry.contains(it) }) {
            log.error("Unknown verification policy specified")
            throw Exception("Unknown verification policy specified")
        }

        val verificationResult = AuditorService.verify(src.readText(), policies.map { PolicyRegistry.getPolicy(it) })

        echo("\nResults:\n")

//        val type = when (verificationResult.verificationType) {
//            VerificationType.VERIFIABLE_PRESENTATION -> "verifiable presentation"
//            VerificationType.VERIFIABLE_CREDENTIAL -> "verifiable credential"
//        }

//        echo(
//            when (verificationResult.verified) {
//                true -> "The $type was verified successfully."
//                false -> "The $type is not valid or could not be verified."
//            }
//        )

        verificationResult.policyResults.forEach { (policy, result) ->
            echo("$policy:\t\t $result")
        }
        echo("Verified:\t\t ${verificationResult.overallStatus}")
    }
}

class ListVerificationPoliciesCommand : CliktCommand(
    name = "policies",
    help = "List verification policies"
) {
    override fun run() {
        PolicyRegistry.listPolicies().forEach { verificationPolicy ->
            echo("${verificationPolicy.id}: ${verificationPolicy.description}")
        }
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

        CustodianService.getService().listCredentials().forEachIndexed { index, vc -> echo("- ${index + 1}: $vc") }
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

class VcTemplatesListCommand : CliktCommand(
    name = "list",
    help = """List VC Templates.

        """
) {

    override fun run() {
        echo("\nListing VC templates ...")

        echo("\nResults:\n")

        Signatory.getService().listTemplates().forEachIndexed { index, vc -> echo("- ${index + 1}: $vc") }
    }
}

class VcTemplatesExportCommand : CliktCommand(
    name = "export",
    help = """Export VC Template.

        """
) {

    val templateName: String by argument(
        "template-name",
        "Name of the template",
    )

    override fun run() {
        echo("\nExporting VC template ...")
        val template = Signatory.getService().loadTemplate(templateName).encode().prettyPrint()
        echo(template)
        File("vc-template-$templateName-${getTimeMillis()}.json").writeText(template)
    }
}
