package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import id.walt.signatory.SignatoryRestAPI
import id.walt.rest.RestAPI

class ServeCommand : CliktCommand(
    name = "serve",
    help = """Run the walt.id SSI KIT as RESTful service.

        Runs the library as RESTful service and exposes following APIs:
         
         - walt.id Core API at ${RestAPI.API_HOST}:${RestAPI.CORE_API_PORT}
                  
         - walt.id ESSIF API at ${RestAPI.API_HOST}:${RestAPI.ESSIF_API_PORT}
         
         - walt.id Signatory API at ${RestAPI.API_HOST}:${SignatoryRestAPI.SIGNATORY_API_PORT}"""
) {
    override fun run() {
        RestAPI.start()
        SignatoryRestAPI.start()

        echo()
        echo(" walt.id Core API:${RestAPI.coreApiUrl}")
        echo(" walt.id ESSIF API: ${RestAPI.essifApiUrl}")
        echo(" walt.id Signatory API: ${SignatoryRestAPI.signatoryApiUrl}")
        echo()
    }
}
