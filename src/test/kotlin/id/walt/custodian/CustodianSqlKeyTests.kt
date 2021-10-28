package id.walt.custodian

import id.walt.custodian.CustodianKeyTestUtils.standardKeyTests
import id.walt.servicematrix.ServiceMatrix
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.keystore.KeyStoreService
import id.walt.services.keystore.SqlKeyStoreService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.StringSpec

class CustodianSqlKeyTests : StringSpec({

    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    val custodian = Custodian.getService()

    ServiceRegistry.registerService<KeyStoreService>(SqlKeyStoreService())
    standardKeyTests(custodian)
})
