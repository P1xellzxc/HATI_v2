# HATI v2 - UI Enhancement Guide

## 🎨 Design Philosophy

**Manga-Inspired Visual Language**
- Bold, high-contrast black and white
- Dynamic motion and impact
- Hand-drawn aesthetic
- Speed lines and action effects

---

## 1. Enhanced Color System

### Current Colors
```kotlin
object MangaColors {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
}
```

### Enhanced Palette
```kotlin
object MangaColors {
    // Primary Colors
    val InkBlack = Color(0xFF000000)
    val PaperWhite = Color(0xFFFFFFFF)
    
    // Accent Colors (subtle manga tones)
    val ActionRed = Color(0xFFE63946)      // For important actions
    val FocusBlue = Color(0xFF457B9D)      // For highlights
    val SuccessGreen = Color(0xFF2A9D8F)   // For confirmations
    val WarningOrange = Color(0xFFF77F00)  // For warnings
    
    // Grayscale (halftone effects)
    val Gray10 = Color(0xFFE6E6E6)
    val Gray20 = Color(0xFFCCCCCC)
    val Gray30 = Color(0xFFB3B3B3)
    val Gray40 = Color(0xFF999999)
    val Gray50 = Color(0xFF808080)
    
    // Transparency layers
    val OverlayLight = Color(0x1A000000)   // 10% black
    val OverlayMedium = Color(0x33000000)  // 20% black
    val OverlayHeavy = Color(0x80000000)   // 50% black
}
```

---

## 2. Enhanced Typography

### Current Typography
Basic Bangers font for headers, default for body.

### Enhanced System
```kotlin
// res/font/ - Add new fonts:
// - manga_temple.ttf (for dramatic headers)
// - cc_wild_words.ttf (for sound effects)
// - anime_ace.ttf (for body text with character)

val MangaTempleFamily = FontFamily(Font(R.font.manga_temple, FontWeight.Bold))
val AnimeAceFamily = FontFamily(Font(R.font.anime_ace, FontWeight.Normal))
val WildWordsFamily = FontFamily(Font(R.font.cc_wild_words, FontWeight.Bold))

object MangaTypography {
    // Dramatic Headers
    val displayLarge = TextStyle(
        fontFamily = MangaTempleFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = 2.sp,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.3f),
            offset = Offset(4f, 4f),
            blurRadius = 0f
        )
    )
    
    // Sound Effects Style
    val soundEffect = TextStyle(
        fontFamily = WildWordsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 4.sp
    )
    
    // Comic Book Style Body
    val bodyManga = TextStyle(
        fontFamily = AnimeAceFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    
    // Speech Bubble Text
    val speechBubble = TextStyle(
        fontFamily = AnimeAceFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        textAlign = TextAlign.Center
    )
}
```

---

## 3. Advanced UI Components

### 3.1 Speech Bubble Component

```kotlin
@Composable
fun SpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    tailPosition: BubbleTail = BubbleTail.BottomLeft
) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            // Draw rounded rectangle
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, size.width - 40.dp.toPx(), size.height - 40.dp.toPx()),
                    radiusX = 12.dp.toPx(),
                    radiusY = 12.dp.toPx()
                )
            )
            
            // Add tail based on position
            when (tailPosition) {
                BubbleTail.BottomLeft -> {
                    moveTo(40.dp.toPx(), size.height - 40.dp.toPx())
                    lineTo(20.dp.toPx(), size.height - 10.dp.toPx())
                    lineTo(60.dp.toPx(), size.height - 40.dp.toPx())
                }
                // ... other positions
            }
        }
        
        // Fill
        drawPath(path, color = Color.White)
        
        // Black border
        drawPath(
            path = path,
            color = Color.Black,
            style = Stroke(width = 3.dp.toPx())
        )
    }
    
    Box(
        modifier = Modifier
            .padding(16.dp)
            .align(Alignment.Center)
    ) {
        BasicText(
            text = text,
            style = MangaTypography.speechBubble
        )
    }
}

enum class BubbleTail {
    BottomLeft, BottomRight, TopLeft, TopRight
}
```

### 3.2 Impact Lines (Speed Lines)

```kotlin
@Composable
fun ImpactLines(
    modifier: Modifier = Modifier,
    lineCount: Int = 20,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = sqrt(size.width * size.width + size.height * size.height)
        
        for (i in 0 until lineCount) {
            val angle = (rotation + (i * 360f / lineCount)) * PI.toFloat() / 180f
            
            drawLine(
                color = Color.Black.copy(alpha = 0.1f),
                start = center,
                end = Offset(
                    center.x + cos(angle) * maxRadius,
                    center.y + sin(angle) * maxRadius
                ),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
```

