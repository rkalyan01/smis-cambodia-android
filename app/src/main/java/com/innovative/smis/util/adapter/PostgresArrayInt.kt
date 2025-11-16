package com.innovative.smis.util.adapter

import com.squareup.moshi.JsonQualifier

/**
 * Qualifier annotation for fields that may contain PostgreSQL array format.
 * Used with PostgresArrayIntAdapter to handle corrupted data from legacy API responses.
 */
@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class PostgresArrayInt
