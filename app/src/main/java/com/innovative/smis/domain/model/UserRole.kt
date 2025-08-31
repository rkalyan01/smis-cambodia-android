package com.innovative.smis.domain.model


enum class UserRole(val displayName: String) {
    FIELD_WORKER("Field Worker"),
    SUPERVISOR("Supervisor"),
    ADMIN("Administrator"),
    EMPTYING("Emptying Service");
    
    companion object {
        fun fromString(role: String): UserRole {
            return when (role.uppercase()) {
                "FIELD_WORKER", "FIELDWORKER" -> FIELD_WORKER
                "SUPERVISOR" -> SUPERVISOR
                "ADMIN", "ADMINISTRATOR" -> ADMIN
                "EMPTYING" -> EMPTYING
                else -> FIELD_WORKER // Default fallback
            }
        }
    }
}