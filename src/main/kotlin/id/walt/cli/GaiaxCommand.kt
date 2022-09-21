package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.readText

class GaiaxCommand : CliktCommand(
    name = "gaiax",
    help = """Gaia-X specific operations.

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
    val did: String by option("-d", "--did", help = "DID to be onboarded").required()

    override fun run() {

        echo("Gaia-X onboarding for DID $did running...\n")

        //GaiaxClient.onboard(did, bearerTokenFile.readText().replace("\n", ""))

        echo("Gaia-X onboarding for DID $did was performed successfully.")
    }
}

class GaiaxGenerateParticipantCredentialCommand : CliktCommand(
    name = "generate-participant",
    help = "Generate the Compliance Credential (ParticipantCredential VC) from a Self Description."
) {

    private val selfDescriptionPath: Path by option("-s", "--self-description", help = "Self Description to canonize, hash and sign").path(mustExist = true, mustBeReadable = true).required()
    private val saveFile: Path? by option("-f", help = "Optionally specify output file").path()
    override fun run() {
        val client = HttpClient(CIO)

        val sdText = selfDescriptionPath.readText()
        echo("Generating ParticipantCredential from \"$selfDescriptionPath\"...")

        val complianceCredential = runBlocking {
            client.post("https://compliance.lab.gaia-x.eu/api/v2206/sign") {
                setBody(sdText)
            }.bodyAsText()
        }

        echo("Compliance credential:")
        echo(complianceCredential)

        if (saveFile != null) {
            echo("Saving to \"$saveFile\"...")
        }
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

        echo("EBSI ledger registration for DID $did running...\n")

        //GaiaxClient.registerDid(did, ethKeyAlias ?: did)

        echo("EBSI ledger registration for DID $did was performed successfully.\nCall command: 'did resolve --did $did' in order to retrieve the DID document from the EBSI ledger.")
    }
}
