# ZootBox Vending Machine - Complete Application Flow

## Comprehensive Mermaid Flowchart: Screen-to-Screen Journey

```mermaid
graph TB
    Start([App Launch]) --> MainActivity

    subgraph MainActivity["MainActivity - Category Selection"]
        MA_Init[Initialize UI<br/>• Fullscreen setup<br/>• Load 8 color schemes<br/>• Animate sections<br/>• Start HardwareService] --> MA_Display[Display 4 Categories:<br/>1. ZYNS<br/>2. VAPES<br/>3. CIGARETTES<br/>4. ZOOTBOX LEGENDARY LOOT]
        MA_Display --> MA_Wait{User Action?}
        MA_Wait -->|Tap Color Toggle| MA_ColorCycle[Cycle Color Scheme<br/>0→1→2→3→4→5→6→7→0]
        MA_ColorCycle --> MA_Display
        MA_Wait -->|Tap Section Button| MA_Nav[Navigate to ProductGrid<br/>Pass category name]
        MA_Wait -->|Back Press| MA_Exit([Exit App])
    end

    MA_Nav --> ProductGridActivity

    subgraph ProductGridActivity["ProductGridActivity - Product Catalog"]
        PGA_Init[Initialize<br/>• Get category from Intent<br/>• Load 11 products<br/>• Filter by category<br/>• Setup 2-column grid] --> PGA_StartTimer[Start Idle Timer<br/>30 seconds]
        PGA_StartTimer --> PGA_Display[Display Filtered Products<br/>in 2-Column Grid]
        PGA_Display --> PGA_Wait{User Action?}
        PGA_Wait -->|Touch Screen| PGA_ResetTimer[Reset Idle Timer]
        PGA_ResetTimer --> PGA_Display
        PGA_Wait -->|Tap Product Card| PGA_NavDetail[Navigate to ProductDetail<br/>Pass product data]
        PGA_Wait -->|Tap Back Button| PGA_Return1([Return to MainActivity])
        PGA_Wait -->|30s Timeout| PGA_NavSaver[Launch ScreensaverActivity]
    end

    PGA_NavDetail --> ProductDetailActivity
    PGA_NavSaver --> ScreensaverActivity

    subgraph ScreensaverActivity["ScreensaverActivity - Idle Display"]
        SSA_Init[Initialize<br/>• Setup dual video players<br/>• 180° rotation<br/>• Cross-fade loop] --> SSA_Play[Play Video Loop<br/>1.5s fade between players]
        SSA_Play --> SSA_Wait{User Action?}
        SSA_Wait -->|Any Touch| SSA_Exit([Return to ProductGrid])
        SSA_Wait -->|Continue Playing| SSA_Play
    end

    SSA_Exit --> PGA_Display

    subgraph ProductDetailActivity["ProductDetailActivity - Product Details"]
        PDA_Init[Initialize<br/>• Extract product from Intent<br/>• Set quantity = 1<br/>• Load product video<br/>• Apply gradient background] --> PDA_Display[Display Product Info:<br/>• Name, Image, Price<br/>• Video playback<br/>• Quantity controls<br/>• Specifications]
        PDA_Display --> PDA_Wait{User Action?}
        PDA_Wait -->|Tap Back Button| PDA_Return([Return to ProductGrid])
        PDA_Wait -->|Tap + Button| PDA_IncQty[Increment Quantity<br/>Update total price]
        PDA_Wait -->|Tap - Button| PDA_DecQty[Decrement Quantity<br/>Min = 1<br/>Update total price]
        PDA_IncQty --> PDA_Display
        PDA_DecQty --> PDA_Display
        PDA_Wait -->|Tap ADD TO CART| PDA_CheckAge{Age Restricted?<br/>ageRestriction > 0}
        PDA_CheckAge -->|Yes: 18 or 21| PDA_NavID[Launch IdScanActivity<br/>Pass requiredAge]
        PDA_CheckAge -->|No Restriction| PDA_AddCart[Show Success Toast<br/>"Added to cart!"]
        PDA_AddCart --> PDA_Display
        PDA_ResultWait{ID Scan Result?}
        PDA_ResultWait -->|RESULT_OK| PDA_Verified[Show Verification Success<br/>Item added to cart]
        PDA_Verified --> PDA_Display
        PDA_ResultWait -->|RESULT_CANCELLED| PDA_Failed[Show Failure Toast<br/>"Verification Failed"]
        PDA_Failed --> PDA_Display
    end

    PDA_NavID --> IdScanActivity

    subgraph CartActivity["CartActivity - Shopping Cart Checkout (Future)"]
        CART_Init[Initialize<br/>• Load cart items<br/>• Calculate total<br/>• Bind HardwareService] --> CART_Display[Display Cart Items<br/>RecyclerView list]
        CART_Display --> CART_Wait{User Action?}
        CART_Wait -->|Tap Back| CART_Return([Return to ProductGrid])
        CART_Wait -->|Tap Checkout| CART_CheckAge{Any Age Restricted?}
        CART_CheckAge -->|Yes| CART_NavID[Launch IdScanActivity<br/>Pass max age required]
        CART_CheckAge -->|No| CART_Payment[Process Payment<br/>NayaxPaymentManager<br/>Pre-Selection Mode]
        CART_ResultWait{ID Scan Result?}
        CART_ResultWait -->|RESULT_OK| CART_Success[Show Success Toast]
        CART_Success --> CART_Payment
        CART_ResultWait -->|RESULT_CANCELLED| CART_Failed[Show Error<br/>"Age verification required"]
        CART_Failed --> CART_Display
        CART_Payment --> CART_Vend[Sequential Vend<br/>MotorControlManager]
        CART_Vend --> CART_Complete([Clear Cart & Finish])
    end

    CART_NavID --> IdScanActivity

    subgraph IdScanActivity["IdScanActivity - Age Verification"]
        ISA_Init[Initialize<br/>• Get requiredAge from Intent<br/>• Bind to HardwareService<br/>• Setup UI layers] --> ISA_Idle[STATE: IDLE<br/>Show "PLACE ID ON SCANNER"]
        ISA_Idle -->|1 Second Delay| ISA_Scanning[STATE: SCANNING<br/>• Animate scanning line<br/>• Show progress bar<br/>• Simulate progress 2%/50ms]
        ISA_Scanning --> ISA_HardwareWait{Hardware Scan Event?}
        ISA_HardwareWait -->|ID Scanned| ISA_Parse[IdScannerManager<br/>• Read USB FTDI serial<br/>• Parse AAMVA format<br/>• Extract DOB field]
        ISA_Parse --> ISA_Verify{Age Verification<br/>Calculate: currentYear - birthYear<br/>Adjust for month/day}
        ISA_Verify -->|Age >= RequiredAge| ISA_ProgressComplete[Complete Progress to 100%]
        ISA_ProgressComplete --> ISA_Success[STATE: SUCCESS<br/>Show success screen<br/>3 seconds]
        ISA_Success -->|After 3s| ISA_OK[setResult RESULT_OK<br/>finish]
        ISA_Verify -->|Age < RequiredAge| ISA_Fail[Show Error Toast<br/>"You must be X or older"]
        ISA_Fail -->|Wait 2s| ISA_Scanning
        ISA_HardwareWait -->|No Scan/Timeout| ISA_Scanning
    end

    ISA_OK --> PDA_ResultWait

    subgraph HardwareLayer["Hardware Service Layer (Background)"]
        HW_Service[HardwareService<br/>Foreground Service] -.-> HW_IDScanner[IdScannerManager<br/>VID=0x0403 PID=0x6001<br/>FTDI Serial 9600 baud]
        HW_Service -.-> HW_Payment[NayaxPaymentManager<br/>VID=0x0403 PID=0x6015<br/>FTDI Serial 115200 baud<br/>Marshall SDK]
        HW_IDScanner -.->|Scan Result| ISA_HardwareWait
        HW_Payment -.->|Payment Flow| CART_Payment
    end

    style MainActivity fill:#e1f5ff
    style ProductGridActivity fill:#fff3e0
    style ProductDetailActivity fill:#f3e5f5
    style CartActivity fill:#fff8e1
    style IdScanActivity fill:#e8f5e9
    style ScreensaverActivity fill:#fce4ec
    style HardwareLayer fill:#f5f5f5,stroke-dasharray: 5 5
    style Start fill:#4caf50,color:#fff
    style MA_Exit fill:#f44336,color:#fff
    style PDA_Complete fill:#f44336,color:#fff
    style PGA_Return1 fill:#f44336,color:#fff
    style PDA_Return fill:#f44336,color:#fff
    style CART_Return fill:#f44336,color:#fff
    style CART_Complete fill:#f44336,color:#fff
    style SSA_Exit fill:#f44336,color:#fff
    style ISA_OK fill:#4caf50,color:#fff
```

