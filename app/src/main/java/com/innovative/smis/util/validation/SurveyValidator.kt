package com.innovative.smis.util.validation

import com.innovative.smis.data.model.BuildingSurveyFormState
import com.innovative.smis.util.FormValidationResult
import java.time.LocalDate
import java.util.regex.Pattern

/**
 * Survey validation utility for building survey forms
 * Provides comprehensive validation for all form fields
 */
object SurveyValidator {
    
    private val PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s()]+$")
    private val EMAIL_PATTERN = Pattern.compile("^[\\w\\.-]+@[\\w\\.-]+\\.[a-zA-Z]{2,}$")
    private val NAME_PATTERN = Pattern.compile("^[A-Za-z\\s]+$")

    /**
     * Validate complete building survey form
     */
    fun validateSurveyForm(formState: BuildingSurveyFormState): FormValidationResult {
        val errors = mutableMapOf<String, String>()

        // Required field validations
        validateRequiredFields(formState, errors)
        
        // Format validations
        validateFormats(formState, errors)
        
        // Logical validations
        validateLogicalConstraints(formState, errors)
        
        // Range validations
        validateRanges(formState, errors)

        return FormValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Validate required fields
     */
    private fun validateRequiredFields(
        formState: BuildingSurveyFormState,
        errors: MutableMap<String, String>
    ) {
        if (formState.bin.isBlank()) {
            errors["bin"] = "Building Identification Number (BIN) is required"
        }

        if (formState.sangkat.isBlank()) {
            errors["sangkat"] = "Sangkat is required"
        }

        if (formState.respondentName.isBlank()) {
            errors["respondentName"] = "Respondent name is required"
        }

        if (formState.respondentGender == null) {
            errors["respondentGender"] = "Please select respondent gender"
        }
    }

    /**
     * Validate field formats
     */
    private fun validateFormats(
        formState: BuildingSurveyFormState,
        errors: MutableMap<String, String>
    ) {
        // Validate respondent name format
        if (formState.respondentName.isNotBlank() && 
            !NAME_PATTERN.matcher(formState.respondentName).matches()) {
            errors["respondentName"] = "Name should only contain letters and spaces"
        }

        // Validate owner name format
        if (formState.ownerName.isNotBlank() && 
            !NAME_PATTERN.matcher(formState.ownerName).matches()) {
            errors["ownerName"] = "Owner name should only contain letters and spaces"
        }

        // Validate contact numbers
        if (formState.respondentContact.isNotBlank() && 
            !PHONE_PATTERN.matcher(formState.respondentContact).matches()) {
            errors["respondentContact"] = "Please enter a valid contact number"
        }

        if (formState.ownerContact.isNotBlank() && 
            !PHONE_PATTERN.matcher(formState.ownerContact).matches()) {
            errors["ownerContact"] = "Please enter a valid contact number"
        }

        // Validate BIN format (alphanumeric)
        if (formState.bin.isNotBlank() && 
            !formState.bin.matches(Regex("^[A-Za-z0-9]+$"))) {
            errors["bin"] = "BIN should contain only letters and numbers"
        }
    }

    /**
     * Validate logical constraints
     */
    private fun validateLogicalConstraints(
        formState: BuildingSurveyFormState,
        errors: MutableMap<String, String>
    ) {
        // If containment present onsite is false, certain fields should be empty
        if (!formState.containmentPresentOnsite) {
            if (formState.typeOfStorageTank.isNotBlank() ||
                formState.numberOfTanks.isNotBlank() ||
                formState.sizeOfTank.isNotBlank()) {
                errors["containmentLogic"] = "Containment details should be empty if no containment is present onsite"
            }
        }

        // If containment present, some fields become required
        if (formState.containmentPresentOnsite) {
            if (formState.typeOfStorageTank.isBlank()) {
                errors["typeOfStorageTank"] = "Storage tank type is required when containment is present"
            }
        }

        // Date logic validations
        validateDateLogic(formState, errors)
    }

    /**
     * Validate date constraints
     */
    private fun validateDateLogic(
        formState: BuildingSurveyFormState,
        errors: MutableMap<String, String>
    ) {
        val currentDate = LocalDate.now()

        // Construction date should be in the past
        if (formState.constructionDate.isNotBlank()) {
            try {
                val constructionDate = LocalDate.parse(formState.constructionDate)
                if (constructionDate.isAfter(currentDate)) {
                    errors["constructionDate"] = "Construction date cannot be in the future"
                }
            } catch (e: Exception) {
                errors["constructionDate"] = "Please enter a valid construction date"
            }
        }

        // Last emptied date should be in the past
        if (formState.lastEmptiedDate.isNotBlank()) {
            try {
                val lastEmptiedDate = LocalDate.parse(formState.lastEmptiedDate)
                if (lastEmptiedDate.isAfter(currentDate)) {
                    errors["lastEmptiedDate"] = "Last emptied date cannot be in the future"
                }
            } catch (e: Exception) {
                errors["lastEmptiedDate"] = "Please enter a valid last emptied date"
            }
        }

        // Construction date should be before last emptied date
        if (formState.constructionDate.isNotBlank() && formState.lastEmptiedDate.isNotBlank()) {
            try {
                val constructionDate = LocalDate.parse(formState.constructionDate)
                val lastEmptiedDate = LocalDate.parse(formState.lastEmptiedDate)
                
                if (constructionDate.isAfter(lastEmptiedDate)) {
                    errors["dateLogic"] = "Construction date should be before last emptied date"
                }
            } catch (e: Exception) {
                // Date parsing errors handled above
            }
        }
    }

    /**
     * Validate numeric ranges
     */
    private fun validateRanges(
        formState: BuildingSurveyFormState,
        errors: MutableMap<String, String>
    ) {
        // Validate construction year
        if (formState.constructionYear.isNotBlank()) {
            try {
                val year = formState.constructionYear.toInt()
                val currentYear = LocalDate.now().year
                
                if (year < 1900 || year > currentYear) {
                    errors["constructionYear"] = "Construction year should be between 1900 and $currentYear"
                }
            } catch (e: NumberFormatException) {
                errors["constructionYear"] = "Please enter a valid construction year"
            }
        }

        // Validate number of floors
        if (formState.numberOfFloors.isNotBlank()) {
            try {
                val floors = formState.numberOfFloors.toInt()
                if (floors <= 0 || floors > 50) {
                    errors["numberOfFloors"] = "Number of floors should be between 1 and 50"
                }
            } catch (e: NumberFormatException) {
                errors["numberOfFloors"] = "Please enter a valid number of floors"
            }
        }

        // Validate household served
        if (formState.householdServed.isNotBlank()) {
            try {
                val households = formState.householdServed.toInt()
                if (households <= 0 || households > 1000) {
                    errors["householdServed"] = "Household served should be between 1 and 1000"
                }
            } catch (e: NumberFormatException) {
                errors["householdServed"] = "Please enter a valid number of households"
            }
        }

        // Validate population served
        if (formState.populationServed.isNotBlank()) {
            try {
                val population = formState.populationServed.toInt()
                if (population <= 0 || population > 10000) {
                    errors["populationServed"] = "Population served should be between 1 and 10000"
                }
            } catch (e: NumberFormatException) {
                errors["populationServed"] = "Please enter a valid population number"
            }
        }

        // Validate floor area
        if (formState.floorArea.isNotBlank()) {
            try {
                val area = formState.floorArea.toDouble()
                if (area <= 0 || area > 10000) {
                    errors["floorArea"] = "Floor area should be between 1 and 10000 square meters"
                }
            } catch (e: NumberFormatException) {
                errors["floorArea"] = "Please enter a valid floor area"
            }
        }

        // Validate number of toilets
        if (formState.numberOfToilets.isNotBlank()) {
            try {
                val toilets = formState.numberOfToilets.toInt()
                if (toilets <= 0 || toilets > 50) {
                    errors["numberOfToilets"] = "Number of toilets should be between 1 and 50"
                }
            } catch (e: NumberFormatException) {
                errors["numberOfToilets"] = "Please enter a valid number of toilets"
            }
        }

        // Validate number of tanks
        if (formState.numberOfTanks.isNotBlank()) {
            try {
                val tanks = formState.numberOfTanks.toInt()
                if (tanks <= 0 || tanks > 20) {
                    errors["numberOfTanks"] = "Number of tanks should be between 1 and 20"
                }
            } catch (e: NumberFormatException) {
                errors["numberOfTanks"] = "Please enter a valid number of tanks"
            }
        }

        // Validate tank size
        if (formState.sizeOfTank.isNotBlank()) {
            try {
                val size = formState.sizeOfTank.toDouble()
                if (size <= 0 || size > 100) {
                    errors["sizeOfTank"] = "Tank size should be between 0.1 and 100 cubic meters"
                }
            } catch (e: NumberFormatException) {
                errors["sizeOfTank"] = "Please enter a valid tank size"
            }
        }

        // Validate distance from well
        if (formState.distanceFromWell.isNotBlank()) {
            try {
                val distance = formState.distanceFromWell.toDouble()
                if (distance < 0 || distance > 1000) {
                    errors["distanceFromWell"] = "Distance from well should be between 0 and 1000 meters"
                }
            } catch (e: NumberFormatException) {
                errors["distanceFromWell"] = "Please enter a valid distance"
            }
        }
    }

    /**
     * Validate individual field
     */
    fun validateField(fieldName: String, value: String, formState: BuildingSurveyFormState): String? {
        return when (fieldName) {
            "bin" -> {
                when {
                    value.isBlank() -> "BIN is required"
                    !value.matches(Regex("^[A-Za-z0-9]+$")) -> "BIN should contain only letters and numbers"
                    else -> null
                }
            }
            "respondentName" -> {
                when {
                    value.isBlank() -> "Respondent name is required"
                    !NAME_PATTERN.matcher(value).matches() -> "Name should only contain letters and spaces"
                    else -> null
                }
            }
            "respondentContact" -> {
                if (value.isNotBlank() && !PHONE_PATTERN.matcher(value).matches()) {
                    "Please enter a valid contact number"
                } else null
            }
            "constructionYear" -> {
                if (value.isNotBlank()) {
                    try {
                        val year = value.toInt()
                        val currentYear = LocalDate.now().year
                        if (year < 1900 || year > currentYear) {
                            "Construction year should be between 1900 and $currentYear"
                        } else null
                    } catch (e: NumberFormatException) {
                        "Please enter a valid construction year"
                    }
                } else null
            }
            else -> null
        }
    }

    /**
     * Check if form is ready for submission
     */
    fun isFormComplete(formState: BuildingSurveyFormState): Boolean {
        return formState.bin.isNotBlank() &&
                formState.sangkat.isNotBlank() &&
                formState.respondentName.isNotBlank() &&
                formState.respondentGender != null
    }
}