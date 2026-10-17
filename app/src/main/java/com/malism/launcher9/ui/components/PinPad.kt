package com.malism.launcher9.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinPad(
    pin: String,
    error: String?,
    enabled: Boolean,
    showBiometric: Boolean,
    submitLabel: String,
    biometricLabel: String,
    deleteLabel: String,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
    onBiometric: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "●".repeat(pin.length).ifEmpty { " " },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
        val rows = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
        )
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { digit ->
                    DigitButton(
                        label = digit.toString(),
                        enabled = enabled,
                        onClick = { onDigit(digit) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onDelete,
                enabled = enabled && pin.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
            ) {
                Text(deleteLabel, fontSize = 18.sp)
            }
            DigitButton(
                label = "0",
                enabled = enabled,
                onClick = { onDigit(0) },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onSubmit,
                enabled = enabled && pin.length in 4..6,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
            ) {
                Text(submitLabel, fontSize = 18.sp)
            }
        }
        if (showBiometric) {
            TextButton(onClick = onBiometric, enabled = enabled) {
                Text(biometricLabel, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun DigitButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(64.dp),
    ) {
        Text(label, fontSize = 24.sp)
    }
}

@Composable
fun LargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Text(text, fontSize = 18.sp)
    }
}
