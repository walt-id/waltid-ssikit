package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.file
import org.letstrust.CliConfig
import java.io.File

class VerifiableCredentialsCommand : CliktCommand(
    help = """Verifiable Credentials (VCs).

        VC related operations like issuing, verifying and revoking VCs

        """
) {

    override fun run() {

        echo("Do some VC magic here ..\n")

    }
}


class IssueCommand : CliktCommand(
    help = """Issues and distributes VC.
        
        """
) {

    val config: CliConfig by requireObject()
    val template: File? by argument().file().optional()
    val dest: File? by argument().file().optional()
    val issuerDid: String by option(
        "--issuer-did",
        "-i",
        help = "Specific DID of the VC subject (receiver of VC)"
    ).prompt()
    val subjectDid: String by option(
        "--subject-did",
        "-s",
        help = "Specific DID of the VC subject (receiver of VC)"
    ).prompt()

    override fun run() {
        echo("\nIssue & send cred ...")
    }
}

class VerifyCommand : CliktCommand(
    help = """Verifies VC.
        
        """
) {

    val src: File? by argument().file()

    override fun run() {
        echo("\nVerify VC form file $src ...")
    }
}
