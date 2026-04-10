package com.hativ2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.NotionWhite
import com.hativ2.ui.theme.NotionYellow

/**
 * Warning dialog shown before exporting CSV files containing financial data.
 *
 * Why a separate dialog instead of exporting immediately:
 *   - CSV files are saved in plain text to a user-chosen location (SAF).
 *     Once exported, the file is outside our control and could be shared,
 *     backed up, or accessed by other apps.
 *   - Explicit confirmation ensures the user understands they're exporting
 *     sensitive financial data, reducing accidental exposure.
 */
@Composable
fun ExportWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NotionWhite,
        modifier = Modifier.border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚠️", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Export Financial Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
        },
        text = {
            Column {
                Text(
                    "This file contains your financial data including expenses, " +
                        "amounts, and participant names.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "The exported CSV file will not be encrypted. " +
                        "Store it securely and avoid sharing it over unencrypted channels.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MangaBlack.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            MangaButton(
                onClick = onConfirm,
                backgroundColor = NotionYellow
            ) {
                Text("Export Anyway", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            MangaButton(
                onClick = onDismiss,
                backgroundColor = NotionWhite
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}
