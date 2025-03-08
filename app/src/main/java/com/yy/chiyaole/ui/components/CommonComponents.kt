package com.yy.chiyaole.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "添加"
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = label,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { 
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            isError = isError,
            supportingText = {
                if (isError && errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                } else if (hint != null) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        )
    }
}