---

## Detailed State Machine Diagrams

### 1. MainActivity State Machine

```mermaid
stateDiagram-v2
    [*] --> Initialize
    Initialize --> ApplyColorScheme: Load 8 color schemes
    ApplyColorScheme --> AnimateSections: Stagger animations (600ms each)
    AnimateSections --> StartHardwareService: startForegroundService()
    StartHardwareService --> Ready: Display 4 category sections

    Ready --> ColorCycle: Tap color toggle button
    ColorCycle --> Ready: Apply next scheme (0-7)

    Ready --> NavigateToGrid: Tap section button
    NavigateToGrid --> [*]: Pass category to ProductGridActivity

    Ready --> Exit: Back button press
    Exit --> [*]

    note right of Ready
        User sees 4 stacked sections:
        1. ZYNS (btn_s1)
        2. VAPES (btn_s2)
        3. CIGARETTES (btn_s3)
        4. ZOOTBOX LEGENDARY LOOT (btn_s4)
    end note
```

### 2. ProductGridActivity State Machine

```mermaid
stateDiagram-v2
    [*] --> Initialize
    Initialize --> FilterProducts: Get category from Intent
    FilterProducts --> SetupGrid: Create 2-column RecyclerView
    SetupGrid --> StartIdleTimer: 30 second countdown
    StartIdleTimer --> DisplayProducts

    DisplayProducts --> ResetTimer: Any touch event
    ResetTimer --> DisplayProducts

    DisplayProducts --> NavigateToDetail: Tap product card
    NavigateToDetail --> [*]: Pass product data

    DisplayProducts --> ReturnHome: Tap back button
    ReturnHome --> [*]: finish()

    DisplayProducts --> Screensaver: 30s timeout
    Screensaver --> ScreensaverActivity
    ScreensaverActivity --> DisplayProducts: Touch to exit

    note right of FilterProducts
        Example: category = "ZyNS"
        Filter result: [ZYN CITRUS, GUM MINT]
    end note
```

