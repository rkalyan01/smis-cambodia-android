package com.innovative.smis.ui.features.login

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.innovative.smis.R
import com.innovative.smis.ui.theme.SmisTheme
import com.innovative.smis.util.common.Resource
import com.innovative.smis.util.constants.PrefConstant
import com.innovative.smis.util.constants.ScreenName
import com.innovative.smis.util.helper.PreferenceHelper
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val loginViewModel: LoginViewModel = koinViewModel()
    val loginState by loginViewModel.loginState.collectAsState()

    var email by remember { mutableStateOf("enumerator@gmail.com") }
    var password by remember { mutableStateOf("password") }
    var rememberMe by remember { mutableStateOf(false) }

    val emailError by loginViewModel.emailError
    val passwordError by loginViewModel.passwordError
    val passwordVisible by loginViewModel.passwordVisible

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // This LaunchedEffect block listens for changes in the loginState.
    LaunchedEffect(loginState) {
        // This log will show the exact state of the API call in Logcat.
        Log.d("LoginScreen", "Login State Changed: $loginState")

        when (val state = loginState) {
            is Resource.Success -> {
                // This log will show the exact data from a successful API call.
                Log.d("LoginScreen", "API Success Response Data: ${state.data}")

                if (state.data?.status == true) {
                    // Save token and user info on success
                    val prefs = PreferenceHelper(context)
                    prefs.setValue(PrefConstant.AUTH_TOKEN, state.data.token ?: "")
                    prefs.setValue(PrefConstant.IS_LOGIN, true)
                    prefs.setValue(PrefConstant.AUTO_LOGIN, rememberMe)

                    // Navigate to home and clear the entire back stack
                    navController.navigate(ScreenName.Map) {
                        popUpTo(0) { inclusive = true }
                    }
                } else {
                    snackbarHostState.showSnackbar(state.data?.message ?: "Login failed")
                }
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(state.message ?: "An unknown error occurred")
            }
            else -> { /* Idle or Loading */ }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 64.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bmw),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Welcome Back!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sign in to continue",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(48.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        isError = emailError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    if (emailError != null) {
                        Text(
                            text = emailError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp).fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        isError = passwordError != null,
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff

                            val description = if (passwordVisible) "Hide password" else "Show password"

                            IconButton(onClick = { loginViewModel.togglePasswordVisibility() }) {
                                Icon(imageVector = image, description)
                            }
                        }
                    )
                    if (passwordError != null) {
                        Text(
                            text = passwordError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp).fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RememberMeCheckbox(
                            isChecked = rememberMe,
                            onCheckedChange = { rememberMe = it }
                        )
                        Text(
                            text = "Forgot Password?",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { /* Handle click */ }
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            loginViewModel.onLoginClicked(email, password)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = email.isNotBlank() && password.isNotBlank() && loginState !is Resource.Loading
                    ) {
                        if (loginState is Resource.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }


            }
        }
    }
}

@Composable
fun RememberMeCheckbox(
    isChecked: Boolean,
    text: String? = "Remember Me",
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.clickable { onCheckedChange(!isChecked) }
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            // FIX: Use the standard Kotlin Elvis operator for null safety
            text = text ?: "Remember Me",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun LoginScreenPreview() {
    SmisTheme {
        LoginScreen(rememberNavController())
    }
}
