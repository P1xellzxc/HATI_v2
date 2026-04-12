# ANDROID APP TESTING RULES
> Comprehensive, production-grade testing rules for any Android application.
> Goal: Test every corner — logic, UI, data, network, performance, security, and accessibility.

---

## 1. TESTING PYRAMID

Follow the **70 / 20 / 10 rule**:

- **70% Unit Tests** — Fast, isolated, no device needed
- **20% Integration Tests** — Components working together
- **10% E2E / UI Tests** — Full user journeys on a real device

```
           /\
          /  \
         / E2E\          ← Few, slow, expensive (10%)
        /──────\
       /Integrat\        ← Some, moderate cost (20%)
      /──────────\
     /  Unit Tests \     ← Many, fast, cheap (70%)
    /______________\
```

---

## 2. CORE TESTING RULES

1. **Test behavior, not implementation** — Test what the code *does*, not how it's written.
2. **Every bug gets a test** — Write a failing test first, then fix the bug.
3. **Never ship without a passing test suite** — Green CI = clearance to release.
4. **Mock all external dependencies** — APIs, DB, sensors, clock, location must be mockable.
5. **Test on real devices before every release** — Emulators miss battery, memory pressure, OEM skins.
6. **Automate what repeats** — If tested manually more than twice, automate it.
7. **Tests must be deterministic** — Flaky tests are bugs. Fix or delete them.
8. **Isolate test state** — No test should depend on or affect another test's state.
9. **Test the sad path as hard as the happy path** — Errors, nulls, empty states, timeouts.
10. **Coverage is a floor, not a ceiling** — Aim for 80%+ line coverage, but meaningful tests matter more than numbers.

---

## 3. UNIT TESTING RULES

> Scope: Individual functions, classes, ViewModels, UseCases, Repositories, Mappers, Utils.

### What to Unit Test
- All business logic (calculations, transformations, validations)
- Every ViewModel — state emissions, error handling, loading states
- Every UseCase / Interactor
- Repository logic (data source selection, caching logic)
- Mapper / data transformation functions
- Utility and extension functions
- All conditional branches (if/else, when expressions)
- Edge cases: empty lists, null values, zero, max Int, negative numbers

### Rules
- Each test must have one reason to fail (single assertion focus)
- Name tests descriptively: `givenEmptyList_whenGetItems_thenReturnsEmptyState()`
- Use `@Before` / `@After` to set up and tear down state cleanly
- Use fakes over mocks where possible — fakes are more reliable
- Use `TestCoroutineDispatcher` / `UnconfinedTestDispatcher` for coroutines
- Test `StateFlow` and `SharedFlow` emissions explicitly
- Never let real time pass in unit tests — use `TestCoroutineScope`

### Tools
- JUnit 4 / JUnit 5
- Mockk (Kotlin-first mocking)
- Turbine (Flow testing)
- Kotlin Coroutines Test
- Truth (Google assertion library)

---

## 4. INTEGRATION TESTING RULES

> Scope: Two or more components working together — ViewModel + Repository, Repository + Room, UseCases + APIs.

### What to Integration Test
- Room database: insert, query, update, delete, migration
- Repository with real or fake data sources
- ViewModel + UseCase together
- WorkManager tasks
- DataStore reads and writes
- Dependency injection graph (Hilt/Koin module correctness)

### Rules
- Use an in-memory Room database for tests — fast and isolated
- Test all DAO queries with real SQL
- Test database migrations explicitly with `MigrationTestHelper`
- Test that the correct data source (local vs remote) is selected under different conditions
- Test caching behavior — what gets cached, when cache expires, stale data handling
- Verify DI modules compile and provide all dependencies correctly

### Tools
- AndroidJUnit4
- Room Testing (`inMemoryDatabaseBuilder`)
- Hilt Testing (`@HiltAndroidTest`)
- MockWebServer (OkHttp) for fake API responses
- DataStore Testing

---

## 5. UI AND END-TO-END TESTING RULES

> Scope: What the user sees and interacts with — screens, flows, navigation, gestures.

### What to UI Test
- Every screen renders its key elements
- Every critical user journey (onboarding, login, checkout, main feature flow)
- Form validation: empty fields, invalid input, max length, special characters
- Error states: API failure banners, empty state screens, retry buttons
- Success states: confirmation dialogs, navigation after action
- Navigation: forward, back, deep links, bottom nav switching
- Dialogs and bottom sheets open and dismiss correctly
- Keyboard behavior: input field focus, IME actions, keyboard dismiss
- RecyclerView / LazyColumn: items load, scroll, click correctly
- Swipe actions, drag-and-drop if applicable

### Rules
- Use Page Object Model (POM) pattern to keep tests readable and maintainable
- Never use `Thread.sleep()` — use `IdlingResource` or `awaitIdle()`
- Test on multiple screen sizes (phone, tablet, foldable)
- Test both portrait and landscape orientations
- Test configuration changes: rotate screen mid-flow, change language
- Test back stack integrity — pressing back must land on the correct screen
- Test deep links produce the correct screen state

