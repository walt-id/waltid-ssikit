package org.letstrust.cli


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import org.letstrust.CliConfig
import java.io.File

class did : CliktCommand(
    help = """Decentralized Identifiers (DIDs).

        DID related operations, like registering, updating and deactivating DIDs.
        
        Supported DID methods are "key", "web" and "ebsi"""
) {
    val config: CliConfig by requireObject()
    val dest: File? by argument().file().optional()
    val method: String by option("--did-method", "-m", help = "Specifiy DID method [key, web, ebsi]").choice("key", "web", "ebsi").prompt()
    val keyAlias: String by option("--key-alias", "-a", help = "Specific key alias").prompt()

    override fun run() {
        val destName = dest?.name ?: "did.json"
        echo("\nRegistering did:${method} (key: ${keyAlias}) and saving it to ${File(config.dataDir + "/" + destName).absolutePath}")

        if (config.verbose) {
            echo("Lets make some noise here ...")
        }
    }
}
