# AEGIS (Autonomous Ethical Guardian Intelligent System)

## 🛡️ The World's First Autonomous Ethical AI Guardian Operating Layer

AEGIS is not just an AI security app—it's a **Guardian Platform** that protects human decisions, digital identity, privacy, money, reputation, mental wellbeing, and online trust. It represents a paradigm shift from traditional device protection to **Guardian Computing**, where AI safeguards the human decision-making process in real-time.

---

## 🎯 Why AEGIS Wins

Instead of protecting phones, AEGIS protects:
- **Human decisions** from manipulation and social engineering
- **Digital identity** from theft and impersonation
- **Privacy** from unauthorized surveillance and data harvesting
- **Money** from financial fraud and scams
- **Reputation** from deepfakes and misinformation
- **Mental wellbeing** from cyberbullying and online abuse
- **Online trust** from phishing and fake content

---

## 🏗️ Full Architecture

### Guardian AI Core
```
Guardian AI
├── Intent AI - Detects psychological manipulation
├── Scam AI - Identifies fraud and phishing
├── Malware AI - Blocks malicious software
├── Deepfake AI - Detects synthetic media
├── Fake News AI - Identifies misinformation
├── Privacy AI - Monitors data exposure
├── Behavioral AI - Detects anomalous patterns
└── Decision AI - Synthesizes all analysis
```

### Protection Layer
- Scam Detection
- Phishing Detection
- SMS Protection
- WhatsApp Analysis
- Telegram Analysis
- Browser Protection
- Fake News Detection
- Deepfake Detection
- Voice Scam Detection
- App Permission Monitor
- Camera Monitor
- Microphone Monitor
- Clipboard Monitor
- VPN Detection
- Malware Detection

### Privacy Layer
- Local Encryption
- Secure Vault
- Password Manager
- Identity Protection

### AI Assistant
- Explain Threats
- Suggest Safe Actions
- Learn User Behaviour
- Offline Protection

---

## 🚀 How it Works (Real-Time Protection)

AEGIS operates as an omnipresent guardian, working everywhere on your device.

1.  **Omnipresent Monitoring**: The system uses specialized Android Services to "see" threats as they happen:
    *   **Notification Guardian**: Scans incoming messages from apps like WhatsApp and Telegram instantly.
    *   **Accessibility Watcher (Deep Scan)**: Monitors on-screen text, clicks, and window changes across all applications to detect real-time manipulation or phishing.
    *   **Clipboard Monitor**: Automatically scans copied text for sensitive data leaks.

2.  **System-Wide Interactive Overlays**: When a threat is detected, AEGIS doesn't just send a notification; it displays an **Interactive Alert** over the current app. You can see the risk, the reasoning, and take immediate action (Secure Now / Ignore) without leaving your interaction.

3.  **Conversational AI Coach**: The built-in assistant is a context-aware security coach. It performs "Meta-Analysis" on your queries and simulates a reasoning phase to provide intelligent, ethically-grounded safety advice.

4.  **Live Dashboard & Telemetry**: Every scan and threat detection is broadcast through a reactive data pipeline. Your **Guardian Score** and **Threat Log** update in real-time as background analysis occurs.

5.  **Emergency Guardian**: For critical threats (ransomware, banking malware, account compromise), AEGIS can automatically isolate the threat, warn you, and guide recovery.

---

## � Key Features

### Guardian Score
Every user receives a live security score with category breakdowns:
- **Overall Score**: 97/100
- **Privacy**: 100
- **Scam Protection**: 96
- **Device Security**: 98
- **Digital Wellbeing**: 91

### Live Threat Map
Display cyberattacks happening across Cameroon in real time, helping authorities and citizens understand emerging threats.

### AI Scam Simulator
Users can practice against simulated phishing messages and scam calls, improving awareness before facing real attacks.

### Family Protection
One guardian account can monitor and protect children and elderly relatives from scams and online abuse.

### Business Dashboard
Organizations can monitor employee cyber-risk trends, awareness levels, and security incidents while respecting privacy.

### Emergency Guardian
If AEGIS detects ransomware, banking malware, or severe account compromise, it can immediately isolate the threat, warn the user, and guide recovery.

---

## �🏗️ Technical Architecture