### 3. ProductDetailActivity State Machine

```mermaid
stateDiagram-v2
    [*] --> Initialize
    Initialize --> LoadProductData: Extract Intent extras
    LoadProductData --> SetQuantity: quantity = 1
    SetQuantity --> LoadMedia: Setup video player
    LoadMedia --> ApplyTheme: Apply gradient background
    ApplyTheme --> DisplayProduct

    DisplayProduct --> IncreaseQuantity: Tap + button
    IncreaseQuantity --> UpdatePrice: quantity++
    UpdatePrice --> DisplayProduct

    DisplayProduct --> DecreaseQuantity: Tap - button (min=1)
    DecreaseQuantity --> UpdatePrice: quantity--

    DisplayProduct --> CheckAgeRestriction: Tap ADD TO CART

    state CheckAgeRestriction <<choice>>
    CheckAgeRestriction --> LaunchIDScan: ageRestriction > 0
    CheckAgeRestriction --> AddToCart: No restriction

    LaunchIDScan --> AwaitResult: Launch IdScanActivity

    state AwaitResult <<fork>>
    AwaitResult --> VerificationSuccess: RESULT_OK
    AwaitResult --> VerificationFailed: RESULT_CANCELLED

    VerificationSuccess --> ShowSuccessToast
    ShowSuccessToast --> AddToCart

    VerificationFailed --> ShowFailureToast
    ShowFailureToast --> DisplayProduct

    AddToCart --> ShowCartToast: "Added to cart!"
    ShowCartToast --> DisplayProduct: Stay on product detail

    DisplayProduct --> ReturnToGrid: Tap back button
    ReturnToGrid --> [*]: finish()

    note right of CheckAgeRestriction
        Age 21: ZYN products, vapes
        Age 18: Lighters, rolling papers
        None: Energy drinks, snacks
    end note
```

### 4. IdScanActivity State Machine

```mermaid
stateDiagram-v2
    [*] --> Initialize
    Initialize --> BindHardwareService: ServiceConnection
    BindHardwareService --> IDLE: Show "PLACE ID ON SCANNER"

    IDLE --> SCANNING: Auto-start after 1 second

    state SCANNING {
        [*] --> AnimateScanLine
        AnimateScanLine --> UpdateProgress: 2% every 50ms
        UpdateProgress --> ListenForScan
        ListenForScan --> AnimateScanLine: Loop every 2 seconds
    }

    SCANNING --> ParseAAMVA: Hardware scan detected
    ParseAAMVA --> ExtractDOB: Read FTDI USB serial (9600 baud)
    ExtractDOB --> CalculateAge: Parse 8-digit YYYYMMDD

    state CalculateAge <<choice>>
    CalculateAge --> AgeVerified: age >= requiredAge
    CalculateAge --> AgeFailed: age < requiredAge

    AgeVerified --> CompleteProgress: Progress to 100%
    CompleteProgress --> SUCCESS

    state SUCCESS {
        [*] --> ShowSuccessUI
        ShowSuccessUI --> Wait3Seconds
        Wait3Seconds --> SetResultOK
    }

    SUCCESS --> [*]: finish()

    AgeFailed --> ShowErrorToast: "You must be X or older"
    ShowErrorToast --> WaitAndReset: 2 second delay
    WaitAndReset --> SCANNING: Reset scan

    note right of ParseAAMVA
        AAMVA PDF417 Format:
        - @ANSI header
        - DBB = DOB (YYYYMMDD)
        - DBA = Expiration
        - Extract using regex
    end note
```

### 5. ScreensaverActivity State Machine

```mermaid
stateDiagram-v2
    [*] --> Initialize
    Initialize --> SetupDualVideo: Create 2 VideoView instances
    SetupDualVideo --> Rotate180: Apply 180° rotation
    Rotate180 --> StartVideoLoop

    state StartVideoLoop {
        [*] --> PlayVideo1
        PlayVideo1 --> FadeToVideo2: 1.5s cross-fade
        FadeToVideo2 --> PlayVideo2
        PlayVideo2 --> FadeToVideo1: 1.5s cross-fade
        FadeToVideo1 --> PlayVideo1
    }

    StartVideoLoop --> DetectTouch: Listen for touch events
    DetectTouch --> ExitScreensaver: Any touch detected
    ExitScreensaver --> [*]: finish() → ProductGridActivity

    note right of StartVideoLoop
        Seamless looping with interpolated fade
        Videos from: /storage/emulated/0/Movies/ZootBox/
    end note
```

---

## Hardware Integration Flow

