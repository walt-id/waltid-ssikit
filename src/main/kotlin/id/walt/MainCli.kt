package id.walt

import deltadao.DeltaDao
import id.walt.cli.WaltCLI

fun main(args: Array<String>) {
    DeltaDao.registerDeltaDaoCredentials()

    WaltCLI.start(args)
}
