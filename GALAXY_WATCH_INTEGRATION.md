# Pyrrha Mobile-Watch Integration Guide

## Samsung Accessory Protocol Integration

This document describes the Samsung Accessory Protocol integration between the Pyrrha Mobile App (Provider) and Pyrrha Watch App (Consumer) for real-time sensor data transmission.

## Architecture Overview

```
Prometeo Device (BLE) → Mobile App (Provider) → Galaxy Watch (Consumer)
```

### Mobile App (Provider)
- **Service**: `ProviderService.java` extends `SAAgent`
- **Role**: Provider (sends sensor data)
- **App Name**: `PyrrhaMobileProvider`
- **Channel ID**: 104
- **Service Profile**: `/org/pyrrha-platform/readings`

### Watch App (Consumer)  
- **Service**: JavaScript consumer in `connect.js`
- **Role**: Consumer (receives sensor data)
- **Expected Provider**: `PyrrhaMobileProvider`
- **Channel ID**: 104

## Data Flow

1. **BLE Reception**: Mobile app receives sensor data from Prometeo device via Bluetooth LE
2. **Data Parsing**: `DeviceDashboard.displayData()` parses space-separated sensor values:
   - `parts[2]` = Temperature (°C)
   - `parts[4]` = Humidity (%)
   - `parts[6]` = Carbon Monoxide (ppm)
   - `parts[8]` = Nitrogen Dioxide (ppm)
3. **Data Validation**: Invalid readings (CO > 1000 or < 0, NO2 > 10 or < 0) are set to 0
4. **JSON Formatting**: Data is formatted as JSON with message type and timestamp
5. **Samsung Accessory Protocol**: Data transmitted via Samsung Accessory Protocol to watch
6. **Watch Display**: Watch receives and displays real-time sensor readings

## JSON Message Format

```json
{
  "messageType": "sensor_data",
  "temperature": 25.4,
  "humidity": 65.2,
  "co": 15.3,
  "no2": 0.8,
  "timestamp": 1672531200000,
  "deviceId": "Prometeo:00:00:00:00:00:01",
  "status": "normal"
}
```

### Status Values
- `"normal"`: All readings within safe thresholds
- `"warning"`: One or more readings at 80% of alert threshold
- `"alert"`: One or more readings exceed safety thresholds

### Alert Thresholds
- **Temperature**: 32°C
- **Humidity**: 80%
- **Carbon Monoxide**: 420 ppm
- **Nitrogen Dioxide**: 8 ppm

## Service Configuration

### Mobile App Configuration

**AndroidManifest.xml**:
```xml
<service android:name="org.pyrrha_platform.galaxy.ProviderService" />
```

**accessoryservices.xml**:
```xml
<application name="PyrrhaMobileProvider">
    <serviceProfile
        id="/org/pyrrha-platform/readings"
        name="PyrrhaSensorProvider"
        role="provider"
        serviceImpl="org.pyrrha_platform.galaxy.ProviderService"
        version="2.0"
        serviceLimit="ANY"
        serviceTimeout="10000">
        <supportedTransports>
            <transport type="TRANSPORT_BT" />
            <transport type="TRANSPORT_WIFI" />
        </supportedTransports>
        <serviceChannel
            id="104"
            dataRate="HIGH"
            priority="HIGH"
            reliability="ENABLE" />
        <supportedFeatures>
            <feature type="message" />
        </supportedFeatures>
    </serviceProfile>
</application>
```

### Watch App Configuration

**config.xml**:
```xml
<tizen:application id="Jqb25DY60P.pyrrha" package="Jqb25DY60P" required_version="5.5"/>
<tizen:setting background-support="enable" encryption="disable" hwkey-event="enable"/>
<tizen:privilege name="http://tizen.org/privilege/healthinfo"/>
<tizen:privilege name="http://tizen.org/privilege/alarm.set"/>
```

## Testing Integration

### Prerequisites
1. Samsung Galaxy A51 with Android 14 (API 34)
2. Samsung Galaxy Watch 3 with Tizen 5.5
3. Both devices paired via Samsung Galaxy Watch app
4. Pyrrha Mobile App installed on phone
5. Pyrrha Watch App installed on watch

### Test Procedure

1. **Start Mobile App**: Launch Pyrrha app and connect to Prometeo device
2. **Check Provider Service**: Verify ProviderService starts and searches for watches
3. **Start Watch App**: Launch Pyrrha app on Galaxy Watch
4. **Connection**: Watch should automatically discover and connect to mobile provider
5. **Data Flow**: Sensor readings should appear on watch within 3 seconds of mobile reception

### Debugging

**Mobile App Logs**:
```bash
adb logcat -s PyrrhaMobileProvider
```

**Watch App Logs**:
Access via Tizen Studio or Samsung Internet debugger on watch

### Common Issues

1. **Connection Failed**: Ensure both devices are on same Samsung account and paired
2. **Service Not Found**: Verify both apps are running and have correct service profiles
3. **No Data**: Check that Prometeo device is connected to mobile app via BLE
4. **Invalid Data**: Sensor validation may filter out invalid readings (CO > 1000, NO2 > 10)

## Implementation Details

### ProviderService Features
- **Automatic Discovery**: Searches for Galaxy Watches on service start
- **Connection Management**: Handles multiple watch connections
- **Error Handling**: Comprehensive error codes and reconnection logic
- **Heartbeat Support**: Responds to watch heartbeat requests
- **Data Broadcasting**: Sends sensor data every 3 seconds to connected watches

### Watch Consumer Features
- **Auto-Connect**: Automatically connects to PyrrhaMobileProvider
- **Message Parsing**: Handles JSON sensor data and control messages
- **UI Updates**: Real-time sensor display with circular Galaxy Watch 3 optimization
- **Alert System**: Vibration alerts when readings exceed thresholds
- **Reconnection**: Automatic reconnection on connection loss

## Security Considerations

- Samsung Accessory Protocol uses built-in Samsung authentication
- Data transmission encrypted via Samsung framework
- No additional authentication required for paired devices
- Service limited to Samsung Galaxy ecosystem

## Performance Notes

- **Update Frequency**: 3-second intervals for optimal battery life
- **Data Size**: JSON messages ~200 bytes each
- **Battery Impact**: Minimal - uses Samsung's optimized protocol
- **Range**: Standard Bluetooth range (10 meters typical)

## Development Notes

- Provider service automatically starts with DeviceDashboard activity
- Watch consumer runs continuously while app is active
- Service connections managed in activity lifecycle
- Error handling includes graceful degradation without watch connectivity

## Future Enhancements

- Historical data synchronization
- Multiple device support
- Custom alert thresholds
- Watch-initiated sensor requests
- Offline data buffering