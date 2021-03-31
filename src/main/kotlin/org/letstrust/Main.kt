package org.letstrust

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.letstrust.cli.*


data class CliConfig(var dataDir: String, val properties: MutableMap<String, String>, var verbose: Boolean)

private val log = KotlinLogging.logger {}

class LetsTrust : CliktCommand(
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

//    val dataDir: String by option("-d", "--data-dir", help = "Set data directory [./data].")
//        .default("data")

    val cliConfig: Map<String, String> by option(
        "-c",
        "--config",
        help = "Overrides a config key/value pair."
    ).associate()
    val verbose: Boolean by option("-v", "--verbose", help = "Enables verbose mode.")
        .flag()

    override fun run() {
        val config = CliConfig("data", HashMap(), verbose)

        this.cliConfig.forEach { (k, v) ->
            config.properties[k] = v
        }

        currentContext.obj = config

        if (config.verbose) {
            log.debug { "Config loaded: ${config}" }
        }
    }
}


fun main(args: Array<String>) {
    try {
        args.forEach {
            if (it.contains("-v") || it.contains("--verbose")) {
                LetsTrustServices.setLogLevel(Level.TRACE)
            }
        }

        log.debug { "Let's Trust CLI started" }

        LetsTrust()
            .subcommands(
                KeyCommand().subcommands(GenCommand(), ListKeysCommand(), ExportKeyCommand()),
                DidCommand().subcommands(CreateDidCommand(), ResolveDidCommand(), ListDidsCommand()),
                VerifiableCredentialsCommand().subcommands(IssueVcCommand(), PresentVcCommand(), VerifyVcCommand(), ListVcCommand()),
                AuthCommand(),
                EssifCommand().subcommands(EssifAuthCommand(), EssifDidCommand(), EssifTirCommand(), EssifTaorCommand(), EssifTsrCommand())
            )
            //.main(arrayOf("-v", "-c", "mykey=myval", "vc", "-h"))
            //.main(arrayOf("vc", "verify", "vc.json"))
            .main(args)
    } catch (e: Exception) {
        echo(e.message)
        log.debug { e.printStackTrace() }
    }

}
