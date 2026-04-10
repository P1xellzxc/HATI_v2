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
