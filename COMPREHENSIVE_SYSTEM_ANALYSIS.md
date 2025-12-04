# COMPREHENSIVE SYSTEM ANALYSIS
## DMVI Android Vending Machine - Complete Hardware, Bootloader & Software Architecture

**Analysis Date:** November 21, 2025
**Device IP:** 192.168.7.246:43039
**Data Sources:** DMVI_Data_20251120_150303, DMVI_Dump_20251120_154254
**Analysis Type:** Forensic System Dump Analysis

---

## EXECUTIVE SUMMARY

This document provides a complete technical analysis of a DMVI (Digital Media Vending International) wall-mounted vending machine Android tablet and its control systems. **The mystery of why DMVI apps persist after factory reset has been definitively solved:** they are installed as **system apps in the read-only `/system` partition**, not as user apps, making them survive all factory resets.

### Critical Findings

1. **System is fully rooted** - userdebug build with adb root enabled
2. **Security is disabled** - SELinux in permissive mode, verified boot "orange" state
3. **Bootloader is unlocked** - ro.boot.flash.locked = 0
4. **DMVI apps are OEM firmware** - installed in /system partition, not /data
5. **Three-app architecture** - Main UI, Hardware Service, Watchdog
6. **Cloud-synced via Astarte IoT** - Product data, config, firmware updates
7. **Custom serial protocols** - Proprietary motor control and MDB payment

---

## 1. HARDWARE SPECIFICATIONS

### 1.1 Main Controller

| Component | Specification | Details |
|-----------|--------------|---------|
| **Model** | SECO SYS-C31-DMV-01-I0 | Industrial embedded computer |
| **Manufacturer** | SECO Spa | Arezzo, Italy |
| **Serial Number** | 221141736 | Board serial |
| **Device Serial** | b0535a1f9f0f6ce0 | Android serial number |
| **Machine Serial** | 00:51:64:84:35:02 | DMVI machine identifier |

### 1.2 Processor & Memory

| Component | Specification |
|-----------|--------------|
| **SoC** | Rockchip RK3399K |
| **CPU Architecture** | ARMv8-A (64-bit) |
| **CPU Cores** | 6 cores (big.LITTLE) |
| - Big Cores | 2x ARM Cortex-A72 @ 1.8GHz (0xd08) |
| - Little Cores | 4x ARM Cortex-A53 @ 1.4GHz (0xd03) |
| **CPU Features** | fp, asimd, evtstrm, aes, pmull, sha1, sha2, crc32, cpuid |
| **RAM** | 2GB LPDDR4 |
| **Storage** | 16GB eMMC (fe330000.sdhci) |
| **GPU** | ARM Mali-T860 MP4 |

### 1.3 Display

| Component | Specification |
|-----------|--------------|
| **Model** | TYALUX TYL-H320KM-F |
| **Size** | 32 inches |
| **Type** | Capacitive touchscreen |
| **Controller** | Goodix 5-0020 (I2C) |
| **Interface** | HDMI + USB (touch controller) |
| **Resolution** | Not specified in dumps |
| **Density** | 160 dpi (mdpi) |

### 1.4 Connectivity

| Interface | Hardware | Status | MAC/ID |
|-----------|----------|--------|--------|
| **WiFi** | AzureWave AW-CM276 | Active | ec:2e:98:d0:9b:3f |
| **Bluetooth** | AW-CM276 (integrated) | Available | - |
| **Cellular** | Quectel EG25-G LTE | Active | IMEI present |
| - Operator | T-Mobile (310260) | Connected | - |
| - Baseband | EG25GGCR07A02M1G | - | - |
| - RIL Driver | V3.3.40 | - | - |
| **Ethernet 1** | eth0 | NO-CARRIER | 00:c0:08:a6:4a:1d |
| **Ethernet 2** | eth1 | NO-CARRIER | 00:c0:08:a6:4a:1e |
| **USB** | Rockchip FUSB302 | Active | Type-C controller |

### 1.5 Peripheral Hardware

| Device | Interface | Purpose | Current Status |
|--------|-----------|---------|----------------|
| **Motor Controller** | /dev/ttyS0 (UART) | 10-coil dispenser | Active (DMVI service) |
| **Payment Terminal** | /dev/ttyUSB1 (FTDI) | Nayax MDB | Available |
| **ID Scanner** | /dev/ttyUSB2 (FTDI) | Age verification | Available |
| **LTE Modem Data** | /dev/ttyUSB3 | 4G data connection | Active |
| **LTE Modem AT** | /dev/ttyUSB4 | AT command interface | Active |
| **Touchscreen** | I2C (Goodix) | User input | Active |
| **HDMI** | dw-hdmi (Rockchip) | Display output | Active |

---

## 2. BOOTLOADER & FIRMWARE ANALYSIS

### 2.1 Bootloader Configuration

**Type:** Rockchip Proprietary Bootloader (NOT U-Boot)

