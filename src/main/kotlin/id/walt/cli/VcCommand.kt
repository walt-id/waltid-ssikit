package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import id.walt.auditor.AuditorService
import id.walt.auditor.PolicyRegistry
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.vclib.Helpers.encode
import io.ktor.util.date.*
import mu.KotlinLogging
import id.walt.common.prettyPrint
import id.walt.custodian.CustodianService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.VerificationType
import id.walt.signatory.ProofConfig
import java.io.File
import java.sql.Timestamp
import java.time.LocalDateTime

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

    private val signatory = Signatory.getService()

    override fun run() {
        echo("Issuing and verifiable credential (using template ${template})...")

        // Loading VC template
        log.debug { "Loading credential template: ${template}" }

        val vcStr = signatory.issue(template, ProofConfig(issuerDid, subjectDid, "Ed25519Signature2018", proofType))
        //signatory.loadTemplate(template)

        //TODO: move the following to Signatory

        // Populating VC with data
        val vcId = Timestamp.valueOf(LocalDateTime.now()).time


//        val vcReq = template.readText().toCredential()
        /*val vcReq = credentialService.defaultVcTemplate()

        val vcReqEnc = Klaxon().toJsonString(when (vcReq) {
            is Europass -> {
                vcReq.apply {
                    id = vcId.toString()
                    issuer = issuerDid
                    credentialSubject!!.id = subjectDid
                    issuanceDate = LocalDateTime.now().toString()
                }
            }
            is VerifiableAttestation -> {
                vcReq.apply {
                    id = vcId.toString()
                    issuer = issuerDid
                    credentialSubject!!.id = subjectDid
                    issuanceDate = LocalDateTime.now().toString()
                }
            }
            is PermanentResidentCard -> vcReq.apply {
                //todo
            }
            else -> throw IllegalArgumentException()
        })

        log.debug { "Credential request:\n$vcReqEnc" }

        echo("\nResults:\n")

        // Signing VC
        val vcStr = credentialService.sign(issuerDid, vcReqEnc)*/


        echo("Generated Credential:\n\n$vcStr")

        // Saving VC to file
        val vcFileName = "data/vc/created/vc-$vcId-${template}.json"

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
    val domain: String? by option("-d", "--domain", help = "Domain name to be used in the LD proof")
    val challenge: String? by option("-c", "--challenge", help = "Challenge to be used in the LD proof")
    // val holderDid: String? by option("-i", "--holder-did", help = "DID of the holder (owner of the VC)")

    override fun run() {
        echo("Creating verifiable presentation form file \"$src\"...")

        if (!src.exists()) {
            log.error("Could not load VC $src")
            throw Exception("Could not load VC $src")
        }

        // Creating the Verifiable Presentation
        val vp = CustodianService.getService().createPresentation(src.readText(), domain, challenge)

        log.debug { "Presentation created (ld-signature):\n$vp" }

        echo("\nResults:\n")

        // FIX: This is required to filter out "type" : [ "Ed25519Signature2018" ] in the proof, which is s bug from signature.ld
//        val vpStr =
//            Klaxon().parse<VerifiablePresentation>(vp).let { Klaxon().toJsonString(it) }

        echo("Presentation created:\n")
        echo(vp)

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
    val policies: List<String> by option("-p", "--policy", help = "Verification policy. Can be specified multiple times. By default, ${PolicyRegistry.defaultPolicyId} is used.").multiple(default = listOf(PolicyRegistry.defaultPolicyId))

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

class ListVerificationPoliciesCommand : CliktCommand (
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

        credentialService.listVCs().forEachIndexed { index, vc -> echo("- ${index + 1}: $vc") }
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
