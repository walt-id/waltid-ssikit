package id.walt.cli

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import id.walt.auditor.Auditor
import id.walt.auditor.PolicyRegistry
import id.walt.auditor.dynamic.DynamicPolicyArg
import id.walt.auditor.dynamic.PolicyEngineType
import id.walt.common.prettyPrint
import id.walt.common.resolveContent
import id.walt.custodian.Custodian
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.signatory.dataproviders.CLIDataProvider
import id.walt.vclib.model.toCredential
import io.ktor.util.date.*
import mu.KotlinLogging
import java.io.File
import java.io.StringReader
import java.nio.file.Path
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import kotlin.io.path.readText

private val log = KotlinLogging.logger {}

class VcCommand : CliktCommand(
    name = "vc", help = """Verifiable Credentials (VCs).

        VC related operations like issuing, verifying and revoking VCs.

        """
) {

    override fun run() {

    }
}

class VcIssueCommand : CliktCommand(
    name = "issue", help = """Issues and save VC.
        
        """
) {
    val config: CliConfig by requireObject()
    val dest: File? by argument().file().optional()
    val template: String by option("-t", "--template", help = "VC template [VerifiableDiploma]").default("VerifiableDiploma")
    val issuerDid: String by option("-i", "--issuer-did", help = "DID of the issuer (associated with signing key)").required()
    val subjectDid: String by option("-s", "--subject-did", help = "DID of the VC subject (receiver of VC)").required()
    val issuerVerificationMethod: String? by option(
        "-v", "--issuer-verification-method", help = "KeyId of the issuers' signing key"
    )
    val proofType: ProofType by option("-y", "--proof-type", help = "Proof type to be used [LD_PROOF]").enum<ProofType>()
        .default(ProofType.LD_PROOF)
    val proofPurpose: String by option(
        "-p", "--proof-purpose", help = "Proof purpose to be used [assertion]"
    ).default("assertion")
    val interactive: Boolean by option(
        "--interactive", help = "Interactively prompt for VC data to fill in"
    ).flag(default = false)

    private val signatory = Signatory.getService()

    override fun run() {
        echo("Issuing a verifiable credential (using template ${template})...")

        // Loading VC template
        log.debug { "Loading credential template: $template" }

        val vcStr = signatory.issue(
            template, ProofConfig(
                issuerDid = issuerDid,
                subjectDid = subjectDid,
                issuerVerificationMethod = issuerVerificationMethod,
                proofType = proofType,
                proofPurpose = proofPurpose
            ), when (interactive) {
                true -> CLIDataProvider
                else -> null
            }
        )

        echo("\nResults:\n")

        echo("Issuer \"$issuerDid\"")
        echo("⇓ issued a \"$template\" to ⇓")
        echo("Holder \"$subjectDid\"")

        echo("\nCredential document (below, JSON):\n\n$vcStr")

        dest?.run {
            log.debug { "Writing VC to DEST file $dest" }
            dest!!.writeText(vcStr)
            echo("\nSaved credential to file: $dest")
        }
    }
}

class VcImportCommand : CliktCommand(
    name = "import", help = "Import VC to custodian store"
) {

    val src: File by argument().file()

    override fun run() {
        if (src.exists()) {
            val cred = src.readText().toCredential()
            val storeId = cred.id ?: "custodian#${UUID.randomUUID()}"
            Custodian.getService().storeCredential(storeId, cred)
            println("Credential stored as $storeId")
        }
    }
}

class PresentVcCommand : CliktCommand(
    name = "present", help = """Present VC.
        
        """
) {
    val src: List<Path> by argument().path(mustExist = true).multiple()
    val holderDid: String by option("-i", "--holder-did", help = "DID of the holder (owner of the VC)").required()
    val verifierDid: String? by option("-v", "--verifier-did", help = "DID of the verifier (recipient of the VP)")
    val domain: String? by option("-d", "--domain", help = "Domain name to be used in the LD proof")
    val challenge: String? by option("-c", "--challenge", help = "Challenge to be used in the LD proof")

    override fun run() {
        echo("Creating a verifiable presentation for DID \"$holderDid\"...")
        echo("Using ${src.size} ${if (src.size > 1) "VCs" else "VC"}:")

        val vcSources: Map<Path, String> = src.associateWith { it.readText() }

        src.forEachIndexed { index, vcPath ->
            echo("- ${index + 1}. $vcPath (${vcSources[vcPath]!!.toCredential().type.last()})")
        }

        val vcStrList = vcSources.values.toList()

        // Creating the Verifiable Presentation
        val vp = Custodian.getService().createPresentation(vcStrList, holderDid, verifierDid, domain, challenge, null)

        log.debug { "Presentation created:\n$vp" }

        echo("\nResults:\n")
        echo("Verifiable presentation generated for holder DID: \"$holderDid\"")
        echo("Verifiable presentation document (below, JSON):\n\n$vp")

        // Storing VP
        val vpFileName = "data/vc/presented/vp-${Timestamp.valueOf(LocalDateTime.now()).time}.json"
        log.debug { "Writing VP to file $vpFileName" }
        File(vpFileName).writeText(vp)
        echo("\nVerifiable presentation was saved to file: \"$vpFileName\"")
    }
}

