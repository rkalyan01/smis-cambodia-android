package com.innovative.smis.ui.util

/**
 * Sealed interface for type-safe dropdown option keys from API
 * Each domain has its own enum for compile-time safety
 */
sealed interface DropdownOptionKey {
    val apiKey: String
    
    /**
     * Containment Issues / Additional Repairing options
     */
    enum class ContainmentIssue(override val apiKey: String) : DropdownOptionKey {
        NO("No"),
        PIERCING_HOLE("Piercing hole for emptying"),
        INSTALL_TOO_DEEP("Install too deep in ground"),
        SEPTIC_REPAIRING("Septic repairing"),
        FAR_FROM_ROAD("Far from main road (more than 30m)"),
        SMALL_ROAD("Small Road"),
        OTHERS("Others, specify");
        
        companion object {
            fun fromApiKey(key: String): ContainmentIssue? =
                values().find { it.apiKey.equals(key, ignoreCase = true) }
        }
    }
    
    /**
     * Emptying Reason / Purpose options
     */
    enum class EmptyingReason(override val apiKey: String) : DropdownOptionKey {
        REGULAR_MAINTENANCE("Regular maintenance"),
        FULL_TANK("Full tank"),
        BAD_SMELL("Bad smell"),
        OVERFLOWING("Overflowing"),
        OTHERS("Others");
        
        companion object {
            fun fromApiKey(key: String): EmptyingReason? =
                values().find { it.apiKey.equals(key, ignoreCase = true) }
        }
    }
    
    /**
     * Emptied No Date Reason options
     */
    enum class EmptiedNoDateReason(override val apiKey: String) : DropdownOptionKey {
        FORGOT("Forgot"),
        RECORDS_LOST("Records lost"),
        FIRST_EMPTYING("First emptying"),
        OTHERS("Others");
        
        companion object {
            fun fromApiKey(key: String): EmptiedNoDateReason? =
                values().find { it.apiKey.equals(key, ignoreCase = true) }
        }
    }
    
    /**
     * Not Emptied Before Reason options
     */
    enum class NotEmptiedReason(override val apiKey: String) : DropdownOptionKey {
        NEW_CONSTRUCTION("New construction"),
        COST_BARRIER("Cost barrier"),
        LACK_OF_AWARENESS("Lack of awareness"),
        NO_SERVICE_AVAILABLE("No service available"),
        OTHERS("Others");
        
        companion object {
            fun fromApiKey(key: String): NotEmptiedReason? =
                values().find { it.apiKey.equals(key, ignoreCase = true) }
        }
    }
}
