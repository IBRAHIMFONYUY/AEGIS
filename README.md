# AEGIS (Autonomous Ethical Guardian Intelligent System)

AEGIS is a privacy-first **AI Guardian Operating Layer** that protects users from cyber threats, misinformation, and digital harm by analyzing intent, context, and behavior in real-time. It represents a shift from traditional device protection to **Guardian Computing**, where the AI safeguards the human decision-making process.

---

## 🚀 How it Works (Real-Time Protection)

AEGIS operates as an omnipresent guardian, working everywhere on your device.

1.  **Omnipresent Monitoring**: The system uses specialized Android Services to "see" threats as they happen:
    *   **Notification Guardian**: Scans incoming messages from apps like WhatsApp and Telegram instantly.
    *   **Accessibility Watcher (Deep Scan)**: Monitors on-screen text, clicks, and window changes across all applications to detect real-time manipulation or phishing.
    *   **Clipboard Monitor**: Automatically scans copied text for sensitive data leaks.

2.  **System-Wide Interactive Overlays**: When a threat is detected, AEGIS doesn't just send a notification; it displays an **Interactive Alert** over the current app. You can see the risk, the reasoning, and take immediate action (Secure Now / Ignore) without leaving your interaction.

3.  **Conversational AI Coach**: The built-in assistant is a context-aware security coach. It performs "Meta-Analysis" on your queries and simulates a reasoning phase to provide intelligent, ethically-grounded safety advice.

4.  **Live Dashboard & Telemetry**: Every scan and threat detection is broadcast through a reactive data pipeline. Your **Safety Score** and **Threat Log** update in real-time as background analysis occurs.

---

## 🏗️ Technical Architecture

### Tech Stack
*   **UI**: Jetpack Compose (Material 3) with fully reactive `StateFlow` integration.
*   **Language**: Kotlin (Built-in Support, AGP 9.2+).
*   **Database**: Room 2.8.4 + SQLCipher 4.6.1 (Full-disk encryption with `cipher_compatibility = 3` support).
*   **AI**: Local Inference via Google LiteRT (TensorFlow Lite) and ONNX Runtime.
*   **Overlay System**: Custom `WindowManager` implementation for system-wide Compose UI injection.
*   **Reactive Core**: `SharedFlow` broadcast system for multi-component real-time updates.

### Key Components
*   `GuardianCore`: The central orchestrator that broadcasts real-time `AnalysisResults`.
*   `ThreatOverlayManager`: Manages the system-wide interactive Compose-based alerts.
*   `AegisAccessibilityService`: The primary bridge for real-time, cross-app text and behavior analysis.
*   `AssistantViewModel`: A conversational engine with simulated reasoning and intent analysis.

---

## 🛠️ Project Structure
*   `com.aegis.agents`: Specialized logic for threat detection (Scam, Privacy, Misinfo, etc.).
*   `com.aegis.ai`: Implementation of local inference engines and model management.
*   `com.aegis.data`: Repository layer and encrypted Room database.
*   `com.aegis.services.overlay`: Logic for displaying interactive alerts over other apps.
*   `com.aegis.services.accessibility`: Cross-app monitoring implementation.
*   `com.aegis.ui`: Reactive Jetpack Compose screens.

---

## 🔧 Setup & Permissions

To function as a true Guardian Layer, AEGIS requires:
1.  **Accessibility Service**: To monitor threats inside other applications.
2.  **Display Over Other Apps**: To show interactive real-time alerts.
3.  **Notification Access**: To scan incoming messages before they are opened.

### Troubleshooting
*   **KSP Errors**: Ensure Room `2.8.4` and KSP `2.3.9` are used for Kotlin `2.4.x` compatibility.
*   **Database Inaccessibility**: AEGIS includes a self-healing mechanism that automatically recovers or re-initializes encrypted databases during library upgrades or corruption.

---
*AEGIS is not just software; it's a new computing layer designed to protect humans in the age of AI.*