```mermaid
sequenceDiagram
    participant MA as MainActivity
    participant HS as HardwareService
    participant ISM as IdScannerManager
    participant NPM as NayaxPaymentManager
    participant ISA as IdScanActivity
    participant PDA as ProductDetailActivity

    MA->>HS: startForegroundService()
    activate HS
    HS->>HS: Create notification channel
    HS->>HS: Start foreground with notification
    HS->>ISM: Initialize USB FTDI (VID=0x0403, PID=0x6001)
    HS->>NPM: Initialize USB CDC-ACM (VID=0x26f1, PID=0x5650)
    Note over HS: Service runs in background<br/>throughout app lifecycle

    PDA->>ISA: Launch for age verification
    ISA->>HS: bindService()
    HS-->>ISA: onServiceConnected(binder)
    ISA->>ISM: Get idScannerManager reference

    ISA->>ISA: setState(SCANNING)
    ISA->>ISM: Start listening to scanResult flow

    Note over ISM: User places ID on scanner<br/>Hardware sends data via USB

    ISM->>ISM: Read USB serial (4096 byte buffer, 9600 baud)
    ISM->>ISM: Parse AAMVA format
    ISM->>ISM: Extract DOB from DBB field
    ISM->>ISM: Validate DOB (1900-2100)
    ISM->>ISM: Calculate age from DOB
    ISM->>ISM: Compare age >= requiredAge
    ISM-->>ISA: Emit scanResult (StateFlow)

    alt Age Verified
        ISA->>ISA: setState(SUCCESS)
        ISA->>ISA: Wait 3 seconds
        ISA->>PDA: setResult(RESULT_OK)
        PDA->>PDA: Show success toast
        PDA->>PDA: Add to cart
    else Age Not Verified
        ISA->>ISA: Show error toast
        ISA->>ISA: Reset to SCANNING
    end

    ISA->>HS: unbindService()
    deactivate HS

    Note over NPM: Nayax Payment Flow (Pre-Selection Mode)<br/>Price sent before card tap
```

---

## Nayax Payment State Machine (Pre-Selection Mode)

```mermaid
stateDiagram-v2
    [*] --> INIT: App startup
    INIT --> IDLE: Marshall SDK initialized
    IDLE --> READER_ENABLED: reader_always_on = true

    state READER_ENABLED {
        [*] --> WaitingForVendRequest
        WaitingForVendRequest --> VendRequestReceived: App sends vend_request(price)
        note right of VendRequestReceived: Price displays on VPOS<br/>"$X.XX - Tap Card"
    }

    READER_ENABLED --> VEND_PROCESS: Card tapped (session_begin)<br/>with pending vend_request

    state VEND_PROCESS {
        [*] --> ProcessingPayment
        ProcessingPayment --> VendApproved: vend_approved event
        ProcessingPayment --> VendDenied: vend_denied event
    }

    VEND_PROCESS --> WAIT_END_SESSION: vend_approved
    WAIT_END_SESSION --> IDLE: session_end

    VEND_PROCESS --> READER_ENABLED: vend_denied (retry)

    note right of READER_ENABLED
        Pre-Selection Mode (always_idle = true):
        1. App sends price FIRST
        2. VPOS shows "$X.XX - Tap Card"
        3. Customer taps card
        4. Payment processes

        This differs from Post-Selection:
        1. Customer taps card FIRST
        2. App sends price
        3. Payment processes
    end note
```

### Pre-Selection vs Post-Selection Flow Comparison

| Aspect | Pre-Selection (ZootBox) | Post-Selection |
|--------|------------------------|----------------|
| **Config** | `always_idle = true` | `always_idle = false` |
| **Sequence** | Price → Card Tap → Payment | Card Tap → Price → Payment |
| **vend_request state** | READER_ENABLED (state 2) | WAIT_VEND_REQUEST (state 3) |
| **Display** | Shows price immediately | Shows "Tap Card" until price sent |
| **Use Case** | Known price before checkout | Dynamic pricing after card tap |

### vmc_vend_t State Machine Reference

| State | ID | Description |
|-------|-----|-------------|
| INIT | 0 | SDK initializing |
| IDLE | 1 | Ready, reader disabled |
| READER_ENABLED | 2 | Card reader active, showing "Tap Card" |
| WAIT_VEND_REQUEST | 3 | Card tapped, waiting for price (Post-Selection only) |
| VEND_PROCESS | 4 | Processing payment |
| WAIT_END_SESSION | 5 | Transaction complete, waiting for cleanup |
| DISABLED | 6 | Reader disabled |

---

## Backend API & Inventory Management Flow

