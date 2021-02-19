package org.letstrust

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.LoggerConfig
import org.letstrust.cli.*
import java.io.File


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

    val dataDir: String by option("-d", "--data-dir", help = "Set data directory [./data].")
        .default("data")

    val cliConfig: Map<String, String> by option(
        "-c",
        "--config",
        help = "Overrides a config key/value pair."
    ).associate()
    val verbose: Boolean by option("-v", "--verbose", help = "Enables verbose mode.")
        .flag()

    override fun run() {
        val config = CliConfig(dataDir, HashMap(), verbose)

        this.cliConfig.forEach { (k, v) ->
            config.properties[k] = v
        }

        currentContext.obj = config

        println("Config loaded: ${config}\n")

        val dataDirFile = File(dataDir)
        if (!dataDirFile.exists()) {
            log.info { "Creating data dir at \"${dataDir}\"..." }
            dataDirFile.mkdirs()
        }
    }
}


fun main(args: Array<String>) {
    log.trace { "trace" }
    log.debug { "debug" }
    log.info { "info" }
    log.warn { "warn" }
    log.error { "error" }

    val ctx: LoggerContext = LogManager.getContext(false) as LoggerContext
    val logConf: Configuration = ctx.configuration
    val logConfig: LoggerConfig = logConf.getLoggerConfig(LogManager.ROOT_LOGGER_NAME)

    args.forEach {
        if (it.contains("-v") || it.contains("--verbose")) {
            logConfig.level = Level.TRACE
        }
    }

    ctx.updateLoggers()

    log.debug { "Let's Trust CLI started" }

    log.trace { "trace" }
    log.debug { "debug" }
    log.info { "info" }
    log.warn { "warn" }
    log.error { "error" }

    return LetsTrust()
        .subcommands(
            KeyCommand().subcommands(GenCommand(), ListKeysCommand(), ExportKeyCommand()),
            Did().subcommands(CreateDidCommand(), ResolveDidCommand(), ListDidsCommand()),
            VerifiableCredentialsCommand().subcommands(IssueCommand(), VerifyCommand()),
            Auth()
        )
        //.main(arrayOf("-v", "-c", "mykey=myval", "vc", "-h"))
        //.main(arrayOf("vc", "verify", "vc.json"))
        .main(args)
}
