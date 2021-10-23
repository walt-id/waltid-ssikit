package id.walt

import com.github.ajalt.clikt.output.TermUi.echo
import id.walt.auditor.AuditorRestAPI
import id.walt.rest.core.CoreAPI
import id.walt.rest.custodian.CustodianAPI
import id.walt.rest.essif.EssifAPI
import id.walt.servicematrix.ServiceMatrix
import id.walt.signatory.rest.SignatoryRestAPI
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

fun main() {

    log.debug { "SSI Kit starting to serve REST APIs" }

    ServiceMatrix("service-matrix.properties")

    val bindAddress = "127.0.0.1"
    CoreAPI.start(7000, bindAddress)
    SignatoryRestAPI.start(7001, bindAddress)
    CustodianAPI.start(7002, bindAddress)
    AuditorRestAPI.start(7003, bindAddress)
    EssifAPI.start(7004, bindAddress)

    echo(" walt.id Core API:      http://${bindAddress}:7000")
    echo(" walt.id Signatory API: http://${bindAddress}:7001")
    echo(" walt.id Custodian API: http://${bindAddress}:7002")
    echo(" walt.id Auditor API:   http://${bindAddress}:7003")
    echo(" walt.id ESSIF API:     http://${bindAddress}:7004")
}
