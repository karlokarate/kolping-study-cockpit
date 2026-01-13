package de.kolping.cockpit.mapping.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

object SchemaDeriver {
    @Serializable
    data class FieldSchema(
        val path: String,
        val inferredType: String,
        val storageHint: String = "json"
    )

    fun deriveSchema(jsonString: String): List<FieldSchema> {
        return try {
            val element = Json.parseToJsonElement(jsonString)
            val fields = mutableListOf<FieldSchema>()
            traverse(element, "$", fields)
            fields
        } catch (e: Exception) {
            emptyList()
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
                val hint = if (type == "string" && element.content.length > 1000) "bytes" else "string"
                fields.add(FieldSchema(path, type, hint))
            }
            else -> fields.add(FieldSchema(path, "null"))
        }
    }
}
