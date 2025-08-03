# PaymentAnalytics - Android IPC Implementation

Payment application with background analytics service using AIDL IPC for real-time device metrics collection.

## Architecture

**MVVM Clean Architecture** with cross-process communication:
- **Main App**: UI layer (MainActivity, ViewModel) → Domain (Use Cases) → Data (Repository)
- **Analytics Service**: Separate `:analytics` process collecting battery/memory/CPU metrics
- **IPC**: AIDL interface with security validation and performance optimizations

```
Main Process ←--AIDL IPC--→ Analytics Process (:analytics)
     │                           │
   UI/VM                    AnalyticsService
     │                           │
Repository ←────────────→ MetricsCollector
```

## Key Components

### AIDL Interface
```kotlin
interface IAnalyticsService {
    AnalyticsData getCurrentStats();
    boolean isServiceReady();
    void resetStats();
}
```

### Security
- Custom permission: `com.sjdroid.paymentanalytics.permission.ACCESS_ANALYTICS`
- Signature-level protection with caller validation
- Security validation caching (30s TTL)

### Performance Optimizations
- **Rate limiting**: 1-second minimum interval between IPC calls
- **Security caching**: Cache validation results to avoid expensive signature checks
- **Smart UI updates**: Prevent unnecessary data refresh cycles

### Service Implementation
```kotlin
// AndroidManifest.xml
<service
    android:name=".service.AnalyticsService"
    android:process=":analytics"
    android:permission="com.sjdroid.paymentanalytics.permission.ACCESS_ANALYTICS" />
```

### Data Flow
```
UI Click → ViewModel → UseCase → Repository → IPC → AnalyticsService → MetricsCollector
```

## Build & Run

Standard Android build:
```bash
./gradlew assembleDebug
```

No special setup required. AIDL files auto-generate during build.

## Technical Implementation

### Repository Pattern
```kotlin
class AnalyticsRepositoryImpl : AnalyticsRepository {
    // Rate limiting + caching
    // Binder death handling with linkToDeath()
    // Connection state management with Flow
}
```

### Metrics Collection
- Real-time collection every 5 seconds
- Battery level via BatteryManager
- Memory usage via ActivityManager  
- CPU usage with fallback simulation (Android 10+ restrictions)

### Process Isolation
- Analytics service runs in separate process for security/performance
- Transaction simulation (15s intervals, 90% success rate)
- Automatic binder death recovery

## Package Structure
```
com.sjdroid.paymentanalytics/
├── presentation/     # UI layer (MainActivity, ViewModel)
├── domain/          # Business logic (Use Cases, Repository interface)  
├── data/            # Data access (Repository implementation)
├── service/         # Background service + metrics collection
└── di/              # Dependency injection
```

**Dependencies**: Standard Android + Compose, no external libraries for IPC implementation.