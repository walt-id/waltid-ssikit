package id.walt

import com.github.ajalt.clikt.output.TermUi.echo
import id.walt.auditor.AuditorRestAPI
import id.walt.rest.CoreAPI
import id.walt.rest.CustodianAPI
import id.walt.rest.EssifAPI
import id.walt.signatory.SignatoryRestAPI

fun main() {
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
