package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import org.letstrust.EssifService

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
        EssifService.authenticate()
    }
}
