package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import org.letstrust.EssifService

class EssifCommand : CliktCommand(
    name = "essif",
    help = """ESSIF Use Cases.

        ESSIF functions & flows."""
) {
    override fun run() {}
}

class EssifAuthCommand : CliktCommand(
    name = "auth",
    help = """ESSIF Auth

        ESSIF Authorization flow"""
) {
    override fun run() {
        EssifService.authenticate()
    }
}
