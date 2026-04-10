package com.hativ2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.NotionWhite
import com.hativ2.ui.theme.NotionYellow

/**
 * Export format enum used by the dialog and callers to distinguish CSV vs JSON.
 */
enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    CSV("CSV (Spreadsheet)", "csv", "text/csv"),
    JSON("JSON (Structured)", "json", "application/json")
}

/**
 * Warning dialog shown before exporting files containing financial data.
 * Now includes format selection (CSV or JSON).
 *
 * Why a separate dialog instead of exporting immediately:
 *   - Export files are saved in plain text to a user-chosen location (SAF).
 *     Once exported, the file is outside our control and could be shared,
 *     backed up, or accessed by other apps.
 *   - Explicit confirmation ensures the user understands they're exporting
 *     sensitive financial data, reducing accidental exposure.
 */
@Composable
fun ExportWarningDialog(
    onConfirm: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }

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
                    "The exported file will not be encrypted. " +
                        "Store it securely and avoid sharing it over unencrypted channels.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MangaBlack.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Format selection
                Text(
                    "EXPORT FORMAT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExportFormat.entries.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                            .border(
                                width = if (selectedFormat == format) 2.dp else 1.dp,
                                color = if (selectedFormat == format) MangaBlack else MangaBlack.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(MangaCornerRadius)
                            )
                            .background(
                                if (selectedFormat == format) NotionYellow.copy(alpha = 0.2f)
                                else NotionWhite,
                                RoundedCornerShape(MangaCornerRadius)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            colors = RadioButtonDefaults.colors(selectedColor = MangaBlack)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            format.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedFormat == format) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    if (format != ExportFormat.entries.last()) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            MangaButton(
                onClick = { onConfirm(selectedFormat) },
                backgroundColor = NotionYellow
            ) {
                Text("Export ${selectedFormat.name}", fontWeight = FontWeight.Bold)
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
