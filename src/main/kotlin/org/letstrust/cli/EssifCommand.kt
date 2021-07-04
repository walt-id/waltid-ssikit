package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import org.letstrust.Values
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
    override fun run() = EssifFlowRunner.onboard()
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
    override fun run() =
        TODO("The \"ESSIF-DID\" operation has not yet been implemented in this Let's Trust snapshot (currently running ${Values.version}).")
}

class EssifTirCommand : CliktCommand(
    name = "tir",
    help = """ESSIF Trusted Issuer Registry operations.

        ESSIF DID operations."""
) {
    override fun run() =
        TODO("The \"ESSIF-TIR\" operation has not yet been implemented in this Let's Trust snapshot (currently running ${Values.version}).")
}

class EssifTaorCommand : CliktCommand(
    name = "taor",
    help = """ESSIF Trusted Accreditation Organization operations.

        ESSIF Trusted Accreditation Organization operations."""
) {
    override fun run() =
        TODO("The \"ESSIF-TAOR\" operation has not yet been implemented in this Let's Trust snapshot (currently running ${Values.version}).")
}

class EssifTsrCommand : CliktCommand(
    name = "tsr",
    help = """ESSIF Trusted Schema Registry operations.

        ESSIF Trusted Schema Registry operations."""
) {
    override fun run() =
        TODO("The \"ESSIF-TSR\" operation has not yet been implemented in this Let's Trust snapshot (currently running ${Values.version}).")
}