### Tools
- Espresso (View-based UI)
- Compose Testing (`composeTestRule`) for Jetpack Compose
- UI Automator (cross-app flows, system UI interactions)
- Kakao / Barista (Espresso wrappers for readability)

---

## 6. NETWORK AND API TESTING RULES

> Scope: All HTTP calls, WebSocket connections, parsing, and error handling.

### What to Test
- Successful API response — data parsed and displayed correctly
- HTTP error codes: 400, 401, 403, 404, 500, 503
- Network timeout — app shows error, does not freeze
- No internet connection — offline state handled gracefully
- Slow network — loading indicators shown, no duplicate requests
- Malformed / unexpected JSON — app does not crash
- Paginated responses — next page loads correctly, end of list handled
- Auth token expiry — refresh flow triggers correctly
- Retry logic — failed requests retry the correct number of times

### Rules
- Always mock network calls in unit and integration tests (never hit real APIs in automated tests)
- Use `MockWebServer` to simulate all server responses
- Test with throttled network speeds using Android Emulator Network Profiles
- Test airplane mode mid-session behavior
- Verify no sensitive data (tokens, PII) is logged in network calls

### Tools
- MockWebServer (OkHttp)
- Retrofit + Gson/Moshi test parsing
- Chucker (network inspector for manual testing)
- Android Emulator Network Throttling

---

## 7. PERFORMANCE TESTING RULES

> Scope: Speed, memory, battery, rendering, and startup.

### What to Test
| Metric | Target |
|---|---|
| Cold startup time | < 2 seconds |
| Warm startup time | < 1 second |
| Frame rate | Consistent 60fps (no jank) |
| Memory usage | No leaks, stable over time |
| Battery drain | No unnecessary background wake-locks |
| APK/AAB size | Minimize, measure impact of new dependencies |

### Rules
- Profile before and after every significant feature addition
- Use `Macrobenchmark` for startup and scroll benchmarks in CI
- Detect memory leaks with LeakCanary in debug builds — zero leaks allowed before release
- Avoid overdraw — use GPU Overdraw tool to visualize
- Avoid main thread blocking — StrictMode must be enabled in debug builds
- Test performance on a low-end device (2GB RAM, older CPU), not just flagship
- Measure and baseline APK size — alert on significant increases

### Tools
- Android Profiler (CPU, Memory, Network, Energy)
- Macrobenchmark library
- LeakCanary
- StrictMode
- Firebase Performance Monitoring
- Systrace / Perfetto

---

## 8. SECURITY TESTING RULES

> Scope: Data storage, transmission, permissions, and attack surface.

### What to Test
- Sensitive data (tokens, passwords, PII) is NOT stored in SharedPreferences unencrypted
- Sensitive data is NOT logged (Logcat must be clean in release builds)
- All network traffic uses HTTPS — no cleartext allowed
- Certificate pinning works and rejects invalid certs (if implemented)
- App does not expose private data via exported Activities/ContentProviders
- Permissions are requested only when needed, not at launch
- Biometric / PIN auth cannot be bypassed
- Deep links validate input and do not allow unauthorized navigation
- WebView does not enable JavaScript on untrusted URLs
- Backup rules exclude sensitive files (`android:allowBackup` configured correctly)

### Rules
- Use `EncryptedSharedPreferences` and `EncryptedFile` for any sensitive storage
- Run ProGuard/R8 obfuscation and verify sensitive class names are obscured in release
- Audit all exported components in `AndroidManifest.xml`
- Test with OWASP Mobile Top 10 as a checklist
- Use `network_security_config.xml` to enforce HTTPS

### Tools
- MobSF (Mobile Security Framework) — static analysis
- OWASP Mobile Top 10 checklist
- Android Lint security checks
- Drozer (dynamic security testing)
- Manual APK inspection (apktool, jadx)

---

## 9. ACCESSIBILITY TESTING RULES

> Scope: Usability for users with visual, motor, or cognitive impairments.

### What to Test
- Every interactive element has a `contentDescription`
- Touch targets are at least 48x48dp
- Color is not the only indicator of meaning (icons, labels must also convey state)
- Text contrast ratio meets WCAG AA (4.5:1 for normal text, 3:1 for large text)
- TalkBack reads all screens in a logical order
- App is fully usable with TalkBack enabled (no dead ends)
- Font scaling up to 200% does not break layouts
- No content is obscured when large text is enabled
- Focus order is logical for keyboard / switch access navigation

### Rules
- Run Accessibility Scanner on every screen before release
- Test manually with TalkBack enabled on a real device
- All images must have meaningful content descriptions or be marked decorative
- Avoid `clickable` containers without role descriptions in Compose

### Tools
- Accessibility Scanner (Google)
- TalkBack (manual)
- Compose Semantics testing
- Espresso Accessibility Checks (`AccessibilityChecks.enable()`)

---

## 10. DEVICE AND COMPATIBILITY TESTING RULES

> Scope: Different Android versions, screen sizes, manufacturers, and configurations.

### Minimum Device Matrix

