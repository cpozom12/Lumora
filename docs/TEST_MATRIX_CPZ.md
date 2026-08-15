# CPZ Test Matrix

Use this matrix before and after meaningful media/Android Auto changes.

## Target baseline

Primary phone target:
- Samsung Galaxy A35

Vehicle/head-unit target:
- Toyota Agya + Android Auto

## Baseline checks

### Phone
- App installs alongside any upstream/debug package as expected.
- App launches without migration/database crash.
- Provider setup screen opens.
- Live stream playback sanity check.
- VOD playback sanity check.
- Audio/subtitle controls sanity check.
- Background/foreground resume sanity check.

### Android Auto — stationary only
- App appears in launcher when required sideload/developer settings are enabled.
- Session opens while vehicle is stationary.
- Catalog/navigation renders correctly.
- Video surface renders correctly while stationary.
- Audio routing is correct.
- Disconnect/reconnect does not corrupt session state.
- Host motion restrictions remain effective.

### Regression gate
- `./gradlew lint --no-daemon`
- `./gradlew test --no-daemon`
- `./gradlew :app:assembleDebug --no-daemon`

## Results log

Record each physical-device run with:
- Date/time
- App commit SHA
- Android version
- Android Auto version
- Wired/wireless connection
- Head-unit firmware if visible
- Pass/fail per check
- Crash/log notes
