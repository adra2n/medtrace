package com.yy.medtrace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yy.medtrace.R
import com.yy.medtrace.data.security.PinManager
import kotlinx.coroutines.delay

private const val PIN_LENGTH = 6

@Composable
fun LockScreen(
    pinEnabled: Boolean,
    biometricEnabled: Boolean,
    onBiometricClick: () -> Unit,
    onPinEntered: (String) -> Boolean,
    onForgotPin: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showForgot by remember { mutableStateOf(false) }
    var lockoutSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000)
            if (PinManager.isLocked()) {
                lockoutSeconds = PinManager.getLockoutRemainingSeconds()
            } else {
                lockoutSeconds = 0
                error = false
            }
        }
    }

    fun onDigit(d: String) {
        if (lockoutSeconds > 0) return
        if (pin.length < PIN_LENGTH) {
            pin += d
            error = false
            if (pin.length == PIN_LENGTH) {
                if (!onPinEntered(pin)) {
                    error = true
                    pin = ""
                    if (PinManager.isLocked()) {
                        lockoutSeconds = PinManager.getLockoutRemainingSeconds()
                    }
                } else {
                    pin = ""
                }
            }
        }
    }

    fun onDelete() {
        if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
            error = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Fingerprint,
                contentDescription = stringResource(R.string.lock_app_name),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))

            // PIN 圆点
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(PIN_LENGTH) { i ->
                    val filled = i < pin.length
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else if (error) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            if (error && lockoutSeconds == 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.lock_pin_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (lockoutSeconds > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.lock_pin_locked, lockoutSeconds),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(32.dp))

            if (pinEnabled) {
                Keypad(
                    onDigit = ::onDigit,
                    onDelete = ::onDelete,
                    showBiometric = biometricEnabled,
                    onBiometric = onBiometricClick
                )
            } else {
                Button(onClick = onBiometricClick) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.lock_btn_verify_biometric))
                }
            }

            if (onForgotPin != null) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { showForgot = true }) {
                    Text(stringResource(R.string.lock_btn_forgot_pin), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showForgot) {
        AlertDialog(
            onDismissRequest = { showForgot = false },
            title = { Text(stringResource(R.string.lock_dialog_forgot_title)) },
            text = {
                Text(
                    stringResource(R.string.lock_dialog_forgot_message)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showForgot = false
                    onForgotPin?.invoke()
                }) { Text(stringResource(R.string.lock_btn_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showForgot = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }
}

@Composable
private fun Keypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    showBiometric: Boolean,
    onBiometric: () -> Unit
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "del")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "" -> {}
                            "del" -> IconButton(onClick = onDelete) {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = stringResource(R.string.lock_cd_delete))
                            }
                            else -> Text(
                                key,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .clickable { onDigit(key) }
                                    .wrapContentSize(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
        if (showBiometric) {
            Spacer(Modifier.height(8.dp))
            IconButton(onClick = onBiometric, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = stringResource(R.string.lock_cd_biometric),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
