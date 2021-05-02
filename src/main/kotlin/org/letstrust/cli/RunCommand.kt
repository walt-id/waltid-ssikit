package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import org.letstrust.rest.RestAPI

class RunCommand : CliktCommand(
    name ="serve",
    help = """Run as RESTful service.

        Runs the library as RESTful service and exposes following APIs:
         
         - LetsTrust Core API at http://localhost:${RestAPI.CORE_API_PORT}
                  
         - LetsTrust ESSIF API at http://localhost:${RestAPI.ESSIF_API_PORT}"""
) {
    override fun run() {
        RestAPI.start()

        echo()
        echo(" LetsTrust Core API: http://localhost:${RestAPI.CORE_API_PORT}")
        echo(" LetsTrust ESSIF API: http://localhost:${RestAPI.ESSIF_API_PORT}")
        echo()
    }
}
