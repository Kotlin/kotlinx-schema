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

    @Test
    fun `Should extract param description from single line tag`() {
        val kdoc =
            """
            |Function description
            |
            |@param name User name to search
            |@param age User age
            """.trimMargin()

        val result = extractParamDescriptionFromKdoc(kdoc, "name")
        result shouldBe "User name to search"
    }

    @Test
    fun `Should extract param description from multi-line tag`() {
        val kdoc =
            """
            |Function description
            |
            |@param name User name to search.
            |            This can be a full name or partial match.
            |            Case insensitive.
            |@param age User age
            """.trimMargin()

        val result = extractParamDescriptionFromKdoc(kdoc, "name")
        result shouldBe "User name to search.\nThis can be a full name or partial match.\nCase insensitive."
    }

    @Test
    fun `Should return null for non-existent param`() {
        val kdoc =
            """
            |@param name User name
            |@param age User age
            """.trimMargin()

        val result = extractParamDescriptionFromKdoc(kdoc, "email")
        result shouldBe null
    }

    @Test
    fun `Should return null for param with null kdoc`() {
        val result = extractParamDescriptionFromKdoc(null, "name")
        result shouldBe null
    }

    @Test
    fun `Should handle param tag with no description`() {
        val kdoc =
            """
            |@param name
            |@param age User age
            """.trimMargin()

        val result = extractParamDescriptionFromKdoc(kdoc, "name")
        result shouldBe null
    }

    @Test
    fun `Should extract property description from single line tag`() {
        val kdoc =
            """
            |Data class description
            |
            |@property name User name
            |@property age User age
            """.trimMargin()

        val result = extractPropertyDescriptionFromKdoc(kdoc, "name")
        result shouldBe "User name"
    }

    @Test
    fun `Should extract property description from multi-line tag`() {
        val kdoc =
            """
            |Data class description
            |
            |@property address User's full address.
            |                  Including street, city, and postal code.
            |                  Required for shipping.
            """.trimMargin()

        val result = extractPropertyDescriptionFromKdoc(kdoc, "address")
        result shouldBe "User's full address.\nIncluding street, city, and postal code.\nRequired for shipping."
    }

    @Test
    fun `Should return null for non-existent property`() {
        val kdoc =
            """
            |@property name User name
            |@property age User age
            """.trimMargin()

        val result = extractPropertyDescriptionFromKdoc(kdoc, "email")
        result shouldBe null
    }

    @Test
    fun `Should return null for property with null kdoc`() {
        val result = extractPropertyDescriptionFromKdoc(null, "name")
        result shouldBe null
    }

    @Test
    fun `Should handle property tag with no description`() {
        val kdoc =
            """
            |@property name
            |@property age User age
            """.trimMargin()

        val result = extractPropertyDescriptionFromKdoc(kdoc, "name")
        result shouldBe null
    }

    @Test
    fun `Should extract param description ignoring indentation`() {
        val kdoc =
            """
            |    @param name     User name with extra spaces
            |    @param age User age
            """.trimMargin()

        val result = extractParamDescriptionFromKdoc(kdoc, "name")
        result shouldBe "User name with extra spaces"
    }

    @Test
    fun `Should stop at next tag when extracting param description`() {
        val kdoc =
            """
            |@param name User name
            |        More details about name
            |@param age User age
            """.trimMargin()

        val result = extractParamDescriptionFromKdoc(kdoc, "name")
        result shouldBe "User name\nMore details about name"
    }

    @Test
    fun `Should match exact parameter name not prefix`() {
        val kdoc =
            """
            |@param name Full name
            |@param namePrefix Just prefix
            """.trimMargin()

        extractParamDescriptionFromKdoc(kdoc, "name") shouldBe "Full name"
        extractParamDescriptionFromKdoc(kdoc, "namePrefix") shouldBe "Just prefix"
    }

    @Test
    fun `Should match exact property name not prefix`() {
        val kdoc =
            """
            |@property id User ID
            |@property identifier Full identifier
            """.trimMargin()

        extractPropertyDescriptionFromKdoc(kdoc, "id") shouldBe "User ID"
        extractPropertyDescriptionFromKdoc(kdoc, "identifier") shouldBe "Full identifier"
    }
}
