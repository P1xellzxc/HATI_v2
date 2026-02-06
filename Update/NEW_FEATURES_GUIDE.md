# HATI v2 - New Features Implementation Guide

## Overview

This document outlines 15+ new features to enhance HATI v2's functionality, user experience, and market competitiveness.

---

## 🎯 Priority 1 Features (Must Have)

### 1. Expense Splitting & Settlement Calculator

**Business Value**: Core feature for expense splitting apps

**Implementation**:

```kotlin
// domain/model/Settlement.kt
data class Settlement(
    val from: String,
    val to: String,
    val amount: Double
)

// domain/usecase/CalculateSettlementsUseCase.kt
class CalculateSettlementsUseCase @Inject constructor() {
    
    /**
     * Calculate optimal settlements using greedy algorithm
     * Minimizes number of transactions needed
     */
    operator fun invoke(transactions: List<Transaction>, members: List<String>): List<Settlement> {
        // 1. Calculate net balance for each member
        val balances = members.associateWith { 0.0 }.toMutableMap()
        
        transactions.forEach { transaction ->
            balances[transaction.paidBy] = balances[transaction.paidBy]!! + transaction.amount
            val splitAmount = transaction.amount / members.size
            members.forEach { member ->
                balances[member] = balances[member]!! - splitAmount
            }
        }
        
        // 2. Separate debtors and creditors
        val debtors = balances.filter { it.value < -0.01 }.toMutableMap()
        val creditors = balances.filter { it.value > 0.01 }.toMutableMap()
        
        // 3. Calculate settlements
        val settlements = mutableListOf<Settlement>()
        
        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val (debtor, debtAmount) = debtors.entries.first()
            val (creditor, creditAmount) = creditors.entries.first()
            
            val settlementAmount = minOf(-debtAmount, creditAmount)
            
            settlements.add(
                Settlement(
                    from = debtor,
                    to = creditor,
                    amount = settlementAmount
                )
            )
            
            debtors[debtor] = debtAmount + settlementAmount
            creditors[creditor] = creditAmount - settlementAmount
            
            if (debtors[debtor]!! >= -0.01) debtors.remove(debtor)
            if (creditors[creditor]!! <= 0.01) creditors.remove(creditor)
        }
        
        return settlements
    }
}

// presentation/screen/SettlementScreen.kt
@Composable
fun SettlementScreen(
    groupId: String,
    viewModel: SettlementViewModel = hiltViewModel()
) {
    val settlements by viewModel.settlements.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        AntigravityEntrance {
            MangaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    BasicText(
                        "Who Owes Whom?",
                        style = MangaTypography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicText(
                        "${settlements.size} transactions to settle",
                        style = MangaTypography.bodyLarge
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn {
            itemsIndexed(settlements) { index, settlement ->
                FallingLayout(delay = (index + 1) * 50L) {
                    SettlementCard(settlement)
                }
            }
        }
    }
}

@Composable
fun SettlementCard(settlement: Settlement) {
    MangaCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                BasicText(
                    settlement.from,
                    style = MangaTypography.headlineSmall
                )
                BasicText("pays", style = MangaTypography.labelMedium)
                BasicText(
                    settlement.to,
                    style = MangaTypography.headlineSmall
                )
            }
            
            BasicText(
                "₱${String.format("%.2f", settlement.amount)}",
                style = MangaTypography.headlineLarge.copy(
                    color = MangaColors.Primary
                )
            )
        }
    }
}
```

---

### 2. Group Management System

**Business Value**: Essential for multi-user expense tracking