```mermaid
sequenceDiagram
    participant Operator as Operator (Browser)
    participant Portal as Portal Frontend<br/>(localhost:3000)
    participant Backend as Backend API Server<br/>(localhost:8080)
    participant DB as SQLite Database<br/>(zootbox.db)
    participant Android as Android App<br/>(Tablet)
    participant Motor as Motor Hardware<br/>(10 Motors)

    Note over Operator,Motor: INITIAL SETUP: Operator Restocks Machine

    Operator->>Portal: Opens inventory portal
    Portal->>Portal: Load from localStorage
    Portal->>Backend: GET /health
    Backend-->>Portal: {status: "healthy", database_ok: true}

    Portal->>Backend: GET /api/coils
    Backend->>DB: SELECT * FROM coils
    DB-->>Backend: [{id: "A1", inventory: 0, ...}, ...]
    Backend-->>Portal: Return coils array
    Portal->>Portal: Render grid (10 rows)

    Operator->>Portal: Click Row 1, set inventory = 10
    Portal->>Backend: PUT /api/coils/A1 {inventory: 10}
    Backend->>DB: UPDATE coils SET inventory = 10 WHERE id = 'A1'
    DB-->>Backend: Success
    Backend-->>Portal: {id: "A1", inventory: 10, ...}
    Portal->>Portal: Update display: "Row 1: 10/10 units"

    Note over Operator,Motor: CUSTOMER PURCHASE FLOW

    Note over Android,Motor: Customer selects product from Android app

    Android->>Motor: Send vend command via JSON-RPC<br/>{col: "1", row: "1", method: "requestProductVend"}
    Motor->>Motor: Spin coil (dispense product)
    Motor-->>Android: Vend result (success/jam/fail)

    alt Successful Vend
        Android->>Backend: POST /api/transactions<br/>{coil_id: "A1", status: "success", timestamp: "..."}
        Backend->>DB: INSERT INTO transactions (coil_id, status, ...)
        Backend->>DB: UPDATE coils SET inventory = inventory - 1<br/>WHERE id = 'A1'
        DB-->>Backend: inventory = 9
        Backend-->>Android: {success: true}
    else Motor Jam
        Android->>Backend: POST /api/transactions<br/>{coil_id: "A1", status: "jam", ...}
        Backend->>DB: INSERT INTO jam_events (coil_id, status = 'open')
        Note over Backend,DB: Inventory NOT decremented
        Backend-->>Android: {success: true}
    else Vend Failed
        Android->>Backend: POST /api/transactions<br/>{coil_id: "A1", status: "failed", ...}
        Backend->>DB: INSERT INTO transactions (coil_id, status = 'failed')
        Note over Backend,DB: Inventory NOT decremented
        Backend-->>Android: {success: true}
    end

    Note over Operator,Motor: OPERATOR VIEWS UPDATED INVENTORY

    Portal->>Portal: Auto-refresh timer (30-60s)
    Portal->>Backend: GET /api/coils
    Backend->>DB: SELECT * FROM coils
    DB-->>Backend: [{id: "A1", inventory: 9, ...}, ...]
    Backend-->>Portal: Return updated coils
    Portal->>Portal: Update display: "Row 1: 9/10 units"

    Note over Operator,Portal: Operator sees updated count

    Note over Operator,Motor: MANUAL SYNC

    Operator->>Portal: Click "Sync Machine" button
    Portal->>Backend: GET /api/coils
    Backend->>DB: SELECT * FROM coils
    DB-->>Backend: Current inventory data
    Backend-->>Portal: Return coils
    Portal->>Portal: Refresh grid immediately
```

### Backend API Endpoints

```mermaid
graph TB
    subgraph "Backend API Server (Go) - Port 8080"
        Health["/health<br/>GET - Health check"]
        Metrics["/metrics<br/>GET - Prometheus metrics"]

        subgraph "Coil Management"
            GetCoils["/api/coils<br/>GET - Fetch all coils"]
            UpdateCoil["/api/coils/:id<br/>PUT - Update coil inventory"]
        end

        subgraph "Transaction Tracking"
            GetTxns["/api/transactions<br/>GET - Transaction history"]
            CreateTxn["/api/transactions<br/>POST - Record vend attempt"]
        end

        subgraph "Jam Management"
            GetJams["/api/jam-events<br/>GET - List jam events"]
            CreateJam["/api/jam-events<br/>POST - Report jam"]
            ResolveJam["/api/jam-events/:id<br/>PUT - Mark resolved"]
        end

        subgraph "Product Links"
            GetLinks["/api/product-links<br/>GET - Product assignments"]
            CreateLink["/api/product-links<br/>POST - Create link"]
            DeleteLink["/api/product-links/:id<br/>DELETE - Remove link"]
        end
    end

    Portal[Portal Frontend] -->|HTTP/JSON| GetCoils
    Portal -->|HTTP/JSON| UpdateCoil
    Portal -->|HTTP/JSON| GetJams
    Portal -->|HTTP/JSON| GetLinks
    Portal -->|HTTP/JSON| Health

    Android[Android App] -->|HTTP/JSON| CreateTxn
    Android -->|HTTP/JSON| CreateJam
    Android -->|HTTP/JSON| GetCoils

    GetCoils --> DB[(SQLite DB)]
    UpdateCoil --> DB
    CreateTxn --> DB
    GetJams --> DB
    CreateJam --> DB

    style Portal fill:#e1f5ff
    style Android fill:#c8e6c9
    style DB fill:#fff9c4
```

### Inventory State Machine

```mermaid
stateDiagram-v2
    [*] --> Empty: Initial state
    Empty --> Restocking: Operator sets inventory
    Restocking --> Stocked: inventory > 0

    Stocked --> LowStock: inventory <= 2 (after vends)
    Stocked --> Empty: inventory = 0 (after vend)

    LowStock --> Empty: Final vend
    LowStock --> Stocked: Operator restocks

    Empty --> Restocking: Operator restocks

    Stocked --> Jammed: Motor jam detected
    LowStock --> Jammed: Motor jam detected
    Empty --> Jammed: Motor jam detected

    Jammed --> Stocked: Jam resolved, restocked
    Jammed --> LowStock: Jam resolved
    Jammed --> Empty: Jam resolved, empty

    note right of Empty
        Status: EMPTY
        Color: Red
        Inventory: 0
    end note

    note right of LowStock
        Status: LOW STOCK
        Color: Orange
        Inventory: 1-2
    end note

    note right of Stocked
        Status: ACTIVE
        Color: Blue/Green
        Inventory: 3-10
    end note

    note right of Jammed
        Status: JAMMED
        Color: Red
        Motor: Not operational
        Needs manual fix
    end note
```

