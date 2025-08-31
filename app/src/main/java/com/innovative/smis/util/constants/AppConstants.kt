package com.innovative.smis.util.constants

object ScreenName {
    const val Login = "login"
    const val Dashboard = "dashboard"
    const val Map = "map"
    const val TodoList = "todolist"
    const val TaskDetails = "task_details/{taskId}"
    const val WorkflowStep = "workflow_step/{stepId}"
    const val Profile = "profile"
    const val Settings = "settings"
    const val DesludgingVehicle = "desludging_vehicle"
    const val TaskManagement = "task_management"
}

object PrefConstant {
    const val AUTH_TOKEN = "auth_token"
    const val IS_LOGIN = "is_login"
    const val AUTO_LOGIN = "auto_login"
    const val USER_ID = "user_id"
    const val USER_ROLE = "user_role"
    const val USER_NAME = "user_name"
    const val USER_EMAIL = "user_email"
    const val USER_PERMISSIONS = "user_permissions"
    const val CURRENT_LANGUAGE = "current_language"
    const val PERMISSIONS_REQUESTED = "permissions_requested"
    const val FIRST_LAUNCH = "first_launch"
}

object ApiConstants {
    const val BASE_URL = "https://smis-11.innovativesolution.com.np/api/"  // Android emulator localhost alias
    const val TIMEOUT_SECONDS = 30L
}

object AppConstants {
    const val ETO_ID = 1  // Default ETO ID for testing
    const val DATABASE_NAME = "smis_database"
    const val DATABASE_VERSION = 15
    const val SYNC_INTERVAL_MINUTES = 15
}
