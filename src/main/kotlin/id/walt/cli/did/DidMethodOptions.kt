package id.walt.cli.did

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int

sealed class DidMethodOption(val method: String) : OptionGroup()
class WebMethodOption : DidMethodOption("web") {
    val domain: String by option("-d", "--domain", help = "did:web - Domain for did:web").default("walt.id")
    val path: String? by option("-p", "--path", help = "did:web - Path for did:web")
}

class EbsiMethodOption : DidMethodOption("ebsi") {
    val version: Int by option("-v", "--version", help = "did:ebsi - Version of did:ebsi. Allowed values: 1 (default), 2").int().default(1)
}

class IotaMethodOption : DidMethodOption("iota")
class JwkMethodOption : DidMethodOption("jwk")
class CheqdMethodOption : DidMethodOption("cheqd") {
    val network: String by option("-n", "--network", help = "did:cheqd - Specify the network [testnet]").choice("mainnet", "testnet").default("testnet")
}
class KeyMethodOption : DidMethodOption("key") {
    val useJwkJcsPubMulticodec: Boolean by option("-j", "--useJwkJcsPub", help = "did:key - use jwk_jcs-pub multicodec (0xeb51)").flag(default = false)
}
