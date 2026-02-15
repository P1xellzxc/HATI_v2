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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun AddDashboardDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, type: String, color: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("blue") }
    var selectedType by remember { mutableStateOf("travel") }

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
                    .offset(x = 4.dp, y = 4.dp)
                    .background(MangaBlack)
            )

            // Content Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NotionWhite)
                    .border(2.dp, MangaBlack)
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        "CREATE NEW VOLUME",
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
                        value = title,
                        onValueChange = { title = it },
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
                        types.forEach { type ->
                            val isSelected = selectedType == type.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(if (isSelected) NotionYellow else NotionWhite)
                                    .border(if (isSelected) 3.dp else 2.dp, MangaBlack)
                                    .clickable { selectedType = type.id }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(type.icon, style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        type.label, 
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
                        colors.forEach { (name, color) ->
                            val isSelected = selectedColor == name
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color)
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
                                .background(NotionWhite)
                                .border(2.dp, MangaBlack)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.labelLarge)
                        }

                        // Create Button
                        Box(
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(x = 4.dp, y = 4.dp)
                                    .background(if (title.isNotBlank()) MangaBlack else Color.Gray)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(if (title.isNotBlank()) NotionGreen else Color.LightGray)
                                    .border(2.dp, MangaBlack)
                                    .clickable(enabled = title.isNotBlank()) {
                                        onCreate(title, selectedType, selectedColor)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "CREATE VOLUME", 
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (title.isNotBlank()) MangaBlack else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
