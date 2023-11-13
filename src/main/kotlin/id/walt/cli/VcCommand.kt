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
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import id.walt.auditor.Auditor
import id.walt.auditor.PolicyRegistry
import id.walt.auditor.dynamic.DynamicPolicyArg
import id.walt.auditor.dynamic.PolicyEngineType
import id.walt.common.prettyPrint
import id.walt.common.resolveContent
import id.walt.credentials.w3c.PresentableCredential
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.VerifiablePresentation
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.crypto.LdSignatureType
import id.walt.custodian.Custodian
import id.walt.model.credential.status.CredentialStatus
import id.walt.sdjwt.DecoyMode
import id.walt.sdjwt.SDMap
import id.walt.signatory.Ecosystem
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.signatory.dataproviders.CLIDataProvider
import id.walt.signatory.revocation.CredentialStatusClientService
import io.ktor.util.date.*
import io.ktor.util.reflect.*
import mu.KotlinLogging
import java.io.File
import java.io.StringReader
import java.net.ConnectException
import java.nio.file.Path
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import kotlin.io.path.readText

private val log = KotlinLogging.logger {}

//region -VC Commands-
class VcCommand : CliktCommand(
    name = "vc", help = """Verifiable Credentials (VCs)

        VC related operations like issuing, verifying and revoking VCs.

        """
) {
    override fun run() {

    }
}

