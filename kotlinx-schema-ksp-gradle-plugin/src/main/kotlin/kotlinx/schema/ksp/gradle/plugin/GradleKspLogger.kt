package kotlinx.schema.ksp.gradle.plugin

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import org.gradle.api.logging.Logger

/**
 * KSP logger implementation that delegates to Gradle's logger.
 *
 * @param logger The Gradle logger to delegate to
 */
internal class GradleKspLogger(
    private val logger: Logger,
) : KSPLogger {
    override fun logging(
        message: String,
        symbol: KSNode?,
    ) {
        logger.lifecycle("KSP: $message")
    }

    override fun info(
        message: String,
        symbol: KSNode?,
    ) {
        logger.info("KSP: $message")
    }

    override fun warn(
        message: String,
        symbol: KSNode?,
    ) {
        logger.warn("KSP: $message")
    }

    override fun error(
        message: String,
        symbol: KSNode?,
    ) {
        logger.error("KSP: $message")
    }

    override fun exception(e: Throwable) {
        logger.error("KSP Exception", e)
    }
}
