package com.hativ2.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hativ2.ui.theme.NotionRed
import com.hativ2.ui.theme.NotionWhite

@Composable
fun MangaDeleteDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        containerColor = NotionWhite,
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = text) },
        confirmButton = {
            MangaButton(
                onClick = onConfirm,
                backgroundColor = NotionRed
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            MangaButton(
                onClick = onDismiss,
                backgroundColor = NotionWhite
            ) {
                Text("Cancel")
            }
        }
    )
}