class VcIssueCommand : CliktCommand(
    name = "issue", help = """Issues and save VC
        
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
        "-p", "--proof-purpose", help = "Proof purpose to be used [assertionMethod]"
    ).default("assertionMethod")
    val interactive: Boolean by option(
        "--interactive", help = "Interactively prompt for VC data to fill in"
    ).flag(default = false)
    val ldSignatureType: LdSignatureType? by option("--ld-signature", "--ld-sig").enum<LdSignatureType>()
    val ecosystem: Ecosystem by option("--ecosystem", help = "Specify ecosystem, for specific defaults of issuing parameters")
        .enum<Ecosystem>()
        .default(Ecosystem.DEFAULT)
    val statusType: CredentialStatus.Types? by option(
        "--status-type",
        help = "Specify the credentialStatus type"
    ).enum<CredentialStatus.Types>()
    val decoyMode: DecoyMode by option("--decoy-mode", help = "SD-JWT Decoy mode: random|fixed|none, default: none").enum<DecoyMode>().default(DecoyMode.NONE)
    val numDecoys: Int by option("--num-decoys", help = "Number of SD-JWT decoy digests to add (fixed mode), or max num of decoy digests (random mode)").int().default(0)
    val selectiveDisclosurePaths: List<String>? by option("--sd", "--selective-disclosure", help = "Path to selectively disclosable fields (if supported by chosen proof type), in a simplified JsonPath format, can be specified multiple times, e.g.: \"credentialSubject.familyName\".").multiple()

    private val signatory = Signatory.getService()

    override fun run() {
        val selectiveDisclosure = selectiveDisclosurePaths?.let { SDMap.generateSDMap(it, decoyMode, numDecoys) }
        echo("Issuing a verifiable credential (using template ${template})...")
        selectiveDisclosure?.also {
            echo("with selective disclosure:")
            echo(it.prettyPrint(2))
        }
        // Loading VC template
        log.debug { "Loading credential template: $template" }

        val vcStr: String = runCatching {
            signatory.issue(
                template, ProofConfig(
                    issuerDid = issuerDid,
                    subjectDid = subjectDid,
                    issuerVerificationMethod = issuerVerificationMethod,
                    proofType = proofType,
                    proofPurpose = proofPurpose,
                    ldSignatureType = ldSignatureType,
                    ecosystem = ecosystem,
                    statusType = statusType,
                    //creator = if (ecosystem == Ecosystem.GAIAX) null else issuerDid
                    creator = issuerDid,
                    selectiveDisclosure = selectiveDisclosure
                ), when (interactive) {
                    true -> CLIDataProvider
                    else -> null
                }
            )
        }.getOrElse { err ->
            when (err) {
                is IllegalArgumentException -> echo("Illegal argument: ${err.message}")
                else -> echo("Error: ${err.message}")
            }
            return
        }

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
            val cred = src.readText().toVerifiableCredential()
            val storeId = cred.id ?: "custodian#${UUID.randomUUID()}"
            Custodian.getService().storeCredential(storeId, cred)
            println("Credential stored as $storeId")
        }
    }
}

class PresentVcCommand : CliktCommand(
    name = "present", help = """Present VC
        
        """,
    epilog = """Note about selective disclosure:
        Selective disclosure flags have NO EFFECT, if the proof type of the presented credential doesn't support selective disclosure!
        Which fields can be selectively disclosed, depends on the credential proof type and credential issuer.
        To select SD-enabled fields for disclosure, refer to the help text of the --sd, --sd-all-for and --sd-all flags.
    """.trimMargin()
) {
    val src: List<Path> by argument().path(mustExist = true).multiple()
    val holderDid: String by option("-i", "--holder-did", help = "DID of the holder (owner of the VC)").required()
    val verifierDid: String? by option("-v", "--verifier-did", help = "DID of the verifier (recipient of the VP)")
    val domain: String? by option("-d", "--domain", help = "Domain name to be used in the LD proof")
    val challenge: String? by option("-c", "--challenge", help = "Challenge to be used in the LD proof")
    val selectiveDisclosure: Map<Int, SDMap>? by option("--sd", "--selective-disclosure", help = "Path to selectively disclosed fields, in a simplified JsonPath format. Can be specified multiple times. By default NONE of the sd fields are disclosed, for multiple credentials, the path can be prefixed with the index of the presented credential, e.g. \"credentialSubject.familyName\", \"0.credentialSubject.familyName\", \"1.credentialSubject.dateOfBirth\".")
        .transformAll { paths ->
            paths.map { path ->
                val hasIdxInPath = path.substringBefore(".").toIntOrNull() != null
                val idx = path.substringBefore('.').toIntOrNull() ?: 0
                Pair(idx, if(hasIdxInPath) {
                    path.substringAfter(".")
                } else {
                    path
                })
            }.groupBy { pair -> pair.first }
            .mapValues { entry -> SDMap.generateSDMap(entry.value.map { item -> item.second }) }
        }
    val discloseAllFor: Set<Int>? by option("--sd-all-for", help = "Selects all selective disclosures for the credential at the specified index to be disclosed. Overrides --sd flags!").int()
        .transformAll { it.toSet() }
    val discloseAllOfAll: Boolean by option("--sd-all", help = "Selects all selective disclosures for all presented credentials to be disclosed. Overrides --sd and --sd-all-for flags!").flag()

    override fun run() {
        echo("Creating a verifiable presentation for DID \"$holderDid\"...")
        echo("Using ${src.size} ${if (src.size > 1) "VCs" else "VC"}:")

        val vcSources: Map<Path, VerifiableCredential> = src.associateWith { it.readText().toVerifiableCredential() }

        src.forEachIndexed { index, vcPath ->
            echo("- ${index + 1}. $vcPath (${vcSources[vcPath]!!.type.last()})")
            selectiveDisclosure?.get(index)?.let {
                echo("  with selective disclosure:")
                echo(it.prettyPrint(4))
            }
        }

        val presentableList = vcSources.values.mapIndexed { idx, cred -> PresentableCredential(cred, selectiveDisclosure?.get(idx), discloseAllOfAll || (discloseAllFor?.contains(idx) == true)) }
            .toList()

        // Creating the Verifiable Presentation
        val vp = Custodian.getService().createPresentation(presentableList, holderDid, verifierDid, domain, challenge, null)

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
    name = "verify", help = """Verify VC or VP
        
        """
) {

    val src: File by argument().file()

    //val isPresentation: Boolean by option("-p", "--is-presentation", help = "In case a VP is verified.").flag()
    val policies: Map<String, String?> by option(
        "-p",
        "--policy",
        help = "Verification policy. Can be specified multiple times. By default, ${PolicyRegistry.defaultPolicyId} is used. " +
                "To specify a policy argument (if required), use the format PolicyName='{\"myParam\": \"myValue\", ...}', to specify the JSON object directly, " +
                "or PolicyName=path/to/arg.json, to read the argument from a JSON file."
    ).associate()


    override fun run() {
        val usedPolicies = policies.ifEmpty { mapOf(PolicyRegistry.defaultPolicyId to null) }

        echo("Verifying from file \"$src\"...\n")

        when {
            !src.exists() -> throw NoSuchElementException("Could not load file: \"$src\".")
            usedPolicies.keys.any { !PolicyRegistry.contains(it) } -> throw NoSuchElementException(
                "Unknown verification policy specified: ${
                    usedPolicies.keys.minus(PolicyRegistry.listPolicies().toSet()).joinToString()
                }"
            )
        }

        val verificationResult = Auditor.getService()
            .verify(
                src.readText().trim(),
                usedPolicies.entries.map { PolicyRegistry.getPolicyWithJsonArg(it.key, it.value?.ifEmpty { null }?.let {
                    resolveContent(it)
                }) })

        echo("\nResults:\n")

        verificationResult.policyResults.forEach { (policy, result) ->
            echo("$policy:\t $result")
        }
        echo("Verified:\t\t ${verificationResult.result}")
    }
}

class ListVcCommand : CliktCommand(
    name = "list", help = """List VCs
        
        """
) {

    override fun run() {
        echo("\nListing verifiable credentials...")

        echo("\nResults:\n")

        Custodian.getService().listCredentials().forEachIndexed { index, vc -> echo("- ${index + 1}: $vc") }
    }
}

class ParseVcCommand : CliktCommand(
    name = "parse", help = """Parse VC from JWT or SD-JWT representation and display JSON body
        
        """
) {
    val vc by option("-c", help = "Credential content or file path").required()
    val recursive by option("-r", help = "Recursively parse credentials in presentation").flag()
    override fun run() {
        echo("\nParsing verifiable credential...")

        echo("\nResults:\n")

        val parsedVc = resolveContent(vc).toVerifiableCredential()
        echo(if(parsedVc is VerifiablePresentation) "- Presentation:" else "- Credential:")
        echo()
        println(parsedVc.toJson().prettyPrint())
        echo()
        parsedVc.selectiveDisclosure?.let {
            echo("  with selective disclosure:")
            echo(it.prettyPrint(4))
        }
        if(parsedVc is VerifiablePresentation && recursive) {
            parsedVc.verifiableCredential?.forEachIndexed { idx, cred ->
                echo("---")
                echo("- Credential ${idx + 1}")
                echo()
                println(cred.toJson().prettyPrint())
                cred.selectiveDisclosure?.let {
                    echo("  with selective disclosure:")
                    echo(it.prettyPrint(4))
                }
            }
        }
    }
}
//endregion

//region -Policy Commands-
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
        PolicyRegistry.listPolicyInfo().filter { vp -> vp.isMutable || !mutablesOnly }
            .forEach { (id, description, argumentType, isMutable) ->
                echo("${if (isMutable) "*" else "-"} ${id}\t ${description ?: "- no description -"},\t Argument: $argumentType")
            }
        echo()
        echo("(*) ... mutable dynamic policy")
    }
}

class CreateDynamicVerificationPolicyCommand : CliktCommand(
    name = "create", help = "Create dynamic verification policy"
) {
    val name: String by option("-n", "--name", help = "Policy name, must not conflict with existing policies").required()
    val description: String? by option("-D", "--description", help = "Policy description (optional)")
    val policy: String by option(
        "-p",
        "--policy",
        help = "Path or URL to policy definition. e.g.: rego file for OPA policy engine"
    ).required()
    val dataPath: String by option(
        "-d",
        "--data-path",
        help = "JSON path to the data in the credential which should be verified, default: \"$\" (whole credential object)"
    ).default("$")
    val policyQuery: String by option(
        "-q",
        "--policy-query",
        help = "Policy query which should be queried by policy engine"
    ).default("data.system.main")
    val input: JsonObject by option(
        "-i",
        "--input",
        help = "Input JSON object for rego query, which can be overridden/extended on verification. Can be a JSON string or JSON file."
    ).convert { Klaxon().parseJsonObject(StringReader(resolveContent(it))) }.default(JsonObject())
    val save: Boolean by option(
        "-s",
        "--save-policy",
        help = "Downloads and/or saves the policy definition locally, rather than keeping the reference to the original URL"
    ).flag(default = false)
    val force: Boolean by option(
        "-f",
        "--force",
        help = "Override existing policy with that name (static policies cannot be overridden!)"
    ).flag(default = false)
    val engine: PolicyEngineType by option(
        "-e",
        "--policy-engine",
        help = "Policy engine type, default: OPA"
    ).enum<PolicyEngineType>().default(PolicyEngineType.OPA)
    val applyToVC: Boolean by option(
        "--vc",
        help = "Apply/Don't apply to verifiable credentials (default: apply)"
    ).flag("--no-vc", default = true, defaultForHelp = "apply")
    val applyToVP: Boolean by option(
        "--vp",
        help = "Apply/Don't apply to verifiable presentations (default: don't apply)"
    ).flag("--no-vp", default = false, defaultForHelp = "don't apply")

    override fun run() {
        if (PolicyRegistry.contains(name)) {
            if (PolicyRegistry.isMutable(name) && !force) {
                echo("Policy $name already exists, use --force to update.")
                return
            } else if (!PolicyRegistry.isMutable(name)) {
                echo("Immutable existing policy $name cannot be overridden.")
                return
            }
        }
        if (PolicyRegistry.createSavedPolicy(
                name,
                DynamicPolicyArg(name, description, input, policy, dataPath, policyQuery, engine, applyToVC, applyToVP),
                force,
                save
            )
        ) {
            echo("Policy created/updated: $name")
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
        if (PolicyRegistry.contains(name)) {
            if (PolicyRegistry.deleteSavedPolicy(name))
                echo("Policy removed: $name")
            else
                echo("Could not be removed: $name")
        } else {
            echo("Policy not found: $name")
        }
    }
}
//endregion

//region -Templates Commands-
class VcTemplatesCommand : CliktCommand(
    name = "templates", help = """VC templates

        VC templates related operations e.g.: list & export.

        """
) {

    override fun run() {

    }
}

class VcTemplatesListCommand : CliktCommand(
    name = "list", help = """List VC templates

        """
) {

    override fun run() {
        echo("\nListing VC templates ...")

        echo("\nResults:\n")

        Signatory.getService().listTemplates().sortedBy { it.name }.forEach { tmpl ->
            echo("${if (tmpl.mutable) "*" else "-"} ${tmpl.name}")
        }
        echo()
        echo("(*) ... custom template")
    }
}

class VcTemplatesExportCommand : CliktCommand(
    name = "export", help = """Export VC template

        """
) {

    val templateName: String by option("-n", "--name", help = "Name of the template").required()

    override fun run() {
        echo("\nExporting VC template ...")
        val template = Signatory.getService().loadTemplate(templateName).encode().prettyPrint()
        echo(template)
        File("vc-template-$templateName-${getTimeMillis()}.json").writeText(template)
    }
}

class VcTemplatesImportCommand : CliktCommand(
    name = "import", help = """Import VC template

        """
) {

    val templateName: String by option("-n", "--name", help = "Name of the template").required()
    val templateFile: String by argument("template-file", "File of template to import")

    override fun run() {
        echo("\nImporting VC template ...")
        val file = File(templateFile)
        if (!file.exists() || !file.isFile) {
            echo("Template file not found")
        } else {
            val template = File(templateFile).readText()
            try {
                // try to parse
                Signatory.getService().importTemplate(templateName, template)
                echo("Template saved as $templateName")
            } catch (exc: Exception) {
                echo("Error parsing credential template: ${exc.message}")
            }
        }
    }
}

class VcTemplatesRemoveCommand : CliktCommand(
    name = "remove", help = """Remove VC template

        """
) {

    val templateName: String by option("-n", "--name", help = "Name of the template").required()

    override fun run() {
        echo("\nRemoving VC template ...")
        try {
            Signatory.getService().removeTemplate(templateName)
            echo("Template removed.")
        } catch (exc: Exception) {
            echo("Error removing template: ${exc.message}")
        }
    }
}
//endregion

//region -Revocation Commands-
class VcRevocationCommand : CliktCommand(
    name = "revocation", help = """VC revocations

        VC revocation related operations e.g.: check & revoke.

        """
) {

    override fun run() {

    }
}

class VcRevocationCheckCommand : CliktCommand(
    name = "check", help = "Check VC revocation status"
) {
    val vcFile: File by argument().file()
    override fun run() = vcFile.takeIf { it.exists() }?.run {
        println("Checking revocation status for credential stored at: ${vcFile.absolutePath}")
        runWithErrorHandling(
            runner = { CredentialStatusClientService.check(this.readText().toVerifiableCredential()) },
            onSuccess = {
                println("Revocation status:")
                println(Klaxon().toJsonString(it).prettyPrint())
            }
        )
    } ?: Unit
}

class VcRevocationRevokeCommand: CliktCommand(
    name = "revoke", help = "Revoke VC"
) {
    val vcFile: File by argument().file()
    override fun run() = vcFile.takeIf { it.exists() }?.run {
        println("Revoking credential stored at: ${vcFile.absolutePath}")
        runWithErrorHandling(
            runner = { CredentialStatusClientService.revoke(this.readText().toVerifiableCredential()) },
            onSuccess = {
                println("Revocation result:")
                println(Klaxon().toJsonString(it).prettyPrint())
            }
        )
    } ?: Unit
}

internal fun <T> runWithErrorHandling(
    runner: () -> T,
    onSuccess: ((T) -> Unit)? = null,
    onFailure: ((Throwable) -> Unit)? = null
) {
    runCatching {
        runner()
    }.onSuccess {
        onSuccess?.invoke(it)
    }.onFailure {
        println(it.localizedMessage)
        if (it.instanceOf(ConnectException::class)) {
            println("Looks like couldn't reach the Signatory API. Make sure to run \"ssikit serve\" first.")
        }
        onFailure?.invoke(it)
    }
}
//endregion
