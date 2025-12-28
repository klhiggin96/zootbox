#!/bin/bash

# Deploy ZootBox Backend to Android tablet via ADB
# Assumes ARM64 binary is already built

set -e  # Exit on error

echo "========================================="
echo "ZootBox Backend Tablet Deployment"
echo "========================================="
echo ""

# Configuration
BINARY_PATH="./build/zootbox-backend-arm64"
TABLET_INSTALL_DIR="/data/local/tmp/zootbox"
TABLET_DB_DIR="/data/local/tmp/zootbox/data"
TABLET_BINARY="$TABLET_INSTALL_DIR/zootbox-backend"
TABLET_SERVICE_NAME="zootbox-backend"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if binary exists
if [ ! -f "$BINARY_PATH" ]; then
    echo -e "${RED}✗ Error: Binary not found at $BINARY_PATH${NC}"
    echo ""
    echo "Please build the binary first:"
    echo "  ./scripts/build_arm64.sh"
    exit 1
fi

echo "Step 1: Checking ADB connection..."
# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo -e "${RED}✗ Error: ADB not found${NC}"
    echo ""
    echo "Please install Android Debug Bridge (ADB):"
    echo "  - macOS: brew install android-platform-tools"
    echo "  - Ubuntu: sudo apt-get install adb"
    echo "  - Windows: Download from Android SDK"
    exit 1
fi

# Check if device is connected
DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)
if [ "$DEVICES" -eq 0 ]; then
    echo -e "${RED}✗ Error: No Android devices connected${NC}"
    echo ""
    echo "Please connect the tablet and enable USB debugging:"
    echo "  1. Settings → About Tablet → Tap 'Build Number' 7 times"
    echo "  2. Settings → Developer Options → Enable 'USB Debugging'"
    echo "  3. Connect via USB and authorize the computer"
    exit 1
fi

echo -e "  ${GREEN}✓ ADB connected${NC}"
adb devices
echo ""

echo "Step 2: Stopping existing service (if running)..."
adb shell "pkill -f zootbox-backend || true"
sleep 2
echo -e "  ${GREEN}✓ Service stopped${NC}"
echo ""

echo "Step 3: Creating directories on tablet..."
adb shell "mkdir -p $TABLET_INSTALL_DIR"
adb shell "mkdir -p $TABLET_DB_DIR"
echo -e "  ${GREEN}✓ Directories created${NC}"
echo ""

echo "Step 4: Transferring binary to tablet..."
adb push "$BINARY_PATH" "$TABLET_BINARY"
if [ $? -eq 0 ]; then
    echo -e "  ${GREEN}✓ Binary transferred${NC}"
else
    echo -e "  ${RED}✗ Transfer failed${NC}"
    exit 1
fi
echo ""

echo "Step 5: Setting permissions..."
adb shell "chmod +x $TABLET_BINARY"
echo -e "  ${GREEN}✓ Permissions set${NC}"
echo ""

echo "Step 6: Running database migrations..."
adb shell "cd $TABLET_INSTALL_DIR && DB_PATH=$TABLET_DB_DIR/inventory.db $TABLET_BINARY --migrate"
if [ $? -eq 0 ]; then
    echo -e "  ${GREEN}✓ Migrations completed${NC}"
else
    echo -e "  ${YELLOW}⚠ Migrations may have already run${NC}"
fi
echo ""

echo "Step 7: Starting backend service..."
# Start the service in the background using nohup
adb shell "cd $TABLET_INSTALL_DIR && nohup DB_PATH=$TABLET_DB_DIR/inventory.db HTTP_HOST=0.0.0.0 HTTP_PORT=8080 $TABLET_BINARY > $TABLET_INSTALL_DIR/backend.log 2>&1 &"
sleep 3
echo -e "  ${GREEN}✓ Service started${NC}"
echo ""

echo "Step 8: Verifying deployment..."
# Check if process is running
PROCESS_COUNT=$(adb shell "ps -A | grep zootbox-backend | wc -l")
if [ "$PROCESS_COUNT" -gt 0 ]; then
    echo -e "  ${GREEN}✓ Process running${NC}"
    adb shell "ps -A | grep zootbox-backend"
else
    echo -e "  ${RED}✗ Process not found${NC}"
    echo ""
    echo "Check logs:"
    echo "  adb shell cat $TABLET_INSTALL_DIR/backend.log"
    exit 1
fi
echo ""

# Test health endpoint
echo "Step 9: Health check..."
sleep 2
adb shell "curl -s http://localhost:8080/health" 2>/dev/null || echo -e "${YELLOW}⚠ Health check unavailable (curl may not be installed on tablet)${NC}"
echo ""

echo "========================================="
echo -e "${GREEN}Deployment Complete!${NC}"
echo "========================================="
echo ""
echo "Backend Information:"
echo "  Install Directory: $TABLET_INSTALL_DIR"
echo "  Database Path:     $TABLET_DB_DIR/inventory.db"
echo "  Log File:          $TABLET_INSTALL_DIR/backend.log"
echo "  HTTP Port:         8080"
echo ""
echo "Useful Commands:"
echo "  View logs:         adb shell cat $TABLET_INSTALL_DIR/backend.log"
echo "  Follow logs:       adb shell tail -f $TABLET_INSTALL_DIR/backend.log"
echo "  Stop service:      adb shell pkill -f zootbox-backend"
echo "  Restart service:   ./scripts/install_tablet.sh"
echo "  Check health:      adb shell curl http://localhost:8080/health"
echo "  View metrics:      adb shell curl http://localhost:8080/metrics"
echo "  Port forward:      adb forward tcp:8080 tcp:8080"
echo ""
echo "Access from host machine (after port forwarding):"
echo "  curl http://localhost:8080/health"
echo "  curl http://localhost:8080/api/v1/coils"
echo ""
