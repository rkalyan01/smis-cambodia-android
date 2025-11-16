package com.innovative.smis.util.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import android.util.Log

/**
 * Custom Moshi adapter to handle PostgreSQL array format in integer fields.
 * 
 * **Background:**
 * Historical mobile clients submitted dropdown IDs as PostgreSQL array syntax (e.g., "{4,3}")
 * due to a bug where display values were sent instead of single IDs. The Laravel backend
 * stored these raw strings without validation.
 * 
 * **Problem:**
 * When the API returns data, fields like `additional_repairing_id` contain "{4,3}" instead
 * of a single integer, causing JSON deserialization to fail.
 * 
 * **Solution:**
 * This adapter extracts the first integer from PostgreSQL array format "{...}" as a temporary
 * workaround while the backend team implements proper validation and data migration.
 * 
 * **Note:**
 * This is a SHORT-LIVED workaround. The proper fix requires:
 * 1. Backend validation to reject array format on write
 * 2. Data migration to clean up corrupted records
 * 3. API response normalization to always return single integers
 */
class PostgresArrayIntAdapter {
    
    @FromJson
    @PostgresArrayInt
    fun fromJson(reader: JsonReader): Int? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull()
            }
            JsonReader.Token.NUMBER -> {
                // Normal case: single integer
                reader.nextInt()
            }
            JsonReader.Token.STRING -> {
                // Handle PostgreSQL array format: "{4,3}" or corrupted data
                val value = reader.nextString()
                parsePostgresArray(value)
            }
            JsonReader.Token.BEGIN_ARRAY -> {
                // Handle actual JSON array format: [4,3] or ["4","3"]
                reader.beginArray()
                val firstElement = try {
                    if (reader.hasNext()) {
                        when (reader.peek()) {
                            JsonReader.Token.NUMBER -> reader.nextInt()
                            JsonReader.Token.STRING -> reader.nextString().toIntOrNull()
                            JsonReader.Token.NULL -> {
                                reader.nextNull<Int?>()
                                null
                            }
                            else -> {
                                reader.skipValue()
                                null
                            }
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("PostgresArrayIntAdapter", "Error reading first array element", e)
                    null
                }
                
                // Skip and count remaining elements
                var extraCount = 0
                while (reader.hasNext()) {
                    reader.skipValue()
                    extraCount++
                }
                reader.endArray()
                
                // Log warning for any multi-element array, regardless of first element validity
                if (extraCount > 0) {
                    if (firstElement != null) {
                        Log.w("PostgresArrayIntAdapter", "Detected JSON array with ${extraCount + 1} elements, using first element: $firstElement (discarded $extraCount element(s))")
                    } else {
                        Log.w("PostgresArrayIntAdapter", "Detected JSON array with ${extraCount + 1} elements, but first element was null/invalid (discarded $extraCount element(s))")
                    }
                }
                firstElement
            }
            else -> {
                Log.e("PostgresArrayIntAdapter", "Unexpected token: ${reader.peek()}")
                reader.skipValue()
                null
            }
        }
    }
    
    @ToJson
    fun toJson(writer: JsonWriter, @PostgresArrayInt value: Int?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value)
        }
    }
    
    /**
     * Parses PostgreSQL array format and extracts first integer.
     * Examples:
     * - "{4,3}" → 4
     * - "{10}" → 10
     * - "5" → 5
     * - "" → null
     */
    private fun parsePostgresArray(value: String): Int? {
        if (value.isBlank()) return null
        
        return try {
            // Check if it's PostgreSQL array format: {x,y,z}
            if (value.startsWith("{") && value.endsWith("}")) {
                // Extract first element from array
                val elements = value.trim('{', '}').split(',')
                val firstElement = elements.firstOrNull()?.trim()
                
                if (!firstElement.isNullOrBlank()) {
                    val result = firstElement.toIntOrNull()
                    if (result != null && elements.size > 1) {
                        // Log warning if array contains multiple values
                        Log.w("PostgresArrayIntAdapter", "Detected multi-value array '$value', using first element: $result")
                    }
                    result
                } else {
                    null
                }
            } else {
                // Try to parse as regular integer string
                value.toIntOrNull()
            }
        } catch (e: Exception) {
            Log.e("PostgresArrayIntAdapter", "Failed to parse value: $value", e)
            null
        }
    }
}
