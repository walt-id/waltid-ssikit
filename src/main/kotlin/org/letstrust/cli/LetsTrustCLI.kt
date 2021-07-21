package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.letstrust.services.LetsTrustServices
import org.letstrust.Values


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

        docker run -itv ${'$'}(pwd)/data:/opt/data letstrust did create -m web
        
        """
) {
    init {
        versionOption(Values.version, message = {
            """
            Let's Trust: $it${if (Values.isSnapshot) " - SNAPSHOT VERSION, use only for demo and testing purposes)" else " - stable release"}
            Environment: ${System.getProperty("java.runtime.name")} of ${System.getProperty("java.vm.name")} (${
                System.getProperty(
                    "java.version.date"
                )
            })
            OS version:  ${System.getProperty("os.name")} ${System.getProperty("os.version")}
        """.trimIndent()
        })
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

        LetsTrustServices

        try {

            log.debug { "Let's Trust CLI starting..." }

            if (args.any { it == "--verbose" || it == "-v" }) {
                LetsTrustServices.setLogLevel(Level.DEBUG)
            }

            LetsTrust()
                .subcommands(
                    KeyCommand().subcommands(
                        GenCommand(),
                        ListKeysCommand(),
                        ImportKeyCommand(),
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
                        ListVcCommand(),
                        VcTemplatesCommand().subcommands(
                            ListVcTemplateCommand(),
                            ExportVcTemplateCommand()
                        )
                    ),
                    //AuthCommand(),
                    EssifCommand().subcommands(
                        EssifOnboardingCommand(),
                        EssifAuthCommand(),
                        EssifVcIssuanceCommand(),
                        EssifVcExchangeCommand(),
                        EssifDidCommand().subcommands(
                            EssifDidRegisterCommand()
                        ),
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
