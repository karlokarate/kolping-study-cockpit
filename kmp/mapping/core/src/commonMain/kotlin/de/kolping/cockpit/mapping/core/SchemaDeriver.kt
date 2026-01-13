package de.kolping.cockpit.mapping.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

object SchemaDeriver {
    /**
     * Threshold for determining when a string field should hint "bytes" storage.
     * Strings longer than this threshold are considered large enough to benefit from
     * optimized storage strategies (e.g., external blob storage or compression).
     */
    private const val LARGE_STRING_THRESHOLD = 1000

    @Serializable
    data class FieldSchema(
        val path: String,
        val inferredType: String,
        val storageHint: String = "json"
    )

    /**
     * Derives a schema from JSON string by analyzing field types and structure.
     * @return Result containing list of field schemas, or error if parsing fails
     */
    fun deriveSchema(jsonString: String): Result<List<FieldSchema>> {
        return try {
            val element = Json.parseToJsonElement(jsonString)
            val fields = mutableListOf<FieldSchema>()
            traverse(element, "$", fields)
            Result.success(fields)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun traverse(element: JsonElement, path: String, fields: MutableList<FieldSchema>) {
        when (element) {
            is JsonObject -> {
                fields.add(FieldSchema(path, "object"))
                element.forEach { (key, value) ->
                    traverse(value, "$path.$key", fields)
                }
            }
            is JsonArray -> {
                fields.add(FieldSchema(path, "array"))
                if (element.isNotEmpty()) {
                    traverse(element[0], "$path[0]", fields)
                }
            }
            is JsonPrimitive -> {
                val type = when {
                    element.isString -> "string"
                    element.booleanOrNull != null -> "boolean"
                    element.intOrNull != null -> "number"
                    element.longOrNull != null -> "number"
                    element.doubleOrNull != null -> "number"
                    else -> "null"
                }
                val hint = if (type == "string" && element.content.length > LARGE_STRING_THRESHOLD) "bytes" else "string"
                fields.add(FieldSchema(path, type, hint))
            }
            else -> fields.add(FieldSchema(path, "null"))
        }
    }
}
