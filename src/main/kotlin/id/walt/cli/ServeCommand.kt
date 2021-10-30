package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import id.walt.auditor.AuditorRestAPI
import id.walt.rest.core.CoreAPI
import id.walt.rest.custodian.CustodianAPI
import id.walt.rest.essif.EssifAPI
import id.walt.signatory.rest.SignatoryRestAPI

/**
 * CLI Command to run the walt.id SSI KIT as RESTful service.
 */
class ServeCommand : CliktCommand(
    name = "serve",
    help = """Run the walt.id SSI Kit as RESTful service.

        Runs the library as RESTful service and exposes following APIs on the specified bind address (default: ${CoreAPI.DEFAULT_BIND_ADDRESS}):
         
         - walt.id Core API at the specified API port (default: ${CoreAPI.DEFAULT_CORE_API_PORT})
                           
         - walt.id Signatory API at the specified Signatory API port (default: ${SignatoryRestAPI.SIGNATORY_API_PORT})
         
         - walt.id Auditor API at the specified Auditor API port (default: ${AuditorRestAPI.AUDITOR_API_PORT})
         
         - walt.id ESSIF API at the specified ESSIF API port (default: ${EssifAPI.DEFAULT_ESSIF_API_PORT})         
         
         Additional API target servers can be specified using the -t option.
         """
) {
    private val apiPort: Int by option(help = "Core API port [${CoreAPI.DEFAULT_CORE_API_PORT}]", names = arrayOf("-p", "--port")).int().default(
        CoreAPI.DEFAULT_CORE_API_PORT)
    private val signatoryPort: Int by option(help = "Signatory API port [${SignatoryRestAPI.SIGNATORY_API_PORT}]", names = arrayOf("-s", "--signatory-port")).int().default(
        SignatoryRestAPI.SIGNATORY_API_PORT)
    private val custodianPort: Int by option(help = "Custodian API port [${CustodianAPI.DEFAULT_Custodian_API_PORT}]", names = arrayOf("-c", "--custodian-port")).int().default(
        CustodianAPI.DEFAULT_Custodian_API_PORT)
    private val auditorPort: Int by option(help = "Auditor API port [${AuditorRestAPI.AUDITOR_API_PORT}]", names = arrayOf("-a", "--auditor-port")).int().default(AuditorRestAPI.AUDITOR_API_PORT)
    private val essifPort: Int by option(help = "Essif API port [${EssifAPI.DEFAULT_ESSIF_API_PORT}]", names = arrayOf("-e", "--essif-port")).int().default(
        EssifAPI.DEFAULT_ESSIF_API_PORT)
    private val bindAddress: String by option(help = "Bind address for API service [127.0.0.1]", names = arrayOf("-b", "--bind-address")).default("127.0.0.1")
    private val apiTargetUrls: List<String> by option(help = "Additional API target urls for swagger UI, defaults to root context '/'", names = arrayOf("-t", "--target-url")).multiple()

    override fun run() {
        CoreAPI.start(apiPort, bindAddress, apiTargetUrls)
        SignatoryRestAPI.start(signatoryPort, bindAddress, apiTargetUrls)
        CustodianAPI.start(custodianPort, bindAddress, apiTargetUrls)
        AuditorRestAPI.start(auditorPort, bindAddress, apiTargetUrls)
        EssifAPI.start(essifPort, bindAddress, apiTargetUrls)

        echo(" walt.id Core API:      http://${bindAddress}:${apiPort}")
        echo(" walt.id Signatory API: http://${bindAddress}:${signatoryPort}")
        echo(" walt.id Custodian API: http://${bindAddress}:${custodianPort}")
        echo(" walt.id Auditor API:   http://${bindAddress}:${auditorPort}")
        echo(" walt.id ESSIF API:     http://${bindAddress}:${essifPort}")
    }
}