class VerifyVcCommand : CliktCommand(
    name = "verify", help = """Verify VC or VP.
        
        """
) {

    val src: File by argument().file()

    //val isPresentation: Boolean by option("-p", "--is-presentation", help = "In case a VP is verified.").flag()
    val policies: Map<String, String?> by option(
        "-p",
        "--policy",
        help = "Verification policy. Can be specified multiple times. By default, ${PolicyRegistry.defaultPolicyId} is used."
    ).associate()


    override fun run() {
        val usedPolicies = if (policies.isNotEmpty()) policies else mapOf(PolicyRegistry.defaultPolicyId to null)

        echo("Verifying from file \"$src\"...\n")

        when {
            !src.exists() -> throw Exception("Could not load file: \"$src\".")
            usedPolicies.keys.any { !PolicyRegistry.contains(it) } -> throw Exception(
                "Unknown verification policy specified: ${
                    usedPolicies.keys.minus(PolicyRegistry.listPolicies().toSet()).joinToString()
                }"
            )
        }

        val verificationResult = Auditor.getService()
            .verify(src.readText(), usedPolicies.entries.map { PolicyRegistry.getPolicyWithJsonArg(it.key, it.value?.ifEmpty { null }) })

        echo("\nResults:\n")

        verificationResult.policyResults.forEach { (policy, result) ->
            echo("$policy:\t $result")
        }
        echo("Verified:\t\t ${verificationResult.valid}")
    }
}

class VerificationPoliciesCommand : CliktCommand(
    name = "policies", help = "Manage verification policies"
) {
    override fun run() {

    }
}

class ListVerificationPoliciesCommand : CliktCommand(
    name = "list", help = "List verification policies"
) {
    val mutablesOnly: Boolean by option("-m", "--mutable", help = "Show only mutable policies").flag(default = false)
    override fun run() {
        PolicyRegistry.listPolicyInfo().filter { vp -> vp.isMutable || !mutablesOnly }.forEach { verificationPolicy ->
            echo("${if(verificationPolicy.isMutable) "*" else "-"} ${verificationPolicy.id}\t ${verificationPolicy.description ?: "- no description -"},\t Argument: ${verificationPolicy.argumentType}")
        }
        echo()
        echo ("(*) ... mutable dynamic policy")
    }
}

class CreateDynamicVerificationPolicyCommand : CliktCommand(
    name = "create", help = "Create dynamic verification policy"
) {
    val name: String by option("-n", "--name", help = "Policy name, must not conflict with existing policies").required()
    val description: String? by option("-D", "--description", help = "Policy description (optional)")
    val policy: String by option("-p", "--policy", help = "Path or URL to policy definition. e.g.: rego file for OPA policy engine").required()
    val dataPath: String by option("-d", "--data-path", help = "JSON path to the data in the credential which should be verified").default("$.credentialSubject")
    val policyQuery: String by option("-q", "--policy-query", help = "Policy query which should be queried by policy engine").default("data.system.main")
    val input: JsonObject by option("-i", "--input", help = "Input JSON object for rego query, which can be overridden/extended on verification").convert { Klaxon().parseJsonObject(StringReader(it)) }.default(JsonObject())
    val save: Boolean by option("-s", "--save-policy", help = "Downloads and/or saves the policy definition locally, rather than keeping the reference to the original URL").flag(default = false)
    val force: Boolean by option("-f", "--force", help = "Override existing policy with that name (static policies cannot be overridden!)").flag(default = false)
    val engine: PolicyEngineType by option("-e", "--policy-engine", help = "Policy engine type, default: OPA").enum<PolicyEngineType>().default(PolicyEngineType.OPA)
    val applyToVC: Boolean by option("--vc", help = "Apply/Don't apply to verifiable credentials (default: apply)").flag("--no-vc", default = true, defaultForHelp = "apply")
    val applyToVP: Boolean by option("--vp", help = "Apply/Don't apply to verifiable presentations (default: don't apply)").flag("--no-vp", default = false, defaultForHelp = "don't apply")

    override fun run() {
        if(PolicyRegistry.contains(name)) {
            if(PolicyRegistry.isMutable(name) && !force) {
                echo("Policy $name already exists, use --force to update.")
                return
            } else if(!PolicyRegistry.isMutable(name)) {
                echo("Immutable existing policy $name cannot be overridden.")
                return
            }
        }
        if(PolicyRegistry.createSavedPolicy(
                name,
                DynamicPolicyArg(name, description, input, policy, dataPath, policyQuery, engine, applyToVC, applyToVP),
                force,
                save
            )) {
            echo("Policy created/updated: ${name}")
        } else {
            echo("Failed to create dynamic policy")
        }
    }
}

class RemoveDynamicVerificationPolicyCommand : CliktCommand(
    name = "remove", help = "Remove a dynamic verification policy"
) {
    val name: String by option("-n", "--name", help = "Name of the dynamic policy to remove").required()

    override fun run() {
        if(PolicyRegistry.contains(name)) {
            if(PolicyRegistry.deleteSavedPolicy(name))
                echo("Policy removed: $name")
            else
                echo("Could not be removed: $name")
        } else {
            echo("Policy not found: $name")
        }
    }
}

class ListVcCommand : CliktCommand(
    name = "list", help = """List VC.
        
        """
) {

    override fun run() {
        echo("\nListing verifiable credentials...")

        echo("\nResults:\n")

        Custodian.getService().listCredentials().forEachIndexed { index, vc -> echo("- ${index + 1}: $vc") }
    }
}

class VcTemplatesCommand : CliktCommand(
    name = "templates", help = """VC Templates.

        VC templates related operations e.g.: list & export.

        """
) {

    override fun run() {

    }
}

class VcTemplatesListCommand : CliktCommand(
    name = "list", help = """List VC Templates.

        """
) {

    override fun run() {
        echo("\nListing VC templates ...")

        echo("\nResults:\n")

        Signatory.getService().listTemplates().sorted().forEachIndexed { index, vc -> echo("- ${index + 1}: $vc") }
    }
}

class VcTemplatesExportCommand : CliktCommand(
    name = "export", help = """Export VC Template.

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