### 3.3 Action Burst Effect

```kotlin
@Composable
fun ActionBurst(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = tween(durationMillis = 500)
    )
    
    Canvas(modifier = modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }) {
        val center = Offset(size.width / 2, size.height / 2)
        val starPoints = 8
        val outerRadius = size.minDimension / 2
        val innerRadius = outerRadius * 0.4f
        
        val path = Path().apply {
            for (i in 0 until starPoints * 2) {
                val angle = (i * PI / starPoints).toFloat()
                val radius = if (i % 2 == 0) outerRadius else innerRadius
                
                val x = center.x + cos(angle) * radius
                val y = center.y + sin(angle) * radius
                
                if (i == 0) moveTo(x, y)
                else lineTo(x, y)
            }
            close()
        }
        
        drawPath(path, color = MangaColors.ActionRed)
        drawPath(path, color = Color.Black, style = Stroke(width = 3.dp.toPx()))
    }
}
```

### 3.4 Manga Panel Divider

```kotlin
@Composable
fun MangaPanelDivider(
    modifier: Modifier = Modifier,
    jagged: Boolean = true
) {
    Canvas(modifier = modifier.fillMaxWidth().height(4.dp)) {
        if (jagged) {
            // Zigzag pattern
            val path = Path().apply {
                var x = 0f
                val zigzagWidth = 20.dp.toPx()
                val zigzagHeight = size.height
                
                moveTo(0f, 0f)
                while (x < size.width) {
                    lineTo(x + zigzagWidth / 2, zigzagHeight)
                    lineTo(x + zigzagWidth, 0f)
                    x += zigzagWidth
                }
            }
            drawPath(path, color = Color.Black, style = Stroke(width = 3.dp.toPx()))
        } else {
            // Straight line
            drawLine(
                color = Color.Black,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}
```

---

## 4. Enhanced Animations

### 4.1 Comic Panel Entrance

```kotlin
@Composable
fun ComicPanelEntrance(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (visible) 0f else -5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )
    
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                rotationZ = rotation
            }
    ) {
        content()
    }
}
```

### 4.2 Onomatopoeia Effect

```kotlin
@Composable
fun OnomatopoeiaEffect(
    text: String,
    trigger: Boolean,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(trigger) {
        if (trigger) {
            visible = true
            delay(1500)
            visible = false
        }
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.3f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = fadeOut()
    ) {
        BasicText(
            text = text,
            style = MangaTypography.soundEffect.copy(
                color = MangaColors.ActionRed,
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(4f, 4f)
                )
            ),
            modifier = modifier.rotate(-15f)
        )
    }
}

// Usage
OnomatopoeiaEffect(
    text = "SWOOSH!",
    trigger = transactionAdded
)
```

---

## 5. Enhanced Screens

### 5.1 Splash Screen

```kotlin
@Composable
fun SplashScreen(onComplete: () -> Unit) {
    var phase by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        delay(500)
        phase = 1
        delay(1000)
        phase = 2
        delay(500)
        onComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MangaColors.InkBlack),
        contentAlignment = Alignment.Center
    ) {
        // Animated logo
        ComicPanelEntrance(visible = phase >= 1) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Large "HATI" in manga style
                BasicText(
                    "HATI",
                    style = MangaTypography.displayLarge.copy(
                        color = MangaColors.PaperWhite,
                        fontSize = 72.sp
                    )
                )
                
                if (phase >= 2) {
                    OnomatopoeiaEffect(
                        text = "²",
                        trigger = true
                    )
                }
            }
        }
        
        // Impact lines in background
        if (phase >= 1) {
            ImpactLines(animated = true)
        }
    }
}
```

### 5.2 Enhanced Transaction Card