| Property | Value | Implication |
|----------|-------|-------------|
| `ro.bootloader` | unknown | No U-Boot branding visible |
| `ro.boot.hardware` | rk30board | Rockchip RK3xxx platform |
| `ro.boot.boot_devices` | fe330000.sdhci | eMMC boot device |
| `ro.boot.storagemedia` | emmc | Internal flash storage |
| `ro.boot.console` | ttyFIQ0 | FIQ-based debug console |
| `ro.boot.selinux` | **permissive** | ⚠️ Security disabled |
| `ro.boot.verifiedbootstate` | **orange** | ⚠️ Unlocked/dev mode |
| `ro.boot.veritymode` | enforcing | dm-verity enabled (but bypassed) |
| `ro.boot.flash.locked` | **0** | ⚠️ Bootloader unlocked |
| `ro.boot.dynamic_partitions` | true | A/B OTA updates supported |

### 2.2 Boot Sequence

Based on kernel logs and system properties:

```
1. Rockchip Bootloader (Stage 1)
   └─> Loads from eMMC partition table
   └─> Initializes DRAM (LPDDR4)
   └─> Loads kernel from boot partition

2. Linux Kernel (v4.19.206-android11)
   └─> Initializes SoC peripherals
   └─> Mounts dynamic partitions (dm-verity overlays)
   └─> Starts init process (PID 1)

3. Android Init System
   └─> Parses init.*.rc files
   └─> Starts system services:
       ├─> servicemanager (Binder IPC)
       ├─> hwservicemanager (HIDL)
       ├─> surfaceflinger (Graphics)
       ├─> audioserver (Audio HAL)
       └─> su_daemon (Root daemon - ACTIVE!)

4. SECO Custom Init Scripts
   ├─> secoInit (stopped - already ran)
   └─> secoBootComplete (stopped - already ran)

5. Android System Server
   └─> Starts Android framework services
   └─> Launches boot animation (bootanim)
   └─> Starts Zygote (app launcher)

6. DMVI Applications (Auto-start)
   ├─> com.digitalmediavending.watchdog (PID 1359)
   │   └─> Monitors main app and hardware service
   ├─> com.digitalmediavending (PID 1770)
   │   └─> Main UI and cloud sync
   └─> com.digitalmediavending.hardware (PID 19863)
       └─> Serial port communication (motor, payment)
```

### 2.3 Firmware Build Information

| Property | Value |
|----------|-------|
| **Build ID** | RQ3A.210705.001 |
| **Build Date** | Fri Aug 26 13:49:12 UTC 2022 |
| **Build User** | root |
| **Build Host** | runner-swjnymdb-project-1972-concurrent-0 |
| **Build Type** | **userdebug** (NOT production!) |
| **Build Flavor** | c31_rk3399_Android11-userdebug |
| **Build Tags** | <v2-02-04>,release-keys |
| **Android Version** | 11 (API 30) |
| **Security Patch** | 2021-08-05 (OUTDATED!) |
| **Manufacturer** | seco |
| **Product** | c31_rk3399_Android11 |
| **Board** | rk30sdk |

### 2.4 OTA Update Configuration

| Property | Value | Notes |
|----------|-------|-------|
| `ro.product.ota.host` | **192.168.1.1:8888** | ⚠️ Local network OTA server! |
| Dynamic Partitions | Enabled | Supports A/B seamless updates |
| Verified Boot | Orange state | Updates not verified |

**SECURITY IMPLICATION:** OTA server on local network means anyone on the network can potentially push firmware updates without authentication.

---

## 3. STORAGE & PARTITION ARCHITECTURE

### 3.1 Partition Layout (16GB eMMC)

| Partition | Block Device | Size | Mount Point | Type | Usage |
|-----------|--------------|------|-------------|------|-------|
| **Boot** | mmcblk2p? | ~32MB | - | Raw | Kernel image |
| **Recovery** | mmcblk2p? | ~32MB | - | Raw | Recovery image |
| **Metadata** | mmcblk2p11 | 11MB | /metadata | ext4 | 2% used |
| **System** | dm-0 | 1.0GB | / | f2fs | **100% FULL** (Read-only) |
| **Scratch** | dm-5 | 600MB | /mnt/scratch | overlay | 2% used |
| **Cache** | mmcblk2p10 | 740MB | /cache | ext4 | 1% used |
| **Data** | dm-6 | 10GB | /data | f2fs | 6% used (574MB) |

### 3.2 Overlay Mount System

The system uses **overlay mounts** for dynamic updates:

```
/system    → overlay on dm-5 (backed by dm-0)
/vendor    → overlay on dm-5 (backed by dm-2)
/odm       → overlay on dm-5 (backed by dm-4)
/product   → overlay on dm-5 (backed by dm-3)
/system_ext→ overlay on dm-5 (backed by dm-1)
```

**Explanation:** This allows A/B OTA updates where the new system can be written to an alternate partition while the device is running, then activated on next boot.

### 3.3 Why DMVI Apps Survive Factory Reset

**CRITICAL FINDING:**