### Database Schema

```mermaid
erDiagram
    COILS ||--o{ TRANSACTIONS : generates
    COILS ||--o{ JAM_EVENTS : experiences
    COILS ||--o{ PRODUCT_LINKS : assigned_to

    COILS {
        string id PK "A1, B1, C1, ..., J1"
        int inventory "0-10 units"
        string status "active/jammed"
        timestamp created_at
        timestamp updated_at
    }

    TRANSACTIONS {
        int id PK
        string coil_id FK
        string status "success/jam/failed"
        decimal amount "Payment amount"
        timestamp created_at
    }

    JAM_EVENTS {
        int id PK
        string coil_id FK
        string status "open/resolved"
        string notes "Optional description"
        timestamp created_at
        timestamp resolved_at
    }

    PRODUCT_LINKS {
        int id PK
        string sku "Product identifier"
        json coil_ids "Array: ['A1', 'B1']"
        string strategy "sync/rotate"
        timestamp created_at
    }
```

### Data Persistence & Caching

```mermaid
flowchart TB
    subgraph Browser["Browser (Portal)"]
        UI[User Interface]
        LocalStorage[localStorage<br/>Machine configs]
        SessionCache[Session Cache<br/>Last fetched inventory]
    end

    subgraph Backend["Backend Server"]
        API[REST API Layer]
        Logic[Business Logic]
    end

    subgraph Storage["Persistent Storage"]
        SQLite[(SQLite Database<br/>zootbox.db)]
    end

    UI -->|Read/Write| LocalStorage
    UI -->|Temp cache| SessionCache

    UI -->|HTTP Request| API
    API -->|Response| UI

    API --> Logic
    Logic -->|SQL Queries| SQLite
    SQLite -->|Results| Logic

    LocalStorage -.->|Survives browser restart| LocalStorage
    SessionCache -.->|Cleared on page reload| SessionCache
    SQLite -.->|Survives backend restart| SQLite

    style LocalStorage fill:#fff9c4
    style SessionCache fill:#ffccbc
    style SQLite fill:#c8e6c9
```

### Network Communication Flow

```mermaid
graph LR
    subgraph Client["Client Side"]
        Browser[Browser<br/>Portal UI]
        Fetch[Fetch API<br/>+ Timeout]
        Cache[Response Cache]
    end

    subgraph Network["Network Layer"]
        HTTP[HTTP/HTTPS<br/>JSON Payload]
        CORS[CORS Headers]
    end

    subgraph Server["Server Side"]
        Router[Chi Router<br/>+ Middleware]
        Handler[Request Handler]
        Response[JSON Response]
    end

    Browser -->|apiClient.get/post/put| Fetch
    Fetch -->|10s timeout| HTTP
    HTTP --> CORS
    CORS --> Router
    Router --> Handler
    Handler --> Response
    Response --> HTTP
    HTTP -->|Parse JSON| Cache
    Cache --> Browser

    style Browser fill:#e1f5ff
    style Fetch fill:#bbdefb
    style HTTP fill:#fff9c4
    style Router fill:#c8e6c9
    style Handler fill:#a5d6a7
```

---

## Data Flow Diagram

```mermaid
flowchart LR
    subgraph UserInput["User Input Layer"]
        UI_Touch[Touch Events]
        UI_Buttons[Button Clicks]
        UI_Navigation[Navigation Actions]
    end

    subgraph Activities["Activity Layer"]
        ACT_Main[MainActivity<br/>Color: 8 schemes]
        ACT_Grid[ProductGridActivity<br/>Filter: Category]
        ACT_Detail[ProductDetailActivity<br/>Qty: 1-99]
        ACT_Scan[IdScanActivity<br/>Age: 18/21]
        ACT_Saver[ScreensaverActivity<br/>Timeout: 30s]
    end

    subgraph DataModels["Data Models"]
        DM_Product[(Product Catalog<br/>11 Items)]
        DM_Intent[Intent Extras<br/>category_name<br/>product data<br/>requiredAge]
        DM_State[UI State<br/>quantity<br/>totalPrice<br/>scanState]
    end

    subgraph Hardware["Hardware Layer"]
        HW_Service[HardwareService<br/>Foreground]
        HW_IDScan[IdScannerManager<br/>USB FTDI]
        HW_Payment[NayaxPaymentManager<br/>USB CDC-ACM]
    end

    subgraph Storage["Storage Layer"]
        ST_Video[Video Files<br/>/storage/emulated/0/Movies/ZootBox/]
        ST_Drawable[Drawable Resources<br/>Product images<br/>Gradients]
    end

    UI_Touch --> ACT_Grid
    UI_Buttons --> ACT_Main
    UI_Navigation --> Activities

    ACT_Main -->|category_name| DM_Intent
    DM_Intent -->|filter| ACT_Grid

    ACT_Grid -->|product data| DM_Intent
    DM_Intent --> ACT_Detail

    ACT_Detail -->|requiredAge| DM_Intent
    DM_Intent --> ACT_Scan

    DM_Product -->|static list| ACT_Grid
    DM_Product -->|price, name, media| ACT_Detail

    ACT_Detail --> DM_State
    DM_State -->|calculate| ACT_Detail

    ACT_Scan --> HW_Service
    HW_Service --> HW_IDScan
    HW_IDScan -->|scanResult| ACT_Scan

    ST_Video --> ACT_Detail
    ST_Video --> ACT_Saver
    ST_Drawable --> ACT_Detail
    ST_Drawable --> ACT_Grid

    ACT_Grid -->|30s timeout| ACT_Saver
    ACT_Saver -->|touch| ACT_Grid

    style UserInput fill:#bbdefb
    style Activities fill:#c8e6c9
    style DataModels fill:#fff9c4
    style Hardware fill:#ffccbc
    style Storage fill:#f8bbd0
```

