# kotlinx-schema-json

**Table of contents:**
<!--- TOC -->

* [Features](#features)
* [Limitations](#limitations)
* [Quick Start](#quick-start)
* [Installation](#installation)
* [Comprehensive Example](#comprehensive-example)
* [Function Calling Schema for LLM APIs](#function-calling-schema-for-llm-apis)
  * [OpenAI Structured Outputs Requirements](#openai-structured-outputs-requirements)
  * [Runtime Generation from Functions](#runtime-generation-from-functions)
* [Conformance Testing](#conformance-testing)

<!--- END -->

Type-safe Kotlin models and DSL for [JSON Schema Draft 2020-12](https://json-schema.org/draft/2020-12/schema) with kotlinx-serialization support.

## Features

- ✅ **JSON Schema Draft 2020-12 compliant** type-safe data models
- ✅ **Type-safe enums** with native Kotlin types (`List<String>`, `List<Number>`, etc.)
- ✅ **Polymorphism** via oneOf, anyOf, allOf with discriminator support
- ✅ **Property types**: string, number, integer, boolean, array, object, generic, reference
- ✅ **Constraints**: required, nullable, enum, const, min/max, length, format, pattern
- ✅ **Nested schemas** with full object and array support
- ✅ **Function calling schemas** for LLM APIs (OpenAI, Anthropic)
- ✅ **Kotlin Multiplatform** support (JVM, JS, Native, Wasm)
- ✅ **kotlinx-serialization** integration for JSON serialization/deserialization

## Limitations

- Unknown elements might be present in the JSON Schema according to the [specification requirement](https://json-schema.org/draft/2020-12/draft-bhutton-json-schema-01#section-4.3.1).
  JsonSchema model does not capture such elements: they are ignored because of the `@JsonIgnoreUnknownKeys` annotation.

## Quick Start

## Installation

For JVM-only projects use [kotlinx-schema-json-jvm](https://central.sonatype.com/artifact/org.jetbrains.kotlinx/kotlinx-schema-json-jvm).
<details>
<summary>Gradle dependency</summary>

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-schema-json:$version")
}
```
</details>

For Multiplatform projects use [kotlinx-schema-json](https://central.sonatype.com/artifact/org.jetbrains.kotlinx/kotlinx-schema-json).

Define schemas with Kotlin DSL:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.json.*
import kotlinx.serialization.json.Json

fun main() {
-->
```kotlin
val schema = jsonSchema {
    property("email") {
        required = true
        string {
            description = "User's email address"
            format = "email"
        }
    }
    property("age") {
        integer {
            description = "User's age"
            minimum = 0.0
            maximum = 150.0
        }
    }
}

// Serialize to JSON
val json = Json { prettyPrint = true }
val jsonString = json.encodeToString(schema)
```
<!--- SUFFIX
}
-->
<!--- KNIT example-knit-json-readme-01.kt -->

## Comprehensive Example

This example demonstrates all major DSL features:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.json.*
import kotlinx.serialization.json.Json

fun main() {
-->
```kotlin
val schema =
    jsonSchema {
        // Schema metadata
        id = "https://example.com/schemas/user-profile"
        schema = "https://json-schema.org/draft/2020-12/schema"
        additionalProperties = false

        // String with format and constraints
        property("email") {
            required = true
            string {
                description = "Email address"
                format = "email"
                minLength = 5
                maxLength = 100
            }
        }

        // Numeric constraints
        property("age") {
            integer {
                description = "User age"
                minimum = 18.0
                maximum = 120.0
            }
        }

        // Type-safe string enum
        property("status") {
            string {
                description = "Account status"
                enum = listOf("active", "inactive", "pending")
            }
        }

        // Nullable property with default
        property("verified") {
            boolean {
                description = "Email verified"
                nullable = true
                default = false
            }
        }

        // Constant value
        property("apiVersion") {
            string {
                description = "API version"
                constValue = "v2.0"
            }
        }

        // Array with constraints
        property("tags") {
            array {
                description = "User tags"
                minItems = 1
                maxItems = 10
                ofString()
            }
        }

        // Nested object
        property("metadata") {
            obj {
                description = "User metadata"
                property("createdAt") {
                    required = true
                    string { format = "date-time" }
                }
                property("lastLogin") {
                    string { format = "date-time" }
                }
            }
        }

        // Array of objects
        property("activities") {
            array {
                description = "Activity log"
                ofObject {
                    property("action") {
                        required = true
                        string()
                    }
                    property("timestamp") {
                        required = true
                        string { format = "date-time" }
                    }
                }
            }
        }

        // Polymorphic with discriminator (inline schemas)
        property("paymentMethod") {
            oneOf {
                discriminator(propertyName = "type") {
                    "card" mappedTo {
                        property("type") {
                            required = true
                            string { constValue = "card" }
                        }
                        property("cardNumber") {
                            required = true
                            string()
                        }
                    }
                    "paypal" mappedTo {
                        property("type") {
                            required = true
                            string { constValue = "paypal" }
                        }
                        property("email") {
                            required = true
                            string { format = "email" }
                        }
                    }
                }
            }
        }

        // Generic property with heterogeneous enum
        property("config") {
            generic {
                description = "Configuration value"
                enum =
                    listOf(
                        "default", // String
                        42, // Number
                        true, // Boolean
                        null, // Null
                        listOf(1, 2, 3), // Array (auto-converted)
                        mapOf("key" to "value"), // Object (auto-converted)
                    )
            }
        }
    
}

val json = Json { prettyPrint = true }
println(json.encodeToString(schema))
```
<!--- SUFFIX
}
-->
<!--- KNIT example-knit-json-readme-02.kt -->

Serialization result:

<details>
<summary>Click to expand</summary>

```json
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://example.com/schemas/user-profile",
    "type": "object",
    "properties": {
        "email": {
            "type": "string",
            "description": "Email address",
            "format": "email",
            "minLength": 5,
            "maxLength": 100
        },
        "age": {
            "type": "integer",
            "description": "User age",
            "minimum": 18.0,
            "maximum": 120.0
        },
        "status": {
            "type": "string",
            "description": "Account status",
            "enum": [
                "active",
                "inactive",
                "pending"
            ]
        },
        "verified": {
            "type": "boolean",
            "description": "Email verified",
            "nullable": true,
            "default": false
        },
        "apiVersion": {
            "type": "string",
            "description": "API version",
            "const": "v2.0"
        },
        "tags": {
            "type": "array",
            "description": "User tags",
            "items": {
                "type": "string"
            },
            "minItems": 1,
            "maxItems": 10
        },
        "metadata": {
            "type": "object",
            "description": "User metadata",
            "properties": {
                "createdAt": {
                    "type": "string",
                    "format": "date-time"
                },
                "lastLogin": {
                    "type": "string",
                    "format": "date-time"
                }
            },
            "required": [
                "createdAt"
            ]
        },
        "activities": {
            "type": "array",
            "description": "Activity log",
            "items": {
                "type": "object",
                "properties": {
                    "action": {
                        "type": "string"
                    },
                    "timestamp": {
                        "type": "string",
                        "format": "date-time"
                    }
                },
                "required": [
                    "action",
                    "timestamp"
                ]
            }
        },
        "paymentMethod": {
            "oneOf": [
                {
                    "type": "object",
                    "properties": {
                        "type": {
                            "type": "string",
                            "const": "card"
                        },
                        "cardNumber": {
                            "type": "string"
                        }
                    },
                    "required": [
                        "type",
                        "cardNumber"
                    ]
                },
                {
                    "type": "object",
                    "properties": {
                        "type": {
                            "type": "string",
                            "const": "paypal"
                        },
                        "email": {
                            "type": "string",
                            "format": "email"
                        }
                    },
                    "required": [
                        "type",
                        "email"
                    ]
                }
            ],
            "discriminator": {
                "propertyName": "type"
            }
        },
        "config": {
            "description": "Configuration value",
            "enum": [
                "default",
                42,
                true,
                null,
                [
                    1,
                    2,
                    3
                ],
                {
                    "key": "value"
                }
            ]
        }
    },
    "additionalProperties": false,
    "required": [
        "email"
    ]
}
```

</details>

Check [kotlinx-schema-json API Reference](https://kotlin.github.io/kotlinx-schema/kotlinx-schema-json/) for details.

## Function Calling Schema for LLM APIs

For LLM function calling (OpenAI, Anthropic), use `FunctionCallingSchema`:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.schema.json.*
import kotlinx.serialization.json.Json

fun main() {
-->
```kotlin
val functionSchema =
FunctionCallingSchema(
    name = "searchDatabase",
    description = "Search for items in the database",
    strict = true,
    parameters =
        ObjectPropertyDefinition(
            properties =
                mapOf(
                    "query" to
                        StringPropertyDefinition(
                            description = "Search query",
                        ),
                    "limit" to
                        NumericPropertyDefinition(
                            type = listOf("integer"),
                            description = "Max results",
                        ),
                ),
            required = listOf("query", "limit"),
            additionalProperties = AdditionalPropertiesConstraint.deny(),
        ),
)

val json = Json { prettyPrint = true }
println(json.encodeToString(functionSchema))
```
<!--- SUFFIX
}
-->
<!--- KNIT example-knit-json-readme-03.kt -->

Produces function schema serializable as JSON:
```json
{
    "type": "function",
    "name": "searchDatabase",
    "description": "Search for items in the database",
    "strict": true,
    "parameters": {
        "type": "object",
        "properties": {
            "query": {
                "type": "string",
                "description": "Search query"
            },
            "limit": {
                "type": "integer",
                "description": "Max results"
            }
        },
        "required": ["query", "limit"],
        "additionalProperties": false
    }
}
```

### OpenAI Structured Outputs Requirements

- All parameters in `required` array (even nullable ones)
- Nullable fields use union types: `type = listOf("string", "null")`
- `additionalProperties = false` by default
- `strict = true` enables [OpenAI Strict Mode](https://platform.openai.com/docs/guides/function-calling#strict-mode)

**Tests:** [FunctionCallingSchemaTest](src/commonTest/kotlin/kotlinx/schema/json/FunctionCallingSchemaTest.kt)

### Runtime Generation from Functions

For runtime schema generation from Kotlin functions, use `ReflectionFunctionCallingSchemaGenerator` from the `kotlinx-schema-generator-json` module:

<!--- CLEAR -->
<!--- INCLUDE
import kotlinx.serialization.json.Json
import kotlinx.schema.Description
import kotlinx.schema.generator.json.ReflectionFunctionCallingSchemaGenerator

data class User(val name: String)
-->
```kotlin
@Description("Search for users by name")
fun searchUsers(
    @Description("Name to search for") query: String,
    @Description("Maximum results") limit: Int = 10
): List<User> = TODO()
```
<!--- INCLUDE
fun main() {
-->
```kotlin
val generator = ReflectionFunctionCallingSchemaGenerator.Default
val functionSchema = generator.generateSchema(::searchUsers)

val json = Json { prettyPrint = true }
println(json.encodeToString(functionSchema))
```
<!--- SUFFIX
}
-->
<!--- KNIT example-knit-json-readme-04.kt -->

Produces function schema serializable as JSON:
```json
{
    "type": "function",
    "name": "searchUsers",
    "description": "Search for users by name",
    "strict": true,
    "parameters": {
        "type": "object",
        "properties": {
            "query": {
                "type": "string",
                "description": "Name to search for"
            },
            "limit": {
                "type": "integer",
                "description": "Maximum results"
            }
        },
        "required": [
            "query",
            "limit"
        ],
        "additionalProperties": false
    }
}

```

## Conformance Testing

kotlinx-schema-json aims for full conformance with JSON Schema Draft 2020-12. 
We run tests against the official [JSON Schema Test Suite](https://github.com/json-schema-org/JSON-Schema-Test-Suite).

**See:** [JsonSchemaConformanceTest](src/commonTest/kotlin/kotlinx/schema/json/conformance/JsonSchemaConformanceTest.kt)
