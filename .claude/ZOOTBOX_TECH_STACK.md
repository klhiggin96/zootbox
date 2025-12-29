# ZootBox Vending Machine - Complete Technical Stack

## Table of Contents
- [Frontend (Android Application)](#frontend-android-application)
- [Hardware Integration](#hardware-integration)
- [Backend Services](#backend-services)
- [Development Tools](#development-tools)
- [Deployment & Distribution](#deployment--distribution)
- [Architecture Patterns](#architecture-patterns)
- [Security & Compliance](#security--compliance)
- [Future Roadmap](#future-roadmap)

---

## Frontend (Android Application)

### Core Framework
| Technology | Version | Purpose |
|-----------|---------|---------|
| **Kotlin** | Latest | Primary programming language |
| **Android SDK** | API 34+ (Android 14+) | Target platform |
| **Min SDK** | API 26 (Android 8.0) | Minimum supported version |
| **Gradle** | 8.x | Build automation tool |
| **Android Gradle Plugin** | 8.x | Android build configuration |

### UI Framework & Components
| Component | Technology | Implementation |
|-----------|-----------|----------------|
| **Activity Framework** | AndroidX AppCompat | 5 activities (MainActivity, ProductGridActivity, ProductDetailActivity, IdScanActivity, ScreensaverActivity) |
| **Layout System** | XML Layouts + ConstraintLayout | Responsive UI with edge-to-edge display |
| **RecyclerView** | AndroidX RecyclerView | Product grid with GridLayoutManager (2 columns) |
| **ViewBinding** | Android View Binding | Type-safe view access |
| **Material Design** | Material Components | Buttons, cards, styling |
| **Custom Animations** | Android Animation Framework | Section entrance animations (600ms stagger), scanning line animations |
| **Video Playback** | Android VideoView | Product videos, screensaver dual video loop |

### AndroidX Libraries
```kotlin
implementation("androidx.core:core-ktx:1.12.0+")
implementation("androidx.appcompat:appcompat:1.6.1+")
implementation("androidx.constraintlayout:constraintlayout:2.1.4+")
implementation("androidx.recyclerview:recyclerview:1.3.2+")
implementation("androidx.activity:activity-ktx:1.8.0+")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2+")
```

### UI/UX Features
- **Fullscreen Immersive Mode**: Edge-to-edge display, hidden nav/status bars
- **Color Theming System**: 8 predefined color schemes with dynamic toggling
- **Gradient Backgrounds**: Per-product gradient backgrounds (XML drawables)
- **Video Backgrounds**: Dual video player with cross-fade (1.5s interpolation)
- **Touch Interactions**: Idle timeout detection (30s), reset on touch
- **Quantity Controls**: +/- buttons with real-time price calculation
- **Progress Animations**: Scanning progress bar, animated scanning line (2s loop)

---

## Hardware Integration

### USB Serial Communication

#### USB Serial Library
```kotlin
implementation("com.github.mik3y:usb-serial-for-android:3.5.1")
```
- **Purpose**: USB host communication with serial devices
- **Drivers**: FTDI, CDC-ACM
- **Protocols**: Custom serial protocols over USB

#### ID Scanner (E-Seek M260)
| Specification | Value |
|--------------|-------|
| **Vendor ID** | 0x0403 |
| **Product ID** | 0x6001 |
| **Driver Type** | FTDI Serial (FtdiSerialDriver) |
| **Baud Rate** | 115200 bps |
| **Data Bits** | 8 |
| **Stop Bits** | 1 |
| **Parity** | None |
| **Buffer Size** | 1024 bytes |
| **Read Timeout** | 1000ms |
| **Data Format** | AAMVA PDF417 Barcode Standard |
| **Parsing** | Regex-based field extraction |

**Key Fields Extracted:**
- `DAA`: Date of Birth (YYYYMMDD)
- `DBA`: Expiration Date
- `DAC`: First Name
- `DCS`: Last Name

**Implementation:**
```kotlin
class IdScannerManager(private val context: Context) {
    private var usbSerialPort: UsbSerialPort? = null
    private val _scanResult = MutableStateFlow<IdScanResult?>(null)
    val scanResult: StateFlow<IdScanResult?> = _scanResult.asStateFlow()

    fun connectToDevice(device: UsbDevice): Boolean
    fun parseAAMVAData(data: String): Map<String, String>
    fun isAgeVerified(requiredAge: Int): Boolean
}
```

#### Payment Reader (Nayax)
| Specification | Value |
|--------------|-------|
| **Vendor ID** | 0x26f1 |
| **Product ID** | 0x5650 |
| **Driver Type** | CDC-ACM Serial (CdcAcmSerialDriver) |
| **Baud Rate** | 115200 bps |
| **Protocol** | Nayax proprietary serial protocol |
| **Transaction Timeout** | 60 seconds |

**Payment States:**
```kotlin
enum class PaymentState {
    IDLE,
    INITIALIZING,
    WAITING_FOR_CARD,
    PROCESSING,
    APPROVED,
    DECLINED,
    ERROR,
    CANCELLED
}
```

**Implementation:**
```kotlin
class NayaxPaymentManager(private val context: Context) {
    private val _paymentState = MutableStateFlow(PaymentState.IDLE)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    suspend fun processPayment(amount: Double): PaymentResult
    private fun sendCommand(command: String)
    private fun parseResponse(response: String): PaymentResult
}
```

### Foreground Service
```kotlin
class HardwareService : Service() {
    private lateinit var idScannerManager: IdScannerManager
    private lateinit var nayaxPaymentManager: NayaxPaymentManager

    override fun onCreate() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        initializeHardware()
    }
}
```

**Permissions Required:**
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>
<uses-feature android:name="android.hardware.usb.host"/>
```

---

## Backend Services

### Current State: No Backend
**Note:** The current implementation is a standalone Android app with no backend services. All data is hardcoded.

### Recommended Backend Stack (Future)

#### API Framework
| Technology | Purpose |
|-----------|---------|
| **Node.js + Express** | RESTful API server |
| **Kotlin + Ktor** | Alternative: Native Kotlin backend |
| **Python + FastAPI** | Alternative: Python-based API |

#### Database
| Database | Use Case |
|----------|----------|
| **PostgreSQL** | Primary relational database (products, transactions, inventory) |
| **Redis** | Session management, caching |
| **MongoDB** | Log storage, analytics data |

#### Cloud Services (Recommended: AWS)
| Service | Purpose |
|---------|---------|
| **EC2** | Application server hosting |
| **RDS (PostgreSQL)** | Managed database |
| **S3** | Video/image asset storage |
| **CloudFront** | CDN for video delivery |
| **Lambda** | Serverless functions (payment webhooks) |
| **API Gateway** | RESTful API management |
| **CloudWatch** | Monitoring and logging |

#### API Endpoints (Proposed)
```
GET    /api/products              - Get all products
GET    /api/products/:id          - Get product by ID
GET    /api/products?category=X   - Filter by category
POST   /api/transactions          - Create transaction
GET    /api/transactions/:id      - Get transaction status
POST   /api/age-verify            - Record age verification
POST   /api/payment/initiate      - Initiate payment
POST   /api/payment/confirm       - Confirm payment
GET    /api/inventory             - Get current inventory
POST   /api/inventory/update      - Update inventory count
GET    /api/analytics             - Get sales analytics
```

#### Authentication & Authorization
```
JWT (JSON Web Tokens) - Machine authentication
API Keys - Hardware device authentication
OAuth 2.0 - Admin dashboard access
```

---

## Development Tools

### IDE & Code Editors
| Tool | Purpose |
|------|---------|
| **Android Studio** | Primary IDE (Hedgehog/Iguana/Jellyfish) |
| **IntelliJ IDEA** | Kotlin development |
| **VS Code** | Markdown, configuration files |

### Version Control
```bash
Git - Version control system
GitHub - Repository hosting (current: local repo)
GitFlow - Branching strategy (main, develop, feature/*, release/*)
```

### Build & CI/CD
| Tool | Purpose |
|------|---------|
| **Gradle** | Build automation |
| **Gradle Wrapper** | Version-consistent builds |
| **ProGuard/R8** | Code obfuscation and minification |
| **GitHub Actions** | CI/CD pipelines (future) |
| **Fastlane** | Automated deployment (future) |

### Testing Framework
```kotlin
// Unit Testing
testImplementation("junit:junit:4.13.2")
testImplementation("org.mockito:mockito-core:5.0.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.0")

// UI Testing
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.test:runner:1.5.2")
androidTestImplementation("androidx.test:rules:1.5.0")

// Mocking
testImplementation("io.mockk:mockk:1.13.5")
androidTestImplementation("io.mockk:mockk-android:1.13.5")
```

### Debugging & Monitoring
| Tool | Purpose |
|------|---------|
| **ADB (Android Debug Bridge)** | Device communication, logcat |
| **Logcat** | Real-time logging |
| **Android Profiler** | CPU, memory, network profiling |
| **LeakCanary** | Memory leak detection (dev builds) |
| **Stetho** | Chrome DevTools integration (future) |

### Code Quality
```kotlin
// Static Analysis
detekt - Kotlin static code analysis
ktlint - Kotlin linter
Android Lint - Android-specific checks

// Code Coverage
JaCoCo - Java/Kotlin code coverage
```

---

## Deployment & Distribution

### Build Variants
```kotlin
buildTypes {
    debug {
        applicationIdSuffix = ".debug"
        versionNameSuffix = "-DEBUG"
        isDebuggable = true
        isMinifyEnabled = false
    }

    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("release")
    }
}

flavorDimensions += "environment"
productFlavors {
    create("dev") {
        dimension = "environment"
        applicationIdSuffix = ".dev"
        versionNameSuffix = "-dev"
    }

    create("prod") {
        dimension = "environment"
    }
}
```

### APK Generation
```bash
# Debug APK
./gradlew assembleDebug

# Release APK (signed)
./gradlew assembleRelease

# Current output location
c:\dev\MyApplication\app\build\outputs\apk\release\app-release.apk
c:\dev\shippable_apk\VendingClient.apk
```

### App Signing
```
Keystore: release.keystore (not in repo)
Algorithm: RSA 2048-bit
Validity: 25+ years
Key Alias: zootbox-release
Signature: SHA-256 with RSA
```

### Distribution Strategy
| Method | Use Case |
|--------|----------|
| **Manual Installation** | Direct APK transfer via ADB/USB |
| **Internal Testing** | Google Play Internal Testing Track (future) |
| **Private Distribution** | Google Play Private Channel (future) |
| **MDM (Mobile Device Management)** | Enterprise deployment (future) |

### Device Management
```
ADB Commands:
adb install -r app-release.apk          # Install/update
adb uninstall com.example.myapplication # Uninstall
adb shell am start -n com.example.myapplication/.MainActivity  # Launch
adb logcat | grep ZootBox                # Filter logs
adb push video.mp4 /storage/emulated/0/Movies/ZootBox/  # Upload videos
```

---

## Architecture Patterns

### Application Architecture

#### MVVM-Lite Pattern
```
View (Activity/Fragment)
    ↓ observes
ViewModel (Implicit - ViewState in Activity)
    ↓ manages
Model (Data Classes + Managers)
```

**Current Implementation:** Activities contain both View and ViewModel logic (suitable for simple kiosk app).

**Recommended Migration (for scalability):**
```kotlin
// ViewModel example
class ProductDetailViewModel : ViewModel() {
    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _quantity = MutableStateFlow(1)
    val quantity: StateFlow<Int> = _quantity.asStateFlow()

    val totalPrice: StateFlow<Double> = combine(product, quantity) { prod, qty ->
        (prod?.price ?: 0.0) * qty
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    fun incrementQuantity() { _quantity.value++ }
    fun decrementQuantity() { if (_quantity.value > 1) _quantity.value-- }
}
```

### Design Patterns Used

#### 1. Service Locator Pattern
```kotlin
// HardwareService acts as service locator
class HardwareService : Service() {
    val idScannerManager: IdScannerManager
    val paymentManager: NayaxPaymentManager

    inner class HardwareBinder : Binder() {
        fun getService(): HardwareService = this@HardwareService
    }
}
```

#### 2. Observer Pattern (StateFlow)
```kotlin
// IdScannerManager publishes scan results
private val _scanResult = MutableStateFlow<IdScanResult?>(null)
val scanResult: StateFlow<IdScanResult?> = _scanResult.asStateFlow()

// IdScanActivity observes
lifecycleScope.launch {
    idScannerManager.scanResult.collect { result ->
        result?.let { handleScanResult(it) }
    }
}
```

#### 3. State Machine Pattern
```kotlin
enum class ScanState { IDLE, SCANNING, SUCCESS }

private var currentState = ScanState.IDLE
    set(value) {
        field = value
        when (value) {
            ScanState.IDLE -> showIdleLayout()
            ScanState.SCANNING -> showScanningLayout()
            ScanState.SUCCESS -> showSuccessLayout()
        }
    }
```

#### 4. Builder Pattern (Intent)
```kotlin
fun navigateToProductDetail(product: Product) {
    Intent(this, ProductDetailActivity::class.java).apply {
        putExtra("name", product.name)
        putExtra("price", product.price)
        putExtra("imageRes", product.imageRes)
        // ... more extras
    }.also { startActivity(it) }
}
```

#### 5. Adapter Pattern (RecyclerView)
```kotlin
class ProductAdapter(
    private val products: List<Product>,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
    // Adapts Product data to ViewHolder presentation
}
```

---

## Security & Compliance

### Data Security

#### Age Verification Compliance
```
AAMVA Standard Compliance - PDF417 barcode parsing
PII Handling - Date of birth extracted, not stored
No Data Persistence - Scan results discarded after verification
Local Processing - No ID data transmitted to servers
```

#### Payment Security
```
PCI DSS Considerations:
- No card data storage in app
- All payment processing via Nayax hardware
- Transaction IDs stored, not card numbers
- Encrypted communication with payment hardware
```

### App Security Features

#### ProGuard Rules
```proguard
# Keep hardware manager classes
-keep class com.example.myapplication.hardware.** { *; }

# Keep USB serial library
-keep class com.hoho.android.usbserial.** { *; }

# Obfuscate remaining code
-obfuscate
-optimizationpasses 5
```

#### Permissions Manifest
```xml
<!-- Required Permissions -->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>

<!-- Storage Permissions (for videos) -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32"/>
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO"/>

<!-- USB Host -->
<uses-feature android:name="android.hardware.usb.host"/>
```

#### Network Security Config
```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.zootbox.com</domain>
    </domain-config>

    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system"/>
        </trust-anchors>
    </base-config>
</network-security-config>
```

### Compliance & Regulations

#### Age-Restricted Sales
```
Tobacco Products (21+) - ZYN, Vapes
Tobacco Accessories (18+) - Lighters, Rolling Papers
ID Scanning - AAMVA PDF417 standard
DOB Verification - Real-time age calculation
Audit Trail - Transaction logging (future)
```

#### Accessibility
```
TalkBack Support - ContentDescription for images
Touch Targets - Minimum 48dp touch targets
High Contrast - Color schemes with sufficient contrast
Text Scaling - Support for system font size
```

---

## File Structure

### Project Directory Layout
```
c:\dev\MyApplication\
├── app\
│   ├── src\
│   │   ├── main\
│   │   │   ├── java\com\example\myapplication\
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ProductGridActivity.kt
│   │   │   │   ├── ProductDetailActivity.kt
│   │   │   │   ├── IdScanActivity.kt
│   │   │   │   ├── ScreensaverActivity.kt
│   │   │   │   ├── hardware\
│   │   │   │   │   ├── HardwareService.kt
│   │   │   │   │   ├── IdScannerManager.kt
│   │   │   │   │   └── NayaxPaymentManager.kt
│   │   │   │   └── adapters\
│   │   │   │       └── ProductAdapter.kt
│   │   │   ├── res\
│   │   │   │   ├── layout\
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── activity_product_grid.xml
│   │   │   │   │   ├── activity_product_detail.xml
│   │   │   │   │   ├── activity_id_scan.xml
│   │   │   │   │   ├── activity_screensaver.xml
│   │   │   │   │   └── item_product.xml
│   │   │   │   ├── drawable\
│   │   │   │   │   ├── bg_*.xml (gradients)
│   │   │   │   │   ├── ic_*.xml (icons)
│   │   │   │   │   └── *.png (product images)
│   │   │   │   ├── values\
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   └── xml\
│   │   │   │       └── network_security_config.xml
│   │   │   └── AndroidManifest.xml
│   │   └── test\ (unit tests)
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle\
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat

External Storage:
/storage/emulated/0/Movies/ZootBox/
├── zyn_citrus.mp4
├── zoot_vape_x.mp4
├── night_owl_cam.mp4
└── ... (product videos)
```

---

## Development Workflow

### Git Workflow
```bash
# Feature development
git checkout -b feature/payment-integration
git add .
git commit -m "Add Nayax payment processing"
git push origin feature/payment-integration

# Current branch
ZOOTED (active development branch)

# Main branch (production-ready code)
main
```

### Build Process
```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Install on device
./gradlew installDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

### Code Review Checklist
- [ ] No hardcoded credentials
- [ ] USB permissions requested properly
- [ ] Hardware errors handled gracefully
- [ ] UI responsive on all screen sizes
- [ ] Video files loaded with error handling
- [ ] Age verification calculations correct
- [ ] Memory leaks checked (LeakCanary)
- [ ] Logcat output clean (no errors)

---

## Performance Optimization

### Current Optimizations
```kotlin
// RecyclerView ViewHolder pattern (recycling views)
class ProductViewHolder(binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

// StateFlow (efficient state management)
val scanResult: StateFlow<IdScanResult?> = _scanResult.asStateFlow()

// Video preloading (dual VideoView for screensaver)
videoView1.setVideoPath(videoPath)
videoView2.setVideoPath(videoPath)
```

### Recommended Optimizations

#### Image Loading
```kotlin
// Use Glide or Coil for image loading
implementation("io.coil-kt:coil:2.5.0")

imageView.load(product.imageRes) {
    crossfade(true)
    placeholder(R.drawable.placeholder)
    error(R.drawable.error_image)
}
```

#### Lazy Loading
```kotlin
// Lazy initialization for heavy objects
private val idScannerManager: IdScannerManager by lazy {
    IdScannerManager(applicationContext)
}
```

#### Coroutines
```kotlin
// Use dispatchers appropriately
lifecycleScope.launch(Dispatchers.IO) {
    val result = performHeavyOperation()
    withContext(Dispatchers.Main) {
        updateUI(result)
    }
}
```

---

## Monitoring & Analytics (Future)

### Crash Reporting
```kotlin
implementation("com.google.firebase:firebase-crashlytics:18.6.0")
implementation("com.google.firebase:firebase-analytics:21.5.0")
```

### Usage Analytics
```
Track:
- Product views
- Purchase attempts
- Age verification success rate
- Payment success/failure
- Screensaver activation frequency
- Category popularity
```

### Hardware Monitoring
```
Monitor:
- USB connection status
- ID scanner read failures
- Payment reader timeouts
- Transaction latency
- Hardware error rates
```

### Performance Metrics
```
- App startup time
- Activity transition time
- Video load time
- Network request latency (future)
- Memory usage
- Battery consumption
```

---

## Future Roadmap

### Phase 1: Backend Integration (Q1 2025)
- [ ] REST API development
- [ ] Product catalog from API
- [ ] Real-time inventory management
- [ ] Transaction recording
- [ ] Remote configuration

### Phase 2: Payment Processing (Q2 2025)
- [ ] Full Nayax integration
- [ ] Card payment flow
- [ ] Mobile payment support (Apple Pay, Google Pay)
- [ ] Receipt generation
- [ ] Refund handling

### Phase 3: Advanced Features (Q3 2025)
- [ ] User accounts (QR code scan)
- [ ] Loyalty program
- [ ] Promotions/discounts
- [ ] Push notifications
- [ ] Multi-language support

### Phase 4: Analytics & Optimization (Q4 2025)
- [ ] Admin dashboard
- [ ] Sales analytics
- [ ] Predictive inventory
- [ ] A/B testing
- [ ] Machine learning recommendations

---

## Dependencies Summary

### Current build.gradle.kts
```kotlin
dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-ktx:1.8.0")

    // USB Serial
    implementation("com.github.mik3y:usb-serial-for-android:3.5.1")

    // Material Design
    implementation("com.google.android.material:material:1.11.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
```

### Recommended Additions
```kotlin
// Networking (when backend ready)
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Image Loading
implementation("io.coil-kt:coil:2.5.0")

// Dependency Injection
implementation("com.google.dagger:hilt-android:2.48")
kapt("com.google.dagger:hilt-compiler:2.48")

// Database (future local caching)
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

---

## System Requirements

### Development Machine
```
OS: Windows 10/11, macOS 12+, or Linux
RAM: 16GB minimum, 32GB recommended
Storage: 10GB free space
CPU: Intel i5/AMD Ryzen 5 or better
GPU: Not required (Android Emulator uses CPU)
```

### Target Hardware (Vending Machine)
```
Device: Android Tablet/Industrial Computer
OS: Android 8.0+ (API 26+)
RAM: 4GB minimum
Storage: 32GB minimum (for video storage)
Screen: 10"+ touchscreen, 1920x1200 recommended
USB: 2x USB ports (ID scanner + payment reader)
Power: Continuous power supply
Network: Wi-Fi (optional, for future backend)
```

### Supported USB Devices
```
ID Scanner:
- E-Seek M260 (VID: 0x0403, PID: 0x6001)
- Other FTDI-based PDF417 scanners

Payment Readers:
- Nayax Onyx (VID: 0x26f1, PID: 0x5650)
- Other CDC-ACM based payment terminals
```

---

## Contact & Support

### Development Team
```
Project Lead: [Name]
Android Developer: [Name]
Hardware Integration: [Name]
Backend Developer: [Name] (future)
QA/Testing: [Name]
```

### External Resources
```
Android Documentation: https://developer.android.com
USB Serial Library: https://github.com/mik3y/usb-serial-for-android
AAMVA Specification: https://www.aamva.org
Nayax Developer Docs: [Vendor provided]
```

### Issue Tracking
```
GitHub Issues: [Repository URL]
Internal Tracker: [Jira/Linear/etc.]
```

---

## License

```
Proprietary - ZootBox Vending Machine System
Copyright (c) 2024 ZootBox Inc.
All rights reserved.
```

---

## Revision History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2024-12-27 | Initial tech stack documentation | Claude |
| 0.9 | 2024-12-XX | Alpha release with hardware integration | Dev Team |
| 0.5 | 2024-11-XX | Initial frontend development | Dev Team |

---

## Glossary

| Term | Definition |
|------|-----------|
| **AAMVA** | American Association of Motor Vehicle Administrators - Standard for driver's license barcodes |
| **ADB** | Android Debug Bridge - Command-line tool for device communication |
| **APK** | Android Package Kit - Android app installation file |
| **CDC-ACM** | Communications Device Class - Abstract Control Model (USB serial protocol) |
| **FTDI** | Future Technology Devices International - USB serial chip manufacturer |
| **Kiosk Mode** | Full-screen application mode for dedicated devices |
| **PDF417** | 2D barcode format used on driver's licenses |
| **StateFlow** | Kotlin coroutines observable state holder |
| **USB Host** | Android device acting as USB host (controlling USB peripherals) |
| **ViewBinding** | Android feature for type-safe view access |

---

**Document Generated:** December 27, 2024
**Tech Stack Version:** 1.0
**Application Version:** 0.9-ZOOTED
**Last Updated:** 2024-12-27
