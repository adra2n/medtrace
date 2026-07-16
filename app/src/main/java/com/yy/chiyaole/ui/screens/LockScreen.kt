package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yy.chiyaole.ui.theme.Primary

private const val PIN_LENGTH = 6

@Composable
fun LockScreen(
    pinEnabled: Boolean,
    biometricEnabled: Boolean,
    onBiometricClick: () -> Unit,
    onPinEntered: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun onDigit(d: String) {
        if (pin.length < PIN_LENGTH) {
            pin += d
            error = false
            if (pin.length == PIN_LENGTH) {
                if (!onPinEntered(pin)) {
                    error = true
                    pin = ""
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
                contentDescription = "医迹",
                tint = Primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("医迹已锁定", style = MaterialTheme.typography.titleLarge)
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
                                if (filled) Primary
                                else if (error) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            if (error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "PIN 错误，请重试",
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
                    Text("点击验证指纹 / 面容")
                }
            }
        }
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
                                Icon(Icons.Filled.Backspace, contentDescription = "删除")
                            }
                            else -> Text(
                                key,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Medium,
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
                    contentDescription = "指纹 / 面容",
                    tint = Primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
