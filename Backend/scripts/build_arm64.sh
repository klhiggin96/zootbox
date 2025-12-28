#!/bin/bash

# Cross-compile ZootBox Backend for ARM64 Android tablet
# Produces a static binary optimized for embedded deployment

set -e  # Exit on error

echo "========================================="
echo "ZootBox Backend ARM64 Build Script"
echo "========================================="
echo ""

# Build configuration
OUTPUT_DIR="./build"
BINARY_NAME="zootbox-backend"
VERSION=$(git describe --tags --always --dirty 2>/dev/null || echo "dev")
BUILD_TIME=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
GO_VERSION=$(go version | awk '{print $3}')

# Target platform
export GOOS=linux
export GOARCH=arm64
export CGO_ENABLED=1

# Android NDK toolchain (if available)
# Set these environment variables if you have Android NDK installed:
# export CC="$NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android30-clang"
# export CXX="$NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android30-clang++"

echo "Build Configuration:"
echo "  Target OS:      $GOOS"
echo "  Target Arch:    $GOARCH"
echo "  CGO Enabled:    $CGO_ENABLED"
echo "  Version:        $VERSION"
echo "  Build Time:     $BUILD_TIME"
echo "  Go Version:     $GO_VERSION"
echo ""

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo "Step 1: Installing dependencies..."
go mod download
go mod verify
echo "  ✓ Dependencies verified"
echo ""

echo "Step 2: Running tests..."
go test ./... -short
echo "  ✓ Tests passed"
echo ""

echo "Step 3: Cross-compiling for ARM64..."

# Build flags for optimization
LDFLAGS="-w -s"  # Strip debug info and symbol table
LDFLAGS="$LDFLAGS -X 'main.Version=$VERSION'"
LDFLAGS="$LDFLAGS -X 'main.BuildTime=$BUILD_TIME'"

# Build the binary
go build \
    -ldflags "$LDFLAGS" \
    -trimpath \
    -o "$OUTPUT_DIR/$BINARY_NAME-arm64" \
    ./cmd/server

if [ $? -eq 0 ]; then
    echo "  ✓ Build successful"
else
    echo "  ✗ Build failed"
    exit 1
fi
echo ""

# Check binary size
BINARY_SIZE=$(du -h "$OUTPUT_DIR/$BINARY_NAME-arm64" | cut -f1)
echo "Step 4: Build Results"
echo "---------------------"
echo "  Binary:         $OUTPUT_DIR/$BINARY_NAME-arm64"
echo "  Size:           $BINARY_SIZE"
echo "  Target:         $GOOS/$GOARCH"
echo ""

# Verify binary format
echo "Step 5: Verification"
echo "--------------------"
file "$OUTPUT_DIR/$BINARY_NAME-arm64"
echo ""

# Size check
MAX_SIZE_MB=15
BINARY_SIZE_BYTES=$(stat -c%s "$OUTPUT_DIR/$BINARY_NAME-arm64" 2>/dev/null || stat -f%z "$OUTPUT_DIR/$BINARY_NAME-arm64")
BINARY_SIZE_MB=$((BINARY_SIZE_BYTES / 1024 / 1024))

if [ $BINARY_SIZE_MB -gt $MAX_SIZE_MB ]; then
    echo "⚠ WARNING: Binary size ($BINARY_SIZE_MB MB) exceeds target (<$MAX_SIZE_MB MB)"
else
    echo "✓ Binary size check passed ($BINARY_SIZE_MB MB < $MAX_SIZE_MB MB)"
fi
echo ""

# Create deployment package
echo "Step 6: Creating deployment package..."
PACKAGE_NAME="zootbox-backend-$VERSION-arm64.tar.gz"
tar -czf "$OUTPUT_DIR/$PACKAGE_NAME" \
    -C "$OUTPUT_DIR" "$BINARY_NAME-arm64" \
    -C .. README.md

echo "  ✓ Package created: $OUTPUT_DIR/$PACKAGE_NAME"
echo ""

echo "========================================="
echo "Build Complete!"
echo "========================================="
echo ""
echo "Deployment files:"
echo "  Binary:  $OUTPUT_DIR/$BINARY_NAME-arm64"
echo "  Package: $OUTPUT_DIR/$PACKAGE_NAME"
echo ""
echo "Next steps:"
echo "  1. Transfer to Android tablet: ./scripts/install_tablet.sh"
echo "  2. Or manually copy: adb push $OUTPUT_DIR/$BINARY_NAME-arm64 /data/local/tmp/"
echo ""
