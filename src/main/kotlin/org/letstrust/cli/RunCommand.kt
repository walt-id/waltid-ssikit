package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import org.letstrust.rest.RestAPI

class RunCommand : CliktCommand(

    help = """Run RESTful service.

        Runs the library as RESTful service."""
) {
    override fun run() {
        RestAPI.start()

        echo("\n LetsTrust Core API: http://localhost:${RestAPI.CORE_API_PORT}")
        echo(" LetsTrust ESSIF API: http://localhost:${RestAPI.ESSIF_API_PORT}")
    }
}