```kotlin
// domain/model/Group.kt
data class Group(
    val id: String,
    val name: String,
    val description: String,
    val createdBy: String,
    val members: List<GroupMember>,
    val currency: String = "PHP",
    val createdAt: Instant,
    val updatedAt: Instant
)

data class GroupMember(
    val userId: String,
    val name: String,
    val email: String,
    val role: GroupRole,
    val joinedAt: Instant
)

enum class GroupRole {
    ADMIN,
    MEMBER,
    VIEWER
}

// domain/usecase/CreateGroupUseCase.kt
class CreateGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        description: String,
        memberEmails: List<String>
    ): Result<Group> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return Result.failure(Exception("User not authenticated"))
            
            val group = Group(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                createdBy = currentUser.id,
                members = listOf(
                    GroupMember(
                        userId = currentUser.id,
                        name = currentUser.name,
                        email = currentUser.email,
                        role = GroupRole.ADMIN,
                        joinedAt = Clock.System.now()
                    )
                ),
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            
            groupRepository.createGroup(group, memberEmails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// presentation/screen/CreateGroupScreen.kt
@Composable
fun CreateGroupScreen(
    onGroupCreated: (String) -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel()
) {
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var memberEmail by remember { mutableStateOf("") }
    val memberEmails = remember { mutableStateListOf<String>() }
    val createState by viewModel.createState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        MangaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                BasicText(
                    "Create New Group",
                    style = MangaTypography.headlineLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Group name input
                MangaTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = "Group Name",
                    placeholder = "e.g., Tokyo Trip 2026"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Description input
                MangaTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description",
                    placeholder = "What's this group for?"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Member emails
                BasicText(
                    "Invite Members",
                    style = MangaTypography.headlineSmall
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MangaTextField(
                        value = memberEmail,
                        onValueChange = { memberEmail = it },
                        placeholder = "member@email.com",
                        modifier = Modifier.weight(1f)
                    )
                    
                    MangaButton(
                        onClick = {
                            if (memberEmail.isNotBlank()) {
                                memberEmails.add(memberEmail)
                                memberEmail = ""
                            }
                        },
                        text = "Add"
                    )
                }
                
                // Display added members
                memberEmails.forEach { email ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicText(email, style = MangaTypography.bodyLarge)
                        MangaButton(
                            onClick = { memberEmails.remove(email) },
                            text = "Remove"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Create button
                when (createState) {
                    is UiState.Loading -> LoadingIndicator()
                    is UiState.Error -> ErrorMessage((createState as UiState.Error).message)
                    else -> MangaButton(
                        onClick = {
                            viewModel.createGroup(groupName, description, memberEmails)
                        },
                        text = "Create Group",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // Handle success
    LaunchedEffect(createState) {
        if (createState is UiState.Success) {
            val groupId = (createState as UiState.Success).data
            onGroupCreated(groupId)
        }
    }
}
```

---

### 3. Receipt Scanning with OCR

**Business Value**: Saves time and reduces manual entry errors

```kotlin
// data/ocr/ReceiptScanner.kt
interface ReceiptScanner {
    suspend fun scanReceipt(imageUri: Uri): Result<ScannedReceipt>
}

data class ScannedReceipt(
    val merchantName: String?,
    val totalAmount: Double?,
    val date: Instant?,
    val items: List<ReceiptItem>,
    val confidence: Float
)

data class ReceiptItem(
    val name: String,
    val price: Double,
    val quantity: Int = 1
)

// Using ML Kit Text Recognition
class MLKitReceiptScanner @Inject constructor(
    @ApplicationContext private val context: Context
) : ReceiptScanner {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    override suspend fun scanReceipt(imageUri: Uri): Result<ScannedReceipt> = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            
            val result = suspendCoroutine<Text> { continuation ->
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
            
            val scannedReceipt = parseReceiptText(result.text)
            Result.success(scannedReceipt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseReceiptText(text: String): ScannedReceipt {
        val lines = text.split("\n").map { it.trim() }
        
        // Extract merchant name (usually at top)
        val merchantName = lines.firstOrNull()
        
        // Find total amount (look for keywords: TOTAL, AMOUNT, etc.)
        val totalAmount = findTotalAmount(lines)
        
        // Extract date
        val date = findDate(lines)
        
        // Extract items
        val items = extractItems(lines)
        
        return ScannedReceipt(
            merchantName = merchantName,
            totalAmount = totalAmount,
            date = date,
            items = items,
            confidence = calculateConfidence(merchantName, totalAmount, items)
        )
    }
    
    private fun findTotalAmount(lines: List<String>): Double? {
        val totalKeywords = listOf("total", "amount", "sum", "balance")
        val amountRegex = """(\d+[,.]?\d*\.?\d+)""".toRegex()
        
        lines.forEach { line ->
            val lowerLine = line.lowercase()
            if (totalKeywords.any { keyword -> lowerLine.contains(keyword) }) {
                amountRegex.find(line)?.let { match ->
                    return match.value.replace(",", "").toDoubleOrNull()
                }
            }
        }
        
        // Fallback: find largest number
        return lines.mapNotNull { line ->
            amountRegex.findAll(line).lastOrNull()?.value?.replace(",", "")?.toDoubleOrNull()
        }.maxOrNull()
    }
    
    private fun findDate(lines: List<String>): Instant? {
        val dateRegex = """(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""".toRegex()
        
        lines.forEach { line ->
            dateRegex.find(line)?.let { match ->
                // Parse date string and convert to Instant
                // Implementation depends on date format
                return tryParseDate(match.value)
            }
        }
        
        return null
    }
    
    private fun extractItems(lines: List<String>): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()
        val itemRegex = """(.+?)\s+(\d+[,.]?\d*\.?\d+)$""".toRegex()
        
        lines.forEach { line ->
            itemRegex.find(line)?.let { match ->
                val (name, price) = match.destructured
                items.add(
                    ReceiptItem(
                        name = name.trim(),
                        price = price.replace(",", "").toDouble()
                    )
                )
            }
        }
        
        return items
    }
    
    private fun calculateConfidence(
        merchantName: String?,
        totalAmount: Double?,
        items: List<ReceiptItem>
    ): Float {
        var confidence = 0f
        if (merchantName != null) confidence += 0.3f
        if (totalAmount != null) confidence += 0.4f
        if (items.isNotEmpty()) confidence += 0.3f
        return confidence
    }
    
    private fun tryParseDate(dateString: String): Instant? {
        // Implement date parsing logic
        return null
    }
}

// presentation/screen/AddTransactionWithScanScreen.kt
@Composable
fun AddTransactionWithScanScreen(
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    var showImagePicker by remember { mutableStateOf(false) }
    val scannedReceipt by viewModel.scannedReceipt.collectAsState()
    
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.scanReceipt(it) }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        MangaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                MangaButton(
                    onClick = { launcher.launch("image/*") },
                    text = "📸 Scan Receipt",
                    modifier = Modifier.fillMaxWidth()
                )
                
                scannedReceipt?.let { receipt ->
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    BasicText(
                        "Scanned Data (${(receipt.confidence * 100).toInt()}% confident)",
                        style = MangaTypography.headlineSmall
                    )
                    
                    receipt.merchantName?.let {
                        BasicText("Merchant: $it")
                    }
                    
                    receipt.totalAmount?.let {
                        BasicText("Amount: ₱${String.format("%.2f", it)}")
                    }
                    
                    if (receipt.items.isNotEmpty()) {
                        BasicText("Items:")
                        receipt.items.forEach { item ->
                            BasicText("  • ${item.name}: ₱${item.price}")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    MangaButton(
                        onClick = { viewModel.confirmScannedReceipt() },
                        text = "Use This Data"
                    )
                }
            }
        }
    }
}
```

