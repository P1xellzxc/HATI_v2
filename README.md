# HATI² (HATI_v2)

> **Handy All-round Transaction Interface, version 2** — A high-performance, offline-first expense management app built with the **Manga x Notion** design aesthetic. Track expenses, manage party balances, and settle up with speed and style.

![Platform](https://img.shields.io/badge/platform-Android-green?style=for-the-badge)
![UI](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge)
![Design](https://img.shields.io/badge/Design-Manga_x_Notion-yellow?style=for-the-badge)

---

## 🎨 Design System: Manga x Notion

HATI² features a unique visual language that combines the bold, high-contrast energy of Shonen Manga with the clean, minimalist utility of Notion.

- **Manga Elements**: Heavy black borders (`MangaBlack`), hard shadows, sharp corners, and high-impact typography.
- **Notion Palette**: Soft, pastel category colors (`NotionYellow`, `NotionBlue`, `NotionRed`, `NotionGreen`, `NotionPurple`) for high readability.
- **Antigravity Experience**: Micro-animations and responsive layouts that keep the interface feeling light and "alive."

---

## 📸 Screenshots

### Hub List — Your Story Volumes
> The home screen. All your expense "volumes" are displayed as manga-style cards. Tap a card to enter the volume, or tap **+ New Volume** to start a new arc.

<p align="center">
  <img src="https://github.com/user-attachments/assets/74f732ca-831b-410d-8d88-2f715b09af48" alt="Hub List Screen" width="320"/>
</p>

Each volume card shows:
- **Cover icon** — auto-selected emoji based on volume type (✈️ travel, 🏠 household, 🎉 event)
- **Colored spine** — your chosen theme color (yellow, blue, green, red)
- **Balance tag** — real-time net balance indicator (green = positive, red = negative)
- **Total spent** and chapter (expense) count at a glance
- **⋮ menu** — quick access to Edit or Delete the volume

---

## 🗺️ App Walkthrough

### 1. Create a New Volume
Tap **+ New Volume** (FAB or the ghost card) → enter a title, pick a type and theme color → the volume is created instantly and appears in your hub.

### 2. Dashboard Detail
Inside a volume you get a full overview:
- **Stats bar** — total spent, participant count, and unsettled debt at a glance
- **Recent transactions** — chronological list of expenses and settlements
- **People panel** — add or remove participants from the split
- **Action buttons** — navigate to Expenses, Balance, Charts, or export data (CSV / JSON)

### 3. Add an Expense
Tap **+ Add Expense** → fill in the title, amount, date, and category → choose who paid and configure the split between participants → confirm to save.

Categories available: 🍽️ Food · 🚗 Transport · 🛍️ Shopping · 🎬 Entertainment · 💡 Utilities · 📦 Other

### 4. Balance Screen
Visual breakdown of who owes whom across all participants. Debts are calculated automatically via `CalculateDebtsUseCase` and displayed per-person with directional arrows.

### 5. Charts Screen
Spending analytics for the selected volume:
- **Donut chart** — category percentage breakdown with color-coded segments
- **Bar chart** — monthly spending totals with a monthly-average reference line
- **Trend indicator** — month-over-month spending delta (↑ increase / ↓ decrease)

### 6. History Screen
A full log of all settlements ("Settle Up" events) recorded in the volume, showing payer, payee, amount, and timestamp.

### 7. Export Data
From the Dashboard Detail screen tap the **share icon** → choose **CSV** or **JSON** → a security notice is shown → confirm and save the file anywhere on your device via the system file picker (Storage Access Framework).

---

## 🚀 Key Features

- **Dashboard "Hubs"** — Manage multiple "Volumes" (dashboards) for different travel arcs, households, or events.
- **Advanced Debt Calculation** — Automated split logic with "Settle Up" history and real-time dashboard stats.
- **Unified UI Architecture** — Standardized components (`MangaCard`, `TransactionCard`, `MangaTextField`) ensured across all screens for a seamless experience.
- **Performance Optimized** — Smooth scrolling and stable recompositions using efficient state management.
- **100% Offline-First** — Local data persistence with Room. Your data stays on your device.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Architecture** | Clean Architecture + MVVM + Usecases |
| **UI Framework** | Jetpack Compose (Modern BOM) |
| **DI** | Hilt (Dagger) |
| **Storage** | Room (SQLite) |
| **Concurrency** | Kotlin Coroutines & Flow |
| **Testing** | JUnit 4, Kotlin-test, Hilt Testing |

---

## 📖 Architecture Overview

The codebase is organized into three distinct layers to ensure testability and scalability:

```text
app/src/main/java/com/hativ2/
├── data/        # Room Database, DAOs, Repository Implementations
├── domain/      # Pure business logic, Usecases, Repository Interfaces
└── ui/          # Compose Screens, ViewModels, Theme, Shared Components
```

---

## ✅ Progress Roadmap

- [x] **v2.0 Core** — Migration to Clean Architecture & Room.
- [x] **Design Unification** — Standardization of all components to the Manga x Notion tokens.
- [x] **Color Audit** — Replacement of all hardcoded colors with standardized `Notion*` constants.
- [x] **Smart Calculation** — Implementation of `CalculateDebtsUseCase` and `Settle Up` logic.
- [x] **Chart Enhancements** — Advanced spending analytics: category percentage breakdown, monthly average line, month-over-month trend indicator.
- [x] **Data Export** — CSV and JSON export for manual backups with format selection and security warning.
- [ ] **Cloud Sync** — Opt-in Supabase synchronization for multi-user party tracking.

---

## ⚙️ Setup & Installation

### 1. Clone the Repository
```bash
git clone https://github.com/P1xellzxc/HATI_v2.git
cd HATI_v2
```

### 2. Requirements
- Android Studio **Iguana** (2023.2.1) or later
- JDK **17**
- Minimum SDK: **26** (Android 8.0)

### 3. Build and Run
1. Open the project in Android Studio.
2. Click **File → Sync Project with Gradle Files**.
3. Select your device or emulator and click **Run ▶**.

---

## 📜 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