```
Factory Reset Operation:
1. Wipes /data partition (dm-6) → All user apps deleted
2. Wipes /cache partition → Cache cleared
3. DOES NOT TOUCH /system partition (dm-0) → System apps remain!

DMVI App Installation Paths:
/system/app/DMVI*/          ← System partition (READ-ONLY)
├─> dmvi_main.apk           (com.digitalmediavending)
├─> dmvi_hardware.apk       (com.digitalmediavending.hardware)
└─> dmvi_watchdog.apk       (com.digitalmediavending.watchdog)

User Apps (Removed by Factory Reset):
/data/app/                  ← Data partition (WRITABLE)
└─> (Your custom apps would go here)
```

**Conclusion:** DMVI apps are **OEM-installed firmware**, not cloud-deployed apps. They have the same persistence as Android system apps like Settings, Phone, etc.

### 3.4 Removal Methods

| Method | Reversible? | Risk | Procedure |
|--------|-------------|------|-----------|
| **Disable** | ✅ Yes | Low | `pm disable-user com.digitalmediavending*` |
| **Delete** | ⚠️ No | Medium | `adb remount && rm -rf /system/app/DMVI*` |
| **Custom ROM** | ⚠️ No | High | Flash AOSP/LineageOS via RKDevTool |

---

## 4. DMVI SOFTWARE ARCHITECTURE

### 4.1 Three-App System

```
┌────────────────────────────────────────────────┐
│  com.digitalmediavending (Main UI)             │
│  PID: 1770 | UID: u0_a132 | Memory: 294MB      │
│                                                 │
│  - User interface (product browsing, checkout) │
│  - Cloud communication (Astarte IoT)           │
│  - Session management                          │
│  - Analytics (Google Analytics/Firebase)       │
└────────────────┬───────────────────────────────┘
                 │ IPC (Binder)
┌────────────────▼───────────────────────────────┐
│  com.digitalmediavending.hardware (Service)    │
│  PID: 19863 | UID: u0_a131 | Memory: 140MB     │
│                                                 │
│  - Serial port management (/dev/ttyS0)         │
│  - Motor control (proprietary protocol)        │
│  - Payment terminal (MDB via Nayax SDK)        │
│  - Tray scanning & door lock                   │
└────────────────────────────────────────────────┘

┌────────────────────────────────────────────────┐
│  com.digitalmediavending.watchdog (Monitor)    │
│  PID: 1359 | UID: u0_a130 | Memory: 150MB      │
│                                                 │
│  - Process health monitoring                   │
│  - Auto-restart crashed apps                   │
│  - System vitals reporting                     │
└────────────────────────────────────────────────┘
```

### 4.2 Software Versions (from Machine.xml)

```json
{
  "main_app_version": "1.0.301",
  "vm_service_version": "1.1.20-release",
  "watchdog_version": "1.4.48",
  "update_successful": true
}
```

### 4.3 Astarte IoT Cloud Integration

**Astarte Property Store** - Cloud-synced configuration via MQTT/HTTP

| Interface | Purpose | File Location |
|-----------|---------|---------------|
| `Planogram` | Product layout & inventory | 29KB JSON embedded in XML |
| `Machine` | Device metadata & versions | Machine.xml |
| `DeviceControl` | Remote commands | DeviceControl.xml |
| `FirmwareUpdate` | OTA update instructions | FirmwareUpdate.xml |
| `Theme` | UI branding/colors | Theme.xml |
| `Advertise` | Promotional content | Advertise.xml (2.3KB) |
| `TaxesConfiguration` | Tax rates | TaxesConfiguration.xml |

**Cloud Endpoints:**
- S3 Bucket: `dmviproduction-cloud.s3.amazonaws.com`
- Product images: Pre-signed URLs (7-day expiry)
- Astarte realm: `o65WCIGTVDCVD4t7ATVNQw`

---

## 5. HARDWARE COMMUNICATION PROTOCOLS

### 5.1 Serial Port Configuration

| Device Path | Owner:Group | Permissions | Baud Rate | Protocol | Connected Hardware |
|-------------|-------------|-------------|-----------|----------|-------------------|
| `/dev/ttyS0` | root:system | `crwxrwxrwx` | 115200 | DMVI Proprietary | **Motor Controller** (10 coils) |
| `/dev/ttyUSB1` | radio:radio | `crw-rw----` | 115200 | MDB (Nayax) | **Payment Terminal** |
| `/dev/ttyUSB2` | radio:radio | `crw-rw----` | 115200 | PDF417/MagStripe | **ID Scanner** |
| `/dev/ttyUSB3` | radio:radio | `crw-rw----` | 115200 | PPP/QMI | **LTE Modem (Data)** |
| `/dev/ttyUSB4` | radio:radio | `crw-rw----` | 115200 | AT Commands | **LTE Modem (Control)** |

### 5.2 Motor Control Protocol (DMVI Proprietary)