---

### 4. Export & Backup Functionality

**Business Value**: Data portability and user trust

```kotlin
// domain/usecase/ExportDataUseCase.kt
class ExportDataUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val groupRepository: GroupRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(
        groupId: String,
        format: ExportFormat
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val transactions = transactionRepository.getAllTransactions(groupId)
            val group = groupRepository.getGroup(groupId)
            
            val exportData = when (format) {
                ExportFormat.CSV -> exportToCsv(transactions, group)
                ExportFormat.PDF -> exportToPdf(transactions, group)
                ExportFormat.JSON -> exportToJson(transactions, group)
            }
            
            val uri = saveToFile(exportData, format, group.name)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun exportToCsv(transactions: List<Transaction>, group: Group): String {
        val csv = StringBuilder()
        csv.append("Date,Description,Amount,Paid By,Category\n")
        
        transactions.forEach { transaction ->
            csv.append("${transaction.createdAt},")
            csv.append("\"${transaction.description}\",")
            csv.append("${transaction.amount},")
            csv.append("\"${transaction.paidBy}\",")
            csv.append("\"${transaction.category}\"\n")
        }
        
        return csv.toString()
    }
    
    private fun exportToJson(transactions: List<Transaction>, group: Group): String {
        val exportData = ExportData(
            group = group,
            transactions = transactions,
            exportedAt = Clock.System.now(),
            version = "1.0"
        )
        return Json.encodeToString(exportData)
    }
    
    private fun exportToPdf(transactions: List<Transaction>, group: Group): ByteArray {
        // Create PDF using iText or similar library
        // Returns PDF as byte array
        return byteArrayOf()
    }
    
    private fun saveToFile(data: Any, format: ExportFormat, groupName: String): Uri {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val fileName = "HATI_${groupName}_$timestamp.${format.extension}"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/HATI_Exports")
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            ?: throw IOException("Failed to create file")
        
        resolver.openOutputStream(uri)?.use { outputStream ->
            when (data) {
                is String -> outputStream.write(data.toByteArray())
                is ByteArray -> outputStream.write(data)
            }
        }
        
        return uri
    }
}

enum class ExportFormat(val extension: String, val mimeType: String) {
    CSV("csv", "text/csv"),
    PDF("pdf", "application/pdf"),
    JSON("json", "application/json")
}

@Serializable
data class ExportData(
    val group: Group,
    val transactions: List<Transaction>,
    val exportedAt: Instant,
    val version: String
)
```

---

