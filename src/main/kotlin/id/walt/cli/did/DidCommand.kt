package id.walt.cli.did


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import id.walt.cli.CliConfig
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.model.DidMethod.*
import id.walt.model.DidUrl
import id.walt.services.crypto.CryptoService
import id.walt.services.did.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

class DidCommand : CliktCommand(
    help = """Decentralized Identifiers (DIDs).

        DID related operations, like creating, updating and deleted DIDs in the associated data store.
        
        Supported DID methods are "key", "jwk", "web", "ebsi" , "iota", "cheqd""""
) {
    override fun run() {}
}

class CreateDidCommand : CliktCommand(
    name = "create",
    help = """Create DID

        Creates a DID document based on the corresponding SSI ecosystem (DID method). 
        Optionally the associated asymmetric key is also created.

        """
) {
    val config: CliConfig by requireObject()
    val method by option("-m", "--did-method", help = "Specify DID method [key]")
        .groupChoice(
            "key" to KeyMethodOption(),
            "web" to WebMethodOption(),
            "ebsi" to EbsiMethodOption(),
            "iota" to IotaMethodOption(),
            "jwk" to JwkMethodOption(),
            "cheqd" to CheqdMethodOption(),
        ).defaultByName("key")

    val keyAlias: String? by option("-k", "--key", help = "Specific key (ID or alias)")
    val dest: Path? by argument("destination-file").path().optional()

    override fun run() {

        val keyId = keyAlias ?: when (method) {
            is EbsiMethodOption -> CryptoService.getService().generateKey(KeyAlgorithm.ECDSA_Secp256k1).id
            else -> CryptoService.getService().generateKey(KeyAlgorithm.EdDSA_Ed25519).id
        }

        echo("Creating did:${method.method} (key: ${keyId})")

        val did = when (method) {
            is WebMethodOption -> DidWebCreateOptions((method as WebMethodOption).domain, (method as WebMethodOption).path)
            is EbsiMethodOption -> DidEbsiCreateOptions((method as EbsiMethodOption).version)
            is CheqdMethodOption -> DidCheqdCreateOptions((method as CheqdMethodOption).network)
            is KeyMethodOption -> DidKeyCreateOptions((method as KeyMethodOption).useJwkJcsPubMulticodec)
            else -> null
        }.let{
            DidService.create(DidMethod.valueOf(method.method), keyId, it)
        }

        echo("\nResults:\n")
        echo("\nDID created: $did\n")

        val encodedDid = DidService.load(did).encodePretty()
        echo("\nDID document (below, JSON):\n\n$encodedDid")

        dest?.let {
            echo("\nSaving DID to file: ${dest!!.absolutePathString()}")
            dest!!.writeText(encodedDid)
        }

        if (method is WebMethodOption) echo(
            "\nInstall this did:web at: " + DidService.getWebPathForDidWeb(
                (method as WebMethodOption).domain,
                (method as WebMethodOption).path
            )
        )
    }
}

fun resolveDidHelper(did: String, raw: Boolean) = when {
    did.contains("web") -> DidService.resolve(DidUrl.from(did)).encodePretty()
    did.contains("ebsi") -> DidService.resolve(did, DidEbsiResolveOptions(raw)).encodePretty()
    else -> DidService.resolve(did).encodePretty()
}

class ResolveDidCommand : CliktCommand(
    name = "resolve",
    help = """Resolve DID

        Resolves the DID document. Use option RAW to disable type checking."""
) {
    val did: String by option("-d", "--did", help = "DID to be resolved").required()
    val raw by option("--raw", "-r").flag("--typed", "-t", default = false)
    val config: CliConfig by requireObject()
    val write by option("--write", "-w").flag(default = false)

    override fun run() {
        echo("Resolving DID \"$did\"...")

        val encodedDid = resolveDidHelper(did, raw)

        echo("\nResults:\n")
        echo("DID resolved: \"$did\"")
        echo("DID document (below, JSON):\n")

        echo(encodedDid)

        if (write) {
            val didFileName = "${did.replace(":", "-").replace(".", "_")}.json"
            val destFile = File(config.dataDir + "/did/resolved/" + didFileName)
            destFile.writeText(encodedDid)
            echo("\nDID document was saved to file: ${destFile.absolutePath}")
        }
    }
}

class ListDidsCommand : CliktCommand(
    name = "list",
    help = """List DIDs

        List all created DIDs."""
) {
    override fun run() {
        echo("Listing DIDs...")

        echo("\nResults:\n")

        DidService.listDids().forEachIndexed { index, did -> echo("- ${index + 1}: $did") }
    }
}

class ImportDidCommand : CliktCommand(
    name = "import",
    help = "Import DID to custodian store"
) {
    val keyId: String? by option(
        "-k",
        "--key-id",
        help = "Specify key ID for imported did, if left empty, only public key will be imported"
    )
    val didOrDoc: String by mutuallyExclusiveOptions(
        option("-f", "--file", help = "Load the DID document from the given file"),
        option("-d", "--did", help = "Try to resolve DID document for the given DID")
    ).single().required()

    override fun run() {
        val did = when (DidUrl.isDidUrl(didOrDoc)) {
            true -> didOrDoc.also {
                DidService.importDid(didOrDoc)
            }

            else -> DidService.importDidFromFile(File(didOrDoc))
        }

        if (!keyId.isNullOrEmpty()) {
            DidService.setKeyIdForDid(did, keyId!!)
        } else {
            DidService.importKeys(did)
        }

        println("DID imported: $did")
    }
}

class DeleteDidCommand : CliktCommand(
    name = "delete",
    help = "Delete DID to custodian store"
) {
    val did: String by option("-d", "--did", help = "DID to be deleted").required()

    override fun run() {
        echo("Deleting \"$did\"...")

        echo("\nDid deleted:\"$did\"\n")
        DidService.deleteDid(did)
    }
}
