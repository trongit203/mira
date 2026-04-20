# Plan: Notification Listener for Auto Transaction Capture

## Context
Mira users transact via MoMo, ZaloPay, and Vietnamese banks but must manually log each transaction. This feature adds a `NotificationListenerService` that silently intercepts payment notifications, parses amount and direction (income/expense), and auto-inserts transactions into Room — tagged with the correct `DataSource` (MOMO, ZALOPAY, BANK_SYNC). The domain layer (`AddTransactionUseCase`, `TransactionRepository`) is already complete; only the service layer, parsers, and permission UX need to be added.

---

## Files to Create

| Path | Role |
|---|---|
| `data/notification/NotificationParser.kt` | Interface: `canHandle(pkg)` + `parse(pkg, title, text, ts): Transaction?` |
| `data/notification/CategoryInferenceEngine.kt` | Pure keyword→category map (no deps, fully unit-testable) |
| `data/notification/MoMoNotificationParser.kt` | Implements parser for `com.mservice.momotransfer` |
| `data/notification/ZaloPayNotificationParser.kt` | Implements parser for `vn.com.vng.zalopay` |
| `data/notification/BankNotificationParser.kt` | Implements parser for a `Set` of bank package names |
| `data/notification/NotificationParserFactory.kt` | `@Singleton`; holds list of parsers, returns first `canHandle()` match |
| `service/MiraNotificationListenerService.kt` | `@AndroidEntryPoint`; deduplicates + dispatches to factory + calls use case |
| `utils/NotificationPermissionUtils.kt` | `isNotificationAccessGranted(context): Boolean` via `NotificationManagerCompat` |
| `test/.../notification/MoMoNotificationParserTest.kt` | JUnit tests for all MoMo regex branches |
| `test/.../notification/ZaloPayNotificationParserTest.kt` | JUnit tests for ZaloPay patterns |
| `test/.../notification/BankNotificationParserTest.kt` | JUnit tests for bank credit/debit patterns |
| `test/.../notification/CategoryInferenceEngineTest.kt` | JUnit tests for keyword inference |
| `test/.../notification/NotificationParserFactoryTest.kt` | Tests routing and null return for unknown packages |
| `test/.../service/NotificationDispatchPipelineTest.kt` | End-to-end pipeline test using `FakeTransactionRepository` |

## Files to Modify

| Path | Change |
|---|---|
| `app/src/main/AndroidManifest.xml` | Add `<uses-permission>` + `<service>` declaration |
| `presentation/dashboard/DashboardScreen.kt` | Wrap Scaffold body in `Column`; add `NotificationPermissionBanner` at top |

---

## Implementation Order

### Step 1 — `CategoryInferenceEngine.kt` (no deps)
Pure `object` with two `Map<String, String>` lookups:
- `inferExpenseCategory(text): String` — keywords: ăn/cafe/grab/điện/siêu thị/shop → display categories
- `inferIncomeCategory(text): String` — keywords: nhận/lương/hoàn/thưởng
- Default: `"Khác"` for expense, `"Thu nhập"` for income

### Step 2 — `NotificationParser.kt` interface
```kotlin
interface NotificationParser {
    fun canHandle(packageName: String): Boolean
    fun parse(packageName: String, title: String, text: String, timestamp: Long): Transaction?
}
```

### Step 3 — Amount extraction (shared helper, companion on each parser)
Regex handles Vietnamese thousand-separator formats: `100.000đ`, `1.000.000 VNĐ`, `500,000đ`
```kotlin
private val AMOUNT_RE = Regex("""([\d]{1,3}(?:[.,][\d]{3})*)(?:\s*)[đĐ]|VNĐ|VND""", IGNORE_CASE)

fun extractAmount(text: String): Double? =
    AMOUNT_RE.find(text)?.groupValues?.get(1)
        ?.replace(",", ".")
        ?.replace(".", "")
        ?.toDoubleOrNull()
```

### Step 4 — `MoMoNotificationParser.kt`
`canHandle`: `packageName == "com.mservice.momotransfer"`

Direction regexes:
```kotlin
val RECEIVE_RE  = Regex("""^Bạn\s+nhận\s+[\d.,]+\s*[đĐ]""", IGNORE_CASE or MULTILINE)
val SEND_RE     = Regex("""^Chuyển\s+[\d.,]+\s*[đĐ]\s+đến""", IGNORE_CASE or MULTILINE)
val PAYMENT_RE  = Regex("""Thanh\s+toán\s+[\d.,]+\s*[đĐ]\s+(?:tại|cho)\s+(.+)""", IGNORE_CASE)
```
- RECEIVE → `INCOME`, `source = MOMO`, category from `inferIncomeCategory(text)`
- SEND → `EXPENSE`, `source = MOMO`, category = `"Chuyển khoản"`
- PAYMENT → `EXPENSE`, `source = MOMO`, category from `inferExpenseCategory("$text $merchant")`
- No match → `null`

### Step 5 — `ZaloPayNotificationParser.kt`
`canHandle`: `packageName == "vn.com.vng.zalopay"`

Direction regexes:
```kotlin
val RECEIVE_RE = Regex("""vừa nhận""", IGNORE_CASE)
val SENT_RE    = Regex("""giao dịch.+thành công""", IGNORE_CASE)
val PAYMENT_RE = Regex("""thanh toán""", IGNORE_CASE)
```
Same INCOME/EXPENSE mapping as MoMo; `source = ZALOPAY`

