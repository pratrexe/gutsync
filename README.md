# GutSync

GutSync is a minimalist, high-performance mobile health application designed to bridge the gap between daily dietary choices and microbiome health. By translating nutritional data into actionable microbial insights, GutSync empowers users to cultivate a robust internal ecosystem.

## 🌟 Key Features

- **AI-Powered Meal Analysis**: Uses Gemini 2.5 Flash to extract nutritional components (Fiber, Polyphenols, etc.) from natural language meal descriptions.
- **Microbe Impact Score**: A proprietary algorithm that calculates the impact of every meal based on AI-analyzed data.
- **Ask Gemini**: A dedicated tab for direct interaction with an AI microbiome expert.
- **Biotic Density Tracking**: Visualizes the concentration of fibers, polyphenols, and fermented cultures in your diet.
- **Longitudinal Trends**: Track shifts in your gut's "Pro-inflammatory" vs "Anti-inflammatory" states over weeks and months.
- **Gut-Bites Library**: A curated collection of bite-sized educational modules explaining the science of the second brain.

## 🛠 Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Language**: Kotlin
- **AI**: Google Gemini (via Firebase Vertex AI)
- **Image Loading**: Coil 3
- **Icons**: Material Icons Extended
- **Architecture**: MVVM (Model-View-ViewModel)

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- Android SDK 37
- A Firebase project with Vertex AI enabled

### Installation
1. Clone the repository.
2. Open the project in Android Studio.
3. Connect your Firebase project (add `google-services.json` to the `app/` directory).
4. Sync Gradle and run the `:app:assembleDebug` task.
5. Deploy to an Android device or emulator running API 24+.

## 🧬 The Science

GutSync tracks specific "Microbiome-Active" compounds:

| Nutrient | Promoted Microbes | Mechanism |
| :--- | :--- | :--- |
| **Dietary Fiber** | Bifidobacterium, Lactobacillus | SCFA production (Butyrate) |
| **Polyphenols** | Akkermansia muciniphila | Enhances gut lining integrity |
| **Fermented Foods** | Transient Lactobacilli | Pathogen inhibition |
| **Saturated Fats** | Firmicutes (Pro-inflammatory) | Increases bile-tolerant bacteria |

## 📁 Project Structure

- `com.example.gutsync.data`: Contains the `MicrobeImpactCalculator` and data models.
- `com.example.gutsync.ui.screens`: Jetpack Compose screens (Dashboard, Log, Trends, Insights).
- `com.example.gutsync.ui.theme`: Custom Material 3 theme with a high-contrast dark aesthetic.
- `com.example.gutsync.GutSyncViewModel.kt`: Handles state and AI interaction.

## 📝 License

This project is part of the GutSync MVP and is for educational/demonstration purposes.
