# ZootBox Vending Machine Management System

This repository contains the various components for the ZootBox vending machine management system, encompassing a Go-based backend for inventory, an Android application for on-device payment and dispensing, and a web portal for remote operator management.

## Project Components

The system is composed of three main applications:

1.  **Backend Inventory Service (Go)**: A lightweight Go microservice managing real-time inventory across 10 vending machine coil slots. It integrates with the ZootBox Android app via a localhost REST API.
2.  **Android Application**: The on-device application responsible for interacting with the Nayax VPOS Touch payment system, managing USB devices (like the Nayax reader and ID scanner), and communicating with the Go backend for inventory and transaction logging. It implements a "Pre-Selection" payment flow where the price is displayed before card tap.
3.  **Inventory Web Portal (Vanilla JS/HTML/CSS)**: A web-based application designed for vending machine operators to remotely monitor and manage multiple machines from a Windows desktop. It provides real-time inventory monitoring, bulk refill operations, manual inventory adjustments, jam event resolution, and product link configuration.

## Technologies Used

### Backend Inventory Service
*   **Language**: Go 1.21+
*   **Database**: SQLite3 with WAL mode
*   **Web Framework**: Chi router v5
*   **Logging**: Zerolog structured logging
*   **Target Platform**: ARM64 Android tablet (Android 8.0+)

### Android Application
*   **Platform**: Android (Kotlin/Java)
*   **Payment Integration**: Marshall SDK for Nayax VPOS Touch
*   **USB Communication**: Custom FTDI serial driver for Nayax VPOS Touch (Chipi-X)
*   **Key Features**: Pre-Selection payment flow, USB auto-grant for devices, permission queue system, tax-inclusive pricing (7.5%), Robust Kiosk Mode (App Pinning + Notification Shade Blocking).

### Inventory Web Portal
*   **Frontend**: Vanilla JavaScript (ES2020+), HTML5, CSS3
*   **Data Storage**: Browser LocalStorage for machine configurations
*   **API Interaction**: REST API (communicates with the Go backend)
*   **Deployment**: Static HTML/CSS/JS (served locally or from any static web server)

## Building and Running

### Backend Inventory Service (Go)

1.  **Prerequisites**: Go 1.21+, SQLite3, Android Debug Bridge (ADB) for tablet deployment.
2.  **Install Dependencies**:
    ```bash
    go mod download
    ```
3.  **Run Database Migrations**:
    ```bash
    make migrate
    ```
4.  **Start the Server Locally**:
    ```bash
    make run
    ```
5.  **Build for ARM64 Android tablet**:
    ```bash
    make build-arm64
    ```
6.  **Install to Tablet via ADB**:
    ```bash
    make install-tablet
    ```
7.  **Run Tests**:
    ```bash
    make test          # Unit tests
    make test-coverage # Generate coverage report
    make test-integration # Integration tests (requires server running)
    make test-all      # Comprehensive test suite
    ```

### Android Application

1.  **Prerequisites**: Android SDK, a running Android emulator or a physical Android tablet with ADB enabled.
2.  **Build and Install to Emulator/Device**:
    Navigate to `MyApplication` directory.
    ```bash
    ./build-and-preview.sh
    ```
    This script will build the debug APK, install it to a running emulator/device, and attempt to launch the app.
    Alternatively, you can use Gradle:
    ```bash
    ./gradlew assembleDebug
    ./gradlew installDebug
    ```
3.  **ADB Logcat for Debugging Nayax**:
    ```bash
    adb logcat -v time | grep -iE "Nayax|vmc_link|vmc_vend_t|handleMessage|Pre-Selection|vend_request"
    ```

### Inventory Web Portal

1.  **Prerequisites**: Python 3.x (for `http.server`) or Node.js (for `http-server`).
2.  **Navigate to Portal Directory**:
    ```bash
    cd portal
    ```
3.  **Start Local Web Server**:
    **Option A: Python HTTP Server**
    ```bash
    python -m http.server 3000
    ```
    **Option B: Node.js http-server (requires `npx http-server` to be available)**
    ```bash
    npx http-server -p 3000
    ```
4.  **Open Portal in Browser**:
    Navigate to `http://localhost:3000`.
5.  **Development Scripts (from `portal/package.json`)**:
    *   `npm run dev`: Starts a live development server.
    *   `npm run build`: Creates a production build.
    *   `npm run lint`: Runs ESLint for code quality.

## Development Conventions

### General
*   **Git Repository**: The root `package.json` indicates a Git repository at `https://github.com/bossmandlow523/zootbox.git`.
*   **Monorepo Tooling**: The presence of `repomix` in the root `package.json` suggests some form of monorepo management or code generation might be in use.

### Backend (Go)
*   **Code Formatting**: `go fmt ./...` (from `Makefile`).
*   **Linting**: `golangci-lint run` (from `Makefile`).
*   **Dependency Management**: `go mod tidy`.
*   **Testing**: Emphasis on unit and integration tests with coverage reporting.

### Android Application
*   **USB Interaction**: Prioritizes FTDI interface (0x0403:0x6015) for Nayax VPOS Touch.
*   **Payment Flow**: Uses "Pre-Selection" mode (`always_idle = true`) for Nayax, where price is displayed before card tap.
*   **Permission Handling**: Implements a permission queue system for multiple USB devices.
*   **Tax Calculation**: Incorporates 7.5% tax directly into payment amounts.
*   **Debugging**: Extensive logging for Nayax communication is available via `adb logcat`.

### Inventory Web Portal
*   **Frontend**: Vanilla JavaScript, HTML, and CSS; no complex frameworks.
*   **Styling**: Structured CSS (`main.css`, `grid.css`, `navigation.css`, `forms.css`).
*   **State Management**: Browser LocalStorage for machine configurations, in-memory cache for inventory.
*   **Security**: Designed for a secure, VPN-only environment; includes XSS protection, input validation, and CORS restrictions.
*   **Testing**: Primarily manual acceptance testing per user story.

## Key Documentation Files

*   `NAYAX_IMPLEMENTATION_STATUS.md`: A highly detailed document outlining the Android application's integration with the Nayax VPOS Touch, including numerous bug fixes, architectural details, and pre-selection flow explanation. This is critical for understanding the Android component.
*   `Backend/README.md`: Provides a comprehensive overview of the Go backend service, its features, architecture, quick start guide, API endpoints, and development workflow.
*   `portal/README.md`: Describes the web-based inventory management portal, its features, quick start instructions, project structure, technology stack, and security considerations.
