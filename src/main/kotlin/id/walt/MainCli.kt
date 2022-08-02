package id.walt

import id.walt.cli.WaltCLI
import id.walt.common.readWhenContent
import id.walt.services.velocitynetwork.VelocityClient
import java.io.File

fun main(args: Array<String>) {
    WaltCLI.start(args)
//    val issuer = "did:ion:EiAD16U5eAJMUulSFMmV3ekV_mQ2rresO9d6mY6Vohg6Hg"
//    val credential = readWhenContent(File("src/main/resources/velocitynetwork/samples/add-offer-req.json"))
//    val token = readWhenContent(File("src/main/resources/velocitynetwork/samples/agent-token.txt"))
//    val uri = VelocityClient.issue(issuer, credential, token)
}