```kotlin
@Composable
fun EnhancedTransactionCard(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }
    
    MangaCard(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        shadowOffset = if (isPressed) 2.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Description with icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(transaction.category)
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicText(
                        transaction.description,
                        style = MangaTypography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Metadata
                Row {
                    BasicText(
                        "Paid by ${transaction.paidBy}",
                        style = MangaTypography.bodyManga.copy(
                            color = MangaColors.Gray40
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicText(
                        "•",
                        style = MangaTypography.bodyManga
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicText(
                        transaction.createdAt.toDisplayDate(),
                        style = MangaTypography.bodyManga.copy(
                            color = MangaColors.Gray40
                        )
                    )
                }
            }
            
            // Amount with emphasis
            SpeechBubble(
                text = transaction.amount.toDisplayAmount(),
                tailPosition = BubbleTail.Left
            )
        }
    }
}

@Composable
fun CategoryIcon(category: String) {
    val emoji = when (category) {
        "food" -> "🍜"
        "transport" -> "🚗"
        "entertainment" -> "🎮"
        "shopping" -> "🛒"
        else -> "💰"
    }
    
    BasicText(
        emoji,
        style = MangaTypography.headlineSmall
    )
}
```

### 5.3 Empty State with Character

```kotlin
@Composable
fun EmptyTransactionsState(
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Manga character illustration
        Image(
            painter = painterResource(R.drawable.manga_character_empty),
            contentDescription = "No transactions",
            modifier = Modifier.size(200.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Speech bubble with message
        SpeechBubble(
            text = "No expenses yet!\nLet's start tracking!",
            tailPosition = BubbleTail.Top
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action button
        MangaButton(
            onClick = onAddClick,
            text = "Add First Expense",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

---

## 6. Dark Mode Support

```kotlin
// Update Color system
object MangaColors {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    
    val Background = if (isSystemInDarkTheme) InkBlack else PaperWhite
    val OnBackground = if (isSystemInDarkTheme) PaperWhite else InkBlack
    val Surface = if (isSystemInDarkTheme) Color(0xFF1A1A1A) else PaperWhite
    val OnSurface = if (isSystemInDarkTheme) PaperWhite else InkBlack
}

@Composable
fun HatiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        MangaColors.copy(
            Background = MangaColors.InkBlack,
            Surface = Color(0xFF1A1A1A),
            // Inverted halftone patterns for dark mode
        )
    } else {
        MangaColors
    }
    
    CompositionLocalProvider(
        LocalMangaColors provides colors,
        content = content
    )
}
```

---

## 7. Accessibility Enhancements

### 7.1 Content Descriptions

```kotlin
@Composable
fun AccessibleTransactionCard(transaction: Transaction) {
    MangaCard(
        modifier = Modifier.semantics {
            contentDescription = """
                Transaction: ${transaction.description}
                Amount: ${transaction.amount.toDisplayAmount()}
                Paid by: ${transaction.paidBy}
                Date: ${transaction.createdAt.toDisplayDate()}
            """.trimIndent()
            
            role = Role.Button
        }
    ) {
        // Card content
    }
}
```

### 7.2 Scalable Text

```kotlin
@Composable
fun ScalableText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale
    val adjustedStyle = style.copy(
        fontSize = style.fontSize * fontScale.coerceIn(0.85f, 1.3f)
    )
    
    BasicText(
        text = text,
        style = adjustedStyle,
        modifier = modifier
    )
}
```

---

## 8. Performance Optimizations

### 8.1 Lazy Loading Images

```kotlin
@Composable
fun OptimizedImage(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier
    )
}
```

### 8.2 Efficient Recomposition

```kotlin
@Composable
fun OptimizedTransactionList(
    transactions: List<Transaction>
) {
    // Use key to prevent unnecessary recomposition
    LazyColumn {
        items(
            items = transactions,
            key = { transaction -> transaction.id }
        ) { transaction ->
            TransactionCard(transaction)
        }
    }
}
```

---

## UI Enhancement Checklist

### Phase 1: Foundation
- [ ] Implement enhanced color system
- [ ] Add new typography styles
- [ ] Create dark mode support
- [ ] Add accessibility features

### Phase 2: Components
- [ ] Speech bubble component
- [ ] Impact lines effect
- [ ] Action burst animation
- [ ] Panel dividers

### Phase 3: Screens
- [ ] Enhanced splash screen
- [ ] Improved transaction cards
- [ ] Better empty states
- [ ] Loading skeletons

### Phase 4: Polish
- [ ] Sound effects (optional)
- [ ] Haptic feedback
- [ ] Micro-interactions
- [ ] Performance optimization

---

## Testing UI

```kotlin
@Test
fun speechBubble_rendersCorrectly() {
    composeTestRule.setContent {
        SpeechBubble(text = "Test")
    }
    
    composeTestRule.onNodeWithText("Test").assertIsDisplayed()
}
```

---

**Remember**: UI enhancements should improve usability, not just aesthetics!