### Step 6 — `BankNotificationParser.kt`
`canHandle`: `packageName in BANK_PACKAGES` where the set contains:
`com.VCB`, `com.mbmobile`, `com.VietinBankiPay`, `com.bidv.smartbanking`, `com.vietinbank.ipay`, `vn.vnpay.merchants`, `com.tpb.mb.gprsandroid`, `vn.agribank.mbplus`

Direction regexes:
```kotlin
val CREDIT_RE = Regex("""(?:số dư tăng|ghi có|credit|nhận được|\+\s*[\d]|cộng tiền)""", IGNORE_CASE)
val DEBIT_RE  = Regex("""(?:số dư giảm|ghi nợ|debit|thanh toán|rút tiền|-\s*[\d]|trừ tiền)""", IGNORE_CASE)
```
`source = BANK_SYNC`

### Step 7 — `NotificationParserFactory.kt`
```kotlin
@Singleton
class NotificationParserFactory @Inject constructor() {
    private val parsers = listOf(MoMoNotificationParser(), ZaloPayNotificationParser(), BankNotificationParser())
    fun findParser(packageName: String): NotificationParser? = parsers.firstOrNull { it.canHandle(packageName) }
}
```
Parsers are plain objects (no Hilt) — stateless, no deps.

### Step 8 — `NotificationPermissionUtils.kt`
```kotlin
object NotificationPermissionUtils {
    fun isNotificationAccessGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
```

### Step 9 — `MiraNotificationListenerService.kt`
```kotlin
@AndroidEntryPoint
class MiraNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var addTransactionUseCase: AddTransactionUseCase
    @Inject lateinit var parserFactory: NotificationParserFactory
    @Inject @Named("IO") lateinit var ioDispatcher: CoroutineDispatcher

    private val serviceJob = SupervisorJob()               // prevents one failure cancelling scope
    private val serviceScope by lazy { CoroutineScope(serviceJob + ioDispatcher) }

    private val processedKeys = mutableSetOf<String>()    // main-thread dedup (onNotificationPosted is main-thread)
    private val MAX_CACHE = 200

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg  = sbn.packageName ?: return
        val key  = sbn.key
        parserFactory.findParser(pkg) ?: return            // fast exit — unknown package

        if (key in processedKeys) return
        if (processedKeys.size >= MAX_CACHE) processedKeys.clear()
        processedKeys.add(key)

        val extras = sbn.notification.extras
        val title  = extras.getString(Notification.EXTRA_TITLE).orEmpty()
        val text   = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (text.isBlank()) return

        val transaction = parserFactory.findParser(pkg)!!.parse(pkg, title, text, sbn.postTime) ?: return

        serviceScope.launch {
            runCatching { addTransactionUseCase(transaction) }   // AddTransactionUseCase uses require() — can throw
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
```
Key notes:
- `by lazy` on `serviceScope`: defers creation until first notification, after Hilt injection is complete
- `SupervisorJob` prevents one failed coroutine from cancelling the whole scope
- `runCatching` catches `IllegalArgumentException` from `require()` in `AddTransactionUseCase`
- Dedup check is synchronous (main thread) before launching coroutine — no concurrent set access

### Step 10 — Manifest changes
Add inside `<manifest>` before `<application>`:
```xml
<uses-permission
    android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    tools:ignore="ProtectedPermissions"/>
```
Add inside `<application>`:
```xml
<service
    android:name=".service.MiraNotificationListenerService"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService"/>
    </intent-filter>
    <meta-data
        android:name="android.service.notification.default_filter_types"
        android:value="alerting"/>
</service>
```
`default_filter_types="alerting"` limits delivery to heads-up notifications — reduces noise from silent ones.

### Step 11 — Dashboard permission banner
`DashboardScreen.kt` Scaffold body currently has a bare `when(uiState)` block at line 112. Wrap it in a `Column`:
```kotlin
} { paddingValues ->
    Column(modifier = Modifier.padding(paddingValues)) {
        val context = LocalContext.current
        if (!NotificationPermissionUtils.isNotificationAccessGranted(context)) {
            NotificationPermissionBanner()
        }
        when (val state = uiState) { /* unchanged */ }
    }
}
```
`NotificationPermissionBanner` — private composable, uses `MaterialTheme.colorScheme.errorContainer` card, Vietnamese label text, `TextButton("Cấp quyền")` that opens `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.
Banner re-evaluates on recomposition — will auto-hide once user grants access and returns to app.

---

## Reused Existing Infrastructure
- `AddTransactionUseCase` — `domain/usecase/TransactionUseCases.kt` (validation + insert)
- `@Named("IO") CoroutineDispatcher` — `di/DispatcherModule.kt`
- `FakeTransactionRepository` — `test/.../utils/FakeTransactionRepository.kt` (reused in pipeline test)
- `MainDispatcherRule` — `test/.../utils/MainDispatcherRule.kt`
- `TransactionFixtures` — `test/.../utils/TransactionFixtures.kt`
- `DataSource.MOMO`, `DataSource.ZALOPAY`, `DataSource.BANK_SYNC` — already in `Transaction.kt`

---

## Verification

### Unit tests (no emulator)
```bash
./gradlew testDebugUnitTest
```
All parser tests, factory routing, category inference, and pipeline test run as pure JVM tests.

### Manual test on emulator (ADB fake notification)
```bash
# Grant notification access first via Settings > Apps > Special app access > Notification access > Mira
adb shell cmd notification post -S bigtext \
  --title "MoMo" \
  --text "Bạn nhận 150.000đ từ Nguyễn Văn A" \
  com.mservice.momotransfer 1001
```
Expected: transaction appears in Dashboard with type INCOME, source MOMO, amount 150000.

### Permission banner
- Revoke notification access → open Dashboard → banner appears
- Grant access → return to app → banner disappears on recomposition
