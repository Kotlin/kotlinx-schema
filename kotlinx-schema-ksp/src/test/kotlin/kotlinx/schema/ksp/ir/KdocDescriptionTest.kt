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

    @Test
    fun `Should return null for null kdoc`() {
        val result = extractDescriptionFromKdoc(null)
        result shouldBe null
    }

    @Test
    fun `Should return null for empty kdoc`() {
        val result = extractDescriptionFromKdoc("")
        result shouldBe null
    }

    @Test
    fun `Should return null for blank kdoc`() {
        val result = extractDescriptionFromKdoc("   \n  \n   ")
        result shouldBe null
    }

    @Test
    fun `Should extract single line description`() {
        val kdoc = "Single line description"
        val result = extractDescriptionFromKdoc(kdoc)
        result shouldBe "Single line description"
    }

    @Test
    fun `Should stop at first tag`() {
        val kdoc =
            """
            |First line
            |Second line
            |@param test
            |More text after param
            """.trimMargin()

        val result = extractDescriptionFromKdoc(kdoc)
        result shouldBe "First line\nSecond line"
    }

    @Test
    fun `Should filter out empty lines`() {
        val kdoc =
            """
            |First line
            |
            |
            |Third line
            |
            |@param test
            """.trimMargin()

        val result = extractDescriptionFromKdoc(kdoc)
        result shouldBe "First line\nThird line"
    }

    @Test
    fun `Should trim whitespace from lines`() {
        val kdoc =
            """
            |  First line with spaces
            |    Second line with more spaces
            |@param test
            """.trimMargin()

        val result = extractDescriptionFromKdoc(kdoc)
        result shouldBe "First line with spaces\nSecond line with more spaces"
    }

    @Test
    fun `Should handle kdoc with only tags`() {
        val kdoc =
            """
            |@param test
            |@return value
            """.trimMargin()

        val result = extractDescriptionFromKdoc(kdoc)
        result shouldBe null
    }

    @Test
    fun `Should handle kdoc with tags starting immediately`() {
        val kdoc = "@param test starts immediately"
        val result = extractDescriptionFromKdoc(kdoc)
        result shouldBe null
    }

    @Test
    fun `Should handle multiline description without tags`() {
        val kdoc =
            """
            |First line
            |Second line
            |Third line
            |Fourth line
            """.trimMargin()

        val result = extractDescriptionFromKdoc(kdoc)
        result shouldBe "First line\nSecond line\nThird line\nFourth line"
    }
}
