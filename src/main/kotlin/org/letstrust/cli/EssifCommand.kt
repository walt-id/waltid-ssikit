package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.letstrust.Values
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.crypto.buildKey
import org.letstrust.services.essif.EosService
import org.letstrust.services.essif.EssifFlowRunner

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
    help = """ESSIF Use Cases.

        ESSIF functions & flows."""
) {
    override fun run() {}
}

class EssifOnboardingCommand : CliktCommand(
    name = "onboard",
    help = """ESSIF Onboarding flow

        ESSIF onboarding flow"""
) {

    val keyId: String by option("-k", "--key-id", help = "Key ID or key alias").default("0ec07d2f853c4b00bd701a6124f1e4c3")
    val did: String by option("-d", "--did", help = "DID to be onboarded").default("did:ebsi:26wnek36z4djq1fdCgTZLTuRCe9gMf5Cr6FG8chyuaEBR4fT")

    override fun run() {

        echo("ESSIF onboarding of DID $did ...\n")

        // Use following key for testing
        // {"kty":"EC","use":"sig","crv":"secp256k1","kid":"0ec07d2f853c4b00bd701a6124f1e4c3","x":"Cyb12xp1x7LfaulXdDkDovXXiAJtR4xPjGQiH9B6lcw","y":"nNV-RFkLeFefO5dM2lOybYebr8qFCi3grdV7fTQTKgo","alg":"ES256K"}
//        val priv = "MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgNMQgxHfsmrHkxXTqj1kh" +
//                "T61DmhEFMHYfdLxwxLhh0OygBwYFK4EEAAqhRANCAAQLJvXbGnXHst9q6Vd0OQOi" +
//                "9deIAm1HjE+MZCIf0HqVzJzVfkRZC3hXnzuXTNpTsm2Hm6/KhQot4K3Ve300EyoK"
//        val pub = "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAECyb12xp1x7LfaulXdDkDovXXiAJtR4xP" +
//                "jGQiH9B6lcyc1X5EWQt4V587l0zaU7Jth5uvyoUKLeCt1Xt9NBMqCg"
//        val key = buildKey("0ec07d2f853c4b00bd701a6124f1e4c3", KeyAlgorithm.ECDSA_Secp256k1.name, "SUN", pub, priv)


        EssifFlowRunner.onboard(did)

        echo("ESSIF onboarding for DID $did was performed successfully.")
        echo("The Verifiable Authorization can be accessed in file: ${EssifFlowRunner.verifiableAuthorizationFile.absolutePath}.")
    }
}

class EssifAuthCommand : CliktCommand(
    name = "auth-api",
    help = """ESSIF Authorization flow

        ESSIF Authorization flow"""
) {
    override fun run() = EssifFlowRunner.authApi()
}

class EssifVcIssuanceCommand : CliktCommand(
    name = "vc-issuance",
    help = """ESSIF VC issuance flow

        ESSIF VC issuance flow"""
) {
    override fun run() = EssifFlowRunner.vcIssuance()
}

class EssifVcExchangeCommand : CliktCommand(
    name = "vc-exchange",
    help = """ESSIF VC exchange flow

        ESSIF VC exchange flow"""
) {
    override fun run() = EssifFlowRunner.vcExchange()
}

class EssifDidCommand : CliktCommand(
    name = "did",
    help = """ESSIF DID operations.

        ESSIF DID operations."""
) {
    override fun run() = TODO("The \"ESSIF-DID\" operation has not yet been implemented in this Let's Trust snapshot (currently running ${Values.version}).")
}

class EssifTirCommand : CliktCommand(
    name = "tir",
    help = """ESSIF Trusted Issuer Registry operations.

        ESSIF DID operations."""
) {
    override fun run() = TODO("The \"ESSIF-TIR\" operation has not yet been implemented in this Let's Trust snapshot (currently running ${Values.version}).")
}

class EssifTaorCommand : CliktCommand(
    name = "taor",
    help = """ESSIF Trusted Accreditation Organization operations.

        ESSIF Trusted Accreditation Organization operations."""
) {
    override fun run() = TODO("The \"ESSIF-TAOR\" operation has not yet been implemented in this Let's Trust snapshot (currently running ${Values.version}).")
}

class EssifTsrCommand : CliktCommand(
    name = "tsr",
    help = """ESSIF Trusted Schema Registry operations.

        ESSIF Trusted Schema Registry operations."""
) {
    override fun run() = TODO("The \"ESSIF-TSR\" operation has not yet been implemented in this Let's Trust snapshot (currently running ${Values.version}).")
}
