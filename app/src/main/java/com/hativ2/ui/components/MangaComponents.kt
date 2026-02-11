package com.hativ2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hativ2.ui.theme.MangaBlack
import com.hativ2.ui.theme.MangaHeaderStyle
import com.hativ2.ui.theme.NotionWhite

val MangaBorderWidth = 2.dp
val MangaCornerRadius = 4.dp
val MangaShadowOffset = 4.dp
val MangaShadowOffsetSmall = 2.dp

@Composable
fun MangaCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = NotionWhite,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    // Hard shadow using a Box behind
    Box(modifier = modifier) {
        // Shadow Box
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = MangaShadowOffset, y = MangaShadowOffset)
                .background(MangaBlack, RoundedCornerShape(MangaCornerRadius))
        )

        // Main Content Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, RoundedCornerShape(MangaCornerRadius))
                .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                .clip(RoundedCornerShape(MangaCornerRadius))
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                )
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun MangaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = MangaBlack,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val currentOffset = if (isPressed) 0.dp else MangaShadowOffsetSmall

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Custom indication via offset
                enabled = enabled,
                onClick = onClick
            )
    ) {
        // Shadow
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = MangaShadowOffsetSmall, y = MangaShadowOffsetSmall)
                    .background(MangaBlack, RoundedCornerShape(MangaCornerRadius))
            )
        }

        // Button Surface
        Box(
            modifier = Modifier
                .offset(x = if (isPressed) MangaShadowOffsetSmall else 0.dp, y = if (isPressed) MangaShadowOffsetSmall else 0.dp)
                .background(if (enabled) backgroundColor else Color.Gray, RoundedCornerShape(MangaCornerRadius))
                .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.labelLarge.copy(color = contentColor, fontWeight = FontWeight.Bold)) {
                // We need to fake a RowScope here or just use a Row
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

@Composable
fun MangaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = keyboardOptions,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NotionWhite, RoundedCornerShape(MangaCornerRadius))
                        .border(MangaBorderWidth, MangaBlack, RoundedCornerShape(MangaCornerRadius))
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "", // Placeholder if needed
                            color = Color.Gray
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
