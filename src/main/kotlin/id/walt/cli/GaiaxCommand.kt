package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.model.Did
import id.walt.model.DidMethod
import id.walt.model.gaiax.GaiaxCredentialGroup
import id.walt.model.gaiax.ParticipantVerificationResult
import id.walt.services.WaltIdServices.httpNoAuth
import id.walt.services.did.DidService
import id.walt.services.did.DidWebCreateOptions
import id.walt.services.ecosystems.gaiax.GaiaxService
import id.walt.services.key.KeyService
import id.walt.signatory.Ecosystem
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.signatory.dataproviders.CLIDataProvider
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

class GaiaxCommand : CliktCommand(
    name = "gaiax",
    help = """Gaia-X specific operations

        Gaia-X functions & flows."""
) {
    override fun run() {}
}

class GaiaxOnboardingCommand : CliktCommand(
    name = "onboard",
    help = """Gaia-X Onboarding flow

        Onboards a new DID to the Gaia-X eco system. 
        
        """
) {
    override fun run() {
        echo("> Gaia-X onboarding wizard:\n")

        echo(">> Loading services...")
        val keyService = KeyService.getService()
        val gaiaxService = GaiaxService.getService()
        val signatory = Signatory.getService()


        // Key
        echo(">> Key generation")

        val keyId: KeyId = when (val rsaKeyFile =
            prompt(">>> Enter RSA key file to import or \"generate\" to create a new key", "generate")) {
            "generate", null -> {
                echo(">>> Generating RSA key...")
                keyService.generate(KeyAlgorithm.RSA)
            }

            else -> {
                val rsaKeyFilePath = Path.of(rsaKeyFile)
                echo("Importing RSA key from: $rsaKeyFilePath")
                echo("File exists: ${rsaKeyFilePath.exists()}, is file: ${rsaKeyFilePath.isRegularFile()}")
                keyService.importKey(rsaKeyFilePath.readText())
            }
        }
        echo(">>> Key id: $keyId")

        // did:web
        echo(">> DID generation")
        val didWebDomain = prompt(">>> Domain for did:web", "walt.id")!!
        var didWebPath = prompt(">>> Path for did:web (defaults internally to .well-known)", "")
        if (didWebPath?.isBlank() == true) didWebPath = null

        echo(">>> Creating did:web from key $keyId...")
        val did = DidService.create(DidMethod.web, keyId.id, DidWebCreateOptions(didWebDomain, didWebPath))
        echo("DID created: $did")

        var encodedDid = DidService.load(did).encodePretty()
        echo(">>> DID document created: (below, JSON)")
        echo("$encodedDid\n")

        // x5u
        val x5u = prompt(">>> You can optionally add a certificate chain URL (x5u)")
        if (x5u != null) {
            val didWeb = Did.decode(encodedDid)!!
            didWeb.verificationMethod = didWeb.verificationMethod!!.apply {
                for (vm in this) {
                    vm.publicKeyJwk = vm.publicKeyJwk!!.copy(x5u = x5u)
                }
            }
            encodedDid = didWeb.encode()

            echo(">>> Make sure to have a valid certificate chain at: $x5u")
        }

        echo(">>> DID created: (below, JSON)")
        echo("$encodedDid\n")

        echo(">>> Install this did:web at: " + DidService.getWebPathForDidWeb(didWebDomain, didWebPath))

        // VC
        echo(">> VC generation")
        val vcStr: String = runCatching {
            signatory.issue(
                templateIdOrFilename = "LegalPerson",
                config = ProofConfig(
                    issuerDid = did,
                    subjectDid = did,
                    creator = did,
                    issuerVerificationMethod = did,
                    proofType = ProofType.LD_PROOF,
                    proofPurpose = "assertionMethod",
                    //ldSignatureType = LdSignatureType.RsaSignature2018,
                    //ldSignatureType = LdSignatureType.JsonWebSignature2020,
                    ecosystem = Ecosystem.GAIAX,
                ),
                dataProvider = CLIDataProvider
            )
        }.getOrElse { err ->
            when (err) {
                is IllegalArgumentException -> echo(">>> Illegal argument: ${err.message}")
                else -> echo(">>> Error: ${err.message}")
            }
            return
        }

        echo(">>> Self-issued LegalPerson, DID: $did")

        echo("\n>>> Credential document (below, JSON):\n\n$vcStr")

        echo(">> Compliance credential generation")
        val complianceCredential = gaiaxService.generateGaiaxComplianceCredential(vcStr)

        echo(">>> Compliance credential:")
        echo(complianceCredential)

    }
}

class GaiaxGenerateParticipantCredentialCommand : CliktCommand(
    name = "generate-participant",
    help = "Generate the Compliance Credential (ParticipantCredential VC) from a Self Description."
) {

    private val selfDescriptionPath: Path by option(
        "-s",
        "--self-description",
        help = "Self Description to canonize, hash and sign"
    ).path(mustExist = true, mustBeReadable = true).required()
    private val saveFile: Path? by option("-f", help = "Optionally specify output file").path()
    override fun run() {
        echo("Generating ParticipantCredential from \"$selfDescriptionPath\"...")
        val sdText = selfDescriptionPath.readText()

        val complianceCredential = GaiaxService.getService().generateGaiaxComplianceCredential(sdText)

        echo("Compliance credential:")
        echo(complianceCredential)

        if (saveFile != null) {
            echo("Saving to \"$saveFile\"...")
            saveFile!!.writeText(complianceCredential)
        }
    }
}

class GaiaxVerifyCredentialGroupCommand : CliktCommand(
    name = "verify",
    help = "Validate a Participant Self Descriptor Gaia-X Credential Group"
) {

    private val selfDescriptionPath: Path by option("-s", "--self-description", help = "Existing Self Description").path(
        mustExist = true,
        mustBeReadable = true
    ).required()
    private val participantCredentialPath: Path by option(
        "-p",
        "--participant",
        help = "Participant Credential"
    ).path(mustExist = true, mustBeReadable = true).required()

    override fun run() {
        echo("Creating Credential Group...")
        val credentialGroup = GaiaxCredentialGroup(
            complianceCredential = selfDescriptionPath.readText().toVerifiableCredential(),
            selfDescriptionCredential = participantCredentialPath.readText().toVerifiableCredential()
        )

        val verificationResult = runBlocking {
            httpNoAuth.post("https://compliance.lab.gaia-x.eu/api/v2206/participant/verify/raw") {
                setBody(credentialGroup)
            }.body<ParticipantVerificationResult>()
        }

        echo("Verification:")
        echo(verificationResult.toString())
    }
}

class GaiaxDidCommand : CliktCommand(
    name = "did",
    help = """Gaia-X DID operations.

        Gaia-X DID operations."""
) {
    override fun run() {

    }
}

class GaiaxDidRegisterCommand : CliktCommand(
    name = "register",
    help = """Register Gaia-X DID.

        Registers a previously created DID with the EBSI ledger."""
) {
    val did: String by option("-d", "--did", help = "DID to be onboarded").required()
    val ethKeyAlias: String? by option("-k", "--eth-key", help = "Key to be used for signing the ETH transaction")

    override fun run() {


        //GaiaxClient.registerDid(did, ethKeyAlias ?: did)

    }
}
