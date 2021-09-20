package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import id.walt.Values
import id.walt.model.DidMethod
import id.walt.services.did.DidService
import id.walt.services.essif.EssifClientVcExchange
import id.walt.services.essif.EssifClient
import id.walt.services.essif.TrustedIssuerClient
import id.walt.services.essif.didebsi.DidEbsiService

// TODO: Support following commands

// essif login -> Verifiable Authorization

// essif auth -> Access Token

// essif did list
// essif did --did DID:ebsi:0987654
// essif did register

// essif tir list
// essif tir --did DID:ebsi:0987654
// essif tir register

// essif taor list
// essif taor --did DID:ebsi:0987654
// essif taor register

// essif tsr list
// essif tsr --id 12345
// essif tsr register

// maybe: "registries"

class EssifCommand : CliktCommand(
    name = "essif",
    help = """ESSIF specific operations.

        ESSIF functions & flows."""
) {
    override fun run() {}
}

class EssifOnboardingCommand : CliktCommand(
    name = "onboard",
    help = """ESSIF Onboarding flow

        ESSIF onboarding flow"""
) {

    val keyId: String by option(
        "-k",
        "--key-id",
        help = "Key ID or key alias"
    ).default("0ec07d2f853c4b00bd701a6124f1e4c3")
    val did: String by option("-d", "--did", help = "DID to be onboarded").required()

    override fun run() {

        echo("ESSIF onboarding of DID $did ...\n")

        EssifClient.onboard(did)

        echo("ESSIF onboarding for DID $did was performed successfully.")
        echo("The Verifiable Authorization can be accessed in file: ${EssifClient.verifiableAuthorizationFile.absolutePath}.")
    }
}

class EssifAuthCommand : CliktCommand(
    name = "auth-api",
    help = """ESSIF Authorization flow

        ESSIF Authorization flow"""
) {

    val did: String by option("-d", "--did", help = "DID to be onboarded").required()

    override fun run() {

        echo("Running EBSI Authentication API flow ...\n")

        EssifClient.authApi(did)

        echo("EBSI Authorization flow was performed successfully.")
        echo("The EBSI Access Token can be accessed in file: ${EssifClient.ebsiAccessTokenFile.absolutePath}.")
    }
}

class EssifDidCommand : CliktCommand(
    name = "did",
    help = """ESSIF DID operations.

        ESSIF DID operations."""
) {
    override fun run() {}
}

class EssifDidRegisterCommand : CliktCommand(
    name = "register",
    help = """Register ESSIF DID.

        Registers a previously created DID with the EBSI ledger."""
) {
    val did: String by option("-d", "--did", help = "DID to be onboarded").required()
    val ethKeyAlias: String? by option("-k", "--eth-key", help = "Key to be used for signing the ETH transaction")

    private val didEbsiService = DidEbsiService.getService()

    override fun run() {

        echo("Registering DID $did on the EBSI ledger using key $ethKeyAlias ...\n")

        didEbsiService.registerDid(did, ethKeyAlias ?: did)

        echo("DID registration was performed successfully. Call command: 'did resolve --did $did' in order to retrieve the DID document from the EBSI ledger.")
    }
}

class EssifVcIssuanceCommand : CliktCommand(
    name = "vc-issuance",
    help = """ESSIF VC issuance flow

        ESSIF VC issuance flow"""
) {
    override fun run() {

        // Mocked flow:
        // EssifFlowRunner.vcIssuance()

        // This runs everything: EssifClient.authenticate()
        val did: String = DidService.create(DidMethod.ebsi) // Client DID

        val oidcReq = TrustedIssuerClient.generateAuthenticationRequest()
        echo("- Authentication request: \n$oidcReq\n\n")

        val didAuthReq = EssifClientVcExchange.validateAuthenticationRequest(oidcReq)
        echo("- Parsed and validated authentication request: \n$didAuthReq\n\n")

        val authResp = EssifClientVcExchange.generateAuthenticationResponse(did, didAuthReq)
        echo("- Authentication response JWT: \n$authResp\n\n")

        val encAccessToken = TrustedIssuerClient.openSession(authResp)
        echo("- Received encrypted access token: \n$encAccessToken\n\n")

        val accessToken = EssifClientVcExchange.decryptAccessToken(encAccessToken)
        echo("- Decrypted and verified access token: \n$accessToken\n\n")

    }
}

class EssifVcExchangeCommand : CliktCommand(
    name = "vc-exchange",
    help = """ESSIF VC exchange flow

        ESSIF VC exchange flow"""
) {
    override fun run() = EssifClient.vcExchange()
}

class EssifTirCommand : CliktCommand(
    name = "tir",
    help = """ESSIF Trusted Issuer Registry operations.

        ESSIF DID operations."""
) {
    override fun run() =
        TODO("The \"ESSIF-TIR\" operation has not yet been implemented in this snapshot (currently running ${Values.version}).")
}

class EssifTaorCommand : CliktCommand(
    name = "taor",
    help = """ESSIF Trusted Accreditation Organization operations.

        ESSIF Trusted Accreditation Organization operations."""
) {
    override fun run() =
        TODO("The \"ESSIF-TAOR\" operation has not yet been implemented in this snapshot (currently running ${Values.version}).")
}

class EssifTsrCommand : CliktCommand(
    name = "tsr",
    help = """ESSIF Trusted Schema Registry operations.

        ESSIF Trusted Schema Registry operations."""
) {
    override fun run() =
        TODO("The \"ESSIF-TSR\" operation has not yet been implemented in this snapshot (currently running ${Values.version}).")
}
