package com.hativ2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.NotionBlue
import com.hativ2.ui.theme.NotionGray
import com.hativ2.ui.theme.NotionGreen
import com.hativ2.ui.theme.NotionOrange
import com.hativ2.ui.theme.NotionPink
import com.hativ2.ui.theme.NotionPurple
import com.hativ2.ui.theme.NotionRed
import com.hativ2.ui.theme.NotionWhite
import com.hativ2.ui.theme.NotionYellow

@Composable
fun EditDashboardDialog(
    title: String,
    type: String,
    color: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var newTitle by remember { mutableStateOf(title) }
    var selectedType by remember { mutableStateOf(type) }
    var selectedColor by remember { mutableStateOf(color) }

    val colors = listOf(
        "gray" to NotionGray,
        "blue" to NotionBlue,
        "green" to NotionGreen,
        "yellow" to NotionYellow,
        "pink" to NotionPink,
        "purple" to NotionPurple,
        "orange" to NotionOrange
    )

    data class VolumeType(val id: String, val label: String, val icon: String)
    val types = listOf(
        VolumeType("travel", "Travel", "✈️"),
        VolumeType("household", "Household", "🏠"),
        VolumeType("event", "Event", "🎉"),
        VolumeType("other", "Other", "📁")
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Hard Shadow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 8.dp, y = 8.dp)
                    .background(MangaBlack)
            )

            // Content Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NotionWhite)
                    .border(3.dp, MangaBlack)
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        "EDIT VOLUME",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.fillMaxWidth(),
                        color = MangaBlack
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Volume Title
                    Text("Volume Title", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    MangaTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = "e.g., Cebu City Arc",
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Story Type Grid
                    Text("Story Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        types.forEach { typeItem ->
                            val isSelected = selectedType == typeItem.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(if (isSelected) NotionYellow else NotionWhite)
                                    .border(if (isSelected) 3.dp else 2.dp, MangaBlack)
                                    .clickable { selectedType = typeItem.id }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(typeItem.icon, style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        typeItem.label, 
                                        style = MaterialTheme.typography.labelSmall, 
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Spine Color
                    Text("Spine Color", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { (name, colorItem) ->
                            val isSelected = selectedColor == name
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(colorItem)
                                    .border(if (isSelected) 3.dp else 2.dp, MangaBlack)
                                    .clickable { selectedColor = name }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Cancel Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(NotionRed)
                                .border(2.dp, MangaBlack)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        // Save Button
                        Box(
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(x = 4.dp, y = 4.dp)
                                    .background(if (newTitle.isNotBlank()) MangaBlack else Color.Gray)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(if (newTitle.isNotBlank()) NotionGreen else Color.LightGray)
                                    .border(2.dp, MangaBlack)
                                    .clickable(enabled = newTitle.isNotBlank()) {
                                        onSave(newTitle, selectedType, selectedColor)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "SAVE CHANGES", 
                                    fontWeight = FontWeight.Black,
                                    color = if (newTitle.isNotBlank()) MangaBlack else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