---

## Product Catalog Data Structure

```mermaid
classDiagram
    class Product {
        +String id
        +String name
        +String row
        +String col
        +Int imageRes
        +Int backgroundRes
        +Boolean isDigital
        +Double price
        +Int? ageRestriction
        +Float scaleX
        +Float scaleY
        +String? videoFileName
        +String category
    }

    class ProductGridActivity {
        -List~Product~ allProducts
        -List~Product~ filteredProducts
        -String categoryName
        +filterByCategory()
        +setupRecyclerView()
    }

    class ProductDetailActivity {
        -Product product
        -Int quantity
        -Double totalPrice
        +updateQuantityDisplay()
        +calculateTotalPrice()
        +handleAddToCart()
    }

    class ProductAdapter {
        -List~Product~ products
        +onBindViewHolder()
        +onCreateViewHolder()
    }

    ProductGridActivity "1" --> "*" Product: contains
    ProductGridActivity "1" --> "1" ProductAdapter: uses
    ProductAdapter "1" --> "*" Product: binds
    ProductDetailActivity "1" --> "1" Product: displays

    note for Product "11 Products Total:
    • ZYN CITRUS ($8.99, 21+)
    • ZOOT VAPE X ($29.99, 21+)
    • LIGHTER GOLD ($2.99, 18+)
    • ROLLING PAPERS ($3.99, 18+)
    • RED BULL 12OZ ($4.99)
    • ENERGY SHOT ($3.49)
    • GUM MINT ($1.99)
    • WATER 500ML ($2.49)
    • CONDOM PACK ($5.99)
    • NIGHT OWL CAM ($15.99)
    • Donate ($5.00, digital)"
```

---

## Complete User Journey Timeline

```mermaid
gantt
    title ZootBox User Journey: From Launch to Purchase
    dateFormat X
    axisFormat %S

    section App Launch
    MainActivity loads           :milestone, 0, 0
    Color scheme applied         :active, 0, 1
    Sections animate             :active, 1, 2
    HardwareService starts       :active, 2, 3
    User sees categories         :milestone, 3, 3

    section Category Selection
    User browses sections        :active, 3, 8
    User taps "ZYNS"            :milestone, 8, 8

    section Product Grid
    ProductGridActivity loads    :active, 8, 9
    Filter by category           :active, 9, 10
    Display 2 products           :active, 10, 11
    User browses grid            :active, 11, 18
    User taps "ZYN CITRUS"      :milestone, 18, 18

    section Product Detail
    ProductDetailActivity loads  :active, 18, 19
    Load product video           :active, 19, 20
    Display product info         :active, 20, 21
    User adjusts quantity        :active, 21, 26
    User taps ADD TO CART        :milestone, 26, 26

    section Age Verification
    IdScanActivity launches      :active, 26, 27
    IDLE state (1s)              :active, 27, 28
    SCANNING state               :active, 28, 33
    User scans ID                :milestone, 30, 30
    Parse AAMVA data             :active, 30, 31
    Calculate age                :active, 31, 32
    Age verified                 :milestone, 32, 32
    SUCCESS state (3s)           :active, 32, 35

    section Purchase Complete
    Return to ProductDetail      :active, 35, 36
    Show success toast           :active, 36, 37
    Add to cart                  :milestone, 37, 37
    Activity finishes            :crit, 37, 38

    section Screensaver Path
    30s idle on grid             :done, 11, 41
    Screensaver activates        :done, 41, 42
    User touches screen          :done, 45, 45
    Return to grid               :done, 45, 46
```

---

## Key Metrics & Configuration

### Timing Configuration
| Event | Duration | Configurable |
|-------|----------|--------------|
| Section animation stagger | 600ms per section | Yes - MainActivity |
| Idle timeout to screensaver | 30 seconds | Yes - ProductGridActivity |
| ID scan IDLE state | 1 second | Yes - IdScanActivity |
| Scanning line animation | 2 seconds per loop | Yes - IdScanActivity |
| Success screen display | 3 seconds | Yes - IdScanActivity |
| Screensaver fade duration | 1.5 seconds | Yes - ScreensaverActivity |
| Progress increment | 2% per 50ms | Yes - IdScanActivity |

### Hardware Configuration
| Device | VID | PID | Protocol | Baud Rate | Driver |
|--------|-----|-----|----------|-----------|--------|
| E-Seek M260 ID Scanner | 0x0403 | 0x6001 | USB Serial | **9600** | FTDI |
| Nayax VPOS Touch Payment | 0x0403 | 0x6015 | USB Serial (FTDI) | **115200** | FtdiSerialDriver |

