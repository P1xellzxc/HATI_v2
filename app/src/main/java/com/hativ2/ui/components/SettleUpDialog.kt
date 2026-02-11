package com.hativ2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hativ2.data.entity.PersonEntity
import com.hativ2.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSettle: (fromId: String, toId: String, amount: Double) -> Unit,
    initialFromId: String?,
    initialToId: String?,
    initialAmount: Double,
    allPeople: List<PersonEntity>
) {
    if (!isOpen) return

    var fromId by remember(isOpen) { mutableStateOf(initialFromId ?: "") }
    var toId by remember(isOpen) { mutableStateOf(initialToId ?: "") }
    var amountText by remember(isOpen) { mutableStateOf(if (initialAmount > 0) initialAmount.toString() else "") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = fromId.isNotEmpty() && toId.isNotEmpty() && fromId != toId && amount > 0

    Dialog(onDismissRequest = onDismiss) {
        Box {
            // Shadow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 6.dp, y = 6.dp)
                    .background(MangaBlack, RoundedCornerShape(MangaCornerRadius))
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NotionWhite, RoundedCornerShape(MangaCornerRadius))
                    .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NotionPurple)
                        .border(width = 0.dp, color = Color.Transparent, shape = RoundedCornerShape(topStart = MangaCornerRadius, topEnd = MangaCornerRadius))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "SETTLE UP",
                            style = MangaHeaderStyle,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Record a payment to clear a debt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Divider(color = MangaBlack, thickness = 2.dp)

                Column(modifier = Modifier.padding(16.dp)) {
                    // Payer -> Recipient Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Payer Dropdown
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Payer",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = fromExpanded,
                                onExpandedChange = { fromExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = allPeople.find { it.id == fromId }?.name ?: "Select",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius)),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = fromExpanded,
                                    onDismissRequest = { fromExpanded = false }
                                ) {
                                    allPeople.forEach { person ->
                                        DropdownMenuItem(
                                            text = { Text(person.name) },
                                            onClick = {
                                                fromId = person.id
                                                fromExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Arrow
                        Box(
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MangaBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "To",
                                tint = NotionWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Recipient Dropdown
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recipient",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = toExpanded,
                                onExpandedChange = { toExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = allPeople.find { it.id == toId }?.name ?: "Select",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius)),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = toExpanded,
                                    onDismissRequest = { toExpanded = false }
                                ) {
                                    allPeople.forEach { person ->
                                        DropdownMenuItem(
                                            text = { Text(person.name) },
                                            onClick = {
                                                toId = person.id
                                                toExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Amount Field
                    Column {
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius)),
                            prefix = { Text("₱", fontWeight = FontWeight.Bold) },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        
                        // Match indicator
                        if (initialAmount > 0 && kotlin.math.abs(amount - initialAmount) < 0.01) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Matches owed amount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel
                        Box(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = 2.dp, y = 2.dp)
                                    .background(MangaBlack, RoundedCornerShape(MangaCornerRadius))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(NotionWhite, RoundedCornerShape(MangaCornerRadius))
                                    .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                                    .clickable { onDismiss() }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cancel", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Settle
                        Box(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = 2.dp, y = 2.dp)
                                    .background(if (isValid) Color(0xFF555555) else Color.Gray, RoundedCornerShape(MangaCornerRadius))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isValid) MangaBlack else Color.LightGray, RoundedCornerShape(MangaCornerRadius))
                                    .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                                    .clickable(enabled = isValid) {
                                        onSettle(fromId, toId, amount)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "SETTLE UP",
                                    color = if (isValid) NotionWhite else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    style = MangaHeaderStyle,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
