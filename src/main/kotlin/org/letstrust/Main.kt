package org.letstrust

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import org.letstrust.cli.*

data class CliConfig(var dataDir: String, val properties: MutableMap<String, String>, var verbose: Boolean)

class letstrust : CliktCommand(
    help = """LetsTrust CLI

        The LetsTrust CLI is a command line tool that allows you to onboard and use
        a SSI (Self-Sovereign-Identity) ecosystem. You can generate and register
        W3C Decentralized Identifiers (DIDs) including your public keys & service endpoints 
        as well as issue & verify W3C Verifiable credentials (VCs). 
        
        Example commands are:
        
        docker run -it letstrust key gen --algorithm Secp256k1

        docker run -it letstrust vc verify vc.json
        
        """
) {
    init {
        versionOption("1.0")
    }

    val dataDir: String by option("-d", "--data-dir", help = "Set data directory [./data].")
        .default(".data")

    val config: Map<String, String> by option("-c", "--config", help = "Overrides a config key/value pair.").associate()
    val verbose: Boolean by option("-v", "--verbose", help = "Enables verbose mode.")
        .flag()

    override fun run() {
        val config = CliConfig(dataDir, HashMap(), verbose)
        for ((k, v) in this.config) {
            config.properties[k] = v
        }
        currentContext.obj = config

        println("Config loaded: ${config}\n")
    }
}


fun main(args: Array<String>) = letstrust()
    .subcommands(
        key().subcommands(gen(), listKeys(), exportKey()),
        did(),
        vc().subcommands(issue(), verify()),
        auth()
    )
    //.main(arrayOf("-v", "-c", "mykey=myval", "vc", "-h"))
    //.main(arrayOf("vc", "verify", "vc.json"))
    .main(args)
