package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import org.letstrust.services.essif.UserWalletService

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

class EssifAuthCommand : CliktCommand(
    name = "auth",
    help = """ESSIF Authorization flow

        ESSIF Authorization flow"""
) {
    override fun run() {
        //EssifService.authenticate()
        UserWalletService.requestVerifiableAuthorization()
    }
}

class EssifDidCommand : CliktCommand(
    name = "did",
    help = """ESSIF DID operations.

        ESSIF DID operations."""
) {
    override fun run() {}
}

class EssifTirCommand : CliktCommand(
    name = "tir",
    help = """ESSIF Trusted Issuer Registry operations.

        ESSIF DID operations."""
) {
    override fun run() {}
}

class EssifTaorCommand : CliktCommand(
    name = "taor",
    help = """ESSIF Trusted Accreditation Organization operations.

        ESSIF Trusted Accreditation Organization operations."""
) {
    override fun run() {}
}

class EssifTsrCommand : CliktCommand(
    name = "tsr",
    help = """ESSIF Trusted Schema Registry operations.

        ESSIF Trusted Schema Registry operations."""
) {
    override fun run() {}
}
