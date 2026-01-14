// Package is intentionally left empty!!!

import kotlinx.schema.Description
import kotlinx.schema.Schema
import kotlin.random.Random

/**
 * Generates a random integer using a seeded random number generator.
 *
 * @param seed An optional seed value used to initialize the random number generator.
 *             If null, a random seed will be used.
 * @return A randomly generated integer.
 */
@Schema
fun emptyPackageTopLevelGetRandom(
    @Description("Random number generator seeded") seed: Int?,
): Int = Random(seed ?: Random.nextInt()).nextInt()

object EmptyPackageFunctionObject {
    @Schema
    fun objectFunction() {
        // todo
    }
}

class EmptyPackageFunctionClass {
    @Schema
    fun classFunction() {
        // todo
    }

    companion object {
        @Schema
        fun companionFunction() {
            // todo
        }
    }
}
