package org.letstrust

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.sksamuel.hoplite.ConfigFilePropertySource
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.ConfigSource
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hikari.HikariDataSourceDecoder
import com.sksamuel.hoplite.parsers.defaultParserRegistry
import com.sksamuel.hoplite.yaml.YamlParser
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.LoggerConfig
import org.letstrust.cli.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


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

        // TODO: move this to some central config-service
        log.debug { "Creating dir-structure at: ${config.dataDir}" }
        Files.createDirectories(Path.of("${config.dataDir}/did/created"))
        Files.createDirectories(Path.of("${config.dataDir}/did/resolved"))
        Files.createDirectories(Path.of("${config.dataDir}/vc/templates"))
        Files.createDirectories(Path.of("${config.dataDir}/vc/created"))
        Files.createDirectories(Path.of("${config.dataDir}/vc/presented"))
        Files.createDirectories(Path.of("${config.dataDir}/ebsi/"))
    }
}


fun main(args: Array<String>) {

    args.forEach {
        if (it.contains("-v") || it.contains("--verbose")) {
            LetsTrustServices.setLogLevel(Level.TRACE)
        }
    }

    log.debug { "Let's Trust CLI started" }

    return LetsTrust()
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
}