| Dimension | Coverage |
|---|---|
| Android versions | Min SDK, mid SDK, latest SDK |
| Screen sizes | Small phone, large phone, tablet, foldable |
| Screen densities | mdpi, hdpi, xhdpi, xxhdpi |
| Manufacturers | Stock Android, Samsung One UI, Xiaomi MIUI |
| RAM | Low-end (2GB), mid-range (4GB), high-end (8GB+) |
| Orientations | Portrait and landscape |

### Rules
- Test on the oldest supported Android version (your minSdkVersion)
- Test on the latest Android release
- Test foldables if your app uses window size classes
- Test RTL (right-to-left) layout mirroring if targeting Arabic, Hebrew, etc.
- Test with system font size set to largest
- Test with dark mode enabled
- Test with battery saver mode enabled
- Test with Developer Options: "Don't keep activities" enabled (tests state restoration)

### Tools
- Firebase Test Lab (real device cloud)
- Android Emulator (various AVDs)
- Play Store Pre-launch Report
- Google Play Device Catalog

---

## 11. REGRESSION AND CI/CD RULES

> Scope: Preventing old bugs from coming back and keeping the pipeline clean.

### Rules
- Every bug fix must be accompanied by a test that reproduces the bug
- All tests run on every pull request — no merge without green CI
- Flaky tests are tracked and fixed within one sprint — never ignored
- Test suite must complete in under 10 minutes for unit/integration tests
- E2E tests run nightly or pre-release, not on every commit
- Code coverage report generated on every CI run
- Coverage drop of more than 5% blocks the PR

### CI Pipeline Order
1. Lint + static analysis
2. Unit tests
3. Integration tests
4. Build (debug + release)
5. UI tests (emulator)
6. Firebase Test Lab (real devices) — pre-release only

### Tools
- GitHub Actions / GitLab CI / Bitrise / CircleCI
- Gradle test tasks
- Codecov / Coveralls (coverage reporting)
- Danger (PR quality checks)

---

## 12. MINIMUM VIABLE TEST COVERAGE PER FEATURE

| What You Build | What You Must Test |
|---|---|
| New screen | Renders, key elements visible, loading + error + success states |
| Form / input | Validation rules, submit success, submit error, empty state |
| API call | Success, HTTP errors, timeout, no internet |
| Database operation | Create, read, update, delete, migration |
| Navigation | Forward, back, deep link, config change |
| Authentication | Login success, wrong credentials, token expiry, logout |
| Permissions | Granted, denied, permanently denied flow |
| Background task | Scheduled, executed, cancelled, retry on failure |
| Notification | Received, tapped (navigates correctly), dismissed |
| In-app purchase | Flow starts, success, cancellation, error |

---

## 13. TESTING CHECKLIST (PRE-RELEASE)

### Logic
- [ ] All ViewModels have unit tests
- [ ] All UseCases have unit tests
- [ ] All Repositories have unit tests
- [ ] All edge cases (null, empty, max values) are covered

### UI
- [ ] All critical user journeys have E2E tests
- [ ] All screens tested in portrait and landscape
- [ ] All error and empty states are tested
- [ ] Configuration change (rotation) tested on key screens

### Data
- [ ] All Room DAOs tested
- [ ] All database migrations tested
- [ ] All network responses (success + error) tested
- [ ] Offline behavior tested

### Performance
- [ ] Cold start under 2 seconds
- [ ] No memory leaks (LeakCanary clean)
- [ ] No jank on main scrollable screens
- [ ] Profiled on a low-end device

### Security
- [ ] No sensitive data in logs
- [ ] No cleartext HTTP traffic
- [ ] Sensitive data encrypted at rest
- [ ] All permissions justified and minimized

### Accessibility
- [ ] TalkBack tested on all screens
- [ ] All touch targets ≥ 48dp
- [ ] Accessibility Scanner passes

### Compatibility
- [ ] Tested on min SDK version
- [ ] Tested on latest Android version
- [ ] Tested on Samsung device
- [ ] Tested with dark mode
- [ ] Tested with large font size
- [ ] "Don't keep activities" option tested

### CI/CD
- [ ] All tests passing in CI
- [ ] Coverage above 80%
- [ ] No new lint warnings
- [ ] Firebase Test Lab run completed

---

## 14. RECOMMENDED TOOL STACK

| Category | Tool |
|---|---|
| Unit Testing | JUnit 4/5, Mockk, Turbine, Truth |
| Coroutines Testing | kotlinx-coroutines-test |
| Integration Testing | AndroidJUnit4, Room Testing, Hilt Testing |
| UI Testing | Espresso, Compose Testing, UI Automator |
| Network Mocking | MockWebServer, OkHttp |
| Performance | Macrobenchmark, Android Profiler, Perfetto |
| Memory Leaks | LeakCanary |
| Security | MobSF, Lint, EncryptedSharedPreferences |
| Accessibility | Accessibility Scanner, TalkBack, Espresso A11y |
| Cloud Devices | Firebase Test Lab |
| CI/CD | GitHub Actions, Bitrise, CircleCI |
| Coverage | JaCoCo, Codecov |
