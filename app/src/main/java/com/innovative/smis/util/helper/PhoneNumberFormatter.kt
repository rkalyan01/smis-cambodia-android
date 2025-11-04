package com.innovative.smis.util.helper

/**
 * Utility object for formatting Cambodian phone numbers
 * Ensures all phone numbers have the leading "0" prefix for proper dialing
 */
object PhoneNumberFormatter {
    
    /**
     * Formats a Cambodian phone number to local format with "0" prefix
     * Converts international format (855/+855) to local format (0XXXXXXXX)
     * 
     * Examples:
     * - "12345678" -> "012345678"
     * - "012345678" -> "012345678" (unchanged)
     * - "85512345678" -> "012345678" (international to local)
     * - "+85512345678" -> "012345678" (international to local)
     * - "" -> "" (empty, unchanged)
     * - null -> "" (null becomes empty)
     * 
     * @param phoneNumber The phone number to format
     * @return Formatted phone number with "0" prefix in local format
     */
    fun formatCambodianNumber(phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank()) return ""
        
        var trimmed = phoneNumber.trim()
        
        // Convert international format to local format
        if (trimmed.startsWith("+855")) {
            trimmed = "0" + trimmed.substring(4) // +855XXXXXXXX -> 0XXXXXXXX
        } else if (trimmed.startsWith("855")) {
            trimmed = "0" + trimmed.substring(3) // 855XXXXXXXX -> 0XXXXXXXX
        }
        
        // If number doesn't start with 0 and looks like local number (8-9 digits)
        if (!trimmed.startsWith("0") && trimmed.length in 8..9 && trimmed.all { it.isDigit() }) {
            return "0$trimmed"
        }
        
        // Return as is (already has 0 prefix)
        return trimmed
    }
    
    /**
     * Formats phone number for display in UI (adds "0" prefix if missing)
     * Same as formatCambodianNumber
     */
    fun formatForDisplay(phoneNumber: String?): String {
        return formatCambodianNumber(phoneNumber)
    }
    
    /**
     * Formats phone number for dialing (adds "0" prefix if missing)
     * Same as formatCambodianNumber
     */
    fun formatForDialing(phoneNumber: String?): String {
        return formatCambodianNumber(phoneNumber)
    }
}
