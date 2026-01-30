import com.example.shapes.Shape
import com.example.shapes.jsonSchema
import com.example.shapes.jsonSchemaString
import com.example.shapes.sayHelloJsonSchema
import com.example.shapes.sayHelloJsonSchemaString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private val json = Json { prettyPrint = true }

fun main() {
    println(Shape::class.jsonSchemaString)

    println("${Shape::class.jsonSchema::class}: ${json.encodeToString(Shape::class.jsonSchema)}")

    val functionCallSchemaString: String = sayHelloJsonSchemaString()
    val functionCallSchema: JsonObject = sayHelloJsonSchema()
    println("$functionCallSchemaString\n\n${functionCallSchema::class}: ${json.encodeToString(functionCallSchema)}")
}