**Command Structure:**
```
[HEADER] [LENGTH] [COMMAND] [DATA...] [CHECKSUM]
  0xAA     byte      byte     N bytes    byte (sum & 0xFF)
```

**Vend Command Example:**
```
Dispense from Coil 1:
[0xAA] [0x03] [0x01] [0x01] [0x01] [0xB0]
 ^^^^   ^^^^   ^^^^   ^^^^   ^^^^   ^^^^
Header Length  Vend  Coil   Duration Checksum
              Command  #1    (100ms)  (170+3+1+1+1=176)
```

**Response Format:**
```
[0xAA] [0x05] [0x01] [ROW] [COL] [STATUS] [CHECKSUM]

Status Codes:
- 0x00 = SUCCESS (product dispensed)
- 0x01 = FAILED (motor jam or timeout)
- 0x02 = TIMED_OUT (no confirmation within 5 seconds)
```

### 5.3 Payment Terminal Protocol (MDB)

- **Standard:** Multi-Drop Bus (MDB) v4.2
- **Adapter:** Nayax (exact model not visible in dumps)
- **SDK:** `com.bitmick.marshall.vmc.vmc_framework`
- **Baud Rate:** 115200 (non-standard - typically MDB uses 9600)
- **Payment Types:** Credit/debit cards, QR codes, NFC
- **PCI Compliance:** Handled by Nayax terminal (app never sees card data)

**Transaction Flow:**
1. RESET → Initialize device
2. SETUP → Exchange capabilities
3. POLL (continuous) → Monitor for payment
4. SESSION START → Customer taps card
5. VEND REQUEST → App requests authorization
6. VEND APPROVED → Funds confirmed
7. [Dispense Product]
8. VEND SUCCESS → Confirm completion
9. SESSION COMPLETE → Close transaction

### 5.4 ID Scanner Protocol

