package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import id.walt.Values
import id.walt.common.prettyPrint
import id.walt.model.encodePretty
import id.walt.services.essif.EssifClient
import id.walt.services.essif.TrustedIssuerClient
import id.walt.services.essif.timestamp.Timestamp
import id.walt.services.essif.timestamp.WaltIdTimestampService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

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

        Onboards a new DID to the EBSI/ESSIF eco system. 
        
        For gaining access to the EBSI service, a bearer token from 
        https://app.preprod.ebsi.eu/users-onboarding must be present."""
) {
    val bearerTokenFile: File by argument("BEARER-TOKEN-FILE", help = "File containing the bearer token from EOS").file()
    val did: String by option("-d", "--did", help = "DID to be onboarded").required()

    override fun run() {

        echo("ESSIF onboarding for DID $did running...\n")

        EssifClient.onboard(did, bearerTokenFile.readText().replace("\n", ""))

        echo("ESSIF onboarding for DID $did was performed successfully.")
    }
}

class EssifAuthCommand : CliktCommand(
    name = "auth-api",
    help = """ESSIF EBSI Authentication flow

        ESSIF EBSI Authentication flow"""
) {

    val did: String by option("-d", "--did", help = "DID to be onboarded").required()

    override fun run() {

        echo("EBSI Authentication API flow for DID $did running...\n")

        EssifClient.authApi(did)

        echo("EBSI Authentication API flow for DID $did was performed successfully.")
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

    override fun run() {

        echo("EBSI ledger registration for DID $did running...\n")

        EssifClient.registerDid(did, ethKeyAlias ?: did)

        echo("EBSI ledger registration for DID $did was performed successfully.\nCall command: 'did resolve --did $did' in order to retrieve the DID document from the EBSI ledger.")
    }
}

//class EssifVcIssuanceCommand : CliktCommand(
//    name = "vc-issuance",
//    help = """ESSIF VC issuance flow
//
//        ESSIF VC issuance flow"""
//) {
//    override fun run() {
//
//        // Mocked flow:
//        // EssifFlowRunner.vcIssuance()
//
//        // This runs everything: EssifClient.authenticate()
//        val did: String = DidService.create(DidMethod.ebsi) // Client DID
//
//        val oidcReq = TrustedIssuerClient.generateAuthenticationRequest()
//        echo("- Authentication request: \n$oidcReq\n\n")
//
//        val didAuthReq = EssifClientVcExchange.validateAuthenticationRequest(oidcReq)
//        echo("- Parsed and validated authentication request: \n$didAuthReq\n\n")
//
//        val authResp = EssifClientVcExchange.generateAuthenticationResponse(did, didAuthReq)
//        echo("- Authentication response JWT: \n$authResp\n\n")
//
//        val encAccessToken = TrustedIssuerClient.openSession(authResp)
//        echo("- Received encrypted access token: \n$encAccessToken\n\n")
//
//        val accessToken = EssifClientVcExchange.decryptAccessToken(encAccessToken)
//        echo("- Decrypted and verified access token: \n$accessToken\n\n")
//
//    }
//}
//
//class EssifVcExchangeCommand : CliktCommand(
//    name = "vc-exchange",
//    help = """ESSIF VC exchange flow
//
//        ESSIF VC exchange flow"""
//) {
//    override fun run() = EssifClient.vcExchange()
//}

class EssifTimestampCommand : CliktCommand(
    name = "timestamp",
    help = """EBSI Timestamp API operations.

        Create and retrieve a timestamp on the EBSI ledger."""
) {
    override fun run() {}
}

class EssifTimestampCreateCommand : CliktCommand(
    name = "create",
    help = """Create timestamp.

        Create timestamp on the EBSI ledger."""
) {
    val dataFile: File by argument("DATA-FILE", help = "File containing data to be used for the timestamp").file()
    val did: String by option("-d", "--did", help = "DID of the issuer.").required()
    val ethKeyAlias: String? by option("-e", "--eth-key", help = "ETH key alias.")

    override fun run() {
        echo("Creating timestamp")

        if (!dataFile.exists()) throw Exception("File ${dataFile.absoluteFile} not found.")

        val transactionHash = WaltIdTimestampService().createTimestamp(did, ethKeyAlias ?: did, "{\"test\": \"${UUID.randomUUID()}\"}")

        echo("\nReturned transaction hash:\n")

        echo(transactionHash)
    }
}

class EssifTimestampGetCommand : CliktCommand(
    name = "get",
    help = """Get timestamp.

        Get timestamp by its ID or transaction hash."""
) {
    val id: String? by option("-i", "--timestamp-id", help = "Timestamp ID.")
    val txhash: String? by option("-s", "--timestamp-txhash", help = "Timestamp transaction hash.")

    override fun run() {
        echo("Getting timestamp.")

        val timestamp: Timestamp? = runBlocking {
             when {
                 id != null -> WaltIdTimestampService().getByTimestampId(id!!)
                 txhash != null -> WaltIdTimestampService().getByTransactionHash(txhash!!)
                 else -> throw Exception("Either timestamp ID or transaction hash need to be specified")
             }
        }

        echo("\nResult:\n")

        echo(Json.encodeToString(timestamp))
    }
}

class EssifTirCommand : CliktCommand(
    name = "tir",
    help = """ESSIF Trusted Issuer Registry operations.

        ESSIF DID operations."""
) {
    override fun run() {}
}

fun getIssuerHelper(did: String, raw: Boolean) = when (raw) { 
    true -> TrustedIssuerClient.getIssuerRaw(did).prettyPrint()
    else -> TrustedIssuerClient.getIssuer(did).encodePretty()
}

class EssifTirGetIssuerCommand : CliktCommand(
    name = "get",
    help = """Get issuer.

        Get issuer by its DID. Use option raw to disable type checking."""
) {
    val did: String by option("-d", "--did", help = "DID of the issuer.").required()
    val raw by option("--raw", "-r").flag("--typed", "-t", default = false)

    override fun run() {
        echo("Getting issuer with DID \"$did\"...")

        val trustedIssuer = getIssuerHelper(did, raw)

        echo("\nResult:\n")

        echo(trustedIssuer)
    }
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
