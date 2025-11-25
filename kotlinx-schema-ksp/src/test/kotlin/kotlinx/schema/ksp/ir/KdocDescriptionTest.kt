package kotlinx.schema.ksp.ir

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class KdocDescriptionTest {
    @Test
    fun `Should extract class description`() {
        val kdoc =
            """
            | This is an example kdoc.
            |
            | It has two lines
            |
            | @param foo is skipped
            | @property bar is skipped
            """.trimMargin()

        val result = extractDescriptionFromKdoc(kdoc)
        result shouldBe "This is an example kdoc.\nIt has two lines"
    }
}
