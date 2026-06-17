# ⚡ InnoGen AI Pro
### Intelligent Concept-to-Code Generation Platform

> Turn your app ideas into complete, production-ready code in seconds using GPT-4o.

---

## 📋 Table of Contents

1. [Project Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [File Structure](#file-structure)
4. [Prerequisites](#prerequisites)
5. [API Keys Setup Guide](#api-keys-setup)
6. [Firebase Setup](#firebase-setup)
7. [Running the App](#running-the-app)
8. [Screens & Features](#screens)
9. [Troubleshooting](#troubleshooting)

---

## 🎯 Project Overview <a name="overview"></a>

**InnoGen AI Pro** is a full Android application built with Kotlin + Jetpack Compose that:

- Takes a plain-English app idea as input
- Calls **GPT-4o** to generate complete frontend, backend, DB schema, Docker config, README, API docs, and test cases
- Saves projects locally (Room DB) and in the cloud (Firebase Firestore)
- Detects bugs, analyzes security, and generates test cases via AI
- Pushes generated code to **GitHub** automatically
- Shows **deployment instructions** with Docker configs

---

## 🛠 Tech Stack <a name="tech-stack"></a>

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt (Dagger) |
| Backend | Firebase (Auth + Firestore + Storage + FCM) |
| AI | OpenAI GPT-4o API |
| Networking | Retrofit + OkHttp |
| Local DB | Room |
| Preferences | DataStore |
| Animations | Compose Animations + Accompanist |
| Navigation | Navigation Compose |

---

## 📁 File Structure <a name="file-structure"></a>

```
InnoGenAIPro/
├── app/
│   ├── src/main/
│   │   ├── java/com/innogen/aipro/
│   │   │   ├── InnoGenApp.kt                    # Application class (Hilt)
│   │   │   ├── MainActivity.kt                  # Single Activity
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── InnoGenDatabase.kt       # Room database
│   │   │   │   │   ├── dao/ProjectDao.kt        # Room DAO
│   │   │   │   │   └── entities/ProjectEntity.kt # Room entity + mappers
│   │   │   │   └── remote/
│   │   │   │       ├── api/
│   │   │   │       │   ├── OpenAIService.kt     # Retrofit service for OpenAI
│   │   │   │       │   └── GitHubService.kt     # Retrofit service for GitHub
│   │   │   │       ├── fcm/
│   │   │   │       │   └── InnoGenMessagingService.kt # Push notifications
│   │   │   │       ├── AIRepositoryImpl.kt      # OpenAI calls + parsing
│   │   │   │       ├── AuthRepositoryImpl.kt    # Firebase Auth
│   │   │   │       ├── GitHubRepositoryImpl.kt  # GitHub API
│   │   │   │       └── ProjectRepositoryImpl.kt # Room + Firestore sync
│   │   │   ├── di/
│   │   │   │   ├── AppModule.kt                 # Hilt: Retrofit, Room, Firebase
│   │   │   │   └── RepositoryModule.kt          # Hilt: Repository bindings
│   │   │   ├── domain/
│   │   │   │   ├── model/Models.kt              # Domain models (Project, User…)
│   │   │   │   └── repository/Repositories.kt  # Repository interfaces
│   │   │   └── presentation/
│   │   │       ├── Components.kt                # Reusable UI components
│   │   │       ├── navigation/NavHost.kt        # Navigation graph
│   │   │       ├── theme/
│   │   │       │   ├── Theme.kt                 # Material3 theme + colors
│   │   │       │   └── Type.kt                  # Typography + Shapes
│   │   │       ├── splash/SplashScreen.kt
│   │   │       ├── onboarding/OnboardingScreen.kt
│   │   │       ├── auth/
│   │   │       │   ├── AuthViewModel.kt
│   │   │       │   └── AuthScreen.kt
│   │   │       ├── dashboard/
│   │   │       │   ├── DashboardViewModel.kt
│   │   │       │   └── DashboardScreen.kt
│   │   │       ├── generation/
│   │   │       │   ├── GenerationViewModel.kt
│   │   │       │   └── GenerationScreen.kt
│   │   │       ├── project/
│   │   │       │   ├── ProjectViewModel.kt
│   │   │       │   └── ProjectOverviewScreen.kt
│   │   │       ├── codeviewer/CodeViewerScreen.kt
│   │   │       ├── github/
│   │   │       │   ├── GitHubViewModel.kt
│   │   │       │   └── GitHubScreen.kt
│   │   │       ├── deployment/DeploymentScreen.kt
│   │   │       └── profile/
│   │   │           ├── ProfileViewModel.kt
│   │   │           └── ProfileScreen.kt
│   │   ├── res/
│   │   │   ├── drawable/ic_splash_logo.xml
│   │   │   ├── mipmap-*/ic_launcher*.xml
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── xml/
│   │   │       ├── backup_rules.xml
│   │   │       └── data_extraction_rules.xml
│   │   └── AndroidManifest.xml
│   ├── google-services.json                     # ⚠️ REPLACE with your own
│   └── build.gradle
├── gradle/wrapper/gradle-wrapper.properties
├── build.gradle
├── settings.gradle
├── gradle.properties
└── local.properties                             # ⚠️ Add your API keys here
```

---

## ✅ Prerequisites <a name="prerequisites"></a>

Before running the app, make sure you have:

| Tool | Version | Download |
|------|---------|----------|
| Android Studio | Hedgehog 2023.1.1+ | [Download](https://developer.android.com/studio) |
| JDK | 17+ | Bundled with Android Studio |
| Android SDK | API 26+ | Via Android Studio SDK Manager |
| Git | Any | [Download](https://git-scm.com) |

---

## 🔑 API Keys Setup Guide <a name="api-keys-setup"></a>

You need **3 things** to run the app:

---

### 1. 🤖 OpenAI API Key (REQUIRED — for AI generation)

1. Go to [https://platform.openai.com/api-keys](https://platform.openai.com/api-keys)
2. Sign in or create a free account
3. Click **"Create new secret key"**
4. Copy the key (starts with `sk-...`)
5. Add it to `local.properties`:
   ```
   OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxx
   ```
> 💡 **Cost:** GPT-4o charges ~$0.005 per generation. $5 credit is enough for 1000+ generations.
> 
> 💡 **Free option:** Change `model = "gpt-4o"` to `model = "gpt-3.5-turbo"` in `AIRepositoryImpl.kt` for free tier (lower quality).

---

### 2. 🔥 Firebase Setup (REQUIRED — for auth, database, notifications)

#### Step 1: Create Firebase Project
1. Go to [https://console.firebase.google.com](https://console.firebase.google.com)
2. Click **"Add project"** → Enter project name: `InnoGenAIPro`
3. Disable Google Analytics (optional) → **Create Project**

#### Step 2: Add Android App
1. Click **"Add app"** → Select **Android** icon
2. Enter Android package name: `com.innogen.aipro`
3. Enter App nickname: `InnoGen AI Pro`
4. Click **Register app**
5. **Download `google-services.json`**
6. **Replace** the placeholder `app/google-services.json` with this file

#### Step 3: Enable Authentication
1. Firebase Console → **Authentication** → **Get started**
2. Click **Sign-in method** tab
3. Enable **Email/Password** → Save
4. Enable **Google** → Set project support email → Save
5. Copy the **Web client ID** (from Google provider settings)
6. Paste it in `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="default_web_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
   ```

#### Step 4: Create Firestore Database
1. Firebase Console → **Firestore Database** → **Create database**
2. Select **Start in test mode** (for development)
3. Choose a region → **Enable**

#### Step 5: Enable Storage
1. Firebase Console → **Storage** → **Get started**
2. Start in test mode → **Done**

#### Step 6: Enable Cloud Messaging
1. Firebase Console → **Messaging** → **Get started**
2. No extra configuration needed (auto-configured)

---

### 3. 🐙 GitHub API (OPTIONAL — for GitHub push feature)

The app uses Personal Access Tokens (PAT), not OAuth:

1. Go to [https://github.com/settings/tokens](https://github.com/settings/tokens)
2. Click **"Generate new token (classic)"**
3. Name: `InnoGen AI Pro`
4. Select scopes: ✅ `repo` (full control of private repositories)
5. Click **Generate token** → Copy it

> Users enter this token in the app's **GitHub Integration** screen. No `local.properties` entry needed.

---

## 🚀 Running the App <a name="running-the-app"></a>

### Step 1: Open the project
```bash
# Open Android Studio
# File → Open → Select the InnoGenAIPro/ folder
```

### Step 2: Set up local.properties
Open `local.properties` in the project root and add:
```properties
sdk.dir=/Users/YourUsername/Library/Android/sdk   # Your SDK path (auto-filled by AS)
OPENAI_API_KEY=sk-proj-your-actual-key-here
GITHUB_CLIENT_ID=your-client-id
GITHUB_CLIENT_SECRET=your-client-secret
```

### Step 3: Replace google-services.json
Replace `app/google-services.json` with the file downloaded from Firebase Console.

### Step 4: Update Web Client ID
In `app/src/main/res/values/strings.xml`, replace:
```xml
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID.apps.googleusercontent.com</string>
```
with your actual Web Client ID from Firebase → Authentication → Google sign-in provider.

### Step 5: Sync Gradle
- In Android Studio: **File → Sync Project with Gradle Files**
- Wait for dependencies to download (first time may take 5-10 minutes)

### Step 6: Run on device or emulator
- Connect Android device (USB debugging enabled) OR create AVD via **Tools → Device Manager**
- Click **▶ Run** (Shift+F10) or use **Run → Run 'app'**
- Minimum Android version: **API 26 (Android 8.0)**

---

## 📱 Screens & Features <a name="screens"></a>

| Screen | Description |
|--------|-------------|
| 🚀 Splash | Animated logo, auto-navigates based on auth state |
| 👋 Onboarding | 3-slide intro with swipe gestures |
| 🔐 Auth | Email/Password + Google Sign-In via Firebase |
| 🏠 Dashboard | Voice input, template suggestions, recent projects |
| 🤖 Generation | 7-step animated AI generation with real GPT-4o calls |
| 📊 Project Overview | Architecture, code preview, features, tech stack |
| 💻 Code Viewer | VS Code-style dark code viewer with copy buttons |
| 🐞 Bug Detection | AI-powered bug scan with fix suggestions |
| 🔐 Security Analysis | Vulnerability detection with risk levels |
| 🧪 Test Generator | Auto-generated unit/integration/e2e test cases |
| 🔄 Edit & Regenerate | Modify prompt and regenerate entire project |
| 🐙 GitHub | 3-step: Auth → Create Repo → Push code |
| 🚀 Deployment | Docker config, deployment steps, cloud platforms |
| 👤 Profile | Dark mode toggle, stats, sign out |

---

## 🐛 Troubleshooting <a name="troubleshooting"></a>

### "Build failed: google-services.json not found"
→ Replace `app/google-services.json` with your real Firebase file

### "OpenAI API error: 401"
→ Your `OPENAI_API_KEY` in `local.properties` is invalid or missing

### "OpenAI API error: 429"
→ Rate limit hit. Wait 1 minute or upgrade your OpenAI plan

### "Google Sign-In failed"
→ The `default_web_client_id` in `strings.xml` doesn't match your Firebase project

### "Hilt component not found" build error
→ Run **Build → Rebuild Project** in Android Studio

### App crashes on launch
→ Check `Logcat` in Android Studio for the full stack trace

### Firestore permission denied
→ In Firebase Console → Firestore → Rules, temporarily set:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 🏗 Architecture

```
┌─────────────────────────────────┐
│         Presentation Layer       │
│  (Compose Screens + ViewModels) │
└────────────┬────────────────────┘
             │ StateFlow / UiState
┌────────────▼────────────────────┐
│           Domain Layer           │
│  (Models + Repository Interfaces)│
└────────────┬────────────────────┘
             │ Implementations (Hilt)
┌────────────▼────────────────────┐
│            Data Layer            │
│  ┌──────────┐  ┌──────────────┐ │
│  │  Room DB │  │  Firebase +  │ │
│  │ (Local)  │  │  OpenAI API  │ │
│  └──────────┘  └──────────────┘ │
└─────────────────────────────────┘
```

---

## 📄 License

MIT License — Free to use for educational and commercial projects.

---

**Built with ❤️ using Kotlin, Jetpack Compose, and GPT-4o**
