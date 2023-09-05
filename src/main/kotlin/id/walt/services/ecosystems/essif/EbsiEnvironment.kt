package id.walt.services.ecosystems.essif

var EBSI_ENV = System.getenv()["EBSI_ENV"] ?: "test"

object EbsiEnvironment {
    private const val pilotUrl = "https://api-pilot.ebsi.eu"
    private const val testUrl = "https://api-test.ebsi.eu"
    private const val conformanceUrl = "https://api-conformance.ebsi.eu"
    private const val localUrl = "http://localhost:8080"

    fun url() = when (EBSI_ENV) {
        "pilot" -> pilotUrl
        "conformance" -> conformanceUrl
        "local" -> localUrl
        else -> testUrl
    }
}
