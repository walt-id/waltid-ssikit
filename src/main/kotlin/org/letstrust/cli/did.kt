package org.letstrust.cli


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.letstrust.CliConfig
import org.letstrust.DidService
import java.io.File

class did : CliktCommand(
    help = """Decentralized Identifiers (DIDs).

        DID related operations, like registering, updating and deactivating DIDs.
        
        Supported DID methods are "key", "web" and "ebsi"""
) {

    override fun run() {

    }
}

class createDid : CliktCommand(
    name = "create",
    help = """Create DID.

        Generates an asymetric keypair and register the DID containing the public key.
        
        """
) {
    val didService = DidService
    val config: CliConfig by requireObject()
    val dest: File? by argument().file().optional()
    val method: String by option("--did-method", "-m", help = "Specifiy DID method [key, web, ebsi]").choice("key", "web", "ebsi").prompt()
    val keyAlias: String by option("--key-alias", "-a", help = "Specific key alias").prompt()

    override fun run() {
        val destName = dest?.name ?: "did.json"
        echo("\nRegistering did:${method} (key: ${keyAlias}) and saving it to ${File(config.dataDir + "/" + destName).absolutePath}")

        val did = didService.createDid(method)

        echo("\ncreated: ${did}")

        val didDoc = didService.resolveDid(did)

        val didDocEncoded = Json { prettyPrint = true }.encodeToString(didDoc)

        echo("\nresolved: ${didDocEncoded}")

        if (config.verbose) {
            echo("Lets make some noise here ...")
        }
    }
}

class resolveDid : CliktCommand(
    name = "resolve",
    help = """Resolve DID.

        Constructs the DID Document."""
) {

    val did: String by option(help = "DID to be resolved").required()

    override fun run() {
        echo("Resolving ${did}")
    }
}

class listDids : CliktCommand(
    name = "list",
    help = """List DIDs

        List all created DIDs."""
) {

    override fun run() {
        echo("List DIDs")
    }
}
