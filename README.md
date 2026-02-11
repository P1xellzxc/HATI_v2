# HATI²

> **HATI²** (HATI v2) — *Handy All-round Transaction Interface, version 2* — is a modern, offline-first Android app for personal finance management. Track expenses, manage budgets, and stay on top of your finances entirely on-device, with no account or internet connection required.

![Platform](https://img.shields.io/badge/platform-Android-green?style=flat-square)
![Min SDK](https://img.shields.io/badge/minSdk-26-blue?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-lightgrey?style=flat-square)
![Status](https://img.shields.io/badge/status-Active%20Development-orange?style=flat-square)

---

## Screenshots

> _Screenshots / demo GIF coming soon._

---

## Features

- **Expense & Income Tracking** — Log transactions by category, date, and amount.
- **Budget Management** — Set monthly budgets and monitor spending against them in real time.
- **100% Local & Private** — All data is stored on-device using Room. No account, no internet, no data leaves your phone.
- **Modern UI** — Built with Jetpack Compose for a beautiful, responsive experience.

---

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Kotlin | 1.9+ |
| UI | Jetpack Compose | BOM 2024.x |
| Dependency Injection | Hilt | 2.51+ |
| Local DB | Room | 2.6+ |
| Navigation | Navigation Compose | 2.7+ |
| Async | Coroutines & Flow | 1.7+ |
| Serialization | Kotlinx Serialization | 1.6+ |

---

## Requirements

- Android Studio **Iguana** (2023.2.1) or later
- JDK **17**
- Minimum SDK: **26** (Android 8.0)
- Target SDK: **34** (Android 14)

---

## Setup & Installation

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/hati-v2.git
cd hati-v2
```

### 2. Build and Run

1. Open the project in Android Studio.
2. Click **File → Sync Project with Gradle Files**.
3. Select your device or emulator and click **Run ▶**.

No API keys or external services are required — the app runs entirely on-device.

---

## Architecture

HATI² follows **Clean Architecture**, keeping concerns clearly separated across three layers so each can be tested and changed independently:

```
UI (Compose Screens)
        ↓
   ViewModel
        ↓
  Use Cases          ← Domain Layer (pure Kotlin, no Android deps)
        ↓
Repository Interface
        ↓
Repository Implementation
        ↓
    Local (Room)
```

### Project Structure

```
app/src/main/
├── presentation/    # Screens, Composables, ViewModels
├── domain/          # Use cases, repository interfaces, domain models
└── data/            # Repository implementation, Room DAOs, entities
```

---

## Roadmap

- [x] Local on-device storage with Room
- [x] Expense & income tracking
- [x] Budget management
- [ ] Spending analytics & charts
- [ ] Recurring transactions
- [ ] CSV / PDF export
- [ ] Home screen widgets
- [ ] Cloud sync with Supabase *(planned)*
- [ ] Multi-device support via Supabase Auth *(planned, depends on sync)*
- [ ] Multi-currency support

---

## Contributing

Contributions are welcome! To get started:

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m 'Add some feature'`
4. Push to the branch: `git push origin feature/your-feature-name`
5. Open a Pull Request describing what you changed and why.

Please follow the existing Clean Architecture patterns and include tests for any new use cases or data layer logic where applicable.

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
