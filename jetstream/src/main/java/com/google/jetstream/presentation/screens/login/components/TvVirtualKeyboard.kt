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

package com.google.jetstream.presentation.screens.login.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/**
 * Virtual keyboard for TV input
 * Displays on the right side of the screen
 */
@Composable
fun TvVirtualKeyboard(
    onKeyPress: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isShiftPressed by remember { mutableStateOf(false) }
    var isSymbolMode by remember { mutableStateOf(false) }
    
    val letters = if (isShiftPressed) {
        listOf(
            listOf('Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'),
            listOf('A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L'),
            listOf('Z', 'X', 'C', 'V', 'B', 'N', 'M')
        )
    } else {
        listOf(
            listOf('q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'),
            listOf('a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l'),
            listOf('z', 'x', 'c', 'v', 'b', 'n', 'm')
        )
    }
    
    val numbers = listOf(
        listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0'),
        listOf('!', '@', '#', '$', '%', '^', '&', '*', '(', ')'),
        listOf('-', '_', '.', ',')
    )
    
    val keys = if (isSymbolMode) numbers else letters
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Numbers row (if not in symbol mode)
        if (!isSymbolMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0').forEach { char ->
                    KeyboardKey(
                        text = char.toString(),
                        onClick = { onKeyPress(char) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Letter rows
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { char ->
                    KeyboardKey(
                        text = char.toString(),
                        onClick = { onKeyPress(char) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Bottom row with special keys
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Shift key
            KeyboardKey(
                text = if (isShiftPressed) "↑" else "",
                icon = if (!isShiftPressed) Icons.Default.KeyboardArrowUp else null,
                onClick = { isShiftPressed = !isShiftPressed },
                modifier = Modifier.weight(1.2f),
                isSpecial = true
            )
            
            // Symbol/Number toggle
            KeyboardKey(
                text = if (isSymbolMode) "ABC" else "?123",
                onClick = {
                    isSymbolMode = !isSymbolMode
                    isShiftPressed = false
                },
                modifier = Modifier.weight(1.2f),
                isSpecial = true
            )
            
            // Hyphen (if in letter mode)
            if (!isSymbolMode) {
                KeyboardKey(
                    text = "-",
                    onClick = { onKeyPress('-') },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Underscore (if in letter mode)
            if (!isSymbolMode) {
                KeyboardKey(
                    text = "_",
                    onClick = { onKeyPress('_') },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Period
            if (!isSymbolMode) {
                KeyboardKey(
                    text = ".",
                    onClick = { onKeyPress('.') },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Left arrow
            KeyboardKey(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                onClick = { /* Move cursor left - handled by text field */ },
                modifier = Modifier.weight(1f),
                isSpecial = true
            )
            
            // Right arrow
            KeyboardKey(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                onClick = { /* Move cursor right - handled by text field */ },
                modifier = Modifier.weight(1f),
                isSpecial = true
            )
            
            // Backspace
            KeyboardKey(
                icon = Icons.Default.Backspace,
                onClick = onBackspace,
                modifier = Modifier.weight(1f),
                isSpecial = true
            )
            
            // Enter/Submit
            KeyboardKey(
                icon = Icons.Default.Check,
                onClick = onEnter,
                modifier = Modifier.weight(1f),
                isSpecial = true,
                isPrimary = true
            )
        }
    }
}

@Composable
private fun KeyboardKey(
    text: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSpecial: Boolean = false,
    isPrimary: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { isFocused = it.hasFocus },
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = when {
                isPrimary && isFocused -> MaterialTheme.colorScheme.primary
                isPrimary -> MaterialTheme.colorScheme.primaryContainer
                isSpecial && isFocused -> MaterialTheme.colorScheme.secondaryContainer
                isSpecial -> MaterialTheme.colorScheme.surfaceVariant
                isFocused -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            },
            focusedContainerColor = when {
                isPrimary -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(6.dp)),
        border = if (isFocused) {
            androidx.tv.material3.ClickableSurfaceDefaults.border(
                focusedBorder = androidx.tv.material3.Border(
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
            )
        } else {
            androidx.tv.material3.ClickableSurfaceDefaults.border()
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text.ifBlank { "Keyboard action" },
                    tint = if (isPrimary && isFocused) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(24.dp)
                )
            } else if (text.isNotBlank()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isPrimary && isFocused) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

