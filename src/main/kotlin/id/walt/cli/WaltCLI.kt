package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import id.walt.Values
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.WaltIdServices
import mu.KotlinLogging

data class CliConfig(var dataDir: String, val properties: MutableMap<String, String>, var verbose: Boolean)

private val log = KotlinLogging.logger {}

class Walt : CliktCommand(
    name = "walt",
    help = """SSI Kit by walt.id

        The SSI Kit by walt.id is a command line tool that allows you to onboard and use
        a SSI (Self-Sovereign-Identity) ecosystem. You can manage cryptographic keys, 
        generate and register W3C Decentralized Identifiers (DIDs) as well as create, 
        issue & verify W3C Verifiable credentials (VCs). 
        
        Example commands are:
        
        ./ssikit.sh key gen --algorithm Secp256k1

        docker run -itv ${'$'}(pwd)/data:/app/data waltid/ssikit did create -m key
        
        """
) {
    init {
        versionOption(Values.version, message = {
            """
            SSI Kit: $it${if (Values.isSnapshot) " - SNAPSHOT VERSION, use only for demo and testing purposes" else " - stable release"}
            Environment: ${System.getProperty("java.runtime.name")} of ${System.getProperty("java.vm.name")} (${
                System.getProperty(
                    "java.version.date"
                )
            })
            OS version:  ${System.getProperty("os.name")} ${System.getProperty("os.version")}
        """.trimIndent()
        })
    }

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

object WaltCLI {

    fun start(args: Array<String>) {

        WaltIdServices

        try {

            log.debug { "SSI Kit CLI starting..." }

            ServiceMatrix("service-matrix.properties")

            Walt()
                .subcommands(
                    KeyCommand().subcommands(
                        GenKeyCommand(),
                        ListKeysCommand(),
                        ImportKeyCommand(),
                        ExportKeyCommand(),
                        DeleteKeyCommand()
                    ),
                    DidCommand().subcommands(
                        CreateDidCommand(),
                        ResolveDidCommand(),
                        ListDidsCommand(),
                        ImportDidCommand(),
                        DeleteDidCommand()
                    ),
                    VcCommand().subcommands(
                        VcIssueCommand(),
                        PresentVcCommand(),
                        VerifyVcCommand(),
                        ListVcCommand(),
                        VerificationPoliciesCommand().subcommands(
                            ListVerificationPoliciesCommand(),
                            CreateDynamicVerificationPolicyCommand(),
                            RemoveDynamicVerificationPolicyCommand()
                        ),
                        VcTemplatesCommand().subcommands(
                            VcTemplatesListCommand(),
                            VcTemplatesExportCommand()
                        ),
                        VcImportCommand()
                    ),
                    EssifCommand().subcommands(
                        EssifOnboardingCommand(),
                        EssifAuthCommand(),
//                        EssifVcIssuanceCommand(),
//                        EssifVcExchangeCommand(),
                        EssifDidCommand().subcommands(
                            EssifDidRegisterCommand()
                        ),
                        EssifTirCommand().subcommands(
                            EssifTirGetIssuerCommand()
                        ),
                        EssifTimestampCommand().subcommands(
                            EssifTimestampCreateCommand(),
                            EssifTimestampGetCommand()
                        ),
                        EssifTaorCommand(),
                        EssifTsrCommand()
                    ),
                    OidcCommand().subcommands(
                        OidcIssuanceCommand().subcommands(
                            OidcIssuanceInfoCommand(),
                            OidcIssuanceNonceCommand(),
                            OidcIssuanceAuthCommand(),
                            OidcIssuanceTokenCommand(),
                            OidcIssuanceCredentialCommand()
                        ),
                        OidcVerificationCommand().subcommands(
                            OidcVerificationGetUrlCommand(),
                            OidcVerificationGenUrlCommand(),
                            OidcVerificationParseCommand(),
                            OidcVerificationRespondCommand()
                        )
                    ),
                    VelocityCommand().subcommands(
                        VelocityOnboardingCommand().subcommands(
                            VelocityOrganizationCommand(),
                            VelocityTenantCommand(),
                            VelocityDisclosureCommand()
                        ),
                        VelocityIssueCommand(),
                        VelocityOfferCommand(),
                        VelocityVerifyCommand(),

                    ),
                    ServeCommand()
                )
                .main(args)

        } catch (e: Exception) {
            TermUi.echo(e.message)

            if (log.isDebugEnabled)
                e.printStackTrace()
        }
    }
}
