package kotlinx.schema.json

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language

/**
 * Deserializes the input JSON string into an object of type [T],
 * re-serializes the object back to a JSON string,
 * and verifies if the re-serialized JSON matches the original input.
 *
 * @param payload The JSON string to be deserialized and re-serialized.
 * @return The deserialized object of type [T].
 *
 * @author Konstantin Pavlov
 */
inline fun <reified T : Any> deserializeAndSerialize(
    @Language("JSON")
    payload: String,
    jsonParser: Json = Json,
): T {
    val model: T = jsonParser.decodeFromString(payload)

    model.shouldNotBeNull()

    val encoded = jsonParser.encodeToString(model)

    encoded shouldEqualJson payload
    return model
}

inline fun <reified T : Any> serializeAndDeserialize(
    value: T,
    @Language("JSON")
    expectedPayload: String,
    jsonParser: Json = Json,
): T {
    val encoded: String = jsonParser.encodeToString(value)

    encoded shouldEqualJson expectedPayload

    val decoded = jsonParser.decodeFromString<T>(encoded)

    decoded shouldBeEqual value
    return decoded
}
