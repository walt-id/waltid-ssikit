import mu.KotlinLogging
import org.junit.Test
import org.slf4j.LoggerFactory;

private val log = KotlinLogging.logger {}

class CliTest {

    @Test
    fun loggingTest() {
        println(log.isTraceEnabled())

        log.trace { "trace" }
        log.debug { "debug" }
        log.info { "info" }
        log.warn { "warn" }
        log.error { "error" }

        try {
            throw RuntimeException("test")
        } catch (ex: Exception) {
            log.error(ex) { "test-exception caught" }
        }
    }
}