**Note:** The Nayax VPOS has two USB interfaces. The FTDI interface (0403:6015) must be used - the CDC-ACM interface (26f1:5650) does NOT work.

### Marshall SDK Configuration (NayaxPaymentManager.kt)
| Parameter | Value | Purpose |
|-----------|-------|---------|
| `reader_always_on` | `true` | Keep card reader enabled, showing "Tap Card" |
| `always_idle` | `true` | Enable Pre-Selection mode (app sends price before card tap) |
| `multi_vend_support` | `false` | Single item transactions |
| `vend_response_timeout` | `30000` | 30 second payment timeout |

### Product Categories
| Category | Product Count | Age Restricted |
|----------|---------------|----------------|
| ZyNS | 2 | 1 @ 21+ |
| VAPES | 1 | 1 @ 21+ |
| CIGERATES | 2 | 2 @ 18+ |
| ZOOTBOX LEGENDARY LOOT | 6 | 0 |

### UI Grid Configuration
- **ProductGridActivity**: 2 columns (GridLayoutManager)
- **Total Products**: 11
- **Filtering**: Category-based exact match
- **Image Scaling**: Per-product scaleX/scaleY values

---

## Notes

1. **Hardware Fully Integrated**: Both ID Scanner (E-Seek M260) and Nayax VPOS Touch payment reader are fully functional via USB.

2. **Payment Integration Complete**: NayaxPaymentManager uses the Marshall SDK to communicate with the Nayax VPOS Touch. The reader shows "Tap Card" and is ready for contactless payments. The `vmc_vend_t.handleMessage()` method was manually reconstructed after JADX decompilation failure.

3. **No Cart Persistence**: "Add to cart" operations show success messages but don't persist to a database or shared cart state.

4. **Video Storage**: Videos must be manually placed in `/storage/emulated/0/Movies/ZootBox/` directory. The app doesn't bundle videos in assets/raw.

5. **Age Verification**: AAMVA parsing extracts DOB from DAA field using 8-digit YYYYMMDD format. Age calculation accounts for current year, month, and day.

6. **Color Schemes**: MainActivity supports 8 predefined color schemes that cycle with the circular toggle button. Each scheme applies colors to all 4 sections.

7. **Screensaver Activation**: Only triggered from ProductGridActivity after 30s of inactivity. Other activities don't implement idle detection.

8. **Back Navigation**: All activities use standard back stack behavior. No custom back handling beyond standard `finish()` calls.

9. **Critical Baud Rate Fix (Jan 2026)**: ID Scanner and Nayax payment terminal now use separate baud rate constants. Previously, when Nayax integration was added, both devices incorrectly shared `SERIAL_BAUD_RATE = 115200`, causing the ID scanner to receive binary garbage instead of ASCII AAMVA text. Now fixed with `ID_SCANNER_BAUD_RATE = 9600` and `NAYAX_BAUD_RATE = 115200`.

10. **Cart Checkout Flow**: CartActivity now checks for age-restricted items before payment. If the cart contains any products requiring age verification, it launches IdScanActivity with the highest required age. After successful verification, payment proceeds normally.

11. **Nayax Payment Flow - Pre-Selection Mode with Motor Dispensing (Jan 2026)**: The Nayax VPOS Touch integration uses the Marshall SDK extracted from DMVI's APK with **Pre-Selection flow** (app sends price BEFORE card tap) and **full motor dispensing integration**. Key configuration: `reader_always_on = true` keeps the card reader enabled showing "Tap Card", and `always_idle = true` enables Pre-Selection mode. The complete end-to-end flow is: Product Selection → ID Scan → Payment Initiation (vend_request) → Card Tap (session_begin) → Payment Approval (vend_approved) → **Motor Dispensing** → Transaction Settlement. Critical bug fixes: (1) `vmc_vend_t.handleMessage()` vend_approved handler now fires callback in both state 2 (READER_ENABLED, Pre-Selection) and state 4 (VEND_PROCESS, Post-Selection); (2) `ProductDetailActivity` refactored to extract `dispenseProducts()` function, eliminating double payment initiation in "Add to Cart" flow; (3) Motors now trigger immediately after payment approval via `MotorControlManager.vendMotor()` which sends JSON-RPC commands to DMVI service on port 57482.

12. **USB Device Priority**: When detecting USB devices, the app prefers the FTDI interface (VID=0x0403, PID=0x6015) over the CDC-ACM interface (VID=0x26f1, PID=0x5650) for Nayax. Both interfaces appear on the same physical device, but only FTDI works with Marshall protocol.

13. **Immediate Inventory Sync (Jan 2026)**: The `InventoryRepository` now triggers `BackgroundSyncService.syncNow()` on every inventory change (`updateInventory()`, `resetAllInventory()`). This ensures the portal sees changes within seconds instead of waiting up to 1 hour. The sync uses `NetworkType.NOT_REQUIRED` constraint since it communicates with localhost. A `network_security_config.xml` was added to allow cleartext HTTP to localhost on Android 9+.

14. **Backend HTTP_HOST Configuration (Jan 2026)**: The Go backend must start with `HTTP_HOST=0.0.0.0` (not `127.0.0.1`) for the portal to connect via Tailscale VPN. The backend database has exactly 10 coils (A1-J1) matching the 10 motors in the vending machine.
