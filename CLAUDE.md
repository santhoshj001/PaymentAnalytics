# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

- **Build Debug APK**: `./gradlew assembleDebug`
- **Build Release APK**: `./gradlew assembleRelease`
- **Run Unit Tests**: `./gradlew test`
- **Run Instrumented Tests**: `./gradlew connectedAndroidTest`
- **Clean Build**: `./gradlew clean`
- **Install Debug APK**: `./gradlew installDebug`

## Architecture Overview

This is an Android payment analytics application demonstrating **MVVM Clean Architecture** with cross-process communication via **AIDL IPC**.

### Key Architecture Components

**Process Isolation**: 
- Main app runs in default process
- `AnalyticsService` runs in separate `:analytics` process for security/performance isolation
- Communication happens via AIDL interface with custom permissions

**AIDL Interface**: `IAnalyticsService.aidl` defines the contract:
```kotlin
interface IAnalyticsService {
    AnalyticsData getCurrentStats();
    boolean isServiceReady();
    void resetStats();
}
```

**Security Model**:
- Custom permission: `com.sjdroid.paymentanalytics.permission.ACCESS_ANALYTICS`
- Signature-level protection with caller validation
- Security validation caching (30s TTL) to optimize performance
- Binder death recovery with `linkToDeath()`

**Performance Optimizations**:
- Rate limiting: 1-second minimum interval between IPC calls
- Security caching: Avoid expensive signature checks
- Smart UI updates: Prevent unnecessary data refresh cycles

### Package Structure

```
com.sjdroid.paymentanalytics/
├── presentation/     # UI layer (MainActivity, ViewModel, UiState)
├── domain/          # Business logic (Use Cases, Repository interface)  
├── data/            # Data access (Repository implementation)
├── service/         # Background service + metrics collection
└── di/              # Dependency injection
```

### Data Flow

```
UI Click → ViewModel → UseCase → Repository → IPC → AnalyticsService → MetricsCollector
```

## Key Implementation Details

**Repository Pattern**: `AnalyticsRepositoryImpl` handles:
- Service connection management with Flow-based state
- Rate limiting for IPC calls
- Binder death handling and automatic reconnection
- Security validation caching

**Metrics Collection**: `MetricsCollector` provides:
- Real-time battery level via `BatteryManager`
- Memory usage via `ActivityManager`
- CPU usage with Android 10+ fallback simulation
- Collection every 5 seconds

**Service Lifecycle**: 
- Transaction simulation (15s intervals, 90% success rate)
- Automatic service restart on process death
- Connection state tracking: Disconnected/Connecting/Connected/Error

## Dependencies

- **UI**: Jetpack Compose with Material3
- **Architecture**: AndroidX Lifecycle, ViewModel
- **Build**: Kotlin 2.0.21, AGP 8.11.1, compileSdk 36
- **Testing**: JUnit, Espresso, Compose UI Testing
- **IPC**: Android AIDL (no external libraries)

## Development Notes

- AIDL files auto-generate during build (no manual steps required)
- Enable AIDL in `build.gradle.kts`: `buildFeatures { aidl = true }`
- Service permission declared in `AndroidManifest.xml`
- No special setup required - standard Android development workflow