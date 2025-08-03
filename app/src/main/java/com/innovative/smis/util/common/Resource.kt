package com.innovative.smis.util.common

/**
 * A generic sealed class that represents the state of a data request.
 * It's used to communicate the current state from the repository/ViewModel to the UI.
 *
 * @param T The type of the data being handled.
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    /**
     * Represents a successful data fetch.
     * @param data The successfully retrieved data.
     */
    class Success<T>(data: T) : Resource<T>(data)

    /**
     * Represents a failed data fetch.
     * @param message A message describing the error.
     * @param data Optional data that might be available even if an error occurred.
     */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    /**
     * Represents the loading state while data is being fetched.
     */
    class Loading<T> : Resource<T>()

    /**
     * Represents the initial, idle state before any request has been made.
     */
    class Idle<T> : Resource<T>()
}
