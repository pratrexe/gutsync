# GutSync

GutSync is a minimalist, high-performance mobile health application designed to bridge the gap between daily dietary choices and microbiome health. By translating nutritional data into actionable microbial insights, GutSync empowers users to cultivate a robust internal ecosystem.

## 🌟 Key Features

- **Advanced AI-Powered Meal Analysis**: Uses **Qwen3.5-397B-A17B (via NVIDIA)** for a multi-stage analysis pipeline:
    1. **Visual Extraction**: Identifies food names, ingredients, portion sizes, brands, and even barcodes or nutrition label OCR from photos.
    2. **Structured Search**: Routes data to **Open Food Facts** (for barcodes) or **USDA FoodData Central** for high-fidelity nutritional matching.
    3. **Microbe Impact Engine**: Calculates precise shifts in Bifidobacterium, Lactobacillus, Akkermansia, and more.
- **AI Explanation**: Qwen provides scientific reasoning for every score, explaining prebiotic/probiotic impact and diversity shifts.
- **Manual Food Logging**: Precise manual entry with photo attachment support.
- **Longitudinal Trends**: Track "Pro-inflammatory" vs "Anti-inflammatory" gut states over time.
- **Google Drive Sync**: Secure, cross-device data backup via personal cloud storage.

## 🛠 Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Language**: Kotlin
- **Primary AI**: NVIDIA Qwen3.5-397B-A17B (Multimodal)
- **Fallback/Chat AI**: Groq Llama 3.3 70B
- **Database**: USDA FoodData Central & Open Food Facts logic
- **Storage**: Google Drive API + Local JSON

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- Android SDK 37
- API Keys for **Groq** and **NVIDIA**

### Installation
1. Clone the repository.
2. Open the project in Android Studio.
3. Add your API keys to `local.properties`:
   ```properties
   GROQ_API_KEY=your_groq_key
   NVIDIA_API_KEY=your_nvidia_key
   ```
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

- `com.example.gutsync.data`: Contains the `MicrobeImpactCalculator`, AI clients, and data models.
- `com.example.gutsync.ui.screens`: Jetpack Compose screens (Dashboard, Log, Trends, Insights, AI).
- `com.example.gutsync.ui.theme`: Custom Material 3 theme with a high-contrast dark aesthetic.
- `com.example.gutsync.GutSyncViewModel.kt`: Handles state, AI interaction, and business logic.

## 📝 License

This project is part of the GutSync MVP and is for educational/demonstration purposes.
