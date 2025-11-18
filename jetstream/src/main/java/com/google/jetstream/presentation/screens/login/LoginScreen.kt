/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.jetstream.presentation.screens.login

import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.google.jetstream.presentation.common.Loading
import com.google.jetstream.presentation.theme.JetStreamCardShape

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when {
            uiState.isLoading -> {
                Loading()
            }
            else -> {
                // Split screen: login form on left, keyboard will appear on right
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Left half - login form
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LoginForm(
                            serverUrl = uiState.serverUrl,
                            username = uiState.username,
                            password = uiState.password,
                            errorMessage = uiState.error,
                            onServerUrlChange = viewModel::updateServerUrl,
                            onUsernameChange = viewModel::updateUsername,
                            onPasswordChange = viewModel::updatePassword,
                            onLoginClick = viewModel::login,
                            modifier = Modifier.padding(top = 80.dp)
                        )
                    }
                    
                    // Right half - reserved for virtual keyboard
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginForm(
    serverUrl: String,
    username: String,
    password: String,
    errorMessage: String?,
    onServerUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val serverUrlFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val loginButtonFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        serverUrlFocusRequester.requestFocus()
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth(0.8f), // Compact form - 80% of left half width
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // Reduced spacing
    ) {
        // Title
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Error message
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Server IP field
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Server IP",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextField(
                value = serverUrl,
                onValueChange = onServerUrlChange,
                placeholder = "192.168.1.100:8096",
                focusRequester = serverUrlFocusRequester,
                onNext = { usernameFocusRequester.requestFocus() },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties {
                        down = usernameFocusRequester
                    }
            )
        }
        
        // Username field
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Username",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextField(
                value = username,
                onValueChange = onUsernameChange,
                placeholder = "Enter username",
                focusRequester = usernameFocusRequester,
                onNext = { passwordFocusRequester.requestFocus() },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties {
                        up = serverUrlFocusRequester
                        down = passwordFocusRequester
                    }
            )
        }
        
        // Password field
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Password",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = "Enter password",
                isPassword = true,
                focusRequester = passwordFocusRequester,
                onNext = { loginButtonFocusRequester.requestFocus() },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties {
                        up = usernameFocusRequester
                        down = loginButtonFocusRequester
                    }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Login button - wrapped in Box with clickable for mobile touch support
        // while keeping Button's onClick for TV remote/keyboard navigation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { onLoginClick() } // Enable touch support for mobile
        ) {
            Button(
                onClick = onLoginClick, // TV remote/keyboard navigation
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(loginButtonFocusRequester)
                    .focusProperties {
                        up = passwordFocusRequester
                    },
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                scale = ButtonDefaults.scale(),
                glow = ButtonDefaults.glow(),
                colors = ButtonDefaults.colors()
            ) {
                Text(
                    "Login",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    onNext: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    // Show virtual keyboard when field gets focus
    LaunchedEffect(isFocused) {
        if (isFocused) {
            val imm = context.getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    
    Surface(
        modifier = modifier
            .height(56.dp),
        onClick = { focusRequester.requestFocus() },
        colors = if (isFocused) {
            ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            ClickableSurfaceDefaults.colors()
        },
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                BorderStroke(
                    width = if (isFocused) 3.dp else 1.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.border
                ),
                shape = JetStreamCardShape
            )
        ),
        shape = ClickableSurfaceDefaults.shape(shape = JetStreamCardShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusable(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onNext() }
                ),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                interactionSource = interactionSource,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                cursorBrush = Brush.verticalGradient(
                    colors = listOf(
                        LocalContentColor.current,
                        LocalContentColor.current
                    )
                ),
                singleLine = true
            )
            if (value.isEmpty() && !isFocused) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}
