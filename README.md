# AutoTrack - Smart Expense Tracker

**AutoTrack** is a professional, privacy-focused Android application designed to automate personal finance management. It intelligently detects transaction SMS from banks and provides deep insights into spending habits through an elegant Material 3 interface.

## 🚀 Why This Project?
In a world of digital payments, keeping track of every expense manually is tedious. Most existing apps either require manual entry or compromise privacy by uploading data to servers. 
**AutoTrack** was built to solve this by:
- **Automating Data Entry**: Using on-device SMS parsing to catch expenses as they happen.
- **Privacy First**: All data remains offline on the user's device.
- **Visual Insights**: Providing professional-grade analytics to help users understand their "Financial Health" at a glance.

---

## ✨ Key Features

### 📊 1. Professional Dashboard
- **Instant Overview**: High-level metrics for Total Expense, Credit, Net Balance, and Remaining Budget.
- **Smart Insights**: Automated comparisons (e.g., "You spent 15% more than last month").
- **Independent Filtering**: Filter by month, year, or specific bank accounts without affecting your global history.
- **Recent Activity**: Quick view of the latest 10 transactions directly on the home screen.

### 📋 2. Advanced Transaction History
- **Smart Grouping**: Transactions are organized by "Today", "Yesterday", and specific dates.
- **Gesture Controls**: Modern UI with **Swipe-to-Delete** and **Swipe-to-Edit** support.
- **Visual Branding**: Automatic category detection with professional iconography.

### 📈 3. Deep Analytics (Pro Feature)
- **Interactive Pie Charts**: Visual distribution of expenses by Merchant/Description.
- **Category Breakdown**: Percentage-based progress bars for spending categories (Food, Travel, Bills, etc.).
- **Account Split**: Analysis of spending across different bank accounts (HDFC, SBI, etc.).

### 🏦 4. Account & Budget Management
- **Multi-Account Support**: Manage different banks using keyword-based detection.
- **Budgeting**: Set a monthly budget and track your remaining balance in real-time.

---

## 🛡️ Security & Privacy
Security is the core of **AutoTrack**. We implemented industry-standard measures:
- **Database Encryption**: All financial records are stored in a **Room Database** encrypted via **256-bit AES SQLCipher**.
- **Code Obfuscation**: Uses **R8/ProGuard** to shrink and obfuscate the source code, preventing reverse engineering of parsing logic.
- **No Cloud Sync**: Your data never leaves your device.
- **System Transparency**: Includes a security system check to inform users about SMS permission usage.

> **Note**: For this implementation, a fixed passphrase is used for SQLCipher. For a production-ready application, integration with **Android Keystore** is recommended to manage encryption keys securely.

---

## 🛠️ Technology Stack
- **Language**: Kotlin (100%)
- **UI Framework**: Jetpack Compose (Modern Declarative UI)
- **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture principles.
- **Local Database**: Room Persistence Library.
- **Security**: SQLCipher for SQLite encryption.
- **Dependency Injection**: Kotlin Symbol Processing (KSP).

---

## 📂 Project Structure
```text
com.example.transaction
├── data                # Database, Entities, DAOs, and Repositories
│   ├── dao             # Room Data Access Objects
│   ├── entity          # Room Database Entities
│   └── AppDatabase.kt  # Secure Room Database Configuration
├── sms                 # SMS Detection & Parsing Logic
│   ├── SmsReceiver.kt  # BroadcastReceiver for SMS
│   └── SmsParser.kt    # Regex-based transaction detection
└── ui                  # Jetpack Compose UI Components
    ├── theme           # Material 3 Color Schemes & Typography
    ├── DashboardScreen # Main overview logic
    ├── HistoryScreen   # Advanced list with swipe actions
    └── AnalyticsScreen # Pie charts and spending splits
```

---

## 🔧 Installation & Setup
1. Clone the repository.
2. Open the project in **Android Studio Ladybug (or newer)**.
3. Sync Gradle and ensure **Kotlin 2.0.21** and **AGP 8.7.3** are installed.
4. Run the app on a physical device (SMS permissions are required for auto-detection).

---

## 📜 License
This project is for educational and personal use. All financial data detection logic is handled locally to ensure maximum user privacy.
