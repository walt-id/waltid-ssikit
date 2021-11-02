package id.walt

import deltadao.DeltaDao
import id.walt.cli.WaltCLI

fun main(args: Array<String>) {
    DeltaDao.registerDeltaDaoCredentials()

    DeltaDao.registerDeltaDaoDataProvider()

    WaltCLI.start(args)
}