### Tech Stack
*   **UI**: Jetpack Compose (Material 3) with fully reactive `StateFlow` integration.
*   **Language**: Kotlin (Built-in Support, AGP 9.2+).
*   **Database**: Room 2.8.4 + SQLCipher 4.6.1 (Full-disk encryption with `cipher_compatibility = 3` support).
*   **AI**: Local Inference via Google LiteRT (TensorFlow Lite) and ONNX Runtime.
*   **Overlay System**: Custom `WindowManager` implementation for system-wide Compose UI injection.
*   **Reactive Core**: `SharedFlow` broadcast system for multi-component real-time updates.

### AI Stack
#### Local AI
*   Gemma 3N (optimized for mobile)
*   Phi-3 Mini
*   TensorFlow Lite
*   ONNX Runtime

#### Cloud AI
*   Llama 3
*   DeepSeek
*   OpenAI GPT
*   Gemini

Cloud AI is used only when necessary, while sensitive analysis stays on the device.

## 🚀 Getting Started

AEGIS is designed to be self-configuring. For the best experience:

1. **Automatic AI Download**: On first launch, the **Gemma 3N (1.5GB)** model will download and initialize automatically. You can track this in the **AI Assistant** tab.
2. **Setup Permissions**: Follow the in-app prompts to enable **Notification Access**, **Accessibility**, and **Overlay** permissions. These are required for the "Guardian Operating Layer" to function.
3. **Offline Protection**: Once the model is loaded, all security reasoning happens **100% locally** on your device.

For a detailed walkthrough, see [HOW_TO_USE.md](./HOW_TO_USE.md).

### Key Components
*   `GuardianCore`: The central orchestrator that broadcasts real-time `AnalysisResults`.
*   `ThreatOverlayManager`: Manages the system-wide interactive Compose-based alerts.
*   `AegisAccessibilityService`: The primary bridge for real-time, cross-app text and behavior analysis.
*   `AssistantViewModel`: A conversational engine with simulated reasoning and intent analysis.
*   `EmergencyGuardian`: Automatic threat isolation and recovery for critical incidents.
*   `GuardianScoreCard`: Enhanced visualization with trend analysis and category breakdowns.
*   `ThreatMapComponent`: Real-time cyberattack visualization across Cameroon.

---

## 🛠️ Project Structure
*   `com.aegis.agents`: Specialized AI agents (Scam, Intent, Malware, Deepfake, Behavioral, Decision, Privacy, Misinformation).
*   `com.aegis.ai`: Implementation of local inference engines and model management.
*   `com.aegis.core`: Core data structures (GuardianScore, ThreatLevel, EmergencyGuardian).
*   `com.aegis.data`: Repository layer and encrypted Room database.
*   `com.aegis.services.overlay`: Logic for displaying interactive alerts over other apps.
*   `com.aegis.services.accessibility`: Cross-app monitoring implementation.
*   `com.aegis.ui`: Reactive Jetpack Compose screens and components.

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

## 💼 Business Model

*   **Free individual version**: Basic protection for all users
*   **Premium Guardian subscription**: Advanced features and AI capabilities
*   **Enterprise cybersecurity platform**: Business dashboard and employee protection
*   **Government cybersecurity partnerships**: National cyber resilience
*   **Schools and universities**: Educational licensing
*   **Banks**: Integration with financial services
*   **Telecom operators**: Carrier partnerships
*   **Insurance partnerships**: Cyber insurance integration
*   **API licensing**: Third-party developer access

---

## 🏆 Why Judges Will Choose AEGIS

Your project directly addresses every evaluation point:

*   **Relevance**: Protects citizens, businesses, and government against scams, misinformation, identity theft, and cybercrime in Cameroon.
*   **Innovation**: Introduces the concept of **Guardian Computing**, where AI protects human decision-making rather than only devices.
*   **Technical Feasibility**: Built with proven Android, AI, and cloud technologies, with a realistic MVP roadmap.
*   **Socio-economic Impact**: Reduces cybercrime losses, improves digital trust, supports digital inclusion, and creates opportunities for cybersecurity services and jobs.
*   **Business Sustainability**: Clear freemium, enterprise, government, and licensing revenue streams.
*   **AI Integration**: AI is central to threat detection, behavioral analysis, personalized guidance, and decision support.
*   **Digital Patriotism & Cybersecurity**: Strengthens national cyber resilience, promotes responsible digital behavior, combats misinformation, and aligns with national cybersecurity goals.

---

*AEGIS is not just software; it's a new computing layer designed to protect humans in the age of AI.*