## 🚀 Priority 2 Features (Should Have)

### 5. Category Management & Budget Tracking

```kotlin
data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val color: Color,
    val monthlyBudget: Double? = null
)

data class BudgetStatus(
    val category: Category,
    val spent: Double,
    val budget: Double,
    val percentageUsed: Float
) {
    val isOverBudget: Boolean get() = spent > budget
    val remaining: Double get() = budget - spent
}
```

### 6. Recurring Expenses

```kotlin
data class RecurringExpense(
    val id: String,
    val templateTransaction: Transaction,
    val frequency: RecurrenceFrequency,
    val startDate: Instant,
    val endDate: Instant?,
    val isActive: Boolean = true
)

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    YEARLY
}
```

### 7. Multi-Currency Support

```kotlin
interface CurrencyConverter {
    suspend fun convert(amount: Double, from: String, to: String): Double
    suspend fun getExchangeRate(from: String, to: String): Double
}

class ExchangeRateRepository @Inject constructor(
    private val apiService: ExchangeRateApiService,
    private val cacheDao: ExchangeRateCacheDao
) {
    suspend fun getRate(from: String, to: String): Double {
        // Try cache first
        val cachedRate = cacheDao.getRate(from, to)
        if (cachedRate != null && !cachedRate.isExpired()) {
            return cachedRate.rate
        }
        
        // Fetch from API
        val rate = apiService.getExchangeRate(from, to)
        cacheDao.insert(ExchangeRateCache(from, to, rate, Clock.System.now()))
        return rate
    }
}
```

### 8. Push Notifications

```kotlin
sealed class NotificationType {
    data class NewExpense(val groupName: String, val amount: Double) : NotificationType()
    data class PaymentReminder(val toWhom: String, val amount: Double) : NotificationType()
    data class GroupInvite(val groupName: String, val inviterName: String) : NotificationType()
}

class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseMessaging: FirebaseMessaging
) {
    fun showNotification(type: NotificationType) {
        val (title, message) = when (type) {
            is NotificationType.NewExpense -> 
                "New Expense" to "${type.groupName}: ₱${type.amount} added"
            is NotificationType.PaymentReminder -> 
                "Payment Due" to "You owe ${type.toWhom} ₱${type.amount}"
            is NotificationType.GroupInvite -> 
                "Group Invite" to "${type.inviterName} invited you to ${type.groupName}"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        NotificationManagerCompat.from(context).notify(notification.hashCode(), notification)
    }
}
```

---

## 💡 Priority 3 Features (Nice to Have)

### 9. Analytics Dashboard
### 10. Expense Trends & Insights
### 11. Dark Mode Support
### 12. Biometric Authentication
### 13. QR Code Payment Integration
### 14. Split by Percentage/Custom Amounts
### 15. Expense Templates

---

## Implementation Priority

**Phase 1 (Weeks 1-2)**:
1. Settlement Calculator
2. Group Management
3. Export Functionality

**Phase 2 (Weeks 3-4)**:
4. Receipt Scanning
5. Categories & Budgets
6. Push Notifications

**Phase 3 (Weeks 5-6)**:
7. Multi-Currency
8. Recurring Expenses
9. Analytics Dashboard

**Phase 4 (Ongoing)**:
10-15. Polish & enhancement features

---

## Dependencies to Add

```kotlin
// build.gradle.kts
dependencies {
    // ML Kit for OCR
    implementation("com.google.mlkit:text-recognition:16.0.0")
    
    // PDF generation
    implementation("com.itextpdf:itext7-core:7.2.5")
    
    // CSV parsing
    implementation("com.opencsv:opencsv:5.7.1")
    
    // Firebase for push notifications
    implementation("com.google.firebase:firebase-messaging:23.4.0")
    
    // Exchange rates API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    
    // Charts for analytics
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
```

---

## Testing Considerations

Each feature should include:
- Unit tests for business logic
- Integration tests for repositories
- UI tests for screens
- Manual QA checklist

Example test structure:
```kotlin
class CalculateSettlementsUseCaseTest {
    @Test
    fun `given simple two-person split, returns one settlement`() {
        val transactions = listOf(
            Transaction(paidBy = "Alice", amount = 100.0),
            Transaction(paidBy = "Bob", amount = 0.0)
        )
        val members = listOf("Alice", "Bob")
        
        val settlements = CalculateSettlementsUseCase()(transactions, members)
        
        assertEquals(1, settlements.size)
        assertEquals("Bob", settlements[0].from)
        assertEquals("Alice", settlements[0].to)
        assertEquals(50.0, settlements[0].amount, 0.01)
    }
}
```
