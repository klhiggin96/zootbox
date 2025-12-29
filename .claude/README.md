# ZootBox Project Documentation

This directory contains comprehensive project documentation for the ZootBox Vending Machine application. These files provide essential context for Claude to understand the project structure, technical implementation, and application flow.

## Documentation Files

### 1. zootbox-project-context.md
**Purpose**: Quick reference guide for common operations and project structure

**Contains**:
- Project overview (package name, SDK versions, location)
- File structure and critical file locations
- Build and deployment commands
- Layout configuration and design specs
- Color palette and typography
- Common modification tasks
- Troubleshooting commands
- Git status snapshot

**Use when**: You need to know where files are, how to build/deploy, or what the current project structure looks like.

### 2. ZOOTBOX_APPLICATION_FLOW.md
**Purpose**: Complete visual documentation of application flow and behavior

**Contains**:
- Comprehensive Mermaid flowcharts (screen-to-screen journey)
- State machine diagrams for each activity
- Hardware integration sequence diagrams
- Data flow diagrams
- Product catalog data structure
- User journey timeline
- Timing and configuration metrics
- Hardware specifications table

**Use when**: You need to understand how the app flows, state transitions, hardware integration, or user journeys.

### 3. ZOOTBOX_TECH_STACK.md
**Purpose**: Complete technical stack and architecture reference

**Contains**:
- Frontend framework details (Kotlin, Android SDK, libraries)
- Hardware integration specs (USB serial, ID scanner, payment reader)
- Backend services architecture (future roadmap)
- Development tools and CI/CD
- Deployment and distribution strategy
- Architecture patterns (MVVM, State Machine, Observer)
- Security and compliance requirements
- Performance optimization strategies
- File structure and dependency lists
- System requirements and supported devices

**Use when**: You need to understand technical implementation details, dependencies, architecture patterns, or hardware specifications.

## Quick Reference

### Project Location
```
c:\dev\MyApplication\
```

### Build Commands
```bash
# Debug build and install
cd /c/dev/MyApplication && ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.example.myapplication/.MainActivity
```

### Key Technologies
- **Language**: Kotlin
- **Platform**: Android (API 26+, Target API 34)
- **Hardware**: USB Serial (FTDI ID Scanner, CDC-ACM Payment Reader)
- **Architecture**: MVVM-Lite with State Machines
- **UI**: ConstraintLayout, RecyclerView, Custom Animations

### Application Structure
- **MainActivity**: Category selection (4 sections with color theming)
- **ProductGridActivity**: 2-column product grid with category filtering
- **ProductDetailActivity**: Product details with quantity selection
- **IdScanActivity**: Age verification with USB ID scanner
- **ScreensaverActivity**: Idle screensaver with dual video loop
- **HardwareService**: Foreground service managing USB hardware

### Current Branch
```
ZOOTED (active development)
```

### Product Categories
1. **ZyNS** (2 products, age 21+)
2. **VAPES** (1 product, age 21+)
3. **CIGERATES** (2 products, age 18+)
4. **ZOOTBOX LEGENDARY LOOT** (6 products, no age restriction)

## Documentation Maintenance

### When to Update
- **zootbox-project-context.md**: When files move, build commands change, or project structure updates
- **ZOOTBOX_APPLICATION_FLOW.md**: When adding/removing activities, changing flow logic, or updating hardware integration
- **ZOOTBOX_TECH_STACK.md**: When adding dependencies, changing architecture, or updating tech stack

### Version Control
All documentation files in this directory should be committed to git to maintain project memory across sessions.

## For Claude
When working on this project, always reference these files to:
1. Understand existing code structure before making changes
2. Follow established patterns and conventions
3. Ensure changes align with the documented architecture
4. Maintain consistency with existing implementation
5. Understand hardware constraints and integration requirements

---

**Last Updated**: 2024-12-27
**Project Version**: 0.9-ZOOTED
**Documentation Version**: 1.0
