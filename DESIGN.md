# DESIGN.md — Manga × Notion Design System

> The complete UI/UX design reference for **HATI²** (Handy All-round Transaction Interface, version 2).
> This document covers color theory, typography rules, component architecture, cinematography principles applied to layouts, motion design, dark mode strategy, and the design decision framework.

---

## Table of Contents

1. [Design Philosophy & Identity](#1-design-philosophy--identity)
2. [Color Theory — The 60-30-10 Rule](#2-color-theory--the-60-30-10-rule)
3. [Typography Rules — MD3 Type Scale](#3-typography-rules--md3-type-scale)
4. [Component Architecture — The Manga Component Kit](#4-component-architecture--the-manga-component-kit)
5. [Cinematography Principles Applied to Layouts](#5-cinematography-principles-applied-to-layouts)
6. [Motion Design — The Antigravity System](#6-motion-design--the-antigravity-system)
7. [Dark Mode Strategy](#7-dark-mode-strategy)
8. [Design Decision Framework](#8-design-decision-framework)
9. [Implementation Reference](#9-implementation-reference)
10. [Mobile-First Reachability Design](#10-mobile-first-reachability-design)
11. [Animation System — The Antigravity Experience](#11-animation-system--the-antigravity-experience)

---

## 1. Design Philosophy & Identity

HATI² fuses two visual worlds into a single coherent aesthetic:

| Influence | What it contributes |
|---|---|
| **Shonen Manga** | Bold energy — heavy ink borders, hard drop shadows, sharp corners, high-contrast black-on-white, high-impact typography |
| **Notion** | Minimalist utility — clean layouts, generous whitespace, soft pastel category accents, readable type hierarchy |

### Core Principles

- **Volume Metaphor.** Each dashboard is a *manga volume*. The hub list is a bookshelf of volumes; opening one is opening a story arc of shared expenses.
- **Heavy Ink Borders.** Every interactive surface is outlined with a 2 dp `MangaBlack` border — cards, buttons, inputs, avatars. This is the single strongest visual signature of the system.
- **Hard Drop Shadows.** Shadows are geometric offsets of a solid black box behind the content, not blurred elevation shadows. They give the UI a printed, physical feel.
- **Pastel Category Accents.** Color communicates meaning (category, status, balance) through soft Notion pastels, never through dominant fills.
- **Antigravity Experience.** Micro-animations (button press depth, page slide, chart reveal) keep the interface feeling light and "alive" without becoming distracting.

### Target Emotional Response

> Confident, fast, playful yet functional.

Users should feel like they are flipping through a well-organized notebook with the speed of a manga panel read — not navigating an enterprise dashboard.

---

## 2. Color Theory — The 60-30-10 Rule

The entire app color budget follows the **60-30-10 rule**: 60% dominant surfaces, 30% structural/text elements, 10% accent highlights.

### 2.1 The Three Layers

#### 60% — Dominant (Backgrounds & Surfaces)

| Token | Hex | Pantone | Usage |
|---|---|---|---|
| `NotionWhite` | `#FFFFFF` | 11-0601 TCX (Bright White) | Light mode background, surface, card fill |
| `DarkSurface` | `#121212` | — | Dark mode base background (Level 0) |

> **Rule:** Pure black (`#000000`) is *never* used as a background. It causes eye strain on OLED displays. Dark mode uses `#121212` as the base.

#### 30% — Secondary (Text, Borders, Structural)

| Token | Hex | Pantone | Usage |
|---|---|---|---|
| `MangaBlack` | `#000000` | Black C | Primary text, borders, shadows |
| `MangaDarkGray` | `#555555` | Cool Gray 10 C | Heavy-disabled text |
| `NotionMuted` | `#6B7280` | Cool Gray 8 C | Secondary text, labels, placeholders |
| `NotionDivider` | `#D1D5DB` | Cool Gray 3 C | Dividers, light separators |
| `NotionDisabled` | `#9CA3AF` | Cool Gray 5 C | Disabled states, inactive controls |

> **Anti-pattern:** Never use raw `Color.Gray` or `Color.LightGray`. Always use the semantic `Notion*` grays so every tint stays inside the design system.

#### 10% — Accent (CTAs, Status, Categories)

| Token | Hex | Pantone | Role |
|---|---|---|---|
| `NotionYellow` | `#FEF08A` | 600 C | **Primary accent** — CTA highlights, active states |
| `NotionRed` | `#FECACA` | 7422 C | Negative balances, soft warnings |
| `NotionGreen` | `#BBF7D0` | 344 C | Positive balances, success states |
| `NotionBlue` | `#BFDBFE` | 2707 C | Informational, tertiary accent |
| `NotionPurple` | `#E9D5FF` | 2635 C | Category accent |
| `NotionPink` | `#FBCFE8` | 7430 C | Category accent |
| `NotionOrange` | `#FED7AA` | 7507 C | Category accent |
| `NotionGray` | `#E5E7EB` | Cool Gray 2 C | Neutral surface variant |
| `MangaSuccess` | `#22C55E` | 7481 C | Strong positive / success |

> **Rule:** Accent colors should never dominate a section. They punctuate — they don't fill.

### 2.2 Error System

| Token | Hex | Usage |
|---|---|---|
| `ErrorRed` | `#B3261E` | Light mode error text/icon (MD3 baseline) |
| `ErrorRedContainer` | `#F9DEDC` | Light mode error container |
| `DarkErrorRed` | `#F2B8B5` | Dark mode on-error |
| `DarkErrorContainer` | `#8C1D18` | Dark mode error container |

> **Rule:** *"Red means broken. Treat it like a fire alarm."* — `ErrorRed` is reserved exclusively for actual errors and failures. Use `NotionRed` for soft warnings or negative balances. Use `NotionYellow` for informational highlights.

### 2.3 Dark Mode Accent Variants

All pastel accents are transformed for dark mode using the rule:

```
Light: hsl(H, S%, L%)  →  Dark: hsl(H, S−20%, L+20%)
```

| Light Token | Dark Token | Dark Hex | HSL |
|---|---|---|---|
| `NotionYellow` | `DarkNotionYellow` | `#FFF3B0` | hsl(50, 60%, 84%) |
| `NotionBlue` | `DarkNotionBlue` | `#CCE5FF` | hsl(210, 60%, 90%) |
| `NotionGreen` | `DarkNotionGreen` | `#B3EACA` | hsl(145, 45%, 82%) |
| `NotionRed` | `DarkNotionRed` | `#FFD4D4` | hsl(0, 60%, 91%) |

### 2.4 Dark Surface Elevation System

Dark mode communicates depth through lightness, not shadow.

| Level | Token | Hex | Lightness Offset | Usage |
|---|---|---|---|---|
| 0 | `DarkSurface` | `#121212` | Base | Screen background |
| 1 | `DarkSurfaceElevated1` | `#1E1E1E` | +5% | Cards, raised surfaces |
| 2 | `DarkSurfaceElevated2` | `#232323` | +10% | Menus, dialogs |
| 3 | `DarkSurfaceElevated3` | `#2C2C2C` | +15% | Modal overlays |

### 2.5 Theme Mapping (MaterialTheme.colorScheme)

<details>
<summary>Light color scheme mapping</summary>

| MD3 Slot | Token |
|---|---|
| `primary` | `MangaBlack` |
| `secondary` | `NotionYellow` |
| `tertiary` | `NotionBlue` |
| `background` | `NotionWhite` |
| `surface` | `NotionWhite` |
| `surfaceVariant` | `NotionGray` |
| `onPrimary` | `NotionWhite` |
| `onSecondary` | `MangaBlack` |
| `onBackground` | `MangaBlack` |
| `onSurface` | `MangaBlack` |
| `onSurfaceVariant` | `NotionMuted` |
| `outline` | `NotionMuted` |
| `outlineVariant` | `NotionDivider` |
| `error` | `ErrorRed` |
| `errorContainer` | `ErrorRedContainer` |

</details>

<details>
<summary>Dark color scheme mapping</summary>

| MD3 Slot | Token |
|---|---|
| `primary` | `DarkOnSurface` |
| `secondary` | `DarkNotionYellow` |
| `tertiary` | `DarkNotionBlue` |
| `background` | `DarkSurface` |
| `surface` | `DarkSurface` |
| `surfaceVariant` | `DarkSurfaceElevated1` |
| `onPrimary` | `DarkSurface` |
| `onSecondary` | `DarkSurface` |
| `onBackground` | `DarkOnSurface` |
| `onSurface` | `DarkOnSurface` |
| `onSurfaceVariant` | `DarkOnSurface` (80% alpha) |
| `outline` | `NotionMuted` |
| `outlineVariant` | `#444444` |
| `error` | `DarkErrorRed` |
| `errorContainer` | `DarkErrorContainer` |

</details>

**Source:** `Color.kt`, `Theme.kt`

---

## 3. Typography Rules — MD3 Type Scale

### 3.1 Font Choice

| Property | Value | Rationale |
|---|---|---|
| Font family | System SansSerif | No custom font files — fast loading, consistent across devices |
| Header weight | **Bold (700)** | Not Black (900) — bold is impactful without being overbearing |
| Body weight | **Regular (400)** | Not Medium (500) — the contrast between 400 and 700 is clearer than between 500 and 700 |

### 3.2 Rules

| Rule | Detail |
|---|---|
| **Weight Contrast** | Pair Regular (400) with Bold (700) only. Skip Medium (500) — it is too subtle for differentiation. |
| **Line Height — Body** | 1.4–1.6× the font size for comfortable readability. |
| **Line Height — Headlines** | 1.1–1.3× (tighter for visual impact). |
| **Letter Spacing — Labels** | ALL CAPS labels get ≥ +0.08 em tracking to stay readable. |
| **Letter Spacing — Body** | Default tracking. Never manually track lowercase body text. |
| **Screen Budget** | Maximum **3 type sizes** on any single screen to maintain visual clarity. |

### 3.3 Named Base Styles

```
MangaHeaderStyle  →  SansSerif, Bold (700), letterSpacing = 0
MangaBodyStyle    →  SansSerif, Regular (400)
```

### 3.4 Full Type Scale

| Role | Size | Line Height | Ratio | Weight | Letter Spacing | Usage |
|---|---|---|---|---|---|---|
| `displayLarge` | 57 sp | 64 sp | ~1.12× | Bold | 0 | Hero moments, splash |
| `displayMedium` | 45 sp | 52 sp | ~1.16× | Bold | 0 | — |
| `displaySmall` | 36 sp | 44 sp | ~1.22× | Bold | 0 | — |
| `headlineLarge` | 32 sp | 40 sp | 1.25× | Bold | 0 | Screen titles |
| `headlineMedium` | 28 sp | 36 sp | ~1.29× | Bold | 0 | Section headers |
| `headlineSmall` | 24 sp | 32 sp | ~1.33× | Bold | 0 | Sub-section headers |
| `titleLarge` | 22 sp | 28 sp | ~1.27× | Bold | 0 | Navigation, top bars |
| `titleMedium` | 16 sp | 24 sp | 1.5× | Bold | 0.15 sp | Card headers |
| `titleSmall` | 14 sp | 20 sp | ~1.43× | Bold | 0.1 sp | Small headers |
| `bodyLarge` | 16 sp | 24 sp | 1.5× | Regular | 0.5 sp | Primary body text |
| `bodyMedium` | 14 sp | 20 sp | ~1.43× | Regular | 0.25 sp | Secondary body text |
| `bodySmall` | 12 sp | 16 sp | ~1.33× | Regular | 0.4 sp | Captions, metadata |
| `labelLarge` | 14 sp | 20 sp | — | Bold | 0.1 sp | Buttons, chips |
| `labelMedium` | 12 sp | 16 sp | — | Bold | 0.5 sp | Small labels (ALL CAPS) |
| `labelSmall` | 11 sp | 16 sp | — | Bold | 0.5 sp | Tiny labels (ALL CAPS) |

**Source:** `Typography.kt`

---

## 4. Component Architecture — The Manga Component Kit

All custom components are built from Compose primitives (`Box`, `Row`, `Column`) with explicit border and shadow styling. They do **not** wrap `Material3 Card` or `Surface` — this is intentional to maintain the hard-edge manga aesthetic.

### 4.1 Design Tokens (Constants)

| Token | Value | Usage |
|---|---|---|
| `MangaBorderWidth` | 2 dp | Border thickness for all interactive surfaces |
| `MangaCornerRadius` | 4 dp | Corner radius for all shapes |
| `MangaShadowOffset` | 4 dp | Shadow offset for cards (large) |
| `MangaShadowOffsetSmall` | 2 dp | Shadow offset for buttons (small) |

### 4.2 MangaCard

The foundational surface component.

```
┌──────────────────────┐
│                      │  ← 2 dp MangaBlack border
│    Content Area      │     4 dp corner radius
│    (16 dp padding)   │     NotionWhite background
│                      │
└──────────────────────┘
   └──────────────────────┘  ← 4 dp offset solid MangaBlack shadow
```

- **Shadow technique:** A second `Box` is drawn behind the content box, offset by `MangaShadowOffset` on both X and Y axes, filled with `MangaBlack`. This creates a hard, geometric shadow — not a blur.
- **Optional click:** Cards can be clickable or static. Click handler is conditionally applied.
- **Background:** Defaults to `NotionWhite`. Customizable via `backgroundColor` parameter.

### 4.3 MangaButton

Interactive button with physical press depth.

| State | Shadow Offset | Visual Effect |
|---|---|---|
| Default | 2 dp (bottom-right) | Button "floats" above shadow |
| Pressed | 0 dp (button shifts into shadow position) | Button "pushes in" |
| Disabled | No shadow, `NotionDisabled` background | Visually recessed |

- **Animation:** `animateDpAsState` with 80 ms `tween` for the offset transition.
- **Haptic feedback:** `HapticFeedbackType.TextHandleMove` fires on every tap.
- **Typography:** Uses `labelLarge` with Bold weight and the specified `contentColor`.
- **Default background:** `MaterialTheme.colorScheme.secondary` (maps to `NotionYellow` in light mode).

### 4.4 MangaTextField

Label-above text input.

- **Pattern:** Label text sits *above* the field (not floating inside it).
- **Implementation:** Uses `BasicTextField` (not `OutlinedTextField`) for full control over decoration.
- **Border:** 2 dp `MangaBlack`, 4 dp corner radius.
- **Placeholder:** Rendered in `NotionMuted` when the field is empty.
- **Background:** `NotionWhite`.

### 4.5 TransactionCard

Specialized card for displaying expenses and settlements.

```
┌─────────────────────────────────────────────┐
│ ┌──────┐                                    │
│ │  A   │  Title (titleMedium, Bold)    $XX  │
│ │      │  Subtitle (bodySmall, Muted)       │
│ └──────┘  Date (labelSmall, Muted)          │
└─────────────────────────────────────────────┘
```

- **Avatar box:** 40 dp, bordered, background color-coded by person identity.
- **Layout:** `Row` with avatar + text column (weighted) + amount.
- Wraps `MangaCard` for consistent shadow and border treatment.

### 4.6 MangaBackButton

32 dp bordered icon button with a back arrow (`Icons.AutoMirrored.Filled.ArrowBack`). Uses hardcoded `MangaBlack` border and `NotionWhite` background.

### 4.7 MangaEmptyState

Centered empty-state display with kaomoji.

```
         ( ◕ ‿ ◕ )

       No expenses yet!
    Add one to get started.
```

- Kaomoji at 48 sp, `FontWeight.Black`.
- Primary message in `titleMedium`, Bold.
- Optional sub-message in `bodySmall` at 60% alpha.
- Uses `MaterialTheme.colorScheme` tokens (adapts to dark mode).

### 4.8 Dialog Components

| Dialog | Purpose | Key Features |
|---|---|---|
| `AddDashboardDialog` | Create a new dashboard | Title input, type selector (Travel/Household/Event/Other), theme color picker |
| `EditDashboardDialog` | Edit existing dashboard | Same fields as Add, pre-filled with current values |
| `SettleUpDialog` | Record a settlement payment | From/To person dropdowns, amount input |
| `ExportWarningDialog` | Confirm before export | Security warning (data is unencrypted), format selector (CSV/JSON) |
| `MangaDeleteDialog` | Confirm destructive action | Simple "Are you sure?" with cancel and confirm |

All dialogs follow the Manga card styling with bordered inputs and `MangaButton` actions.

**Source:** `MangaComponents.kt`, `MangaBackButton.kt`, `MangaEmptyState.kt`, dialog component files

---

## 5. Cinematography Principles Applied to Layouts

### 5.1 Panel Layout — Hub List

The hub list treats the screen as a **manga page with panel grid**.

- **Grid:** `LazyVerticalGrid` with `GridCells.Fixed(2)` — two columns of volume cards.
- **Why 2 columns:** Maximizes visible dashboards (volumes), reduces navigation depth. Users scan the grid like manga panels — left-to-right, top-to-bottom.
- **Volume cards:** Each card shows type icon + colored spine (instant identity cue), title, expense count/total, and net balance tag (positive/negative signal).
- **Dead-end prevention:** Both a FAB and a ghost "New Volume" card exist as creation entry points. Whether the list is empty or full, users always have a clear path forward.

### 5.2 Single-Page Dashboard — Detail View

The dashboard detail is designed as a **manga spread** (two-page opening):

- All key information is on one scrolling page: balance summary, member list, recent expenses, action buttons (Add Expense, History, Charts, Settle Up), and export.
- **Why:** Reduces context switching. Users can inspect status, act, and confirm outcomes without navigating through tabs.
- **Information density:** Dense but scannable, using the type hierarchy (headlines for sections, body for details, labels for metadata) and the 60-30-10 color layers to create visual separation.

### 5.3 Form Screens — Add Expense

Forms are laid out as a **vertical manga page** read top-to-bottom:

- Each input group (description, amount, category, payer, split participants) is a visual "frame" separated by spacing.
- The save action is at the bottom — the natural endpoint of the reading flow.
- Validation is front-loaded: errors appear inline, preventing bad data before save.

### 5.4 Information Hierarchy

The 60-30-10 color rule directly maps to information priority:

| Layer | Color Budget | Content |
|---|---|---|
| 60% background | `NotionWhite` / `DarkSurface` | Clean canvas — visual breathing room |
| 30% structural | `MangaBlack` borders, `NotionMuted` text | Borders, secondary text, metadata |
| 10% accent | `NotionYellow`, semantic pastels | FAB, CTA buttons, balance tags, category chips |

### 5.5 Visual Scanning Cues

- **Type icon + colored spine** on volume cards → instant dashboard identity.
- **Balance tags** → positive (green) or negative (red) signal without reading numbers.
- **Avatar initials** on transaction cards → quick person identification.
- **Category color coding** in charts and expense lists → pattern recognition across the app.

**Source:** `DashboardListScreen.kt`, `DashboardDetailScreen.kt`, `AddExpenseScreen.kt`

---

## 6. Motion Design — The Antigravity System

Motion in HATI² serves function, not decoration. Every animation communicates a state change.

### 6.1 Page Transitions

| Direction | Animation | Duration | Detail |
|---|---|---|---|
| Enter (forward) | `fadeIn` + `slideInHorizontally` (¼ width from right) | 300 ms | Slides in from the right, fading in |
| Exit (forward) | `fadeOut` | 200 ms | Fades out quickly |
| Pop Enter (back) | `fadeIn` + `slideInHorizontally` (¼ width from left) | 300 ms | Slides in from the left |
| Pop Exit (back) | `fadeOut` + `slideOutHorizontally` (¼ width to right) | 200 ms | Fades out with slight rightward slide |

> **Asymmetric timing:** Enter transitions (300 ms) are slower than exits (200 ms). This makes the destination feel like it's "arriving" while the origin disappears quickly — perceived smoothness.

### 6.2 Button Press Animation

```
Default:   [Button Surface]
              └─[Shadow at +2dp, +2dp]

Pressed:   [Button Surface shifts to +2dp, +2dp]
           (Shadow hidden — button is "pushed in")
```

- **Mechanism:** `animateDpAsState` animates the button offset from `(0, 0)` to `(MangaShadowOffsetSmall, MangaShadowOffsetSmall)` on press.
- **Duration:** 80 ms `tween` — fast enough to feel immediate.
- **Haptic:** `HapticFeedbackType.TextHandleMove` accompanies every tap.

### 6.3 Chart Animations

- Bar heights and percentage values animate in using `animateFloatAsState` with `FastOutSlowInEasing`.
- Data reveals smoothly rather than appearing instantly — gives the user a moment to follow the visual change.

### 6.4 Timing Philosophy

| Category | Duration | Examples |
|---|---|---|
| Micro-interaction | 80 ms | Button press, toggle |
| Page transition | 200–300 ms | Navigate forward/back |
| Data reveal | 300–500 ms | Chart bar fill, percentage counter |
| **Maximum** | **500 ms** | Never exceed this for any animation |

### 6.5 Easing

| Easing | When to use |
|---|---|
| `FastOutSlowInEasing` | Standard Material easing — chart reveals, data transitions |
| `tween` (linear) | Simple offset animations — button press, shadow movement |

**Source:** `MainActivity.kt` (nav transitions), `MangaComponents.kt` (button press), `DashboardDetailScreen.kt` (chart animations)

---

## 7. Dark Mode Strategy

### 7.1 Surface System

Dark mode uses `#121212` as the base surface — **not pure black** (`#000000`).

Elevation is communicated through lightness (see §2.4 Dark Surface Elevation System). Shadows are invisible on dark backgrounds, so they are replaced by surface lightness differentiation.

### 7.2 Text

`DarkOnSurface` (`#E0E0E0`) is slightly off-white to avoid glare on dark backgrounds. Full `#FFFFFF` white would create too much contrast on OLED screens.

### 7.3 Accent Desaturation

All pastel accents are desaturated and lightened for dark mode using the `hsl(H, S−20%, L+20%)` rule. This prevents vibrant colors from causing visual strain against dark surfaces (see §2.3).

### 7.4 Theme Switching

| Mechanism | Behavior |
|---|---|
| `isSystemInDarkTheme()` | Automatic detection from system settings |
| `isDarkMode: StateFlow<Boolean?>` | User override in `MainViewModel` — `null` follows system, `true`/`false` forces a mode |

### 7.5 Status Bar

- Status bar color is set to `Color.Transparent`.
- `isAppearanceLightStatusBars` is toggled based on the current theme — light icons on dark background, dark icons on light background.

### 7.6 Dynamic Colors

Material You dynamic colors (API 31+) are **intentionally disabled**. The Manga × Notion palette is the product's visual identity — dynamic theming would dilute it.

**Source:** `Theme.kt`, `Color.kt`, `MainViewModel.kt`

---

## 8. Design Decision Framework

Use this section as a quick reference when making UI decisions.

### When to Use Accent Color

✅ Primary CTA buttons (the one action you want the user to take)
✅ Active/selected states (selected tab, active toggle)
✅ Category identification (chart segments, category chips)
✅ Balance indicators (green = positive, red = negative)

❌ Section backgrounds (accent should never dominate a section)
❌ Body text color (use `MangaBlack` or `NotionMuted`)
❌ Decorative fills (keep the 60% layer clean)

### When to Use Borders vs Shadows

| Element | Border | Shadow |
|---|---|---|
| Cards | ✅ Always (2 dp `MangaBlack`) | ✅ Hard offset shadow |
| Buttons | ✅ Always | ✅ Shadow disappears on press |
| Text inputs | ✅ Always | ❌ No shadow |
| Avatars | ✅ Always | ❌ No shadow |
| Dialogs | ✅ From card wrapper | ✅ Card shadow |
| Dividers | ❌ Use `NotionDivider` line | ❌ No shadow |

### When to Add Animation

✅ State changes (pressed → released, page A → page B, empty → data loaded)
✅ Data reveals (chart bars filling, counters incrementing)

❌ Idle/decorative animation (no pulsing, bouncing, or looping effects)
❌ Loading spinners (prefer skeleton/shimmer if needed, but currently not implemented)

### When to Break the Grid

**Never.** All content follows the column/row grid system. No absolute positioning, no overlapping elements (except shadows, which are structural).

### Error vs Warning vs Info

| Severity | Color | Token | Example |
|---|---|---|---|
| **Error** | Deep red | `ErrorRed` (#B3261E) | Failed save, network error, validation failure |
| **Warning** | Soft red pastel | `NotionRed` (#FECACA) | Negative balance, overdue payment |
| **Info/Highlight** | Yellow pastel | `NotionYellow` (#FEF08A) | Active selection, primary CTA |
| **Success** | Green | `MangaSuccess` (#22C55E) / `NotionGreen` (#BBF7D0) | Positive balance, successful action |

### Accessibility

| Aspect | Implementation |
|---|---|
| **Color contrast** | `MangaBlack` on `NotionWhite` = 21:1 ratio (exceeds WCAG AAA) |
| **Touch targets** | Minimum 32 dp (back button); most interactive elements are larger |
| **Haptic feedback** | All `MangaButton` taps trigger haptic feedback as a non-visual confirmation |
| **Screen readers** | Standard Compose semantics — `contentDescription` on icons, meaningful text on all interactive elements |

---

## 9. Implementation Reference

### 9.1 File Map

| Design Aspect | Implementation File |
|---|---|
| Color palette + Pantone annotations | `ui/theme/Color.kt` |
| MD3 color scheme mapping | `ui/theme/Theme.kt` |
| Typography scale + named styles | `ui/theme/Typography.kt` |
| MangaCard, MangaButton, MangaTextField, TransactionCard | `ui/components/MangaComponents.kt` |
| MangaBackButton | `ui/components/MangaBackButton.kt` |
| MangaEmptyState | `ui/components/MangaEmptyState.kt` |
| AddDashboardDialog | `ui/components/AddDashboardDialog.kt` |
| EditDashboardDialog | `ui/components/EditDashboardDialog.kt` |
| SettleUpDialog | `ui/components/SettleUpDialog.kt` |
| ExportWarningDialog | `ui/components/ExportWarningDialog.kt` |
| MangaDeleteDialog | `ui/components/MangaDeleteDialog.kt` |
| Navigation transitions | `MainActivity.kt` |
| Dark mode state management | `ui/MainViewModel.kt` |
| Biometric auth gate UI | `ui/auth/BiometricAuthGate.kt` |
| Hub list (panel grid) | `ui/screens/DashboardListScreen.kt` |
| Dashboard detail (spread) | `ui/screens/DashboardDetailScreen.kt` |
| Expense form | `ui/screens/AddExpenseScreen.kt` |
| History timeline | `ui/screens/HistoryScreen.kt` |
| Charts analytics | `ui/screens/ChartsScreen.kt` |

All paths are relative to `app/src/main/java/com/hativ2/`.

### 9.2 Design Token Reference

| Token | Value | Defined In |
|---|---|---|
| `MangaBorderWidth` | 2 dp | `MangaComponents.kt` |
| `MangaCornerRadius` | 4 dp | `MangaComponents.kt` |
| `MangaShadowOffset` | 4 dp | `MangaComponents.kt` |
| `MangaShadowOffsetSmall` | 2 dp | `MangaComponents.kt` |
| `HEX_NOTION_ORANGE` | `"#FED7AA"` | `Color.kt` |

### 9.3 Anti-Pattern Checklist

Before merging any UI change, verify:

- [ ] **No raw Color values.** Every color is a named `Notion*`, `Manga*`, `Dark*`, or `Error*` token from `Color.kt`.
- [ ] **No Medium (500) weight.** Only Regular (400) and Bold (700) are used.
- [ ] **No pure black background.** Dark mode surfaces use `#121212` (`DarkSurface`), never `#000000`.
- [ ] **No animation > 500 ms.** Check all `tween()` and `animateXAsState` durations.
- [ ] **No accent as dominant color.** Accent fills should be small — chips, tags, CTA buttons — never section backgrounds.
- [ ] **No `OutlinedTextField`.** Use `MangaTextField` (wraps `BasicTextField`) for the manga aesthetic.
- [ ] **No blurred shadows.** Use the hard offset `Box` pattern from `MangaCard`.
- [ ] **Max 3 type sizes per screen.** Count the distinct `sp` values on each screen.
- [ ] **Borders on all interactive elements.** Cards, buttons, inputs, and avatars get the 2 dp `MangaBlack` border.

---

## 10. Mobile-First Reachability Design

### 10.1 The Thumb Zone Problem

> **In normie terms:** Most people hold their phone with one hand. The bottom of the screen is easy to reach with your thumb. The top corners are almost impossible to reach without shifting your grip — which means you might drop your phone. So we put all the important buttons at the bottom.

On a modern 6"+ phone, the screen divides into three ergonomic zones when held in one hand:

| Zone | Location | Comfort | What goes here |
|---|---|---|---|
| 🟢 Easy | Bottom 40% | Natural thumb arc | **Primary actions** — Add, Navigate, Confirm |
| 🟡 Stretch | Middle 30% | Reachable with effort | **Content** — lists, cards, information |
| 🔴 Hard | Top 30% | Requires grip shift | **Read-only info** — titles, status, back button |

### 10.2 Bottom Action Bar

The `DashboardDetailScreen` uses a **persistent bottom action bar** instead of inline action cards or a floating action button (FAB).

**Why this is better than a FAB:**
- A FAB gives you **one** action. The bottom bar gives you **four** (History, Charts, Add, Member) — all within thumb reach.
- Users don't need to scroll back to find actions. They're always visible.
- The "Add" action is visually prominent (green background, larger icon) so it still stands out as the primary action.

**Why this is better than the old in-content ActionGrid:**
- The ActionGrid scrolled with content, so you had to scroll to the top to find it.
- Now the actions are always pinned to the bottom of the screen — zero scrolling needed.

**Implementation details:**
- 2dp `MangaBlack` top border (matches the manga ink aesthetic)
- `NotionWhite` background (stays in the 60% dominant layer)
- Touch targets ≥ 48dp (Google's minimum for accessibility)
- "Add" button uses `NotionGreen` with border to pop visually

### 10.3 Design Decisions in Plain English

| Decision | Why (the normie version) |
|---|---|
| Bottom bar instead of top menu | Your thumb lives at the bottom of your phone. Put buttons where your thumb already is. |
| "Add" is green and bigger | The thing you do most should be the easiest to spot and tap. Green = go. |
| Cards scroll, actions don't | You want to browse your data freely without losing access to what you can DO about it. |
| Back button stays at top-left | This is where every Android app puts it. Don't mess with muscle memory. |
| Large touch targets (48dp+) | Small buttons = mis-taps = frustration. Fat buttons = happy thumbs. |

---

## 11. Animation System — The Antigravity Experience

### 11.1 Animation Philosophy

> **In normie terms:** Animations aren't just eye candy — they tell your brain what's happening. When a card slides in, your brain understands "this is new." When a button pushes down, your brain feels "I pressed something." Without animations, apps feel like flipping a light switch. With them, apps feel alive.

Every animation in HATI² serves one of three purposes:

| Purpose | What it does | Example |
|---|---|---|
| **Feedback** | Confirms your action | Button press depth, haptic buzz |
| **Orientation** | Shows where things came from / are going | Screen slide transitions, staggered list entrance |
| **Delight** | Makes the app feel polished and alive | Kaomoji breathing, pulse on "+" icon |

### 11.2 Entrance Animations

**Staggered List Entrance** (DashboardListScreen):
- Each volume card fades in and slides up with a 50ms stagger per item
- Duration: 400ms with `FastOutSlowInEasing`
- This creates a "cascade" effect — like cards being dealt onto a table

> **Why 50ms stagger?** Too fast (10ms) and everything appears at once — might as well not animate. Too slow (200ms) and users wait forever. 50ms is the sweet spot: fast enough to feel snappy, slow enough to notice the cascade.

**Section Entrance** (DashboardDetailScreen):
- Balance card, member list, and summary cards fade in and slide up as you scroll
- Uses `AnimatedVisibility` with `fadeIn + slideInVertically`

**Transaction Cards** (MangaComponents):
- Slide in from the right with a fade
- Creates a "swipe in" feel for list items

### 11.3 Interaction Animations

**Press Depth** (MangaCard, MangaButton, VolumeCard, ActionCard):
- When pressed, the hard shadow offset decreases to 0dp and the card moves down by the same amount
- Simulates physically pressing a card into a surface
- Uses `spring` animation for a natural, bouncy feel

> **Why spring instead of tween?** A tween (linear or eased animation) has a fixed duration. A spring naturally overshoots and settles — like pressing a physical button. It feels more real because real objects have momentum.

**Scale Bounce** (MangaButton):
- On press, button scales to 0.95x then springs back to 1.0x
- Combined with haptic feedback for a tactile feel

**Card Appearance Scale** (MangaCard):
- Cards scale from 0.95f to 1.0f on first appearance with a low-bouncy spring
- Subtle enough to not be annoying, noticeable enough to feel premium

### 11.4 Ambient Animations

**Kaomoji Breathing** (MangaEmptyState):
- The `( ◕ ‿ ◕ )` face scales between 1.0x and 1.08x on an infinite loop
- 1200ms per cycle with reverse repeat
- Makes empty states feel friendly instead of dead

**"+" Icon Pulse** (NewVolumeCard):
- The add icon scales between 1.0x and 1.15x continuously
- Draws attention to the creation entry point

### 11.5 Screen Transitions (NavHost)

| Transition | Animation | Duration |
|---|---|---|
| Enter (forward) | Fade in + slide from right (1/3 screen) | 400ms |
| Exit (forward) | Fade out + scale down to 92% | 250ms |
| Pop enter (back) | Fade in + slide from left (1/3 screen) | 400ms |
| Pop exit (back) | Fade out + slide right (1/3 screen) | 250ms |

> **Why scale-out on forward exit?** When you navigate forward, the current screen shrinks slightly as it fades — like it's receding into the background. This gives a sense of depth and forward motion. The standard slide-out feels flat by comparison.

### 11.6 Animation Timing Rules

| Guideline | Value | Reason |
|---|---|---|
| Max duration | 500ms | Anything longer feels sluggish |
| Stagger delay | 50ms per item | Sweet spot between "instant" and "slow" |
| Spring stiffness | `StiffnessLow` to `StiffnessMediumLow` | Bouncy enough to notice, not enough to distract |
| Easing | `FastOutSlowInEasing` | Objects accelerate quickly, decelerate gradually — matches real-world physics |
| Infinite animations | Scale only, max 8% change | Prevents eye strain; breathing, not bouncing |

**Source:** `MangaComponents.kt`, `MangaEmptyState.kt`, `DashboardListScreen.kt`, `DashboardDetailScreen.kt`, `MainActivity.kt`

---

## Known Deviations & Future Work

The following are existing implementation gaps to be addressed in future updates:

1. **Hardcoded colors in components.** Several components use `MangaBlack` and `NotionWhite` directly instead of `MaterialTheme.colorScheme` tokens. This means they will not fully adapt in dark mode:
   - `MangaTextField` — label color is hardcoded to `MangaBlack`
   - `TransactionCard` — title color is hardcoded to `MangaBlack`
   - `MangaBackButton` — border, background, and icon tint are all hardcoded

2. **No `Shape.kt` file.** The corner radius is defined as a `MangaCornerRadius` constant in `MangaComponents.kt` but is not registered as a Material3 `Shapes` object in the theme. A future `Shape.kt` could centralize shape definitions and make them accessible via `MaterialTheme.shapes`.

---

*This document reflects the design system as implemented in the codebase. For the project README and feature overview, see [README.md](README.md).*
