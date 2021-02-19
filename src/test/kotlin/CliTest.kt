import mu.KotlinLogging
import org.junit.Test
import org.slf4j.LoggerFactory;

private val logger = KotlinLogging.logger {}

class CliTest {

    @Test
    fun loggintTest() {

        val log = LoggerFactory.getLogger("CliTest");

        println(logger.isTraceEnabled())
        logger.trace { "trace" }
        logger.debug { "debug" }
        logger.info { "info" }
        logger.error { "error" }

        try {
            throw RuntimeException("test")
        } catch (ex: Exception) {
            logger.error(ex) { "test-exception caught" }
        }


    }
}
