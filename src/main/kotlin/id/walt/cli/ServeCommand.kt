package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import id.walt.signatory.SignatoryRestAPI
import id.walt.rest.RestAPI

class ServeCommand : CliktCommand(
    name = "serve",
    help = """Run the walt.id SSI KIT as RESTful service.

        Runs the library as RESTful service and exposes following APIs on the specified bind address (default: ${RestAPI.BIND_ADDRESS}):
         
         - walt.id Core API at the specified API port (default: ${RestAPI.CORE_API_PORT})
                  
         - walt.id ESSIF API at the specified ESSIF API port (default: ${RestAPI.ESSIF_API_PORT})
         
         - walt.id Signatory API at the specified Signatory API port (default: ${SignatoryRestAPI.SIGNATORY_API_PORT})
         
         Additional API target servers can be specified using the -s option.
         """
) {
    val apiPort: Int by option(help = "Core API port, default: ${RestAPI.CORE_API_PORT}", names = *arrayOf("-p", "--port")).int().default(RestAPI.CORE_API_PORT)
    val essifPort: Int by option(help = "Essif API port, default: ${RestAPI.ESSIF_API_PORT}", names = *arrayOf("-e", "--essif-port")).int().default(RestAPI.ESSIF_API_PORT)
    val signatoryPort: Int by option(help = "Signatory API port, default: ${SignatoryRestAPI.SIGNATORY_API_PORT}", names = *arrayOf("-s", "--signatory-port")).int().default(SignatoryRestAPI.SIGNATORY_API_PORT)
    val bindAddress: String by option(help = "Bind address for API service, default: 127.0.0.1", names = *arrayOf("-b", "--bind-address")).default("127.0.0.1")
    val additionalApiServers: List<String> by option(help = "API Server urls, defaults to root context '/'", names = *arrayOf("-S", "--server")).multiple()

    override fun run() {
        RestAPI.start(bindAddress, apiPort, essifPort, additionalApiServers)
        SignatoryRestAPI.start(bindAddress, signatoryPort, additionalApiServers)

        echo()
        echo(" walt.id Core API: http://${bindAddress}:${apiPort}")
        echo(" walt.id ESSIF API: http://${bindAddress}:${essifPort}")
        echo(" walt.id Signatory API: http://${bindAddress}:${signatoryPort}")
        echo()
    }
}
