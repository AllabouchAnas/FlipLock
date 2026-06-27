# Contributing to FlipLock

First off, thank you for taking the time to contribute to FlipLock! It’s developers like you who make open-source productivity tools great. 

We welcome contributions of all types: bug fixes, feature suggestions, UI polish, documentation improvements, and translation updates.

---

## 🚀 How Can I Contribute?

### 1. Reporting Bugs
*   Check if the issue is already reported in the **Issues** tab.
*   If not, open a new issue. Clearly describe the bug, how to reproduce it, your device model, and Android version.
*   Include screenshots or logs if possible.

### 2. Suggesting Enhancements
*   Open an issue stating your feature suggestion.
*   Describe the use case, why this feature would benefit FlipLock users, and how you envision its design or behavior.

### 3. Submitting Pull Requests (PRs)
1.  **Fork the repository** and create your branch from `main`:
    ```bash
    git checkout -b feature/my-awesome-feature
    ```
2.  Ensure your code builds and compiles cleanly without errors:
    ```bash
    ./gradlew compileDebugKotlin
    ```
3.  Commit your changes with clear, descriptive commit messages.
4.  Push to your fork and **submit a Pull Request** to our `main` branch.

---

## 🎨 Code Style and Guidelines

*   **Kotlin and Jetpack Compose**: Follow standard Kotlin coding styles and modern Jetpack Compose layout guidelines.
*   **Architecture**: FlipLock uses an MVVM (Model-View-ViewModel) architectural pattern. Keep UI screens decoupled from business logic inside the ViewModels.
*   **Sensor Usage**: Ensure any adjustments to proximity or accelerometer parameters are thoroughly tested on real hardware, as sensor behaviors vary across mobile devices.
*   **Clean & Immersive UI**: Respect the design identity. The app uses an OLED pitch-black background with delicate gold accents and micro-animations.

---

## 📄 License

By contributing to FlipLock, you agree that your contributions will be licensed under the project's [GNU General Public License Version 3](LICENSE).