- **SDK:** IDScan Components (`net.idscan.components.android.hwreaders`)
- **Formats:** PDF417 (driver's license barcode), Magnetic stripe, OCR MRZ
- **Extracted Data:** Name, address, birthdate, expiration
- **Age Verification:** Automatic calculation for 21+ requirement
- **Compliance:** Must retain logs for 2-3 years (vape product regulations)

---

## 6. NETWORK CONFIGURATION

### 6.1 Active Interfaces

```
Interface: wlan0 (WiFi - PRIMARY)
IP Address: 192.168.7.246/22
Broadcast: 192.168.7.255
MAC Address: ec:2e:9b:d0:9b:3f
Status: UP, RUNNING
Gateway: 192.168.4.1 (inferred)
DNS: DHCP-assigned

Interface: eth0 (Ethernet 1)
MAC Address: 00:c0:08:a6:4a:1d
Status: NO-CARRIER (cable unplugged)

Interface: eth1 (Ethernet 2)
MAC Address: 00:c0:08:a6:4a:1e
Status: NO-CARRIER (cable unplugged)

Interface: usb0 (USB Ethernet)
MAC Address: 26:a5:c7:83:33:40
Status: DOWN (not configured)
```

### 6.2 Cellular Connection

```
Operator: T-Mobile (MCC: 310, MNC: 260)
SIM State: LOADED
Country: US
Network Type: Unknown (possibly LTE)
Roaming: false
```

### 6.3 ADB Configuration

```
ADB Status: RUNNING (init.svc.adbd)
ADB Port: 43039 (WiFi debugging)
Connection: 192.168.7.246:43039
Root Access: ENABLED (userdebug build)
Authentication: Unknown (likely disabled in userdebug)
```

---

## 7. SECURITY ANALYSIS

### 7.1 Security Status: ⚠️ CRITICALLY INSECURE

| Security Feature | Status | Risk Level | Details |
|------------------|--------|------------|---------|
| **Build Type** | userdebug | 🔴 CRITICAL | Root access enabled |
| **ADB Root** | ENABLED | 🔴 CRITICAL | `adb root` works |
| **SELinux** | Permissive | 🔴 CRITICAL | All security bypassed |
| **Verified Boot** | Orange | 🟡 HIGH | Bootloader unlocked |
| **dm-verity** | Enforcing (bypassed) | 🟡 HIGH | Not protecting /system |
| **Bootloader Lock** | Unlocked | 🟡 HIGH | Custom ROMs flashable |
| **OTA Server** | Local network | 🟡 HIGH | No HTTPS/authentication |
| **Security Patch** | 2021-08-05 | 🟡 HIGH | 4+ years outdated |
| **su_daemon** | RUNNING | 🔴 CRITICAL | Root shell accessible |

### 7.2 Attack Surface

**Accessible via Network:**
- ADB WiFi debugging (port 43039) - Root shell access
- HTTP OTA server (192.168.1.1:8888) - Firmware injection
- Astarte IoT (MQTT) - Cloud command injection
- WiFi/Cellular - Standard network attacks

**Physical Access:**
- USB debugging (also has root)
- Serial console (ttyFIQ0)
- Bootloader (fastboot mode)

### 7.3 Recommendations for Production

1. **Flash `user` build** - Disables root and ADB by default
2. **Lock bootloader** - Prevents custom ROM installation
3. **Enable SELinux enforcing** - Restore Android security model
4. **Update security patch** - Apply 4 years of security updates
5. **Implement TLS for OTA** - Encrypt and authenticate updates
6. **Disable WiFi ADB** - Require physical USB connection
7. **Network segmentation** - Isolate vending machines from public WiFi

---

## 8. INDUSTRIAL PC INTEGRATION

### 8.1 Control System Architecture

**The Android tablet IS the industrial PC** - there is no separate control computer. The SECO C31 is an industrial-grade embedded Android computer designed for:
- 24/7 operation
- Wide temperature range
- Industrial I/O (serial ports, GPIO)
- Ruggedized hardware

### 8.2 Hardware Integration

```
┌─────────────────────────────────────────────┐
│ SECO C31 Industrial PC (Rockchip RK3399)   │
│                                              │
│  ┌────────────────────────────────────────┐ │
│  │ Android 11 OS                          │ │
│  │                                        │ │
│  │  ┌──────────────────────────────────┐ │ │
│  │  │ DMVI Applications                │ │ │
│  │  └──────────────────────────────────┘ │ │
│  └────────────────────────────────────────┘ │
│                                              │
│  ┌────────────────────────────────────────┐ │
│  │ Hardware Abstraction Layer (HAL)      │ │
│  │ - Serial Port HAL                     │ │
│  │ - Android SerialPort API              │ │
│  └────────────────────────────────────────┘ │
│                                              │
│  ┌────────────────────────────────────────┐ │
│  │ Linux Kernel Drivers                  │ │
│  │ - UART driver (ttyS0)                 │ │
│  │ - USB-Serial (FTDI)                   │ │
│  │ - Touch controller                    │ │
│  │ - Display driver                      │ │
│  └────────────────────────────────────────┘ │
│                                              │
│  Physical I/O:                               │
│  [UART] [USB-C] [HDMI] [Ethernet] [GPIO]    │
└───┬──────┬──────┬──────┬──────────┬─────────┘
    │      │      │      │          │
    │      │      │      │          └─> Network
    │      │      │      └───────────> Display
    │      │      └──────────────────> Power/USB
    │      └─────────────────────────> Payment
    └────────────────────────────────> Motors

External Hardware:
├─> Motor Controller (10-coil dispenser)
├─> Nayax Payment Terminal (MDB)
├─> ID Scanner (USB)
├─> LTE Modem (Quectel EG25-G)
└─> TYALUX 32" Touchscreen
```

### 8.3 System Integration Points

1. **Power Management:**
   - Main power input → SECO C31
   - C31 controls motor power via serial commands
   - 24/7 operation with automatic restart on power failure

2. **User Input:**
   - Capacitive touchscreen → Goodix controller → USB → Android touch events
   - No physical buttons (all touch-based)

3. **Payment Processing:**
   - Payment terminal connected via USB-Serial (FTDI)
   - Nayax SDK in hardware service handles MDB protocol
   - Encrypted communication (PCI compliant at terminal level)

4. **Product Dispensing:**
   - Serial commands from Android → Motor controller
   - Motor controller manages 10 individual coil motors
   - Confirmation feedback via serial response

5. **Network Connectivity:**
   - Primary: WiFi (AW-CM276 module)
   - Backup: 4G LTE (Quectel modem)
   - Local: Ethernet (unused in current deployment)

---

## 9. BOOT PROCESS DEEP DIVE

### 9.1 Rockchip Boot Stages

```
┌────────────────────────────────────────────┐
│ Stage 1: Mask ROM (BootROM)               │
│ - Embedded in SoC (read-only)             │
│ - Loads IDBLOADER from eMMC               │
└────────────────┬───────────────────────────┘
                 ▼
┌────────────────────────────────────────────┐
│ Stage 2: IDBLOADER (DDR Init + SPL)       │
│ - Initializes LPDDR4 RAM                  │
│ - Loads U-Boot or Miniloader             │
└────────────────┬───────────────────────────┘
                 ▼
┌────────────────────────────────────────────┐
│ Stage 3: Bootloader (Rockchip Miniloader) │
│ - Reads parameter.txt (partition table)   │
│ - Loads kernel from boot partition        │
│ - Passes kernel command line              │
│ - ro.boot.* properties set here           │
└────────────────┬───────────────────────────┘
                 ▼
┌────────────────────────────────────────────┐
│ Stage 4: Linux Kernel (4.19.206-android11)│
│ - Decompresses kernel                     │
│ - Initializes device tree                 │
│ - Mounts initramfs                        │
│ - Starts init (PID 1)                     │
└────────────────┬───────────────────────────┘
                 ▼
┌────────────────────────────────────────────┐
│ Stage 5: Android Init                     │
│ - Parses /*.rc files                      │
│ - Mounts /system, /vendor, /data          │
│ - Applies SELinux policy (permissive!)    │
│ - Starts core services                    │
└────────────────┬───────────────────────────┘
                 ▼
┌────────────────────────────────────────────┐
│ Stage 6: SECO Init Scripts                │
│ - secoInit.rc                             │
│ - Hardware-specific initialization        │
│ - GPIO configuration                      │
└────────────────┬───────────────────────────┘
                 ▼
┌────────────────────────────────────────────┐
│ Stage 7: Android System Server            │
│ - Starts all Android framework services   │
│ - PackageManager scans /system/app/       │
│ - Installs DMVI apps as system apps       │
│ - Launches boot animation                 │
└────────────────┬───────────────────────────┘
                 ▼
┌────────────────────────────────────────────┐
│ Stage 8: DMVI Auto-Start                  │
│ - Watchdog starts first                   │
│ - Hardware service starts (opens serial)  │
│ - Main UI starts (launcher)               │
│ - Connects to Astarte IoT cloud           │
└────────────────────────────────────────────┘
```

### 9.2 Init Services (from system properties)

**Running Services:**
```
init.svc.adbd = running                  (ADB daemon - ROOT ACCESS)
init.svc.su_daemon = running             (Superuser daemon)
init.svc.audioserver = running
init.svc.cameraserver = running
init.svc.surfaceflinger = running        (Display compositor)
init.svc.netd = running                  (Network daemon)
init.svc.hwservicemanager = running      (HIDL services)
```

**SECO Custom Services:**
```
init.svc.secoInit = stopped              (Ran at boot, then stopped)
init.svc.secoBootComplete = stopped      (Boot completion hook)
```

**Stopped/Disabled Services:**
```
init.svc.ril-daemon = stopped            (RIL disabled - using Quectel)
init.svc.bootanim = stopped              (Boot animation finished)
```

### 9.3 Kernel Boot Parameters

```
Kernel Command Line (reconstructed from properties):
androidboot.hardware=rk30board
androidboot.selinux=permissive
androidboot.verifiedbootstate=orange
androidboot.flash.locked=0
androidboot.serialno=b0535a1f9f0f6ce0
androidboot.console=ttyFIQ0
androidboot.boot_devices=fe330000.sdhci
androidboot.storagemedia=emmc
```

---

## 10. SYSTEM SERVICES & PROCESSES

### 10.1 Critical System Processes

| Process | PID | Purpose | Status |
|---------|-----|---------|--------|
| `init` | 1 | Init system (PID 1) | Running |
| `kthreadd` | 2 | Kernel thread manager | Running |
| `ueventd` | 137 | Device node manager | Running |
| `logd` | 157 | Android logging daemon | Running |
| `servicemanager` | ~160s | Binder IPC manager | Running |
| `hwservicemanager` | ~160s | HIDL service manager | Running |
| `surfaceflinger` | ~160s | Graphics compositor | Running |
| `su_daemon` | Running | **Root access daemon** | Running |

### 10.2 DMVI Application Processes

| Process | PID | UID | Memory | Purpose |
|---------|-----|-----|--------|---------|
| `com.digitalmediavending.watchdog` | 1359 | u0_a130 | 150MB | Monitoring |
| `com.digitalmediavending` | 1770 | u0_a132 | 294MB | Main UI |
| `com.digitalmediavending.hardware` | 19863 | u0_a131 | 140MB | Hardware control |

**Key Observations:**
- All three apps running as separate processes
- Hardware service has the highest PID (started last or restarted)
- Watchdog has lowest memory (lightweight monitoring)
- Main UI has highest memory (graphics + cloud sync)

### 10.3 Hardware-Related Kernel Threads

```
[rkvdec]        - Rockchip video decoder
[vdpu]          - Video processing unit
[vepu]          - Video encoder unit
[mali_*]        - ARM Mali GPU threads
[fusb302_wq]    - USB Type-C controller workqueue
[stmmac_wq]     - Ethernet controller workqueue
[goodix_wq]     - Touchscreen controller workqueue
```

---

## 11. COMPARISON WITH YOUR CUSTOM SYSTEM

### 11.1 Current DMVI System vs. Your Custom Stack

| Component | DMVI System | Your Custom System |
|-----------|-------------|-------------------|
| **UI Framework** | Android (Java/Kotlin) | React Native 0.82 (TypeScript) |
| **Backend** | AWS S3 + Astarte IoT | Node.js Express (local/cloud) |
| **Motor Control** | Hardware Service APK | SerialPortModule.kt (native bridge) |
| **Payment** | Nayax SDK (integrated) | Stubbed (free vend mode) |
| **ID Verification** | IDScan SDK | Placeholder modal |
| **Data Storage** | Astarte cloud sync | Mock data (PostgreSQL pending) |
| **Kiosk Mode** | Built-in (launcher replacement) | MyDeviceAdminReceiver.kt |
| **Auto-Start** | System app (automatic) | BootReceiver.kt |

### 11.2 Key Architectural Differences

**DMVI Approach:**
- **Monolithic system apps** - All functionality in pre-installed APKs
- **Cloud-first** - All config and product data from Astarte
- **Closed ecosystem** - Proprietary SDKs and protocols
- **OTA updates** - Centralized firmware management

**Your Custom Approach:**
- **Modular architecture** - Separate frontend (RN) and backend (Node.js)
- **API-driven** - RESTful backend with local/cloud flexibility
- **Open protocols** - Standard serial communication, documented protocols
- **Developer-friendly** - Full source code control and customization

---

## 12. BOOTLOADER FLASHING & RECOVERY

### 12.1 Rockchip Flash Tools

**RKDevTool (Windows):**
- Official Rockchip flashing utility
- Supports Maskrom mode and Loader mode
- Can flash individual partitions or full firmware
- Location: Typically `RKDevTool_v2.84` or newer

**Upgrade_Tool (Linux):**
- Command-line flashing tool
- Same capabilities as RKDevTool
- Usage: `upgrade_tool uf update.img`

### 12.2 Entering Flash Modes

**Maskrom Mode (Emergency):**
1. Power off device
2. Hold **Recovery** button
3. Short **eMASK** pin to ground (or use hardware button)
4. Connect USB-C cable
5. Device appears as "Found One MASKROM Device"

**Loader Mode (Normal Update):**
1. `adb reboot bootloader` (or hold Volume+)
2. Device enters fastboot/loader mode
3. Use RKDevTool to flash partitions

### 12.3 Partition Backup Commands

```bash
# Via ADB (requires root)
adb root
adb shell dd if=/dev/block/mmcblk2p1 of=/sdcard/boot.img
adb pull /sdcard/boot.img

# Via RKDevTool
# Use "Advanced Function" → "Read Flash" to backup partitions
```

### 12.4 Factory Firmware Availability

**No public firmware found** - SECO C31-specific images are likely proprietary. Contact:
- SECO Spa (hardware manufacturer)
- DMVI (software provider)
- Or extract from working device using RKDevTool

---

## 13. CONCLUSIONS & ANSWERS TO YOUR QUESTIONS

### 13.1 Why Do DMVI Apps Persist After Factory Reset?

**DEFINITIVELY ANSWERED:**

The DMVI apps are installed as **system apps in the `/system` partition**, not as user apps in `/data`. Android's factory reset operation:
- ✅ Wipes `/data` partition (user apps, settings, accounts)
- ✅ Wipes `/cache` partition (temporary files)
- ❌ **DOES NOT** wipe `/system` partition (OS and system apps)

**Analogy:** Factory reset removes your installed apps, but doesn't remove Android's built-in Settings app, Phone app, etc. DMVI apps are at the same level as those built-in apps.

**Deep System Running:** Yes, there IS a "deeper program" - it's the entire Android OS + DMVI apps baked into the system partition. It's not malware or a hidden rootkit; it's intentional OEM provisioning by SECO/DMVI during manufacturing.

### 13.2 Bootloader Analysis

**Bootloader Type:** Rockchip Miniloader (proprietary)
- NOT U-Boot (despite Rockchip sometimes using U-Boot, this device doesn't show U-Boot branding)
- Unlocked (flash.locked=0)
- Supports dynamic partitions (A/B updates)
- Verified boot disabled (orange state)

**Boot Process:** 7-stage boot from Mask ROM → IDBLOADER → Miniloader → Kernel → Init → SECO scripts → Android → DMVI apps

### 13.3 Industrial PC Architecture

**There is only ONE computer:** The SECO C31 embedded Android device IS the industrial PC. There's no separate "controller PC." Everything runs on Android:
- User interface (touchscreen)
- Motor control (via serial port)
- Payment processing (via USB-Serial + Nayax SDK)
- Network communication (WiFi/4G)
- Cloud sync (Astarte IoT)

**Integration:** The Android device communicates with external peripherals (motor controller, payment terminal, ID scanner) via serial ports. The Android OS has native serial port access, and the DMVI hardware service manages all communication.

### 13.4 Security Posture

**CRITICAL INSECURITY:** This device is running a development build with:
- Root access enabled globally
- SELinux in permissive mode (all security disabled)
- ADB WiFi debugging active and accessible
- 4-year-old security patches
- Unlocked bootloader

**For Production:** This is acceptable for development/testing but **ABSOLUTELY NOT** for public deployment. Anyone on the same WiFi network can gain root access to this device.

---

## 14. RECOMMENDATIONS

### 14.1 Immediate Actions

1. ✅ **Document everything** - This report captures the current state
2. ⚠️ **Isolate network** - Keep device on separate VLAN from public WiFi
3. ⚠️ **Disable ADB WiFi** - Use USB debugging only
4. ✅ **Continue custom development** - Your React Native approach is sound

### 14.2 For Production Deployment

1. **Security hardening:**
   - Flash production `user` build
   - Lock bootloader
   - Enable SELinux enforcing
   - Apply latest security patches
   - Implement network authentication

2. **Complete your custom stack:**
   - Finish payment integration (Nayax MDB)
   - Implement real ID scanner (IDScan SDK license)
   - Deploy PostgreSQL backend
   - Implement TLS/HTTPS for API

3. **Testing & Compliance:**
   - Age verification logging (2-3 year retention)
   - PCI DSS compliance for payments
   - Penetration testing
   - OTA update authentication

4. **Monitoring:**
   - Remote logging (Sentry, Datadog, etc.)
   - Uptime monitoring
   - Transaction analytics
   - Hardware diagnostics

### 14.3 Alternative Paths

**Option A: Hybrid Approach**
- Keep DMVI hardware service (motor/payment working)
- Replace only the UI with your React Native app
- Communicate via Binder IPC

**Option B: Complete Replacement** (Your current path)
- Replace all three DMVI apps
- Full control over entire stack
- More work, but total flexibility

**Option C: Custom ROM**
- Flash AOSP or LineageOS
- Remove all SECO/DMVI customizations
- Start with clean Android base
- Maximum control, highest risk

---

## 15. APPENDIX: FILE INVENTORY

### 15.1 Analyzed Dump Files

**DMVI_Data_20251120_150303/** (26 files)
- `dmvi_main.apk` (20.6MB) - Main UI application
- `dmvi_hardware.apk` (6.2MB) - Hardware service
- `system_properties.txt` (59KB) - Build info, bootloader config
- `serial_ports.txt` (9KB) - Port configuration
- `storage.txt` (1.8KB) - Partition layout
- `cpu.txt` (2.5KB) - Processor specifications
- `processes.txt` (44KB) - Running processes snapshot
- `network.txt` (4.3KB) - Network interface configuration
- `planogram.xml` (60KB) - Product catalog from cloud
- `machine_config.xml` (1.7KB) - Machine metadata
- `dmesg_kernel_log.txt` - Kernel boot messages
- `logcat.txt` (2.6MB) - Android system logs
- Plus 14 additional system analysis files

**DMVI_Dump_20251120_154254/** (9 files)
- `dmvi_main.apk` (20.6MB) - Same as above
- `dmvi_hardware.apk` (6.2MB) - Same as above
- `dmvi_watchdog.apk` (3.8MB) - Watchdog service (NEW)
- `dmesg_kernel_log.txt` (991KB) - Kernel boot log
- `config_ui/` (12 XML files) - Astarte IoT configurations
  - Planogram, Machine, DeviceControl, FirmwareUpdate, Theme, etc.
- `hardware_open_files.txt` (16KB) - File descriptor analysis
- `process_list.txt` (44KB) - Process snapshot
- `system_properties.txt` (59KB) - System configuration

### 15.2 Key Configuration Files

**Astarte IoT Configs (XML with embedded JSON):**
- `astarte...Planogram.xml` (29KB) - Product inventory
- `astarte...Machine.xml` (840B) - Software versions
- `astarte...Theme.xml` (1.5KB) - UI branding
- `astarte...Advertise.xml` (2.3KB) - Promotional content
- `digitalmediavendingsharedfile.xml` (5KB) - Encrypted credentials

---

## 16. GLOSSARY

- **A/B Updates** - Android's seamless update system (update in background, activate on reboot)
- **ADB** - Android Debug Bridge (command-line tool for device control)
- **Astarte** - Open-source IoT platform for device management
- **Binder IPC** - Android's inter-process communication mechanism
- **dm-verity** - Android's partition integrity verification system
- **eMMC** - Embedded MultiMediaCard (internal flash storage)
- **FIQ** - Fast Interrupt Request (ARM processor interrupt mode)
- **HIDL** - Hardware Interface Definition Language (Android HAL)
- **MDB** - Multi-Drop Bus (vending machine payment protocol)
- **Miniloader** - Rockchip's proprietary bootloader (alternative to U-Boot)
- **OTA** - Over-The-Air (wireless firmware updates)
- **SELinux** - Security-Enhanced Linux (Android's mandatory access control)
- **UART** - Universal Asynchronous Receiver/Transmitter (serial port)
- **userdebug** - Android build type with debugging enabled (NOT for production)

---

## DOCUMENT CONTROL

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-11-21 | Claude Code | Initial comprehensive analysis |

**Data Sources:**
- ADB system dumps (DMVI_Data_20251120_150303)
- Extended dumps (DMVI_Dump_20251120_154254)
- System property analysis (system_properties.txt)
- Kernel logs (dmesg_kernel_log.txt)
- Process snapshots (processes.txt, hardware_open_files.txt)
- Network configuration (network.txt)
- Serial port analysis (serial_ports.txt)
- APK metadata (3 system apps)
- Astarte IoT configurations (12 XML files)

**Analysis Tools:**
- Static file analysis
- System property correlation
- Partition layout reconstruction
- Boot sequence inference
- Serial protocol documentation review

**CONFIDENTIALITY:** This document contains proprietary information about DMVI systems and should be kept confidential.

---

**END OF REPORT**
