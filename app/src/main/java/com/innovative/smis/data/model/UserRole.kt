package com.innovative.smis.data.model

enum class UserRole(val roleName: String) {
    FIELD_WORKER("field_worker"),
    SUPERVISOR("supervisor"),
    ADMIN("admin"),
    EMPTYING("emptying"),
    ENUMERATOR("enumerator");

    companion object {
        fun fromString(role: String): UserRole {
            return values().find { it.roleName.equals(role, ignoreCase = true) }
                ?: FIELD_WORKER
        }
    }
}