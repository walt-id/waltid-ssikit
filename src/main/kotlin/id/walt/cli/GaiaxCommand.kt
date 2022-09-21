package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

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
    val bearerTokenFile: File by argument("BEARER-TOKEN-FILE", help = "File containing the bearer token from EOS").file()
    val did: String by option("-d", "--did", help = "DID to be onboarded").required()

    override fun run() {

        echo("Gaia-X onboarding for DID $did running...\n")

        //GaiaxClient.onboard(did, bearerTokenFile.readText().replace("\n", ""))

        echo("Gaia-X onboarding for DID $did was performed successfully.")
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
