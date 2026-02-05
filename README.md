# HATI²

**HATI²** (HATI v2) is a modern, offline-first Android application designed for personal finance management with a focus on simplicity and user experience. It leverages Clean Architecture, Jetpack Compose, and Supabase to deliver a robust and seamless experience.

## Features

*   **Offline-First**: Built with Room Database support, ensuring full functionality even without an internet connection. Data syncs automatically when back online.
*   **Modern UI**: Designed with Jetpack Compose for a beautiful, responsive, and intuitive user interface.
*   **Secure Authentication**: Integrated with Supabase Auth for secure user sign-up and login.
*   **Real-time Capabilities**: Utilizes Supabase Realtime features for live data updates (when online).
*   **Clean Architecture**: Structured using Clean Architecture principles (Presentation, Domain, Data layers) for maintainability and scalability.

## Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose
*   **Dependency Injection**: Hilt
*   **Backend / Database**: Supabase (PostgreSQL, Auth, Realtime)
*   **Local Database**: Room
*   **Navigation**: Navigation Compose
*   **Asynchronous Processing**: Coroutines & Flow
*   **Serialization**: Kotlinx Serialization

## Setup & Installation

To build and run the project locally, follow these steps:

1.  **Prerequisites**:
    *   Android Studio Iguana or later.
    *   JDK 17.

2.  **Clone the Repository**:
    ```bash
    git clone https://github.com/yourusername/hati-v2.git
    cd hati-v2
    ```

3.  **Configuration**:
    *   The project uses `gradle.properties` for Supabase configuration.
    *   Add your Supabase credentials to your `gradle.properties` file (usually found in `~/.gradle/gradle.properties` or create one in the project root if safe to do so, but **NEVER commit secrets**):
        ```properties
        SUPABASE_URL=your_supabase_url
        SUPABASE_ANON_KEY=your_supabase_anon_key
        ```

4.  **Build and Run**:
    *   Open the project in Android Studio.
    *   Sync Gradle with Project Files.
    *   Select your device/emulator and click "Run".

## Architecture

The app follows the **Clean Architecture** guide:

*   **Presentation Layer**: Contains UI components (Screens, Components) and ViewModels.
*   **Domain Layer**: Contains Use Cases and Interfaces (Business Logic).
*   **Data Layer**: Contains Repositories, Data Sources (Local/Remote), and DTOs.

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
