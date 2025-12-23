# Hardware Interface Matrix

| Peripheral | VID:PID | Kernel Node | Notes |
|------------|---------|-------------|-------|
| Nayax VPOS Touch | `26f1:5650` | `/dev/ttyACM0` | Enumerates as CDC-ACM; use USB host APIs/serial driver at 115200 baud for payment authorization. |
| E-Seek M260 | `0403:6001` | (FTDI) | Requires FTDI serial support; claim over USB host to stream AAMVA data for ID age verification. |
| CP2102N bridge | `10c4:ea60` | `/dev/ttyUSB0` | Internal MCU UART (leave untouched). |
| Quectel EG25 modem | `2c7c:0125` | `/dev/ttyUSB1-4` | Cellular modem exposed as option driver. |

`dmesg` confirms FTDI and Nayax attachments; scanner lacks an auto-bound `/dev/ttyUSB*`, so the app claims the interface directly and keeps both devices alive inside `HardwareService`.

