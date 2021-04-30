package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import mu.KotlinLogging
import org.letstrust.LetsTrustServices
import org.apache.logging.log4j.Level


data class CliConfig(var dataDir: String, val properties: MutableMap<String, String>, var verbose: Boolean)

private val log = KotlinLogging.logger {}

class LetsTrust : CliktCommand(
    name = "letstrust",
    help = """LetsTrust CLI

        The LetsTrust CLI is a command line tool that allows you to onboard and use
        a SSI (Self-Sovereign-Identity) ecosystem. You can generate and register
        W3C Decentralized Identifiers (DIDs) including your public keys & service endpoints 
        as well as issue & verify W3C Verifiable credentials (VCs). 
        
        Example commands are:
        
        ./letstrust key gen --algorithm Secp256k1

        docker run -it -v ${'$'}(pwd)/data:/opt/data letstrust did create -m web
        
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

        config.properties.putAll(this.cliConfig)

        currentContext.obj = config

        if (config.verbose) {
            log.debug { "Config loaded: $config" }
        }
    }
}

object LetsTrustCLI {

    fun start(args: Array<String>) {

        try {

            log.debug { "Let's Trust CLI starting..." }

            args.forEach {
                if (it.contains("-v") || it.contains("--verbose")) {
                    LetsTrustServices.setLogLevel(Level.TRACE)
                }
            }

            LetsTrust()
                .subcommands(
                    KeyCommand().subcommands(
                        GenCommand(),
                        ListKeysCommand(),
                        ExportKeyCommand()
                    ),
                    DidCommand().subcommands(
                        CreateDidCommand(),
                        ResolveDidCommand(),
                        ListDidsCommand()
                    ),
                    VerifiableCredentialsCommand().subcommands(
                        IssueVcCommand(),
                        PresentVcCommand(),
                        VerifyVcCommand(),
                        ListVcCommand()
                    ),
                    //AuthCommand(),
                    EssifCommand().subcommands(
                        EssifOnboardingCommand(),
                        EssifAuthCommand(),
                        EssifVcIssuanceCommand(),
                        EssifVcExchangeCommand(),
                        EssifDidCommand(),
                        EssifTirCommand(),
                        EssifTaorCommand(),
                        EssifTsrCommand()
                    ),
                    RunCommand()
                )
                //.org.letstrust.examples.org.letstrust.examples.org.letstrust.examples.main(arrayOf("-v", "-c", "mykey=myval", "vc", "-h"))
                //.org.letstrust.examples.org.letstrust.examples.org.letstrust.examples.main(arrayOf("vc", "verify", "vc.json"))
                .main(args)

            log.debug { "Let's Trust CLI started." }
        } catch (e: Exception) {
            TermUi.echo(e.message)
            log.debug { e.printStackTrace() }
        }
    }
}
