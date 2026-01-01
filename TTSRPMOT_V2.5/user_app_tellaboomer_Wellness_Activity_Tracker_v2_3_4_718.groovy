/**
 * ============================================================================
 * TELLABOOMER RPM - Wellness Activity Tracker v2.3.2
 * ============================================================================
 * 
 * PATCH NOTES v2.3.2:
 * - ADDED: Multi-sensor-type room mapping (motion, pressure pad, flush, temp/humidity)
 * - ADDED: Separate state management for each sensor type
 * - ADDED: Room assignment UI for all 4 sensor types
 * - ADDED: Temperature/Humidity sensor support
 * - ENHANCED: Auto-import now handles all sensor types from Hubitat
 * - ENHANCED: Summary page shows all sensors by type per room
 * - ENHANCED: Backward compatibility with v2.3.1 configurations (auto-migration)
 * 
 * PATCH NOTES v2.3.1:
 * - FIXED: Float sensor capability - changed from contactSensor to waterSensor
 * - FIXED: Float sensor event subscription - now listens to water.wet instead of contact.open
 * - FIXED: Syntax error in sensorsPage() - missing closing braces
 * - ENHANCED: Float sensor selection now properly shows Aqara and other water leak detectors
 * - ENHANCED: Updated UI text to reflect WET/DRY states instead of OPEN/CLOSED
 * 
 * PATCH NOTES v2.3.0:
 * - ADDED: Multi-Profile Monitoring - Define custom time windows beyond overnight
 * - ADDED: Hubitat Mode Integration - Trigger profiles by location mode changes
 * - ADDED: Whole-House Room Tracking - Track all room transitions throughout the day
 * - ADDED: Room activity summary in API response (visits per room, dwell times)
 * 
 * Production-ready Hubitat app for overnight wellness monitoring.
 * Designed for aging-in-place remote patient monitoring.
 * 
 * @version 2.3.2
 * @author TellaBoomer RPM Platform
 * @date December 2025
 */

definition(
    name: "Wellness Activity Tracker v2.3.4",
    namespace: "tellaboomer",
    author: "TellaBoomer RPM",
    description: "Comprehensive wellness monitoring with full Pushover driver integration - priority levels, dashboard links, custom sounds, and rich notifications",
    category: "Health & Wellness",
    iconUrl: "",
    iconX2Url: "",
    singleInstance: true,
    oauth: true
)

// ============================================================================
// PREFERENCES - Configuration Pages
// ============================================================================

preferences {
    page(name: "mainPage")
    page(name: "sensorsPage")
    page(name: "roomsPage")
    page(name: "addRoomPage")
    page(name: "editRoomPage")
    page(name: "overnightSettingsPage")
    page(name: "monitoringProfilesPage")
    page(name: "detectionSettingsPage")
    page(name: "notificationsPage")
    page(name: "enhancedNotificationsPage")
    page(name: "integrationPage")
    page(name: "advancedPage")
    page(name: "summaryPage")
}

// ============================================================================
// STATE INITIALIZATION v2.3.2 - Multi-Sensor Room Mapping
// ============================================================================

def ensureStateInitialized() {
    // Basic room structures
    if (state.rooms == null) {
        state.rooms = [:]
        log.info "[TellaBoomer] Initialized state.rooms"
    }
    
    // NEW v2.3.2: Separate mappings for each sensor type
    if (state.motionSensorMappings == null) {
        state.motionSensorMappings = [:]
        log.info "[TellaBoomer] Initialized state.motionSensorMappings"
    }
    if (state.pressurePadMappings == null) {
        state.pressurePadMappings = [:]
        log.info "[TellaBoomer] Initialized state.pressurePadMappings"
    }
    if (state.flushSensorMappings == null) {
        state.flushSensorMappings = [:]
        log.info "[TellaBoomer] Initialized state.flushSensorMappings"
    }
    if (state.tempHumiditySensorMappings == null) {
        state.tempHumiditySensorMappings = [:]
        log.info "[TellaBoomer] Initialized state.tempHumiditySensorMappings"
    }
    
    // Quick lookup maps for each type
    if (state.motionRoomMap == null) {
        state.motionRoomMap = [:]
        log.info "[TellaBoomer] Initialized state.motionRoomMap"
    }
    if (state.pressurePadRoomMap == null) {
        state.pressurePadRoomMap = [:]
        log.info "[TellaBoomer] Initialized state.pressurePadRoomMap"
    }
    if (state.flushSensorRoomMap == null) {
        state.flushSensorRoomMap = [:]
        log.info "[TellaBoomer] Initialized state.flushSensorRoomMap"
    }
    if (state.tempHumidityRoomMap == null) {
        state.tempHumidityRoomMap = [:]
        log.info "[TellaBoomer] Initialized state.tempHumidityRoomMap"
    }
    
    // Migration: Convert old v2.3.1 sensorMappings to new structure
    if (state.sensorMappings && !state.migrated_to_v232) {
        state.motionSensorMappings = state.sensorMappings
        state.sensorMappings = null
        state.migrated_to_v232 = true
        log.info "[TellaBoomer] Migrated v2.3.1 sensorMappings to v2.3.2 motionSensorMappings"
    }
    if (state.roomMap && !state.migrated_roommap_v232) {
        state.motionRoomMap = state.roomMap
        state.roomMap = null
        state.migrated_roommap_v232 = true
        log.info "[TellaBoomer] Migrated v2.3.1 roomMap to v2.3.2 motionRoomMap"
    }
    
    // Overnight monitoring structures
    if (state.tonightBathroomVisits == null) {
        state.tonightBathroomVisits = []
    }
    if (state.tonightActivities == null) {
        state.tonightActivities = []
    }
    if (state.historicalData == null) {
        state.historicalData = []
    }
    
    // v2.3.0: Whole-house room tracking
    if (state.roomTransitions == null) {
        state.roomTransitions = []
        log.info "[TellaBoomer] Initialized state.roomTransitions"
    }
    if (state.roomActivitySummary == null) {
        state.roomActivitySummary = [:]
        log.info "[TellaBoomer] Initialized state.roomActivitySummary"
    }
    if (state.activeMonitoringProfile == null) {
        state.activeMonitoringProfile = "overnight"
    }
    if (state.lastRoomTransition == null) {
        state.lastRoomTransition = [:]
    }
    
    // Version migration
    def currentVersion = "2.3.2"
    if (state.version != currentVersion) {
        log.info "[TellaBoomer] Migrating from ${state.version ?: 'unknown'} to ${currentVersion}"
        state.version = currentVersion
    }
}

// ============================================================================
// MAIN PAGE
// ============================================================================

def mainPage() {
    ensureStateInitialized()
    
    state.version = "2.3.4"
    
    dynamicPage(name: "mainPage", title: "TellaBoomer Wellness Tracker v${state.version}", install: true, uninstall: true) {
        def setupProgress = getSetupProgress()
        
        section("📋 Setup Progress") {
            paragraph setupProgress.status
            
            if (!setupProgress.sensorsDone) {
                paragraph "<b>Step 1:</b> <span style='color:#f59e0b'>Select Motion Sensors</span> (Required)"
                href "sensorsPage", title: "➡️ Select Motion Sensors", description: "Choose motion sensors for monitoring", state: "incomplete"
            } else {
                paragraph "<b>Step 1:</b> ✅ Motion Sensors: ${settings.motionSensors?.size() ?: 0} sensors"
                href "sensorsPage", title: "Select Motion Sensors", description: "Modify sensor selection", state: "complete"
            }
            
            if (setupProgress.sensorsDone) {
                if (!setupProgress.roomsDone) {
                    paragraph "<b>Step 2:</b> <span style='color:#f59e0b'>Configure Rooms</span> (Required)"
                    href "roomsPage", title: "➡️ Configure Rooms", description: "Map sensors to rooms", state: "incomplete"
                } else {
                    paragraph "<b>Step 2:</b> ✅ Rooms: ${state.rooms?.size() ?: 0} configured"
                    href "roomsPage", title: "Configure Rooms", description: roomsDescription(), state: "complete"
                }
            } else {
                paragraph "<b>Step 2:</b> Configure Rooms (complete Step 1 first)"
            }
            
            if (setupProgress.roomsDone) {
                paragraph "<b>Step 3:</b> Configure Overnight Monitoring"
                href "overnightSettingsPage", title: "🌙 Overnight Settings", description: "Configure monitoring schedule", state: setupProgress.overnightDone ? "complete" : "incomplete"
            }
        }
        
        section("🌙 Overnight Monitoring") {
            def monitoringStatus = state.overnightMonitoringActive ? 
                "<span style='color:#10b981'>● Active</span>" : 
                "<span style='color:#94a3b8'>○ Inactive</span>"
            paragraph "Status: ${monitoringStatus}"
            
            if (state.lastOvernightSummary) {
                paragraph "Last night: ${state.lastOvernightSummary.bathroomVisits} bathroom visits, Risk: ${state.lastOvernightSummary.riskLevel}"
            }
            
            input name: "btnStartOvernight", type: "button", title: "▶️ Start Overnight Monitoring", width: 6
            input name: "btnEndOvernight", type: "button", title: "⏹️ End Overnight Monitoring", width: 6
        }
        
        section("📊 Monitoring Profiles (v2.3.0)") {
            def activeProfile = state.activeMonitoringProfile ?: "overnight"
            def currentMode = location.mode
            paragraph "Active Profile: <b>${activeProfile}</b> | Hub Mode: <b>${currentMode}</b>"
            href "monitoringProfilesPage", title: "⏰ Configure Monitoring Profiles", description: "Define custom time windows and mode triggers"
            
            def transitionCount = state.roomTransitions?.size() ?: 0
            paragraph "Room transitions today: ${transitionCount}"
        }

        section("📊 Dashboard Integration") {
            input "enableDashboard", "bool", title: "Enable Dashboard Integration", defaultValue: true, submitOnChange: true
            if (enableDashboard != false) {
                href "integrationPage", title: "Integration Settings", description: "Configure Maker API and data export"
            }
        }
        
        section("🔔 Notifications") {
            href "notificationsPage", title: "Configure Notifications", description: "Set up alerts and reports"
            href "enhancedNotificationsPage", title: "📱 Enhanced Pushover Messages", description: "Rich narrative notifications"
        }
        
        section("⚙️ Advanced") {
            input "debugLogging", "bool", title: "Enable Debug Logging", defaultValue: true, submitOnChange: true
            href "advancedPage", title: "Advanced Settings", description: "Hub variables, diagnostics"
            href "summaryPage", title: "Configuration Summary", description: "Review your setup"
        }
        
        section("📌 System Info") {
            paragraph "Version: ${state.version ?: '2.3.2'}"
            paragraph "Rooms: ${state.rooms?.size() ?: 0} | Motion: ${state.motionSensorMappings?.size() ?: 0} | Pressure: ${state.pressurePadMappings?.size() ?: 0} | Flush: ${state.flushSensorMappings?.size() ?: 0} | Temp: ${state.tempHumiditySensorMappings?.size() ?: 0}"
            paragraph "Last Update: ${state.lastProcessing ? new Date(state.lastProcessing).format('MM/dd HH:mm') : 'Never'}"
        }
    }
}

// ============================================================================
// SENSORS PAGE v2.3.2 - Added Temperature/Humidity Sensors
// ============================================================================

def sensorsPage() {
    ensureStateInitialized()
    
    dynamicPage(name: "sensorsPage", title: "Step 1: Select Sensors", nextPage: "mainPage") {
        section("Motion Sensors") {
            paragraph "Select ALL motion sensors that should be monitored, including bathroom, bedroom, and common areas."
            input "motionSensors", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: true, submitOnChange: true
        }
        
        if (settings.motionSensors) {
            section("Selected Motion Sensors (${settings.motionSensors.size()})") {
                def sensorInfo = settings.motionSensors.collect { sensor ->
                    def roomName = getSensorRoomName(sensor)
                    "• <b>${sensor.displayName}</b> → <i>${roomName ?: 'No Hubitat room'}</i>"
                }.join("<br>")
                paragraph sensorInfo
            }
        }
        
        section("🪑 Pressure Pads (Optional)") {
            paragraph "Contact sensors used as pressure pads for sitting/occupancy detection."
            paragraph "<small>OPEN = pressure applied (sitting), CLOSED = no pressure (empty)</small>"
            input "pressurePadSensors", "capability.contactSensor", title: "Pressure Pad Sensors", multiple: true, required: false, submitOnChange: true
        }
        
        if (settings.pressurePadSensors) {
            section("Pressure Pad Configuration") {
                settings.pressurePadSensors.each { sensor ->
                    input "pressurePadLocation_${sensor.id}", "text", title: "${sensor.displayName} - Location Name", 
                          description: "e.g., Sofa, Recliner, Toilet Seat", required: false, submitOnChange: true
                }
            }
        }
        
        section("🚽 Toilet Float Sensors (Optional)") {
            paragraph "Water/leak sensors with float switches for counting toilet flushes."
            paragraph "<small>WET = flush detected (tank refilling), DRY = normal state</small>"
            paragraph "<i>Works with Aqara water sensors and other leak detectors with external float switches wired to terminals.</i>"
            input "flushSensors", "capability.waterSensor", title: "Toilet Float Sensors (Water Leak Detectors)", multiple: true, required: false, submitOnChange: true
        }
        
        if (settings.flushSensors) {
            section("Selected Float Sensors (${settings.flushSensors.size()})") {
                settings.flushSensors.each { sensor ->
                    def currentState = sensor.currentValue("water") ?: "unknown"
                    def stateIcon = currentState == "wet" ? "💧" : "✓"
                    paragraph "• <b>${sensor.displayName}</b> → <i>Current: ${stateIcon} ${currentState}</i>"
                }
            }
        }
        
        section("🌡️ Temperature/Humidity Sensors (Optional)") {
            paragraph "Temperature and humidity sensors for environmental monitoring."
            paragraph "<small>Monitors ambient conditions in each room for comfort and safety.</small>"
            input "tempHumiditySensors", "capability.temperatureMeasurement", title: "Temperature/Humidity Sensors", multiple: true, required: false, submitOnChange: true
        }
        
        if (settings.tempHumiditySensors) {
            section("Selected Temperature/Humidity Sensors (${settings.tempHumiditySensors.size()})") {
                settings.tempHumiditySensors.each { sensor ->
                    def temp = sensor.currentValue("temperature") ?: "N/A"
                    def humidity = sensor.hasCapability("RelativeHumidityMeasurement") ? sensor.currentValue("humidity") : null
                    def roomName = getSensorRoomName(sensor)
                    if (humidity) {
                        paragraph "• <b>${sensor.displayName}</b> → ${temp}°F / ${humidity}% RH → <i>${roomName ?: 'No Hubitat room'}</i>"
                    } else {
                        paragraph "• <b>${sensor.displayName}</b> → ${temp}°F → <i>${roomName ?: 'No Hubitat room'}</i>"
                    }
                }
            }
        }
    }
}

// ============================================================================
// ROOMS PAGE v2.3.2 - Display All Sensor Types
// ============================================================================

def roomsPage() {
    ensureStateInitialized()
    
    dynamicPage(name: "roomsPage", title: "Configure Rooms", nextPage: "mainPage") {
        
        section("🔄 Auto-Import from Hubitat") {
            paragraph "Automatically create rooms based on your Hubitat room assignments."
            input name: "btnAutoImport", type: "button", title: "🔄 Auto-Import Rooms from Hubitat"
            paragraph "<small>Reads room assignments from ALL sensor types and creates matching rooms.</small>"
        }
        
        section("📍 Current Rooms (${state.rooms?.size() ?: 0})") {
            if (state.rooms && state.rooms.size() > 0) {
                state.rooms.each { roomId, roomData ->
                    def allSensors = getAllRoomSensors(roomId)
                    def icon = getRoomIcon(roomData.type)
                    
                    def sensorParts = []
                    if (allSensors.motion) sensorParts << "📱 ${allSensors.motion.size()}"
                    if (allSensors.pressurePads) sensorParts << "🪑 ${allSensors.pressurePads.size()}"
                    if (allSensors.flushSensors) sensorParts << "🚽 ${allSensors.flushSensors.size()}"
                    if (allSensors.tempHumidity) sensorParts << "🌡️ ${allSensors.tempHumidity.size()}"
                    
                    def sensorSummary = sensorParts ? sensorParts.join(', ') : '<i>No sensors</i>'
                    
                    href "editRoomPage", title: "${icon} ${roomData.name}", 
                         description: "${roomData.type?.capitalize() ?: 'Other'} | ${sensorSummary}",
                         params: [roomId: roomId]
                }
            } else {
                paragraph "<i>No rooms configured yet.</i>"
                paragraph "Use <b>Auto-Import</b> above or <b>Add Room</b> below to get started."
            }
        }
        
        section("➕ Add Room Manually") {
            href "addRoomPage", title: "➕ Add New Room", description: "Create a room and assign sensors"
        }
        
        section("🗑️ Reset") {
            input name: "btnResetRooms", type: "button", title: "Reset All Rooms"
        }
    }
}

// ============================================================================
// ADD ROOM PAGE
// ============================================================================

def addRoomPage() {
    ensureStateInitialized()
    
    dynamicPage(name: "addRoomPage", title: "Add New Room", nextPage: "roomsPage") {
        section("Room Details") {
            input "newRoomName", "text", title: "Room Name *", required: true, submitOnChange: true
            input "newRoomType", "enum", title: "Room Type *", options: getRoomTypeOptions(), defaultValue: "other", required: true, submitOnChange: true
        }
        
        section("Assign Sensors") {
            def availableSensors = getAvailableMotionSensors(null)
            if (availableSensors) {
                input "newRoomSensors", "enum", title: "Motion Sensors for this room", options: availableSensors, multiple: true, required: false, submitOnChange: true
            } else {
                paragraph "<i>All sensors are already assigned to other rooms, or no sensors selected in Step 1.</i>"
            }
        }
        
        if (settings.newRoomName) {
            section("") {
                paragraph "<b>Ready to save:</b> ${settings.newRoomName} (${settings.newRoomType ?: 'other'})"
                if (settings.newRoomSensors) {
                    paragraph "Sensors: ${settings.newRoomSensors.size()} selected"
                }
                input name: "btnSaveNewRoom", type: "button", title: "💾 Save Room"
            }
        }
    }
}

// ============================================================================
// EDIT ROOM PAGE v2.3.2 - Multi-Sensor Type Assignment
// ============================================================================

def editRoomPage(params) {
    ensureStateInitialized()
    
    def roomId = params?.roomId ?: state.editingRoomId
    if (params?.roomId) {
        state.editingRoomId = params.roomId
    }
    
    def roomData = state.rooms?."${roomId}"
    
    if (!roomData) {
        return dynamicPage(name: "editRoomPage", title: "Room Not Found", nextPage: "roomsPage") {
            section("") {
                paragraph "The requested room was not found. It may have been deleted."
                paragraph "Room ID: ${roomId}"
            }
        }
    }
    
    dynamicPage(name: "editRoomPage", title: "Edit: ${roomData.name}", nextPage: "roomsPage") {
        section("📍 Room Details") {
            input "editRoomName", "text", title: "Room Name", defaultValue: roomData.name, required: true, submitOnChange: true
            input "editRoomType", "enum", title: "Room Type", options: getRoomTypeOptions(), defaultValue: roomData.type ?: "other", required: true, submitOnChange: true
        }
        
        section("📱 Assign Motion Sensors") {
            def currentMotionIds = getRoomMotionSensorIds(roomId)
            def availableMotion = getAvailableMotionSensors(roomId)
            
            if (availableMotion || currentMotionIds) {
                input "editRoomMotionSensors", "enum", title: "Motion Sensors", options: availableMotion, multiple: true, defaultValue: currentMotionIds, submitOnChange: true
            } else {
                paragraph "<i>No motion sensors available.</i>"
            }
        }
        
        section("🪑 Assign Pressure Pads") {
            def currentPressureIds = getRoomPressurePadIds(roomId)
            def availablePressure = getAvailablePressurePads(roomId)
            
            if (availablePressure || currentPressureIds) {
                input "editRoomPressurePads", "enum", title: "Pressure Pad Sensors", options: availablePressure, multiple: true, defaultValue: currentPressureIds, submitOnChange: true
            } else if (settings.pressurePadSensors) {
                paragraph "<i>All pressure pads are assigned to other rooms.</i>"
            } else {
                paragraph "<i>No pressure pads configured. Add them in Sensors Page.</i>"
            }
        }
        
        section("🚽 Assign Flush Sensors") {
            def currentFlushIds = getRoomFlushSensorIds(roomId)
            def availableFlush = getAvailableFlushSensors(roomId)
            
            if (availableFlush || currentFlushIds) {
                input "editRoomFlushSensors", "enum", title: "Toilet Float Sensors", options: availableFlush, multiple: true, defaultValue: currentFlushIds, submitOnChange: true
            } else if (settings.flushSensors) {
                paragraph "<i>All flush sensors are assigned to other rooms.</i>"
            } else {
                paragraph "<i>No flush sensors configured. Add them in Sensors Page.</i>"
            }
        }
        
        section("🌡️ Assign Temperature/Humidity Sensors") {
            def currentTempIds = getRoomTempHumidityIds(roomId)
            def availableTemp = getAvailableTempHumiditySensors(roomId)
            
            if (availableTemp || currentTempIds) {
                input "editRoomTempHumiditySensors", "enum", title: "Temperature/Humidity Sensors", options: availableTemp, multiple: true, defaultValue: currentTempIds, submitOnChange: true
            } else if (settings.tempHumiditySensors) {
                paragraph "<i>All temp/humidity sensors are assigned to other rooms.</i>"
            } else {
                paragraph "<i>No temp/humidity sensors configured. Add them in Sensors Page.</i>"
            }
        }
        
        section("⚙️ Actions") {
            input name: "btnSaveEdit", type: "button", title: "💾 Save Changes", width: 6
            input name: "btnDeleteRoom", type: "button", title: "🗑️ Delete Room", width: 6
        }
        
        section("🔍 Debug Info") {
            paragraph "<small>Room ID: ${roomId}</small>"
            def allSensors = getAllRoomSensors(roomId)
            paragraph "<small>Motion: ${allSensors.motion?.size() ?: 0}, Pressure: ${allSensors.pressurePads?.size() ?: 0}, Flush: ${allSensors.flushSensors?.size() ?: 0}, Temp: ${allSensors.tempHumidity?.size() ?: 0}</small>"
        }
    }
}

// ============================================================================
// OVERNIGHT SETTINGS PAGE
// ============================================================================

def overnightSettingsPage() {
    ensureStateInitialized()
    
    dynamicPage(name: "overnightSettingsPage", title: "🌙 Overnight Monitoring Settings", nextPage: "mainPage") {
        section("Monitoring Schedule") {
            paragraph "Configure when overnight monitoring begins and ends."
            input "overnightStartTime", "time", title: "Monitoring Start Time", defaultValue: "21:00", required: true
            input "overnightEndTime", "time", title: "Monitoring End Time", defaultValue: "08:00", required: true
            input "autoStartOvernight", "bool", title: "Auto-start at scheduled time", defaultValue: true
        }
        
        section("Bedroom Selection") {
            def bedroomOptions = state.rooms?.findAll { k, v -> v.type == "bedroom" }?.collectEntries { k, v -> [(k): v.name] } ?: [:]
            if (bedroomOptions) {
                input "primaryBedroom", "enum", title: "Primary Bedroom", options: bedroomOptions, required: false
            } else {
                paragraph "<i>No bedrooms configured. Add a room with type 'Bedroom' first.</i>"
            }
        }
        
        section("Bathroom Selection") {
            def bathroomOptions = state.rooms?.findAll { k, v -> v.type == "bathroom" }?.collectEntries { k, v -> [(k): v.name] } ?: [:]
            if (bathroomOptions) {
                input "trackedBathrooms", "enum", title: "Bathrooms to Track", options: bathroomOptions, multiple: true, required: false
            } else {
                paragraph "<i>No bathrooms configured. Add a room with type 'Bathroom' first.</i>"
            }
        }
        
        section("Deep Sleep Window") {
            paragraph "The deep sleep window (typically 2-4 AM) is highest risk for falls."
            input "deepSleepStart", "number", title: "Deep Sleep Start Hour (0-23)", defaultValue: 2, range: "0..23"
            input "deepSleepEnd", "number", title: "Deep Sleep End Hour (0-23)", defaultValue: 4, range: "0..23"
        }
        
        section("Duration Thresholds") {
            input "normalVisitDuration", "number", title: "Normal Visit Duration (minutes)", defaultValue: 7, range: "1..30"
            input "extendedVisitDuration", "number", title: "Extended Visit Alert (minutes)", defaultValue: 15, range: "5..60"
        }
    }
}

// ============================================================================
// v2.3.0: MONITORING PROFILES PAGE
// ============================================================================

def monitoringProfilesPage() {
    ensureStateInitialized()
    
    dynamicPage(name: "monitoringProfilesPage", title: "⏰ Monitoring Profiles", nextPage: "mainPage") {
        section("📋 About Monitoring Profiles") {
            paragraph "Define custom time windows beyond overnight monitoring. Each profile tracks activity during specific periods."
            paragraph "<small>Profiles can be triggered by time OR by Hubitat mode changes.</small>"
        }
        
        section("🌙 Overnight Profile (Built-in)") {
            paragraph "Primary overnight monitoring - configured in Overnight Settings"
            paragraph "Start: ${settings.overnightStartTime ?: '21:00'} | End: ${settings.overnightEndTime ?: '08:00'}"
        }
        
        section("☀️ Morning Routine Profile") {
            input "enableMorningProfile", "bool", title: "Enable Morning Routine Tracking", defaultValue: false, submitOnChange: true
            if (settings.enableMorningProfile) {
                input "morningStartTime", "time", title: "Morning Start Time", defaultValue: "06:00", required: true
                input "morningEndTime", "time", title: "Morning End Time", defaultValue: "10:00", required: true
                input "morningTriggerMode", "mode", title: "Or Trigger on Mode Change (Optional)", required: false
            }
        }
        
        section("🌤️ Daytime Activity Profile") {
            input "enableDaytimeProfile", "bool", title: "Enable Daytime Activity Tracking", defaultValue: false, submitOnChange: true
            if (settings.enableDaytimeProfile) {
                input "daytimeStartTime", "time", title: "Daytime Start Time", defaultValue: "10:00", required: true
                input "daytimeEndTime", "time", title: "Daytime End Time", defaultValue: "17:00", required: true
                input "daytimeTriggerMode", "mode", title: "Or Trigger on Mode Change (Optional)", required: false
            }
        }
        
        section("🌆 Evening Wind-Down Profile") {
            input "enableEveningProfile", "bool", title: "Enable Evening Tracking", defaultValue: false, submitOnChange: true
            if (settings.enableEveningProfile) {
                input "eveningStartTime", "time", title: "Evening Start Time", defaultValue: "17:00", required: true
                input "eveningEndTime", "time", title: "Evening End Time", defaultValue: "21:00", required: true
                input "eveningTriggerMode", "mode", title: "Or Trigger on Mode Change (Optional)", required: false
            }
        }
        
        section("🏠 Hubitat Mode Integration") {
            paragraph "Subscribe to location mode changes to automatically switch profiles."
            input "enableModeIntegration", "bool", title: "Enable Mode-Based Profile Switching", defaultValue: true
            paragraph "<small>When enabled, mode changes (Night, Home, Away, etc.) can trigger profile switches.</small>"
        }
        
        section("📊 Whole-House Tracking") {
            paragraph "Track room-to-room transitions throughout the day, regardless of active profile."
            input "enableWholeHouseTracking", "bool", title: "Enable Whole-House Room Tracking", defaultValue: true
            input "transitionRetentionHours", "number", title: "Keep Transition History (hours)", defaultValue: 24, range: "1..168"
            
            def transitionCount = state.roomTransitions?.size() ?: 0
            paragraph "Current transitions stored: ${transitionCount}"
            input name: "btnClearTransitions", type: "button", title: "🗑️ Clear Transition History"
        }
    }
}

// ============================================================================
// OTHER PAGES
// ============================================================================

def detectionSettingsPage() {
    dynamicPage(name: "detectionSettingsPage", title: "Detection Settings", nextPage: "mainPage") {
        section("Risk Thresholds") {
            input "highRiskVisitCount", "number", title: "High Risk: Bathroom Visits ≥", defaultValue: 5, range: "3..10"
            input "moderateRiskVisitCount", "number", title: "Moderate Risk: Bathroom Visits ≥", defaultValue: 3, range: "2..8"
            input "restlessnessHighThreshold", "number", title: "High Restlessness % ≥", defaultValue: 30, range: "10..50"
        }
        
        section("Sedentary Detection") {
            paragraph "Alert when sitting continuously for too long."
            input "sedentaryThreshold", "number", title: "Sedentary Alert Threshold (minutes)", defaultValue: 120, range: "30..300"
        }
    }
}

def notificationsPage() {
    dynamicPage(name: "notificationsPage", title: "Notification Settings", nextPage: "mainPage") {
        section("Alert Notifications") {
            input "enableAlerts", "bool", title: "Enable Alerts", defaultValue: true, submitOnChange: true
            if (enableAlerts != false) {
                input "notifyDevice", "capability.notification", title: "Notification Device (Pushover, etc.)", required: false, multiple: false
                input "alertOnHighRisk", "bool", title: "Alert on High Risk", defaultValue: true
                input "alertOnExtendedVisit", "bool", title: "Alert on Extended Bathroom Visit", defaultValue: true
                input "extendedVisitMinutes", "number", title: "Extended Visit Threshold (minutes)", defaultValue: 15, range: "5..60"
            }
        }
        
        section("Morning Summary") {
            input "enableMorningSummary", "bool", title: "Send Morning Summary", defaultValue: true, submitOnChange: true
            if (enableMorningSummary != false) {
                input "summaryTime", "time", title: "Summary Time", defaultValue: "07:00"
                input "summaryDevice", "capability.notification", title: "Summary Notification Device", required: false, multiple: false
                paragraph "<small>Leave blank to use the same device as alerts</small>"
            }
        }
    }
}

def enhancedNotificationsPage() {
    dynamicPage(name: "enhancedNotificationsPage", title: "📱 Enhanced Pushover Notifications", nextPage: "mainPage") {
        section("📨 Notification Style") {
            paragraph "Choose how wellness data is presented in your Pushover messages."
            input "notificationStyle", "enum", title: "Message Style", 
                  options: ["standard": "Standard (Simple)", "rich": "Rich (Detailed + Emojis)", "narrative": "Narrative (Storytelling)"],
                  defaultValue: "rich", submitOnChange: true
            
            paragraph "<small><b>Standard:</b> Basic alerts with essential info\n<b>Rich:</b> Detailed data with emojis and formatting (RECOMMENDED)\n<b>Narrative:</b> Story-driven format with chapters and insights</small>"
        }
        
        section("🔔 Enhanced Alert Types") {
            input "enableEnhancedAlerts", "bool", title: "Enable Enhanced Alerts", defaultValue: true, submitOnChange: true
            
            if (enableEnhancedAlerts) {
                input "alertOnDeepSleep", "bool", title: "🌙 Alert on Deep Sleep Activity (2-4 AM)", defaultValue: true
                paragraph "<small>Notifies when bathroom visits occur during high fall-risk hours</small>"
                
                input "enableFollowUp", "bool", title: "⚠️ Send Follow-Up Check (Extended Visits)", defaultValue: true
                paragraph "<small>Sends a follow-up alert 5 minutes after initial extended visit warning</small>"
                
                input "alertOnSedentary", "bool", title: "🪑 Alert on Prolonged Sitting", defaultValue: true
                input "enhancedHighRisk", "bool", title: "🔴 Enhanced High Risk Alerts", defaultValue: true
                paragraph "<small>Provides detailed concern list and recommendations</small>"
            }
        }
        
        section("☀️ Enhanced Morning Summary") {
            input "enableEnhancedMorningSummary", "bool", title: "Enable Enhanced Morning Summary", defaultValue: true, submitOnChange: true
            
            if (enableEnhancedMorningSummary) {
                input "summaryStyle", "enum", title: "Summary Format",
                      options: ["data": "Data-Focused (Metrics & Numbers)", 
                               "narrative": "Narrative Journey (Storytelling)", 
                               "both": "Both Styles (2 Messages)"],
                      defaultValue: "data"
                paragraph "<small><b>Data-Focused:</b> Organized sections with all metrics\n<b>Narrative:</b> Story chapters with interpretive insights\n<b>Both:</b> Sends data summary first, then narrative (2 separate messages)</small>"
            }
        }
        
        section("📈 Periodic Reports") {
            input "enableWeeklyTrends", "bool", title: "📊 Send Weekly Trends Report", defaultValue: false, submitOnChange: true
            if (enableWeeklyTrends) {
                input "weeklyReportDay", "enum", title: "Report Day", 
                      options: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"],
                      defaultValue: "Sunday"
                input "weeklyReportTime", "time", title: "Report Time", defaultValue: "20:00"
                paragraph "<small>Provides 7-day averages, risk distribution, and trend analysis</small>"
            }
            
            input "enableDailyCheckIn", "bool", title: "🔔 Send Daily Status Check-In", defaultValue: false, submitOnChange: true
            if (enableDailyCheckIn) {
                input "checkInTime", "time", title: "Check-In Time", defaultValue: "14:00"
                paragraph "<small>Midday status update with current location and activity</small>"
            }
        }
        
        section("🔗 Dashboard Integration") {
            paragraph "<b>Make Your Notifications Interactive!</b>"
            paragraph "Add a clickable button to every notification that opens your wellness dashboard."
            
            input "dashboardUrl", "text", 
                  title: "Dashboard URL (Optional)", 
                  description: "e.g., http://192.168.1.123/dashboard",
                  required: false
            
            input "dashboardLinkText", "text",
                  title: "Link Button Text",
                  description: "Text shown on clickable button",
                  defaultValue: "View Dashboard",
                  required: false
            
            paragraph "<small><b>How it works:</b> Notifications will include a clickable button using your Pushover driver's Supplementary URL feature. Tapping the button opens your dashboard directly.</small>"
            paragraph "<small><b>Example URL:</b> http://192.168.1.123:8080/local/wellness-dashboard</small>"
        }
        
        section("🔊 Notification Sounds") {
            paragraph "Customize sounds for different types of alerts. Uses Pushover driver sound options."
            
            input "soundMorningSummary", "enum", title: "Morning Summary Sound",
                  options: getPushoverSounds(),
                  defaultValue: "cosmic",
                  required: false
            
            input "soundHighRisk", "enum", title: "High Risk Alert Sound",
                  options: getPushoverSounds(),
                  defaultValue: "siren",
                  required: false
            
            input "soundExtendedVisit", "enum", title: "Extended Visit Sound",
                  options: getPushoverSounds(),
                  defaultValue: "persistent",
                  required: false
            
            input "soundDeepSleep", "enum", title: "Deep Sleep Alert Sound",
                  options: getPushoverSounds(),
                  defaultValue: "none",
                  required: false
            
            input "soundSedentary", "enum", title: "Sedentary Alert Sound",
                  options: getPushoverSounds(),
                  defaultValue: "magic",
                  required: false
            
            paragraph "<small>Sounds are played via your Pushover device. 'none' means vibrate only (good for night alerts).</small>"
        }
        
        section("🧪 Testing") {
            paragraph "Test your notification formats before enabling them."
            input name: "btnTestMorningSummary", type: "button", title: "Test Morning Summary", width: 4
            input name: "btnTestNarrative", type: "button", title: "Test Narrative Journey", width: 4
            input name: "btnTestWeekly", type: "button", title: "Test Weekly Trends", width: 4
        }
        
        section("ℹ️ About Enhanced Notifications") {
            paragraph """<small>Enhanced notifications provide rich, informative messages with:
• <b>Emojis</b> for quick visual scanning (✅⚠️🔴🌙😴🚿🪑)
• <b>Baseline comparisons</b> showing trends (📈📉➡️)
• <b>Organized sections</b> for easy reading on mobile
• <b>Contextual insights</b> explaining what metrics mean
• <b>Actionable recommendations</b> for high-risk situations
• <b>Clickable dashboard links</b> for quick access
• <b>Custom sounds</b> per notification type
• <b>Priority levels</b> (silent, low, normal, high)

All messages respect your privacy and present data with dignity.</small>"""
        }
    }
}

def integrationPage() {
    dynamicPage(name: "integrationPage", title: "Integration Settings", nextPage: "mainPage") {
        section("Maker API Access") {
            paragraph "Your dashboard can access data via these endpoints:"
            paragraph "<b>Hub IP:</b> ${location.hub.localIP}"
            
            if (state.accessToken) {
                paragraph "<b>Access Token:</b> ${state.accessToken}"
                paragraph "<b>Wellness Data Endpoint:</b>"
                paragraph "<code>${getApiServerUrl()}/${app.id}/wellnessData?access_token=${state.accessToken}</code>"
            } else {
                paragraph "<i>OAuth not enabled. Click button below to create token.</i>"
                input name: "btnEnableOAuth", type: "button", title: "🔑 Create Access Token"
            }
        }
        
        section("🌐 CORS Settings (Cross-Origin Access)") {
            paragraph "Enable CORS to allow external dashboards, SharpTools, or remote access."
            input "enableCors", "bool", title: "Enable CORS Headers", defaultValue: true, submitOnChange: true
            if (enableCors != false) {
                input "corsAllowedOrigins", "text", title: "Allowed Origins", 
                      description: "Comma-separated list (e.g., https://cloud.sharptools.io, https://claude.ai) or * for all",
                      defaultValue: "*", required: false
                paragraph "<small>Use <b>*</b> to allow all origins (development mode) or specify exact origins for production.</small>"
            }
        }
        
        section("Update Settings") {
            input "updateIntervalMinutes", "number", title: "Hub Variable Update Interval (minutes)", defaultValue: 15, range: "1..60"
            input "enableRealtimeUpdates", "bool", title: "Enable Real-time Updates", defaultValue: false
        }
    }
}

def advancedPage() {
    ensureStateInitialized()
    
    dynamicPage(name: "advancedPage", title: "Advanced Settings", nextPage: "mainPage") {
        section("Hub Variables") {
            paragraph "This app writes to 51 hub variables for dashboard integration."
            input name: "btnValidateVars", type: "button", title: "✅ Validate Hub Variables"
        }
        
        section("Data Management") {
            input "dataRetentionDays", "number", title: "Data Retention (days)", defaultValue: 30, range: "7..90"
            input name: "btnClearData", type: "button", title: "🗑️ Clear Overnight Data"
        }
        
        section("Current State") {
            paragraph "• <b>Rooms:</b> ${state.rooms?.size() ?: 0}"
            paragraph "• <b>Motion Sensors:</b> ${state.motionSensorMappings?.size() ?: 0}"
            paragraph "• <b>Pressure Pads:</b> ${state.pressurePadMappings?.size() ?: 0}"
            paragraph "• <b>Flush Sensors:</b> ${state.flushSensorMappings?.size() ?: 0}"
            paragraph "• <b>Temp/Humidity:</b> ${state.tempHumiditySensorMappings?.size() ?: 0}"
            paragraph "• <b>Overnight Active:</b> ${state.overnightMonitoringActive ?: false}"
            paragraph "• <b>Tonight's Visits:</b> ${state.tonightBathroomVisits?.size() ?: 0}"
            paragraph "• <b>Days Monitored:</b> ${state.daysMonitored ?: 0}"
        }
        
        section("Diagnostics") {
            input name: "btnRunDiagnostics", type: "button", title: "🔍 Run Full Diagnostics"
        }
        
        section("Room Details") {
            if (state.rooms && state.rooms.size() > 0) {
                state.rooms.each { roomId, roomData ->
                    def allSensors = getAllRoomSensors(roomId)
                    def summary = []
                    if (allSensors.motion) summary << "📱${allSensors.motion.size()}"
                    if (allSensors.pressurePads) summary << "🪑${allSensors.pressurePads.size()}"
                    if (allSensors.flushSensors) summary << "🚽${allSensors.flushSensors.size()}"
                    if (allSensors.tempHumidity) summary << "🌡️${allSensors.tempHumidity.size()}"
                    paragraph "• <b>${roomData.name}</b> (${roomData.type}): ${summary.join(' ')}"
                }
            } else {
                paragraph "<i>No rooms in state</i>"
            }
        }
    }
}


// ============================================================================
// SUMMARY PAGE v2.3.2 - Show All Sensor Types
// ============================================================================

def summaryPage() {
    ensureStateInitialized()
    
    dynamicPage(name: "summaryPage", title: "Configuration Summary", nextPage: "mainPage") {
        section("Motion Sensors") {
            paragraph "<b>Motion Sensors:</b> ${settings.motionSensors?.size() ?: 0}"
            if (settings.motionSensors) {
                paragraph settings.motionSensors.collect { "• ${it.displayName}" }.join("<br>")
            }
        }
        
        section("Pressure Pads") {
            paragraph "<b>Pressure Pads:</b> ${settings.pressurePadSensors?.size() ?: 0}"
            if (settings.pressurePadSensors) {
                settings.pressurePadSensors.each { sensor ->
                    def location = settings."pressurePadLocation_${sensor.id}" ?: "Not named"
                    paragraph "• ${sensor.displayName} → <i>${location}</i>"
                }
            }
        }
        
        section("Toilet Float Sensors") {
            paragraph "<b>Water/Leak Sensors (Float):</b> ${settings.flushSensors?.size() ?: 0}"
            if (settings.flushSensors) {
                settings.flushSensors.each { sensor ->
                    def currentState = sensor.currentValue("water") ?: "unknown"
                    def stateIcon = currentState == "wet" ? "💧" : "✓"
                    paragraph "• <b>${sensor.displayName}</b> → <i>Current: ${stateIcon} ${currentState}</i>"
                }
            }
        }
        
        section("Temperature/Humidity Sensors") {
            paragraph "<b>Temp/Humidity Sensors:</b> ${settings.tempHumiditySensors?.size() ?: 0}"
            if (settings.tempHumiditySensors) {
                settings.tempHumiditySensors.each { sensor ->
                    def temp = sensor.currentValue("temperature") ?: "N/A"
                    def humidity = sensor.hasCapability("RelativeHumidityMeasurement") ? sensor.currentValue("humidity") : null
                    if (humidity) {
                        paragraph "• <b>${sensor.displayName}</b> → ${temp}°F / ${humidity}% RH"
                    } else {
                        paragraph "• <b>${sensor.displayName}</b> → ${temp}°F"
                    }
                }
            }
        }
        
        section("Rooms & Sensor Assignments") {
            if (state.rooms && state.rooms.size() > 0) {
                state.rooms.each { roomId, roomData ->
                    def allSensors = getAllRoomSensors(roomId)
                    def icon = getRoomIcon(roomData.type)
                    
                    paragraph "<b>${icon} ${roomData.name}</b> (${roomData.type})"
                    
                    if (allSensors.motion) {
                        paragraph "  📱 Motion: ${allSensors.motion.collect{it.displayName}.join(', ')}"
                    }
                    if (allSensors.pressurePads) {
                        paragraph "  🪑 Pressure: ${allSensors.pressurePads.collect{settings."pressurePadLocation_${it.id}" ?: it.displayName}.join(', ')}"
                    }
                    if (allSensors.flushSensors) {
                        paragraph "  🚽 Flush: ${allSensors.flushSensors.collect{it.displayName}.join(', ')}"
                    }
                    if (allSensors.tempHumidity) {
                        paragraph "  🌡️ Temp: ${allSensors.tempHumidity.collect{it.displayName}.join(', ')}"
                    }
                    
                    if (!allSensors.motion && !allSensors.pressurePads && !allSensors.flushSensors && !allSensors.tempHumidity) {
                        paragraph "  <i>No sensors assigned</i>"
                    }
                }
            } else {
                paragraph "<i>No rooms configured</i>"
            }
        }
        
        section("Overnight Settings") {
            paragraph "<b>Start Time:</b> ${settings.overnightStartTime ?: 'Not set'}"
            paragraph "<b>End Time:</b> ${settings.overnightEndTime ?: 'Not set'}"
            paragraph "<b>Primary Bedroom:</b> ${settings.primaryBedroom ? state.rooms[settings.primaryBedroom]?.name : 'Not set'}"
            paragraph "<b>Tracked Bathrooms:</b> ${settings.trackedBathrooms ? settings.trackedBathrooms.collect { state.rooms[it]?.name }.join(', ') : 'Not set'}"
            paragraph "<b>Sedentary Alert:</b> ${settings.sedentaryThreshold ?: 120} minutes"
        }
        
        section("Integration Settings") {
            paragraph "<b>CORS Enabled:</b> ${settings.enableCors != false ? 'Yes ✅' : 'No'}"
            if (settings.enableCors != false) {
                paragraph "<b>Allowed Origins:</b> ${settings.corsAllowedOrigins ?: '* (all)'}"
            }
            paragraph "<b>Access Token:</b> ${state.accessToken ? 'Configured ✅' : 'Not set'}"
        }
    }
}

// ============================================================================
// v2.3.2: MULTI-SENSOR HELPER FUNCTIONS
// ============================================================================

def getRoomMotionSensors(roomId) {
    if (!state.motionSensorMappings || !settings.motionSensors) return []
    
    def sensorIds = state.motionSensorMappings.findAll { sId, rId -> rId == roomId }.keySet()
    return sensorIds.collect { sensorId -> 
        settings.motionSensors?.find { it.id.toString() == sensorId.toString() } 
    }.findAll { it != null }
}

def getRoomPressurePads(roomId) {
    if (!state.pressurePadMappings || !settings.pressurePadSensors) return []
    
    def sensorIds = state.pressurePadMappings.findAll { sId, rId -> rId == roomId }.keySet()
    return sensorIds.collect { sensorId -> 
        settings.pressurePadSensors?.find { it.id.toString() == sensorId.toString() } 
    }.findAll { it != null }
}

def getRoomFlushSensors(roomId) {
    if (!state.flushSensorMappings || !settings.flushSensors) return []
    
    def sensorIds = state.flushSensorMappings.findAll { sId, rId -> rId == roomId }.keySet()
    return sensorIds.collect { sensorId -> 
        settings.flushSensors?.find { it.id.toString() == sensorId.toString() } 
    }.findAll { it != null }
}

def getRoomTempHumiditySensors(roomId) {
    if (!state.tempHumiditySensorMappings || !settings.tempHumiditySensors) return []
    
    def sensorIds = state.tempHumiditySensorMappings.findAll { sId, rId -> rId == roomId }.keySet()
    return sensorIds.collect { sensorId -> 
        settings.tempHumiditySensors?.find { it.id.toString() == sensorId.toString() } 
    }.findAll { it != null }
}

def getAllRoomSensors(roomId) {
    def allSensors = [
        motion: getRoomMotionSensors(roomId),
        pressurePads: getRoomPressurePads(roomId),
        flushSensors: getRoomFlushSensors(roomId),
        tempHumidity: getRoomTempHumiditySensors(roomId)
    ]
    return allSensors
}

def getRoomMotionSensorIds(roomId) {
    if (!state.motionSensorMappings) return []
    return state.motionSensorMappings.findAll { sId, rId -> rId == roomId }.keySet().collect { it.toString() }
}

def getRoomPressurePadIds(roomId) {
    if (!state.pressurePadMappings) return []
    return state.pressurePadMappings.findAll { sId, rId -> rId == roomId }.keySet().collect { it.toString() }
}

def getRoomFlushSensorIds(roomId) {
    if (!state.flushSensorMappings) return []
    return state.flushSensorMappings.findAll { sId, rId -> rId == roomId }.keySet().collect { it.toString() }
}

def getRoomTempHumidityIds(roomId) {
    if (!state.tempHumiditySensorMappings) return []
    return state.tempHumiditySensorMappings.findAll { sId, rId -> rId == roomId }.keySet().collect { it.toString() }
}

def getAvailableMotionSensors(excludeRoomId) {
    if (!settings.motionSensors) return [:]
    
    def assignedSensorIds = []
    state.motionSensorMappings?.each { sensorId, roomId ->
        if (roomId != excludeRoomId) {
            assignedSensorIds << sensorId.toString()
        }
    }
    
    def options = [:]
    settings.motionSensors.each { sensor ->
        def sensorId = sensor.id.toString()
        if (!assignedSensorIds.contains(sensorId)) {
            options[sensorId] = sensor.displayName
        }
    }
    
    if (excludeRoomId) {
        state.motionSensorMappings?.each { sensorId, roomId ->
            if (roomId == excludeRoomId) {
                def sensor = settings.motionSensors?.find { it.id.toString() == sensorId.toString() }
                if (sensor) {
                    options[sensorId.toString()] = sensor.displayName
                }
            }
        }
    }
    
    return options
}

def getAvailablePressurePads(excludeRoomId) {
    if (!settings.pressurePadSensors) return [:]
    
    def assignedSensorIds = []
    state.pressurePadMappings?.each { sensorId, roomId ->
        if (roomId != excludeRoomId) {
            assignedSensorIds << sensorId.toString()
        }
    }
    
    def options = [:]
    settings.pressurePadSensors.each { sensor ->
        def sensorId = sensor.id.toString()
        if (!assignedSensorIds.contains(sensorId)) {
            def location = settings."pressurePadLocation_${sensor.id}" ?: sensor.displayName
            options[sensorId] = location
        }
    }
    
    if (excludeRoomId) {
        state.pressurePadMappings?.each { sensorId, roomId ->
            if (roomId == excludeRoomId) {
                def sensor = settings.pressurePadSensors?.find { it.id.toString() == sensorId.toString() }
                if (sensor) {
                    def location = settings."pressurePadLocation_${sensor.id}" ?: sensor.displayName
                    options[sensorId.toString()] = location
                }
            }
        }
    }
    
    return options
}

def getAvailableFlushSensors(excludeRoomId) {
    if (!settings.flushSensors) return [:]
    
    def assignedSensorIds = []
    state.flushSensorMappings?.each { sensorId, roomId ->
        if (roomId != excludeRoomId) {
            assignedSensorIds << sensorId.toString()
        }
    }
    
    def options = [:]
    settings.flushSensors.each { sensor ->
        def sensorId = sensor.id.toString()
        if (!assignedSensorIds.contains(sensorId)) {
            options[sensorId] = sensor.displayName
        }
    }
    
    if (excludeRoomId) {
        state.flushSensorMappings?.each { sensorId, roomId ->
            if (roomId == excludeRoomId) {
                def sensor = settings.flushSensors?.find { it.id.toString() == sensorId.toString() }
                if (sensor) {
                    options[sensorId.toString()] = sensor.displayName
                }
            }
        }
    }
    
    return options
}

def getAvailableTempHumiditySensors(excludeRoomId) {
    if (!settings.tempHumiditySensors) return [:]
    
    def assignedSensorIds = []
    state.tempHumiditySensorMappings?.each { sensorId, roomId ->
        if (roomId != excludeRoomId) {
            assignedSensorIds << sensorId.toString()
        }
    }
    
    def options = [:]
    settings.tempHumiditySensors.each { sensor ->
        def sensorId = sensor.id.toString()
        if (!assignedSensorIds.contains(sensorId)) {
            options[sensorId] = sensor.displayName
        }
    }
    
    if (excludeRoomId) {
        state.tempHumiditySensorMappings?.each { sensorId, roomId ->
            if (roomId == excludeRoomId) {
                def sensor = settings.tempHumiditySensors?.find { it.id.toString() == sensorId.toString() }
                if (sensor) {
                    options[sensorId.toString()] = sensor.displayName
                }
            }
        }
    }
    
    return options
}

def updateRoomMaps() {
    state.motionRoomMap = [:]
    state.pressurePadRoomMap = [:]
    state.flushSensorRoomMap = [:]
    state.tempHumidityRoomMap = [:]
    
    state.motionSensorMappings?.each { sensorId, roomId ->
        state.motionRoomMap[sensorId.toString()] = roomId
    }
    state.pressurePadMappings?.each { sensorId, roomId ->
        state.pressurePadRoomMap[sensorId.toString()] = roomId
    }
    state.flushSensorMappings?.each { sensorId, roomId ->
        state.flushSensorRoomMap[sensorId.toString()] = roomId
    }
    state.tempHumiditySensorMappings?.each { sensorId, roomId ->
        state.tempHumidityRoomMap[sensorId.toString()] = roomId
    }
    
    log.debug "[TellaBoomer] Room maps updated - Motion: ${state.motionRoomMap.size()}, Pressure: ${state.pressurePadRoomMap.size()}, Flush: ${state.flushSensorRoomMap.size()}, Temp: ${state.tempHumidityRoomMap.size()}"
}

// ============================================================================
// ROOM HELPER FUNCTIONS
// ============================================================================

def getRoomTypeOptions() {
    return [
        "bedroom": "🛏️ Bedroom",
        "bathroom": "🚿 Bathroom", 
        "living": "🛋️ Living Room",
        "kitchen": "🍳 Kitchen",
        "dining": "🍽️ Dining Room",
        "office": "💼 Office",
        "hallway": "🚶 Hallway",
        "stairs": "📶 Stairs",
        "entry": "🚪 Entry/Foyer",
        "other": "📍 Other"
    ]
}

def getRoomIcon(type) {
    def icons = [
        "bedroom": "🛏️", "bathroom": "🚿", "living": "🛋️", "kitchen": "🍳",
        "dining": "🍽️", "office": "💼", "hallway": "🚶", "stairs": "📶",
        "entry": "🚪", "other": "📍"
    ]
    return icons[type] ?: "📍"
}

def getSensorRoomName(sensor) {
    try {
        if (sensor.device?.roomName) {
            return sensor.device.roomName
        }
        
        def roomId = sensor.device?.roomId
        if (roomId) {
            def room = location.rooms?.find { it.id == roomId }
            if (room?.name) return room.name
        }
        
        def dataRoomId = sensor.device?.getDataValue("roomId")
        if (dataRoomId) {
            def room = location.rooms?.find { it.id.toString() == dataRoomId.toString() }
            if (room?.name) return room.name
        }
        
        def deviceName = sensor.displayName?.toLowerCase()
        if (deviceName) {
            if (deviceName.contains("bathroom") || deviceName.contains("bath ")) return "Bathroom"
            if (deviceName.contains("bedroom") || deviceName.contains("bed ")) return "Bedroom"
            if (deviceName.contains("living")) return "Living Room"
            if (deviceName.contains("kitchen")) return "Kitchen"
            if (deviceName.contains("hallway") || deviceName.contains("hall ")) return "Hallway"
            if (deviceName.contains("office")) return "Office"
            if (deviceName.contains("mbr")) return "Master Bedroom"
            if (deviceName.contains("mbath")) return "Master Bath"
        }
        
    } catch (e) {
        log.debug "[TellaBoomer] Could not get room for ${sensor.displayName}: ${e.message}"
    }
    return null
}

def detectRoomType(roomName) {
    if (!roomName) return "other"
    def name = roomName.toLowerCase()
    
    if (name.contains("bath") || name.contains("shower") || name.contains("toilet") || name.contains("wc")) return "bathroom"
    if (name.contains("bed") || name.contains("master") || name.contains("mbr") || name.contains("guest room")) return "bedroom"
    if (name.contains("living") || name.contains("family") || name.contains("great room") || name.contains("den")) return "living"
    if (name.contains("kitchen") || name.contains("pantry")) return "kitchen"
    if (name.contains("dining")) return "dining"
    if (name.contains("office") || name.contains("study") || name.contains("work")) return "office"
    if (name.contains("hall") || name.contains("corridor")) return "hallway"
    if (name.contains("stair")) return "stairs"
    if (name.contains("entry") || name.contains("foyer") || name.contains("mudroom") || name.contains("front door")) return "entry"
    
    return "other"
}

def getSetupProgress() {
    def sensorsDone = settings.motionSensors?.size() > 0
    def roomsDone = state.rooms?.size() > 0 && state.motionSensorMappings?.size() > 0
    def overnightDone = settings.overnightStartTime && settings.overnightEndTime
    
    def status = ""
    if (!sensorsDone) {
        status = "⚠️ <b>Please select motion sensors to begin</b>"
    } else if (!roomsDone) {
        status = "⚠️ <b>Please configure rooms</b>"
    } else if (!overnightDone) {
        status = "⚠️ <b>Please configure overnight monitoring schedule</b>"
    } else {
        status = "✅ <b>Setup complete! Ready to monitor.</b>"
    }
    
    return [
        sensorsDone: sensorsDone,
        roomsDone: roomsDone,
        overnightDone: overnightDone,
        status: status
    ]
}

def roomsDescription() {
    def count = state.rooms?.size() ?: 0
    def sensorCount = state.motionSensorMappings?.size() ?: 0
    return "${count} rooms, ${sensorCount} sensors mapped"
}

// ============================================================================
// BUTTON HANDLER
// ============================================================================

def appButtonHandler(evt) {
    def buttonName = evt.value
    if (buttonName instanceof byte[]) {
        buttonName = new String(buttonName)
    } else {
        buttonName = buttonName?.toString()
    }
    
    log.info "[TellaBoomer] Button pressed: '${buttonName}'"
    
    if (!buttonName) {
        log.warn "[TellaBoomer] Button name is null or empty"
        return
    }
    
    switch(buttonName) {
        case "btnStartOvernight":
            startOvernightMonitoring()
            break
        case "btnEndOvernight":
            endOvernightMonitoring()
            break
        case "btnAutoImport":
            autoImportRoomsFromHubitat()
            break
        case "btnResetRooms":
            resetAllRooms()
            break
        case "btnSaveNewRoom":
            doSaveNewRoom()
            break
        case "btnSaveEdit":
            doSaveRoomEdit()
            break
        case "btnDeleteRoom":
            doDeleteRoom()
            break
        case "btnValidateVars":
            validateHubVariables()
            break
        case "btnClearData":
            clearOvernightData()
            break
        case "btnRunDiagnostics":
            runDiagnostics()
            break
        case "btnEnableOAuth":
            createOAuthToken()
            break
        case "btnClearTransitions":
            state.roomTransitions = []
            state.roomActivitySummary = [:]
            state.lastRoomTransition = [:]
            log.info "[TellaBoomer] Room transition history cleared"
            break
        case "btnTestMorningSummary":
            sendEnhancedMorningSummary()
            break
        case "btnTestNarrative":
            sendNarrativeJourney()
            break
        case "btnTestWeekly":
            sendWeeklyTrendsReport()
            break
        default:
            log.warn "[TellaBoomer] Unhandled button: ${buttonName}"
    }
}

// ============================================================================
// ROOM CRUD OPERATIONS v2.3.2 - Multi-Sensor Support
// ============================================================================

def doSaveNewRoom() {
    log.info "[TellaBoomer] doSaveNewRoom() called"
    
    if (!settings.newRoomName) {
        log.warn "[TellaBoomer] Cannot save room: No name provided"
        return
    }
    
    ensureStateInitialized()
    
    def roomId = "room_${now()}"
    def roomData = [
        id: roomId,
        name: settings.newRoomName,
        type: settings.newRoomType ?: "other",
        created: now()
    ]
    
    state.rooms[roomId] = roomData
    log.info "[TellaBoomer] Created room: ${roomData.name} (${roomData.type}) with ID: ${roomId}"
    
    if (settings.newRoomSensors) {
        settings.newRoomSensors.each { sensorId ->
            def sid = sensorId.toString()
            state.motionSensorMappings[sid] = roomId
            log.info "[TellaBoomer] Mapped motion sensor ${sid} to room ${roomId}"
        }
    }
    
    updateRoomMaps()
    
    app.removeSetting("newRoomName")
    app.removeSetting("newRoomType")
    app.removeSetting("newRoomSensors")
    
    log.info "[TellaBoomer] Room '${roomData.name}' saved successfully!"
}

def doSaveRoomEdit() {
    def roomId = state.editingRoomId
    log.info "[TellaBoomer] doSaveRoomEdit() called for room: ${roomId}"
    
    if (!roomId || !state.rooms?."${roomId}") {
        log.warn "[TellaBoomer] Cannot save: Room not found (${roomId})"
        return
    }
    
    if (settings.editRoomName) {
        state.rooms[roomId].name = settings.editRoomName
    }
    if (settings.editRoomType) {
        state.rooms[roomId].type = settings.editRoomType
    }
    
    log.info "[TellaBoomer] Updated room: ${state.rooms[roomId].name} (${state.rooms[roomId].type})"
    
    state.motionSensorMappings = state.motionSensorMappings?.findAll { k, v -> v != roomId } ?: [:]
    state.pressurePadMappings = state.pressurePadMappings?.findAll { k, v -> v != roomId } ?: [:]
    state.flushSensorMappings = state.flushSensorMappings?.findAll { k, v -> v != roomId } ?: [:]
    state.tempHumiditySensorMappings = state.tempHumiditySensorMappings?.findAll { k, v -> v != roomId } ?: [:]
    
    if (settings.editRoomMotionSensors) {
        settings.editRoomMotionSensors.each { sensorId ->
            def sid = sensorId.toString()
            state.motionSensorMappings[sid] = roomId
            log.info "[TellaBoomer] Mapped motion sensor ${sid} to room ${roomId}"
        }
    }
    
    if (settings.editRoomPressurePads) {
        settings.editRoomPressurePads.each { sensorId ->
            def sid = sensorId.toString()
            state.pressurePadMappings[sid] = roomId
            log.info "[TellaBoomer] Mapped pressure pad ${sid} to room ${roomId}"
        }
    }
    
    if (settings.editRoomFlushSensors) {
        settings.editRoomFlushSensors.each { sensorId ->
            def sid = sensorId.toString()
            state.flushSensorMappings[sid] = roomId
            log.info "[TellaBoomer] Mapped flush sensor ${sid} to room ${roomId}"
        }
    }
    
    if (settings.editRoomTempHumiditySensors) {
        settings.editRoomTempHumiditySensors.each { sensorId ->
            def sid = sensorId.toString()
            state.tempHumiditySensorMappings[sid] = roomId
            log.info "[TellaBoomer] Mapped temp/humidity sensor ${sid} to room ${roomId}"
        }
    }
    
    updateRoomMaps()
    
    app.removeSetting("editRoomName")
    app.removeSetting("editRoomType")
    app.removeSetting("editRoomMotionSensors")
    app.removeSetting("editRoomPressurePads")
    app.removeSetting("editRoomFlushSensors")
    app.removeSetting("editRoomTempHumiditySensors")
    
    log.info "[TellaBoomer] Room edit saved. Total mappings - Motion: ${state.motionSensorMappings.size()}, Pressure: ${state.pressurePadMappings.size()}, Flush: ${state.flushSensorMappings.size()}, Temp: ${state.tempHumiditySensorMappings.size()}"
}

def doDeleteRoom() {
    def roomId = state.editingRoomId
    log.info "[TellaBoomer] doDeleteRoom() called for room: ${roomId}"
    
    if (!roomId || !state.rooms?."${roomId}") {
        log.warn "[TellaBoomer] Cannot delete: Room not found"
        return
    }
    
    def roomName = state.rooms[roomId].name
    state.rooms.remove(roomId)
    
    state.motionSensorMappings = state.motionSensorMappings?.findAll { k, v -> v != roomId } ?: [:]
    state.pressurePadMappings = state.pressurePadMappings?.findAll { k, v -> v != roomId } ?: [:]
    state.flushSensorMappings = state.flushSensorMappings?.findAll { k, v -> v != roomId } ?: [:]
    state.tempHumiditySensorMappings = state.tempHumiditySensorMappings?.findAll { k, v -> v != roomId } ?: [:]
    
    updateRoomMaps()
    state.editingRoomId = null
    
    app.removeSetting("editRoomName")
    app.removeSetting("editRoomType")
    app.removeSetting("editRoomMotionSensors")
    app.removeSetting("editRoomPressurePads")
    app.removeSetting("editRoomFlushSensors")
    app.removeSetting("editRoomTempHumiditySensors")
    
    log.info "[TellaBoomer] Deleted room: ${roomName} and all associated sensor mappings"
}

def resetAllRooms() {
    log.info "[TellaBoomer] Resetting all room configurations"
    state.rooms = [:]
    state.motionSensorMappings = [:]
    state.pressurePadMappings = [:]
    state.flushSensorMappings = [:]
    state.tempHumiditySensorMappings = [:]
    state.motionRoomMap = [:]
    state.pressurePadRoomMap = [:]
    state.flushSensorRoomMap = [:]
    state.tempHumidityRoomMap = [:]
    log.info "[TellaBoomer] All rooms and sensor mappings reset"
}


// ============================================================================
// AUTO-IMPORT v2.3.2 - Multi-Sensor Type Support
// ============================================================================

def autoImportRoomsFromHubitat() {
    log.info "[TellaBoomer] Auto-importing rooms from Hubitat with multi-sensor support..."
    
    ensureStateInitialized()
    
    def importedCount = 0
    def roomsCreated = [:]
    
    if (settings.motionSensors) {
        log.info "[TellaBoomer] Processing ${settings.motionSensors.size()} motion sensors..."
        settings.motionSensors.each { sensor ->
            def roomName = getSensorRoomName(sensor)
            
            if (!roomName) {
                log.debug "[TellaBoomer] No room found for motion sensor: ${sensor.displayName}"
                return
            }
            
            def roomId = getOrCreateRoom(roomName, roomsCreated)
            
            def sensorId = sensor.id.toString()
            state.motionSensorMappings[sensorId] = roomId
            importedCount++
            log.info "[TellaBoomer] Mapped motion sensor '${sensor.displayName}' → '${roomName}'"
        }
    }
    
    if (settings.pressurePadSensors) {
        log.info "[TellaBoomer] Processing ${settings.pressurePadSensors.size()} pressure pads..."
        settings.pressurePadSensors.each { sensor ->
            def roomName = getSensorRoomName(sensor)
            
            if (!roomName) {
                log.debug "[TellaBoomer] No room found for pressure pad: ${sensor.displayName}"
                return
            }
            
            def roomId = getOrCreateRoom(roomName, roomsCreated)
            
            def sensorId = sensor.id.toString()
            state.pressurePadMappings[sensorId] = roomId
            importedCount++
            log.info "[TellaBoomer] Mapped pressure pad '${sensor.displayName}' → '${roomName}'"
        }
    }
    
    if (settings.flushSensors) {
        log.info "[TellaBoomer] Processing ${settings.flushSensors.size()} flush sensors..."
        settings.flushSensors.each { sensor ->
            def roomName = getSensorRoomName(sensor)
            
            if (!roomName) {
                log.debug "[TellaBoomer] No room found for flush sensor: ${sensor.displayName}"
                return
            }
            
            def roomId = getOrCreateRoom(roomName, roomsCreated)
            
            def sensorId = sensor.id.toString()
            state.flushSensorMappings[sensorId] = roomId
            importedCount++
            log.info "[TellaBoomer] Mapped flush sensor '${sensor.displayName}' → '${roomName}'"
        }
    }
    
    if (settings.tempHumiditySensors) {
        log.info "[TellaBoomer] Processing ${settings.tempHumiditySensors.size()} temp/humidity sensors..."
        settings.tempHumiditySensors.each { sensor ->
            def roomName = getSensorRoomName(sensor)
            
            if (!roomName) {
                log.debug "[TellaBoomer] No room found for temp/humidity sensor: ${sensor.displayName}"
                return
            }
            
            def roomId = getOrCreateRoom(roomName, roomsCreated)
            
            def sensorId = sensor.id.toString()
            state.tempHumiditySensorMappings[sensorId] = roomId
            importedCount++
            log.info "[TellaBoomer] Mapped temp/humidity sensor '${sensor.displayName}' → '${roomName}'"
        }
    }
    
    updateRoomMaps()
    
    log.info "[TellaBoomer] ========== AUTO-IMPORT COMPLETE =========="
    log.info "[TellaBoomer] Total sensors processed: ${importedCount}"
    log.info "[TellaBoomer] New rooms created: ${roomsCreated.size()}"
    log.info "[TellaBoomer] Total rooms: ${state.rooms.size()}"
    log.info "[TellaBoomer] Sensor mappings:"
    log.info "[TellaBoomer]   - Motion: ${state.motionSensorMappings.size()}"
    log.info "[TellaBoomer]   - Pressure Pads: ${state.pressurePadMappings.size()}"
    log.info "[TellaBoomer]   - Flush Sensors: ${state.flushSensorMappings.size()}"
    log.info "[TellaBoomer]   - Temp/Humidity: ${state.tempHumiditySensorMappings.size()}"
    log.info "[TellaBoomer] ==========================================="
}

def getOrCreateRoom(roomName, roomsCreated) {
    def roomId = roomsCreated[roomName]
    
    if (!roomId) {
        roomId = state.rooms?.find { k, v -> v.name == roomName }?.key
    }
    
    if (!roomId) {
        roomId = "room_${now()}_${roomsCreated.size()}"
        def roomType = detectRoomType(roomName)
        
        state.rooms[roomId] = [
            id: roomId,
            name: roomName,
            type: roomType,
            created: now()
        ]
        
        roomsCreated[roomName] = roomId
        log.info "[TellaBoomer] Created room: ${roomName} (${roomType}) with ID: ${roomId}"
    }
    
    return roomId
}

// ============================================================================
// LIFECYCLE METHODS
// ============================================================================

def installed() {
    log.info "[TellaBoomer] Installing Wellness Activity Tracker v2.3.2"
    initialize()
}

def updated() {
    log.info "[TellaBoomer] Updating Wellness Activity Tracker"
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    log.info "[TellaBoomer] Initializing v2.3.2"
    
    ensureStateInitialized()
    state.lastProcessing = now()
    
    if (!state.accessToken) {
        createOAuthToken()
    }
    
    updateRoomMaps()
    
    if (settings.motionSensors) {
        subscribe(motionSensors, "motion.active", motionActiveHandler)
        subscribe(motionSensors, "motion.inactive", motionInactiveHandler)
        log.info "[TellaBoomer] Subscribed to ${settings.motionSensors.size()} motion sensors"
    }
    
    if (settings.pressurePadSensors) {
        subscribe(pressurePadSensors, "contact.open", pressurePadActiveHandler)
        subscribe(pressurePadSensors, "contact.closed", pressurePadInactiveHandler)
        log.info "[TellaBoomer] Subscribed to ${settings.pressurePadSensors.size()} pressure pad sensors"
    }
    
    if (settings.flushSensors) {
        subscribe(flushSensors, "water.wet", flushSensorHandler)
        log.info "[TellaBoomer] Subscribed to ${settings.flushSensors.size()} toilet float sensors (water sensors)"
    }
    
    if (settings.enableModeIntegration != false) {
        subscribe(location, "mode", modeChangeHandler)
        log.info "[TellaBoomer] Subscribed to location mode changes"
    }
    
    if (settings.autoStartOvernight && settings.overnightStartTime) {
        schedule(settings.overnightStartTime, startOvernightMonitoring)
        log.info "[TellaBoomer] Scheduled overnight start: ${settings.overnightStartTime}"
    }
    if (settings.overnightEndTime) {
        schedule(settings.overnightEndTime, endOvernightMonitoring)
        log.info "[TellaBoomer] Scheduled overnight end: ${settings.overnightEndTime}"
    }
    
    if (settings.enableMorningProfile && settings.morningStartTime) {
        schedule(settings.morningStartTime, startMorningProfile)
        log.info "[TellaBoomer] Scheduled morning profile start: ${settings.morningStartTime}"
    }
    if (settings.enableDaytimeProfile && settings.daytimeStartTime) {
        schedule(settings.daytimeStartTime, startDaytimeProfile)
        log.info "[TellaBoomer] Scheduled daytime profile start: ${settings.daytimeStartTime}"
    }
    if (settings.enableEveningProfile && settings.eveningStartTime) {
        schedule(settings.eveningStartTime, startEveningProfile)
        log.info "[TellaBoomer] Scheduled evening profile start: ${settings.eveningStartTime}"
    }
    
    if (settings.enableMorningSummary && settings.summaryTime) {
        schedule(settings.summaryTime, sendMorningSummary)
    }
    
    // Enhanced notification scheduling
    if (settings.enableWeeklyTrends && settings.weeklyReportTime) {
        def cronExpression = getCronForWeeklyReport()
        schedule(cronExpression, sendWeeklyTrendsReport)
        log.info "[TellaBoomer] Scheduled weekly trends: ${settings.weeklyReportDay} at ${settings.weeklyReportTime}"
    }
    
    if (settings.enableDailyCheckIn && settings.checkInTime) {
        schedule(settings.checkInTime, sendQuickStatusCheckIn)
        log.info "[TellaBoomer] Scheduled daily check-in: ${settings.checkInTime}"
    }
    
    def interval = settings.updateIntervalMinutes ?: 15
    schedule("0 */${interval} * ? * *", updateAllHubVariables)
    
    schedule("0 0 4 * * ?", performDailyMaintenance)
    
    runIn(5, "updateAllHubVariables")
    
    log.info "[TellaBoomer] Initialization complete v2.3.4"
    log.info "[TellaBoomer] - Rooms: ${state.rooms?.size() ?: 0}"
    log.info "[TellaBoomer] - Motion mappings: ${state.motionSensorMappings?.size() ?: 0}"
    log.info "[TellaBoomer] - Pressure pad mappings: ${state.pressurePadMappings?.size() ?: 0}"
    log.info "[TellaBoomer] - Flush sensor mappings: ${state.flushSensorMappings?.size() ?: 0}"
    log.info "[TellaBoomer] - Temp/humidity mappings: ${state.tempHumiditySensorMappings?.size() ?: 0}"
    log.info "[TellaBoomer] - Mode integration: ${settings.enableModeIntegration != false}"
    log.info "[TellaBoomer] - Whole-house tracking: ${settings.enableWholeHouseTracking != false}"
}

def createOAuthToken() {
    try {
        createAccessToken()
        log.info "[TellaBoomer] Access token created: ${state.accessToken}"
    } catch (e) {
        log.warn "[TellaBoomer] Could not create access token: ${e.message}. Enable OAuth in Apps Code."
    }
}


// ============================================================================
// MOTION EVENT HANDLERS
// ============================================================================

def motionActiveHandler(evt) {
    def deviceId = evt.deviceId.toString()
    def roomId = state.motionRoomMap?.get(deviceId)
    def currentTime = now()
    
    if (!roomId) {
        if (settings.debugLogging) {
            log.debug "[TellaBoomer] Motion from unmapped sensor: ${evt.device.displayName} (${deviceId})"
        }
        return
    }
    
    def roomData = state.rooms[roomId]
    def roomName = roomData?.name ?: "Unknown"
    def roomType = roomData?.type ?: "other"
    
    if (settings.debugLogging) {
        log.debug "[TellaBoomer] Motion ACTIVE in ${roomName} (${roomType})"
    }
    
    if (settings.enableWholeHouseTracking != false) {
        def previousRoom = state.currentLocation
        if (previousRoom && previousRoom != roomName) {
            recordRoomTransition(previousRoom, roomName, currentTime)
        }
        updateRoomActivitySummary(roomId, roomName, roomType, currentTime)
    }
    
    state.currentLocation = roomName
    state.lastMotionTime = currentTime
    
    if (!state.roomEntryTime) state.roomEntryTime = [:]
    state.roomEntryTime[roomId] = currentTime
    
    if (state.overnightMonitoringActive) {
        processOvernightMotion(roomId, roomName, roomType, currentTime, "active")
    }
    
    if (roomType != "bathroom") {
        state.lastActiveRoom = roomName
    }
    
    if (settings.enableRealtimeUpdates) {
        updateHubVariable("wellness_current_location", roomName)
    }
}

def motionInactiveHandler(evt) {
    def deviceId = evt.deviceId.toString()
    def roomId = state.motionRoomMap?.get(deviceId)
    def currentTime = now()
    
    if (!roomId) return
    
    def roomData = state.rooms[roomId]
    def roomName = roomData?.name ?: "Unknown"
    def roomType = roomData?.type ?: "other"
    
    def entryTime = state.roomEntryTime?.get(roomId)
    if (entryTime) {
        def durationMinutes = Math.round((currentTime - entryTime) / 60000)
        
        if (state.overnightMonitoringActive && roomType == "bathroom") {
            recordBathroomVisitEnd(roomId, roomName, durationMinutes, currentTime)
        }
        
        state.roomEntryTime?.remove(roomId)
    }
}

// ============================================================================
// v2.3.0: ROOM TRANSITION TRACKING
// ============================================================================

def recordRoomTransition(fromRoom, toRoom, timestamp) {
    if (!state.roomTransitions) state.roomTransitions = []
    
    def transition = [
        from: fromRoom,
        to: toRoom,
        timestamp: timestamp,
        time: formatTime(new Date(timestamp)),
        hour: new Date(timestamp).format("H").toInteger()
    ]
    
    state.roomTransitions << transition
    state.lastRoomTransition = transition
    
    if (settings.debugLogging) {
        log.debug "[TellaBoomer] Room transition: ${fromRoom} → ${toRoom}"
    }
    
    def retentionHours = settings.transitionRetentionHours ?: 24
    def cutoffTime = now() - (retentionHours * 3600000)
    state.roomTransitions = state.roomTransitions.findAll { it.timestamp > cutoffTime }
}

def updateRoomActivitySummary(roomId, roomName, roomType, timestamp) {
    if (!state.roomActivitySummary) state.roomActivitySummary = [:]
    
    def today = new Date().format("yyyy-MM-dd")
    def key = "${roomName}"
    
    if (!state.roomActivitySummary[key]) {
        state.roomActivitySummary[key] = [
            visits: 0,
            lastVisit: null,
            type: roomType,
            date: today
        ]
    }
    
    if (state.roomActivitySummary[key].date != today) {
        state.roomActivitySummary[key] = [
            visits: 0,
            lastVisit: null,
            type: roomType,
            date: today
        ]
    }
    
    state.roomActivitySummary[key].visits++
    state.roomActivitySummary[key].lastVisit = formatTime(new Date(timestamp))
}

def buildRoomActivitySummary() {
    def summary = [:]
    state.roomActivitySummary?.each { roomName, data ->
        summary[roomName] = [
            visits: data.visits ?: 0,
            type: data.type ?: "other",
            lastVisit: data.lastVisit ?: ""
        ]
    }
    return summary
}

// ============================================================================
// v2.3.0: HUBITAT MODE INTEGRATION
// ============================================================================

def modeChangeHandler(evt) {
    def newMode = evt.value
    log.info "[TellaBoomer] Location mode changed to: ${newMode}"
    
    if (settings.enableModeIntegration != false) {
        
        if (settings.enableMorningProfile && settings.morningTriggerMode == newMode) {
            log.info "[TellaBoomer] Mode ${newMode} triggers morning profile"
            startMorningProfile()
        }
        
        if (settings.enableDaytimeProfile && settings.daytimeTriggerMode == newMode) {
            log.info "[TellaBoomer] Mode ${newMode} triggers daytime profile"
            startDaytimeProfile()
        }
        
        if (settings.enableEveningProfile && settings.eveningTriggerMode == newMode) {
            log.info "[TellaBoomer] Mode ${newMode} triggers evening profile"
            startEveningProfile()
        }
        
        if (newMode.toLowerCase().contains("night") && settings.autoStartOvernight) {
            log.info "[TellaBoomer] Night mode detected, starting overnight monitoring"
            startOvernightMonitoring()
        }
    }
    
    updateHubVariable("wellness_current_mode", newMode)
}

// ============================================================================
// v2.3.0: MONITORING PROFILE FUNCTIONS
// ============================================================================

def startMorningProfile() {
    log.info "[TellaBoomer] ☀️ Starting MORNING profile"
    state.activeMonitoringProfile = "morning"
    state.morningProfileActive = true
    state.morningProfileStart = now()
    state.roomActivitySummary = [:]
}

def startDaytimeProfile() {
    log.info "[TellaBoomer] 🌤️ Starting DAYTIME profile"
    state.activeMonitoringProfile = "daytime"
    state.daytimeProfileActive = true
    state.daytimeProfileStart = now()
}

def startEveningProfile() {
    log.info "[TellaBoomer] 🌆 Starting EVENING profile"
    state.activeMonitoringProfile = "evening"
    state.eveningProfileActive = true
    state.eveningProfileStart = now()
}

// ============================================================================
// PRESSURE PAD HANDLERS (Contact Sensors for Sitting/Occupancy)
// ============================================================================

def pressurePadActiveHandler(evt) {
    def deviceId = evt.deviceId.toString()
    def currentTime = now()
    def locationName = settings."pressurePadLocation_${evt.deviceId}" ?: evt.device.displayName
    
    log.info "[TellaBoomer] 🪑 Pressure pad ACTIVE: ${locationName}"
    
    if (!state.sittingSessions) state.sittingSessions = [:]
    if (!state.sittingTotals) state.sittingTotals = [:]
    
    state.sittingSessions[deviceId] = [
        startTime: currentTime,
        startTimeStr: formatTime(new Date(currentTime)),
        location: locationName
    ]
    
    state.currentSittingLocation = locationName
    state.currentSittingStart = currentTime
    
    updateHubVariable("wellness_current_sitting_location", locationName)
    updateHubVariable("wellness_sitting_session_start", formatTime(new Date(currentTime)))
    
    if (state.overnightMonitoringActive) {
        if (!state.tonightSittingSessions) state.tonightSittingSessions = []
        state.tonightSittingSessions << [
            location: locationName,
            startTime: formatTime(new Date(currentTime)),
            startTimestamp: currentTime
        ]
    }
}

def pressurePadInactiveHandler(evt) {
    def deviceId = evt.deviceId.toString()
    def currentTime = now()
    def locationName = settings."pressurePadLocation_${evt.deviceId}" ?: evt.device.displayName
    
    def session = state.sittingSessions?.get(deviceId)
    if (!session) {
        log.debug "[TellaBoomer] No sitting session found for ${locationName}"
        return
    }
    
    def durationMinutes = Math.round((currentTime - session.startTime) / 60000)
    
    log.info "[TellaBoomer] 🪑 Pressure pad INACTIVE: ${locationName}, Duration: ${durationMinutes} min"
    
    if (!state.sittingTotals) state.sittingTotals = [:]
    state.sittingTotals[locationName] = (state.sittingTotals[locationName] ?: 0) + durationMinutes
    
    state.sittingSessionCount = (state.sittingSessionCount ?: 0) + 1
    
    state.totalSittingMinutes = (state.totalSittingMinutes ?: 0) + durationMinutes
    
    if (durationMinutes > (state.longestSittingSession ?: 0)) {
        state.longestSittingSession = durationMinutes
    }
    
    state.sittingSessions.remove(deviceId)
    if (state.currentSittingLocation == locationName) {
        state.currentSittingLocation = null
        state.currentSittingStart = null
    }
    
    updateHubVariable("wellness_sitting_total_minutes", state.totalSittingMinutes ?: 0)
    updateHubVariable("wellness_current_sitting_location", state.currentSittingLocation ?: "")
    updateHubVariable("wellness_sitting_session_start", "")
    updateHubVariable("wellness_longest_sitting_session", state.longestSittingSession ?: 0)
    updateHubVariable("wellness_sitting_session_count", state.sittingSessionCount ?: 0)
    updateHubVariable("wellness_pressure_pad_json", groovy.json.JsonOutput.toJson(state.sittingTotals ?: [:]))
    
    def sedentaryThreshold = settings.sedentaryThreshold ?: 120
    if (durationMinutes >= sedentaryThreshold) {
        state.sedentaryAlert = true
        updateHubVariable("wellness_sedentary_alert", true)
        if (settings.enableAlerts) {
            if (settings.enableEnhancedAlerts && settings.alertOnSedentary) {
                sendEnhancedSedentaryAlert(locationName, durationMinutes)
            } else {
                sendAlert("Sedentary Alert", "Sitting at ${locationName} for ${durationMinutes} minutes")
            }
        }
    }
    
    if (state.overnightMonitoringActive && state.tonightSittingSessions) {
        def lastSession = state.tonightSittingSessions.find { !it.endTime && it.location == locationName }
        if (lastSession) {
            lastSession.endTime = formatTime(new Date(currentTime))
            lastSession.endTimestamp = currentTime
            lastSession.duration = durationMinutes
        }
    }
}

// ============================================================================
// TOILET FLOAT SENSOR HANDLER - v2.3.1
// ============================================================================

def flushSensorHandler(evt) {
    def currentTime = now()
    def timeStr = formatTime(new Date(currentTime))
    
    log.info "[TellaBoomer] 🚽 Toilet flush detected at ${timeStr} (${evt.device.displayName} = ${evt.value})"
    
    state.toiletFlushCount = (state.toiletFlushCount ?: 0) + 1
    
    if (!state.flushTimestamps) state.flushTimestamps = []
    state.flushTimestamps << [
        time: timeStr,
        timestamp: currentTime,
        sensor: evt.device.displayName
    ]
    
    if (state.flushTimestamps.size() > 50) {
        state.flushTimestamps = state.flushTimestamps[-50..-1]
    }
    
    updateHubVariable("wellness_toilet_flush_count", state.toiletFlushCount)
    updateHubVariable("wellness_flush_times_json", groovy.json.JsonOutput.toJson(
        state.flushTimestamps.collect { [time: it.time, sensor: it.sensor] }
    ))
    
    if (state.overnightMonitoringActive) {
        state.tonightFlushCount = (state.tonightFlushCount ?: 0) + 1
        if (!state.tonightFlushTimes) state.tonightFlushTimes = []
        state.tonightFlushTimes << timeStr
    }
}

// ============================================================================
// OVERNIGHT MONITORING
// ============================================================================

def startOvernightMonitoring() {
    log.info "[TellaBoomer] 🌙 Starting overnight monitoring"
    
    state.overnightMonitoringActive = true
    state.overnightStartTimestamp = now()
    state.tonightBathroomVisits = []
    state.tonightActivities = []
    state.tonightRestlessEvents = 0
    state.firstBedroomTime = null
    state.lastActiveRoomBeforeBed = state.lastActiveRoom
    
    state.tonightSittingSessions = []
    state.totalSittingMinutes = 0
    state.longestSittingSession = 0
    state.sittingSessionCount = 0
    state.sittingTotals = [:]
    state.sedentaryAlert = false
    
    state.tonightFlushCount = 0
    state.tonightFlushTimes = []
    
    updateHubVariable("wellness_monitoring_start", formatTime(new Date()))
    updateHubVariable("wellness_monitoring_active", true)
    updateHubVariable("wellness_last_active_room", state.lastActiveRoomBeforeBed ?: "")
    updateHubVariable("wellness_sitting_total_minutes", 0)
    updateHubVariable("wellness_toilet_flush_count", 0)
    updateHubVariable("wellness_sedentary_alert", false)
    
    log.info "[TellaBoomer] Overnight monitoring started"
}

def endOvernightMonitoring() {
    if (!state.overnightMonitoringActive) {
        log.debug "[TellaBoomer] Overnight monitoring not active"
        return
    }
    
    log.info "[TellaBoomer] ☀️ Ending overnight monitoring"
    
    state.overnightMonitoringActive = false
    state.overnightEndTimestamp = now()
    
    def summary = calculateOvernightSummary()
    state.lastOvernightSummary = summary
    
    updateAllOvernightVariables(summary)
    updateBaselines(summary)
    
    state.daysMonitored = (state.daysMonitored ?: 0) + 1
    updateHubVariable("wellness_days_monitored", state.daysMonitored)
    
    storeHistoricalData(summary)
    
    if (summary.riskLevel == "HIGH" && settings.alertOnHighRisk) {
        if (settings.enableEnhancedAlerts && settings.enhancedHighRisk) {
            sendEnhancedHighRiskAlert(summary)
        } else {
            sendAlert("High Risk Night", "Last night had ${summary.bathroomVisits} bathroom visits with ${summary.deepSleepDisruptions} during deep sleep hours.")
        }
    }
    
    log.info "[TellaBoomer] Overnight summary: ${summary.bathroomVisits} visits, Risk: ${summary.riskLevel}"
}

def processOvernightMotion(roomId, roomName, roomType, timestamp, motionState) {
    def timeStr = formatTime(new Date(timestamp))
    def hour = new Date(timestamp).format("H").toInteger()
    
    def activity = [
        time: timeStr,
        timestamp: timestamp,
        location: roomName,
        roomType: roomType,
        hour: hour
    ]
    if (!state.tonightActivities) state.tonightActivities = []
    state.tonightActivities << activity
    
    if (roomType == "bedroom" && !state.firstBedroomTime) {
        state.firstBedroomTime = timeStr
        updateHubVariable("wellness_first_bedroom_time", timeStr)
    }
    
    if (roomType == "bathroom") {
        def shouldTrack = !settings.trackedBathrooms || settings.trackedBathrooms.contains(roomId)
        if (shouldTrack) {
            recordBathroomVisitStart(roomId, roomName, timestamp, hour)
        }
    }
    
    trackRestlessness(timestamp)
}

def recordBathroomVisitStart(roomId, roomName, timestamp, hour) {
    def timeStr = formatTime(new Date(timestamp))
    
    def lastVisit = state.tonightBathroomVisits?.find { !it.endTime }
    if (lastVisit) return
    
    def isDeepSleep = hour >= (settings.deepSleepStart ?: 2) && hour < (settings.deepSleepEnd ?: 4)
    
    def visit = [
        startTime: timeStr,
        startTimestamp: timestamp,
        roomName: roomName,
        roomId: roomId,
        hour: hour,
        isDeepSleep: isDeepSleep
    ]
    
    if (!state.tonightBathroomVisits) state.tonightBathroomVisits = []
    state.tonightBathroomVisits << visit
    
    log.info "[TellaBoomer] 🚿 Bathroom visit started at ${timeStr} (Deep Sleep: ${isDeepSleep})"
    
    // Send deep sleep alert if enabled
    if (isDeepSleep && settings.enableEnhancedAlerts && settings.alertOnDeepSleep) {
        sendDeepSleepAlert(roomName)
    }
    
    if (state.tonightBathroomVisits.size() == 1) {
        updateHubVariable("wellness_first_bathroom_time", timeStr)
    }
}

def recordBathroomVisitEnd(roomId, roomName, durationMinutes, timestamp) {
    def visits = state.tonightBathroomVisits
    if (!visits) return
    
    def lastVisit = visits.find { !it.endTime && it.roomId == roomId }
    if (!lastVisit) return
    
    def timeStr = formatTime(new Date(timestamp))
    lastVisit.endTime = timeStr
    lastVisit.endTimestamp = timestamp
    lastVisit.duration = durationMinutes
    
    log.info "[TellaBoomer] 🚿 Bathroom visit ended, Duration: ${durationMinutes} min"
    
    updateHubVariable("wellness_bathroom_visits", visits.size())
    updateHubVariable("wellness_last_bathroom_time", timeStr)
    
    if (durationMinutes >= (settings.extendedVisitMinutes ?: 15) && settings.alertOnExtendedVisit) {
        if (settings.enableEnhancedAlerts) {
            sendEnhancedExtendedVisitAlert(roomName, durationMinutes, lastVisit.startTime)
        } else {
            sendAlert("Extended Bathroom Visit", "Visit lasted ${durationMinutes} minutes at ${lastVisit.startTime}")
        }
    }
}

def trackRestlessness(timestamp) {
    if (!state.recentMotionEvents) state.recentMotionEvents = []
    state.recentMotionEvents << timestamp
    
    def thirtyMinutesAgo = timestamp - (30 * 60 * 1000)
    state.recentMotionEvents = state.recentMotionEvents.findAll { it > thirtyMinutesAgo }
    
    if (state.recentMotionEvents.size() > 5) {
        state.tonightRestlessEvents = (state.tonightRestlessEvents ?: 0) + 1
    }
}


// ============================================================================
// CALCULATIONS
// ============================================================================

def calculateOvernightSummary() {
    def visits = state.tonightBathroomVisits ?: []
    def activities = state.tonightActivities ?: []
    
    def bathroomVisits = visits.size()
    def deepSleepVisits = visits.count { it.isDeepSleep }
    def durations = visits.findAll { it.duration != null }.collect { it.duration }
    def avgDuration = durations ? safeRound(durations.sum() / durations.size(), 1) : 0
    def maxDuration = durations ? durations.max() : 0
    
    def monitoringDuration = state.overnightEndTimestamp && state.overnightStartTimestamp ?
        safeRound((state.overnightEndTimestamp - state.overnightStartTimestamp) / 3600000, 1) : 0
    def restlessnessPercent = activities.size() > 0 ? 
        Math.min(100, Math.round((state.tonightRestlessEvents ?: 0) * 10)) : 0
    
    def longestRestPeriod = 0
    if (activities.size() > 1) {
        def sortedTimestamps = activities.findAll { it.timestamp }.collect { it.timestamp }.sort()
        for (int i = 1; i < sortedTimestamps.size(); i++) {
            def gap = (sortedTimestamps[i] - sortedTimestamps[i-1]) / 3600000
            if (gap > longestRestPeriod) longestRestPeriod = gap
        }
        longestRestPeriod = safeRound(longestRestPeriod, 1)
    } else if (monitoringDuration > 0) {
        longestRestPeriod = monitoringDuration
    }
    
    def baselineVisits = 2.0
    if (state.baselineHistory && state.baselineHistory.size() >= 3) {
        baselineVisits = safeRound(state.baselineHistory.collect { it.visits }.sum() / state.baselineHistory.size(), 1)
    }
    
    def riskScore = calculateRiskScore(bathroomVisits, deepSleepVisits, avgDuration, maxDuration, restlessnessPercent)
    def riskLevel = riskScore >= 50 ? "HIGH" : (riskScore >= 25 ? "MODERATE" : "LOW")
    def wellnessScore = Math.max(0, 100 - riskScore)
    
    def activitiesJson = visits.collect { visit ->
        [time: visit.startTime, location: visit.roomName ?: "Bathroom", duration: visit.duration ?: 0, isDeepSleep: visit.isDeepSleep]
    }
    
    def sittingSessions = state.tonightSittingSessions ?: []
    def sittingSessionsJson = sittingSessions.collect { session ->
        [location: session.location, startTime: session.startTime, duration: session.duration ?: 0]
    }
    
    return [
        date: new Date().format("yyyy-MM-dd"),
        bathroomVisits: bathroomVisits,
        deepSleepDisruptions: deepSleepVisits,
        avgVisitDuration: avgDuration,
        longestVisitDuration: maxDuration,
        totalRestHours: monitoringDuration,
        restlessnessPercent: restlessnessPercent,
        longestRestPeriod: longestRestPeriod,
        baselineVisits: baselineVisits,
        totalEvents: activities.size(),
        wellnessScore: wellnessScore,
        riskScore: riskScore,
        riskLevel: riskLevel,
        firstBedroomTime: state.firstBedroomTime,
        lastActiveRoom: state.lastActiveRoomBeforeBed,
        activities: activitiesJson,
        toiletFlushCount: state.tonightFlushCount ?: 0,
        flushTimes: state.tonightFlushTimes ?: [],
        sittingTotalMinutes: state.totalSittingMinutes ?: 0,
        sittingSessionCount: state.sittingSessionCount ?: 0,
        longestSittingSession: state.longestSittingSession ?: 0,
        sittingSessions: sittingSessionsJson,
        sittingByLocation: state.sittingTotals ?: [:],
        sedentaryAlert: state.sedentaryAlert ?: false
    ]
}

def calculateRiskScore(visits, deepSleepVisits, avgDuration, maxDuration, restlessness) {
    def score = 0
    
    def highRisk = settings.highRiskVisitCount ?: 5
    def modRisk = settings.moderateRiskVisitCount ?: 3
    
    if (visits >= highRisk) score += 30
    else if (visits >= modRisk) score += 15
    else if (visits > 2) score += 5
    
    score += deepSleepVisits * 15
    
    def extendedThreshold = settings.extendedVisitDuration ?: 15
    if (maxDuration >= extendedThreshold) score += 20
    else if (maxDuration >= (settings.normalVisitDuration ?: 7)) score += 10
    
    def highRestless = settings.restlessnessHighThreshold ?: 30
    if (restlessness >= highRestless) score += 20
    else if (restlessness >= 20) score += 10
    
    return Math.min(100, score)
}

def updateBaselines(summary) {
    if (!state.baselineHistory) state.baselineHistory = []
    state.baselineHistory << [
        date: summary.date,
        visits: summary.bathroomVisits,
        duration: summary.avgVisitDuration,
        restHours: summary.totalRestHours,
        restlessness: summary.restlessnessPercent,
        bedtime: summary.firstBedroomTime
    ]
    
    while (state.baselineHistory.size() > 7) {
        state.baselineHistory.remove(0)
    }
    
    if (state.baselineHistory.size() >= 3) {
        def avgVisits = safeRound(state.baselineHistory.collect { it.visits }.sum() / state.baselineHistory.size(), 1)
        def avgDuration = safeRound(state.baselineHistory.collect { it.duration }.sum() / state.baselineHistory.size(), 1)
        def avgRestHours = safeRound(state.baselineHistory.collect { it.restHours }.sum() / state.baselineHistory.size(), 1)
        def avgRestlessness = safeRound(state.baselineHistory.collect { it.restlessness }.sum() / state.baselineHistory.size(), 0)
        
        updateHubVariable("wellness_baseline_visits", avgVisits)
        updateHubVariable("wellness_baseline_duration", avgDuration)
        updateHubVariable("wellness_baseline_sleep_hours", avgRestHours)
        updateHubVariable("wellness_baseline_restlessness", avgRestlessness)
        
        if (state.baselineHistory.size() >= 5) {
            def recentAvg = state.baselineHistory[-3..-1].collect { it.visits }.sum() / 3
            def olderAvg = state.baselineHistory[0..2].collect { it.visits }.sum() / 3
            def trend = recentAvg - olderAvg
            
            def trendDirection = trend > 0.5 ? "declining" : (trend < -0.5 ? "improving" : "stable")
            updateHubVariable("wellness_trend_direction", trendDirection)
            updateHubVariable("wellness_visit_trend", Math.round(trend))
        }
    }
}

// ============================================================================
// HUB VARIABLE MANAGEMENT
// ============================================================================

def updateAllHubVariables() {
    if (settings.debugLogging) {
        log.debug "[TellaBoomer] Updating hub variables"
    }
    
    if (state.overnightMonitoringActive) {
        updateBathroomMetrics()
    }
    
    updateHubVariable("wellness_last_update", new Date().format("yyyy-MM-dd'T'HH:mm:ss"))
    updateHubVariable("wellness_monitoring_active", state.overnightMonitoringActive ?: false)
    updateHubVariable("wellness_app_version", state.version ?: "2.3.4")
    updateHubVariable("wellness_sensor_status", settings.motionSensors ? "ok" : "error")
    updateHubVariable("wellness_data_quality", (state.daysMonitored ?: 0) >= 7 ? "good" : "partial")
}

def updateBathroomMetrics() {
    def visits = state.tonightBathroomVisits ?: []
    def durations = visits.findAll { it.duration != null }.collect { it.duration }
    
    updateHubVariable("wellness_bathroom_visits", visits.size())
    updateHubVariable("wellness_deep_sleep_disruptions", visits.count { it.isDeepSleep })
    
    if (durations) {
        updateHubVariable("wellness_avg_visit_duration", safeRound(durations.sum() / durations.size(), 1))
        updateHubVariable("wellness_longest_visit_duration", durations.max())
    }
    
    updateHubVariable("wellness_total_events", state.tonightActivities?.size() ?: 0)
}

def updateAllOvernightVariables(summary) {
    updateHubVariable("wellness_overnight_score", summary.wellnessScore)
    updateHubVariable("wellness_bathroom_visits", summary.bathroomVisits)
    updateHubVariable("wellness_total_rest_hours", summary.totalRestHours)
    updateHubVariable("wellness_restlessness_percent", summary.restlessnessPercent)
    updateHubVariable("wellness_deep_sleep_disruptions", summary.deepSleepDisruptions)
    updateHubVariable("wellness_avg_visit_duration", summary.avgVisitDuration)
    updateHubVariable("wellness_longest_visit_duration", summary.longestVisitDuration)
    updateHubVariable("wellness_total_events", summary.totalEvents)
    
    updateHubVariable("wellness_first_bedroom_time", summary.firstBedroomTime ?: "")
    updateHubVariable("wellness_last_bedroom_time", formatTime(new Date()))
    updateHubVariable("wellness_last_active_room", summary.lastActiveRoom ?: "")
    updateHubVariable("wellness_monitoring_end", formatTime(new Date()))
    
    updateHubVariable("wellness_risk_level", summary.riskLevel)
    updateHubVariable("wellness_risk_score", summary.riskScore)
    
    def alertActive = summary.riskLevel == "HIGH"
    updateHubVariable("wellness_alert_active", alertActive)
    updateHubVariable("wellness_alert_priority", alertActive ? "PRIORITY" : "ROUTINE")
    updateHubVariable("wellness_alert_message", alertActive ? "High risk overnight activity detected" : "")
    
    def activitiesJson = groovy.json.JsonOutput.toJson(summary.activities ?: [])
    updateHubVariable("wellness_activities_json", activitiesJson)
    
    def hourlySummary = [:]
    (state.tonightActivities ?: []).each { activity ->
        def hour = activity.hour.toString()
        hourlySummary[hour] = (hourlySummary[hour] ?: 0) + 1
    }
    updateHubVariable("wellness_hourly_summary_json", groovy.json.JsonOutput.toJson(hourlySummary))
    
    def roomSummary = [:]
    (state.tonightBathroomVisits ?: []).each { visit ->
        def room = visit.roomName ?: "Bathroom"
        roomSummary[room] = (roomSummary[room] ?: 0) + 1
    }
    updateHubVariable("wellness_room_summary_json", groovy.json.JsonOutput.toJson(roomSummary))
    
    updateHubVariable("wellness_last_update", new Date().format("yyyy-MM-dd'T'HH:mm:ss"))
}

def updateHubVariable(name, value) {
    try {
        setGlobalVar(name, value)
    } catch (Exception e) {
        if (settings.debugLogging) {
            log.warn "[TellaBoomer] Failed to update ${name}: ${e.message}"
        }
    }
}

// ============================================================================
// MAKER API ENDPOINTS
// ============================================================================

mappings {
    path("/wellnessData") { action: [GET: "getWellnessData", OPTIONS: "corsPreflightHandler"] }
    path("/status") { action: [GET: "getStatus", OPTIONS: "corsPreflightHandler"] }
    path("/startMonitoring") { action: [POST: "apiStartMonitoring", OPTIONS: "corsPreflightHandler"] }
    path("/endMonitoring") { action: [POST: "apiEndMonitoring", OPTIONS: "corsPreflightHandler"] }
}

def corsPreflightHandler() {
    def headers = getCorsHeaders()
    render contentType: "text/plain", data: "", headers: headers, status: 204
}

def getCorsHeaders() {
    def headers = [:]
    
    if (settings.enableCors != false) {
        def allowedOrigins = settings.corsAllowedOrigins ?: "*"
        headers["Access-Control-Allow-Origin"] = allowedOrigins
        headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"
        headers["Access-Control-Allow-Headers"] = "Content-Type, Authorization, Accept"
        headers["Access-Control-Max-Age"] = "86400"
    }
    
    return headers
}

def renderJsonWithCors(data) {
    def headers = getCorsHeaders()
    def jsonStr = groovy.json.JsonOutput.toJson(data)
    render contentType: "application/json", data: jsonStr, headers: headers
}

def getWellnessData() {
    def baselineVisits = 2.0
    def baselineDuration = 5.0
    def baselineRestHours = 7.5
    def baselineRestlessness = 15
    if (state.baselineHistory && state.baselineHistory.size() >= 3) {
        baselineVisits = safeRound(state.baselineHistory.collect { it.visits }.sum() / state.baselineHistory.size(), 1)
        baselineDuration = safeRound(state.baselineHistory.collect { it.duration }.sum() / state.baselineHistory.size(), 1)
        baselineRestHours = safeRound(state.baselineHistory.collect { it.restHours }.sum() / state.baselineHistory.size(), 1)
        baselineRestlessness = safeRound(state.baselineHistory.collect { it.restlessness }.sum() / state.baselineHistory.size(), 0)
    }
    
    def trendDirection = "stable"
    if (state.baselineHistory && state.baselineHistory.size() >= 5) {
        def recentAvg = state.baselineHistory[-3..-1].collect { it.visits }.sum() / 3
        def olderAvg = state.baselineHistory[0..2].collect { it.visits }.sum() / 3
        def trend = recentAvg - olderAvg
        trendDirection = trend > 0.5 ? "declining" : (trend < -0.5 ? "improving" : "stable")
    }
    
    def lastNightData = state.lastOvernightSummary ?: [:]
    
    if (state.overnightMonitoringActive) {
        def visits = state.tonightBathroomVisits ?: []
        def activities = state.tonightActivities ?: []
        
        def bathroomVisits = visits.size()
        def deepSleepVisits = visits.count { it.isDeepSleep }
        def durations = visits.findAll { it.duration != null }.collect { it.duration }
        def avgDuration = durations ? safeRound(durations.sum() / durations.size(), 1) : 0
        def maxDuration = durations ? durations.max() : 0
        
        def monitoringDuration = state.overnightStartTimestamp ?
            safeRound((now() - state.overnightStartTimestamp) / 3600000, 1) : 0
        def restlessnessPercent = activities.size() > 0 ? 
            Math.min(100, Math.round((state.tonightRestlessEvents ?: 0) * 10)) : 0
        
        def longestRestPeriod = monitoringDuration
        if (activities.size() > 1) {
            def sortedTimestamps = activities.findAll { it.timestamp }.collect { it.timestamp }.sort()
            for (int i = 1; i < sortedTimestamps.size(); i++) {
                def gap = (sortedTimestamps[i] - sortedTimestamps[i-1]) / 3600000
                if (gap > longestRestPeriod) longestRestPeriod = gap
            }
            longestRestPeriod = safeRound(longestRestPeriod, 1)
        }
        
        def riskScore = calculateRiskScore(bathroomVisits, deepSleepVisits, avgDuration, maxDuration, restlessnessPercent)
        def riskLevel = riskScore >= 50 ? "HIGH" : (riskScore >= 25 ? "MODERATE" : "LOW")
        def wellnessScore = Math.max(0, 100 - riskScore)
        
        def tonightData = [
            status: "IN_PROGRESS",
            bathroomVisits: bathroomVisits,
            deepSleepDisruptions: deepSleepVisits,
            avgVisitDuration: avgDuration,
            longestVisitDuration: maxDuration,
            totalRestHours: monitoringDuration,
            restlessnessPercent: restlessnessPercent,
            longestRestPeriod: longestRestPeriod,
            wellnessScore: wellnessScore,
            riskScore: riskScore,
            riskLevel: riskLevel,
            firstBedroomTime: state.firstBedroomTime ?: "",
            lastActiveRoom: state.lastActiveRoomBeforeBed ?: "",
            totalEvents: activities.size(),
            monitoringStart: state.overnightStartTimestamp ? formatTime(new Date(state.overnightStartTimestamp)) : "",
            currentTime: formatTime(new Date())
        ]
        
        renderJsonWithCors([
            tonight: tonightData,
            lastNight: lastNightData,
            baselines: [
                visits: baselineVisits,
                duration: baselineDuration,
                restHours: baselineRestHours,
                restlessness: baselineRestlessness
            ],
            trends: [
                direction: trendDirection,
                daysMonitored: state.daysMonitored ?: 0
            ],
            currentLocation: state.currentLocation ?: "",
            monitoringActive: true,
            appVersion: state.version ?: "2.3.2",
            roomActivity: buildRoomActivitySummary(),
            activeProfile: state.activeMonitoringProfile ?: "overnight",
            currentMode: location.mode
        ])
    } else {
        renderJsonWithCors([
            tonight: [:],
            lastNight: lastNightData,
            baselines: [
                visits: baselineVisits,
                duration: baselineDuration,
                restHours: baselineRestHours,
                restlessness: baselineRestlessness
            ],
            trends: [
                direction: trendDirection,
                daysMonitored: state.daysMonitored ?: 0
            ],
            currentLocation: state.currentLocation ?: "",
            monitoringActive: false,
            appVersion: state.version ?: "2.3.2",
            roomActivity: buildRoomActivitySummary(),
            activeProfile: state.activeMonitoringProfile ?: "overnight",
            currentMode: location.mode
        ])
    }
}

def getStatus() {
    renderJsonWithCors([
        monitoringActive: state.overnightMonitoringActive ?: false,
        currentLocation: state.currentLocation ?: "",
        lastUpdate: state.lastProcessing ? formatTime(new Date(state.lastProcessing)) : "",
        appVersion: state.version ?: "2.3.2",
        daysMonitored: state.daysMonitored ?: 0,
        roomCount: state.rooms?.size() ?: 0,
        sensorCount: settings.motionSensors?.size() ?: 0,
        currentMode: location.mode,
        activeProfile: state.activeMonitoringProfile ?: "overnight"
    ])
}

def apiStartMonitoring() {
    startOvernightMonitoring()
    renderJsonWithCors([success: true, message: "Monitoring started"])
}

def apiEndMonitoring() {
    endOvernightMonitoring()
    renderJsonWithCors([success: true, message: "Monitoring ended"])
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

def formatTime(date) {
    return date.format("h:mm a")
}

def safeRound(value, decimals) {
    try {
        return Math.round(value * Math.pow(10, decimals)) / Math.pow(10, decimals)
    } catch (e) {
        return 0
    }
}

def storeHistoricalData(summary) {
    if (!state.historicalData) state.historicalData = []
    state.historicalData << summary
    
    def retentionDays = settings.dataRetentionDays ?: 30
    def cutoffDate = new Date(now() - (retentionDays * 24 * 3600000)).format("yyyy-MM-dd")
    state.historicalData = state.historicalData.findAll { it.date >= cutoffDate }
}

def sendAlert(title, message) {
    def device = settings.notifyDevice
    if (device) {
        device.deviceNotification("${title}: ${message}")
        log.info "[TellaBoomer] Alert sent: ${title}"
    }
}

def sendMorningSummary() {
    if (!state.lastOvernightSummary) return
    
    // Use enhanced notifications if enabled
    if (settings.enableEnhancedMorningSummary) {
        if (settings.summaryStyle == "narrative") {
            sendNarrativeJourney()
        } else if (settings.summaryStyle == "both") {
            sendEnhancedMorningSummary()
            pauseExecution(2000)  // Wait 2 seconds between messages
            sendNarrativeJourney()
        } else {
            // Default to data-focused
            sendEnhancedMorningSummary()
        }
    } else {
        // Fall back to standard simple summary
        def summary = state.lastOvernightSummary
        def device = settings.summaryDevice ?: settings.notifyDevice
        
        if (device) {
            def message = "Wellness Report: ${summary.bathroomVisits} bathroom visits, " +
                         "Score: ${summary.wellnessScore}, Risk: ${summary.riskLevel}"
            device.deviceNotification(message)
            log.info "[TellaBoomer] Standard morning summary sent"
        }
    }
}

def validateHubVariables() {
    log.info "[TellaBoomer] Validating hub variables..."
    def count = 0
    try {
        updateHubVariable("wellness_test_variable", "OK")
        count = 1
        log.info "[TellaBoomer] Hub variables: Working ✅"
    } catch (e) {
        log.error "[TellaBoomer] Hub variables: ERROR - ${e.message}"
    }
}

def clearOvernightData() {
    log.info "[TellaBoomer] Clearing overnight data..."
    state.tonightBathroomVisits = []
    state.tonightActivities = []
    state.tonightRestlessEvents = 0
    state.overnightMonitoringActive = false
    log.info "[TellaBoomer] Overnight data cleared"
}

def performDailyMaintenance() {
    log.info "[TellaBoomer] Performing daily maintenance..."
    
    if (state.historicalData) {
        def retentionDays = settings.dataRetentionDays ?: 30
        def cutoffDate = new Date(now() - (retentionDays * 24 * 3600000)).format("yyyy-MM-dd")
        def before = state.historicalData.size()
        state.historicalData = state.historicalData.findAll { it.date >= cutoffDate }
        def after = state.historicalData.size()
        log.info "[TellaBoomer] Purged ${before - after} old records"
    }
    
    if (state.roomTransitions) {
        def retentionHours = settings.transitionRetentionHours ?: 24
        def cutoffTime = now() - (retentionHours * 3600000)
        def before = state.roomTransitions.size()
        state.roomTransitions = state.roomTransitions.findAll { it.timestamp > cutoffTime }
        def after = state.roomTransitions.size()
        log.info "[TellaBoomer] Purged ${before - after} old transitions"
    }
    
    log.info "[TellaBoomer] Daily maintenance complete"
}

def runDiagnostics() {
    log.info "[TellaBoomer] ========== DIAGNOSTICS =========="
    log.info "[TellaBoomer] Version: ${state.version}"
    log.info "[TellaBoomer] Rooms: ${state.rooms?.size() ?: 0}"
    log.info "[TellaBoomer] Motion Sensors: ${settings.motionSensors?.size() ?: 0}"
    log.info "[TellaBoomer] Motion Mappings: ${state.motionSensorMappings?.size() ?: 0}"
    log.info "[TellaBoomer] Pressure Pads: ${settings.pressurePadSensors?.size() ?: 0}"
    log.info "[TellaBoomer] Pressure Pad Mappings: ${state.pressurePadMappings?.size() ?: 0}"
    log.info "[TellaBoomer] Flush Sensors: ${settings.flushSensors?.size() ?: 0}"
    log.info "[TellaBoomer] Flush Sensor Mappings: ${state.flushSensorMappings?.size() ?: 0}"
    log.info "[TellaBoomer] Temp/Humidity Sensors: ${settings.tempHumiditySensors?.size() ?: 0}"
    log.info "[TellaBoomer] Temp/Humidity Mappings: ${state.tempHumiditySensorMappings?.size() ?: 0}"
    log.info "[TellaBoomer] Monitoring Active: ${state.overnightMonitoringActive}"
    log.info "[TellaBoomer] Days Monitored: ${state.daysMonitored ?: 0}"
    log.info "[TellaBoomer] ==================================="
}

// ============================================================================
// ENHANCED PUSHOVER NOTIFICATION SYSTEM v2.3.3
// ============================================================================

// ============================================================================
// ENHANCED MORNING WELLNESS SUMMARY
// ============================================================================

def sendEnhancedMorningSummary() {
    def summary = state.lastOvernightSummary
    if (!summary) {
        log.debug "[TellaBoomer] No overnight summary available for enhanced notification"
        return
    }
    
    // Build risk emoji
    def riskEmoji = summary.riskLevel == "LOW" ? "✅" : 
                    (summary.riskLevel == "MODERATE" ? "⚠️" : "🔴")
    
    // Build comparative analysis
    def baseline = summary.baselineVisits ?: 2.0
    def comparison = summary.bathroomVisits > baseline ? "above" : 
                     (summary.bathroomVisits < baseline ? "below" : "matching")
    def comparisonEmoji = comparison == "above" ? "📈" : 
                          (comparison == "below" ? "📉" : "➡️")
    
    // Determine priority and sound based on risk level
    def priority = summary.riskLevel == "HIGH" ? "[H]" : "[N]"
    def sound = summary.riskLevel == "HIGH" ? (settings.soundHighRisk ?: "siren") : (settings.soundMorningSummary ?: "cosmic")
    
    // Create rich narrative message with HTML formatting
    def messageBody = """<b>☀️ Good Morning! Last Night's Wellness Report</b>

<b>Overnight Score: ${summary.wellnessScore}/100</b> ${riskEmoji}

<b>🌙 Sleep Quality</b>
• Total Rest: ${summary.totalRestHours} hours
• First to Bed: ${summary.firstBedroomTime ?: 'Not detected'}
• Restlessness: ${summary.restlessnessPercent}%

<b>🚿 Bathroom Activity</b>
• Visits: ${summary.bathroomVisits} ${comparisonEmoji} (Baseline: ${baseline})
• Deep Sleep Trips: ${summary.deepSleepDisruptions} 😴
• Avg Duration: ${summary.avgVisitDuration} min
• Longest Visit: ${summary.longestVisitDuration} min

<b>🪑 Sitting Activity</b>
• Total Time: ${summary.sittingTotalMinutes ?: 0} min
• Sessions: ${summary.sittingSessionCount ?: 0}
• Longest Session: ${summary.longestSittingSession ?: 0} min

<b>🚽 Flush Count</b>
• Flushes Detected: ${summary.toiletFlushCount ?: 0}

<b>Risk Assessment: ${summary.riskLevel}</b> ${riskEmoji}

<i>Monitored: ${summary.totalRestHours}h from ${settings.overnightStartTime} to ${settings.overnightEndTime}</i>"""
    
    // Build message with Pushover driver tags
    def fullMessage = buildPushoverMessage(messageBody, [
        priority: priority,
        title: "TellaBoomer: ${summary.riskLevel} Risk Report",
        sound: sound
    ])
    
    def device = settings.summaryDevice ?: settings.notifyDevice
    if (device) {
        device.deviceNotification(fullMessage)
        log.info "[TellaBoomer] 📨 Enhanced morning wellness summary sent with priority ${priority}"
    }
}

// ============================================================================
// NARRATIVE JOURNEY - STORYTELLING SUMMARY
// ============================================================================

def sendNarrativeJourney() {
    def summary = state.lastOvernightSummary
    if (!summary) return
    
    // Build story chapters
    def bedtimeStory = summary.firstBedroomTime ? 
        "The evening began at <b>${summary.firstBedroomTime}</b> when movement to the bedroom was detected." :
        "Evening activity continued in the <b>${summary.lastActiveRoom ?: 'living areas'}</b> before settling in."
    
    def nightActivity = ""
    if (summary.bathroomVisits == 0) {
        nightActivity = "The night passed peacefully with <b>no bathroom visits</b> - a restful, uninterrupted sleep. ✅"
    } else if (summary.bathroomVisits == 1) {
        nightActivity = "There was <b>one bathroom visit</b> during the night, which is typical and healthy. ✅"
    } else {
        def visits = summary.bathroomVisits
        def deepSleep = summary.deepSleepDisruptions
        nightActivity = "The night included <b>${visits} bathroom visits</b>"
        if (deepSleep > 0) {
            nightActivity += ", with ${deepSleep} occurring during deep sleep hours (2-4 AM) 😴"
        }
        nightActivity += ". "
        
        def baselineVisits = summary.baselineVisits ?: 2.0
        if (visits > baselineVisits) {
            nightActivity += "This is <b>above the typical baseline</b> of ${baselineVisits} visits."
        }
    }
    
    def sittingStory = ""
    if (summary.sittingTotalMinutes > 0) {
        sittingStory = "\n\n<b>🪑 Sitting Patterns:</b>\nTotal sitting time was ${summary.sittingTotalMinutes} minutes across ${summary.sittingSessionCount} sessions."
        if (summary.longestSittingSession >= 120) {
            sittingStory += " The longest session (${summary.longestSittingSession} min) suggests extended rest periods."
        }
    }
    
    def conclusion = ""
    if (summary.riskLevel == "LOW") {
        conclusion = "\n\n<b>✅ Overall Assessment:</b>\nA <b>healthy, normal night</b> with patterns matching established baselines. Continue current routines."
    } else if (summary.riskLevel == "MODERATE") {
        conclusion = "\n\n<b>⚠️ Overall Assessment:</b>\n<b>Some elevated activity</b> compared to normal. Worth monitoring if this pattern continues."
    } else {
        conclusion = "\n\n<b>🔴 Overall Assessment:</b>\n<b>Significant elevation in overnight activity.</b> Review with healthcare provider if pattern persists."
    }
    
    def message = """📖 <b>Last Night's Wellness Journey</b>

<b>Chapter 1: Evening Wind-Down</b> 🌙
${bedtimeStory}

<b>Chapter 2: The Night Watch</b> 🌃
${nightActivity}${sittingStory}

<b>Chapter 3: Sleep Quality</b> 😴
Rest duration was <b>${summary.totalRestHours} hours</b> with <b>${summary.restlessnessPercent}% restlessness</b>.
${conclusion}

<b>📊 Wellness Score: ${summary.wellnessScore}/100</b>

<i>Monitored from ${settings.overnightStartTime} to ${settings.overnightEndTime}</i>"""
    
    // Build message with Pushover driver tags
    def priority = summary.riskLevel == "HIGH" ? "[H]" : "[N]"
    def sound = settings.soundMorningSummary ?: "cosmic"
    def fullMessage = buildPushoverMessage(message, [
        priority: priority,
        title: "TellaBoomer: Last Night's Journey",
        sound: sound
    ])
    
    def device = settings.summaryDevice ?: settings.notifyDevice
    if (device) {
        device.deviceNotification(fullMessage)
        log.info "[TellaBoomer] 📖 Narrative journey summary sent"
    }
}

// ============================================================================
// ENHANCED EXTENDED VISIT ALERT WITH FOLLOW-UP
// ============================================================================

def sendEnhancedExtendedVisitAlert(roomName, durationMinutes, startTime) {
    def urgencyEmoji = durationMinutes >= 20 ? "🔴" : "⚠️"
    def priority = durationMinutes >= 20 ? "[H]" : "[N]"
    def sound = settings.soundExtendedVisit ?: "persistent"
    
    def messageBody = """${urgencyEmoji} <b>Extended Bathroom Visit Alert</b>

<b>Duration: ${durationMinutes} minutes</b>
Location: ${roomName}
Started: ${startTime}
Current Time: ${formatTime(new Date())}

<i>${durationMinutes >= 20 ? 'This is longer than usual - you may want to check in.' : 'Longer than typical visit - monitoring continues.'}</i>"""
    
    def fullMessage = buildPushoverMessage(messageBody, [
        priority: priority,
        title: "Extended Visit Alert",
        sound: sound
    ])
    
    def device = settings.notifyDevice
    if (device) {
        device.deviceNotification(fullMessage)
        
        // Schedule follow-up check in 5 minutes if enabled and duration >= 15
        if (settings.enableFollowUp && durationMinutes >= 15) {
            state.extendedVisitCheckNeeded = [
                room: roomName, 
                startTime: startTime,
                originalDuration: durationMinutes
            ]
            runIn(300, "sendExtendedVisitFollowUp")
        }
        
        log.warn "[TellaBoomer] ⚠️ Enhanced extended visit alert sent: ${durationMinutes} min in ${roomName}"
    }
}

def sendExtendedVisitFollowUp() {
    if (!state.extendedVisitCheckNeeded) return
    
    def data = state.extendedVisitCheckNeeded
    def newDuration = data.originalDuration + 5
    
    def message = """🔴 <b>Follow-Up: Extended Visit Continues</b>

<b>Duration now: ~${newDuration} minutes</b>
Location: ${data.room}
Original alert: ${data.originalDuration} min ago

Would you like to check on them?

<i>Bathroom visit still in progress.</i>"""
    
    settings.notifyDevice?.deviceNotification(message)
    state.extendedVisitCheckNeeded = null
    log.warn "[TellaBoomer] 🔴 Extended visit follow-up sent"
}

// ============================================================================
// DEEP SLEEP DISRUPTION ALERT
// ============================================================================

def sendDeepSleepAlert(roomName) {
    def deepSleepVisits = state.tonightBathroomVisits?.count{it.isDeepSleep} ?: 0
    
    def messageBody = """😴 <b>Deep Sleep Activity Detected</b>

<b>Time: ${formatTime(new Date())}</b>
Location: ${roomName}
Risk Window: 2:00 AM - 4:00 AM 🌙

<b>Tonight's Stats So Far:</b>
• Bathroom Visits: ${state.tonightBathroomVisits?.size() ?: 0}
• Deep Sleep Trips: ${deepSleepVisits}

<i>Monitoring continues - Fall risk is elevated during deep sleep hours.</i>"""
    
    // LOW priority + vibrate only (won't wake user)
    def sound = settings.soundDeepSleep ?: "none"
    def fullMessage = buildPushoverMessage(messageBody, [
        priority: "[L]",
        title: "Deep Sleep Activity",
        sound: sound
    ])
    
    def device = settings.notifyDevice
    if (device && settings.alertOnDeepSleep) {
        device.deviceNotification(fullMessage)
        log.info "[TellaBoomer] 😴 Deep sleep alert sent (low priority)"
    }
}

// ============================================================================
// ENHANCED HIGH RISK ALERT WITH CONCERNS LIST
// ============================================================================

def sendEnhancedHighRiskAlert(summary) {
    def concerns = []
    if (summary.bathroomVisits >= 5) concerns << "Frequent bathroom visits (${summary.bathroomVisits})"
    if (summary.deepSleepDisruptions >= 2) concerns << "Multiple deep sleep disruptions (${summary.deepSleepDisruptions})"
    if (summary.longestVisitDuration >= 15) concerns << "Extended visit (${summary.longestVisitDuration} min)"
    if (summary.restlessnessPercent >= 30) concerns << "High restlessness (${summary.restlessnessPercent}%)"
    
    def messageBody = """🔴 <b>HIGH RISK: Overnight Wellness Alert</b>

<b>Wellness Score: ${summary.wellnessScore}/100</b>
<b>Risk Level: ${summary.riskLevel}</b>

<b>Concerns Identified:</b>
${concerns.collect{"• ${it}"}.join('\n')}

<b>Key Metrics:</b>
• Bathroom Visits: ${summary.bathroomVisits} (vs baseline ${summary.baselineVisits})
• Sleep Quality: ${summary.totalRestHours}h with ${summary.restlessnessPercent}% restlessness
• Deep Sleep Trips: ${summary.deepSleepDisruptions} 😴

<b>📊 Recommended Actions:</b>
1. Review overnight activity details
2. Consider hydration/medication timing
3. Consult with healthcare provider if pattern continues

<i>This represents a significant deviation from baseline patterns.</i>"""
    
    // HIGH priority + urgent sound
    def sound = settings.soundHighRisk ?: "siren"
    def fullMessage = buildPushoverMessage(messageBody, [
        priority: "[H]",
        title: "⚠️ HIGH RISK ALERT",
        sound: sound
    ])
    
    def device = settings.notifyDevice
    if (device) {
        device.deviceNotification(fullMessage)
        log.warn "[TellaBoomer] 🔴 HIGH RISK alert sent"
    }
}

// ============================================================================
// ENHANCED SEDENTARY ALERT
// ============================================================================

def sendEnhancedSedentaryAlert(locationName, durationMinutes) {
    def riskLevel = durationMinutes >= 180 ? "HIGH" : "MODERATE"
    def emoji = riskLevel == "HIGH" ? "🔴" : "⚠️"
    def priority = riskLevel == "HIGH" ? "[H]" : "[N]"
    
    def locationsList = state.sittingTotals?.collect{ k, v -> "${k} (${v}min)" }?.join(', ') ?: locationName
    
    def messageBody = """${emoji} <b>Prolonged Sitting Alert</b>

<b>Duration: ${durationMinutes} minutes</b>
Location: ${locationName}
Risk Level: ${riskLevel}

<b>Health Impact:</b>
• Extended sitting reduces circulation
• Movement recommended every 2 hours
• Short walk or position change advised

<b>Tonight's Sitting:</b>
• Total: ${state.totalSittingMinutes ?: 0} min
• Sessions: ${state.sittingSessionCount ?: 0}
• Locations: ${locationsList}

<i>Gentle reminder to encourage movement.</i>"""
    
    def sound = settings.soundSedentary ?: "magic"
    def fullMessage = buildPushoverMessage(messageBody, [
        priority: priority,
        title: "Prolonged Sitting Alert",
        sound: sound
    ])
    
    def device = settings.notifyDevice
    if (device) {
        device.deviceNotification(fullMessage)
        log.info "[TellaBoomer] 🪑 Enhanced sedentary alert sent"
    }
}

// ============================================================================
// WEEKLY TRENDS REPORT
// ============================================================================

def sendWeeklyTrendsReport() {
    def historicalData = state.historicalData ?: []
    def last7Days = historicalData.takeLast(7)
    
    if (last7Days.size() < 3) {
        log.debug "[TellaBoomer] Not enough data for weekly trends (need 3+ days, have ${last7Days.size()})"
        return
    }
    
    def avgVisits = last7Days.collect{it.bathroomVisits}.sum() / last7Days.size()
    def avgScore = last7Days.collect{it.wellnessScore}.sum() / last7Days.size()
    def avgRestHours = last7Days.collect{it.totalRestHours}.sum() / last7Days.size()
    
    def highRiskNights = last7Days.count{it.riskLevel == "HIGH"}
    def lowRiskNights = last7Days.count{it.riskLevel == "LOW"}
    
    def trend = state.trendDirection ?: "stable"
    def trendEmoji = trend == "improving" ? "📈" : (trend == "declining" ? "📉" : "➡️")
    
    def message = """📈 <b>Weekly Wellness Trends</b>

<b>7-Day Averages:</b>
• Wellness Score: ${Math.round(avgScore)}/100
• Bathroom Visits: ${Math.round(avgVisits * 10) / 10}/night
• Sleep Duration: ${Math.round(avgRestHours * 10) / 10} hours

<b>Risk Distribution:</b>
• ✅ Low Risk Nights: ${lowRiskNights}
• ⚠️ Moderate Risk: ${7 - highRiskNights - lowRiskNights}
• 🔴 High Risk: ${highRiskNights}

<b>Trend: ${trend.toUpperCase()} ${trendEmoji}</b>

<b>Days Monitored:</b> ${state.daysMonitored ?: 0} total

<i>Weekly patterns help identify changes before they become concerning.</i>"""
    
    // LOW priority for informational reports
    def sound = settings.soundMorningSummary ?: "cosmic"
    def fullMessage = buildPushoverMessage(message, [
        priority: "[L]",
        title: "Weekly Wellness Trends",
        sound: sound
    ])
    
    def device = settings.summaryDevice ?: settings.notifyDevice
    if (device) {
        device.deviceNotification(fullMessage)
        log.info "[TellaBoomer] 📈 Weekly trends report sent"
    }
}

// ============================================================================
// QUICK STATUS CHECK-IN
// ============================================================================

def sendQuickStatusCheckIn() {
    def lastMotion = state.lastMotionTime ? formatTimeSince(state.lastMotionTime) : 'Unknown'
    
    def message = """🔔 <b>TellaBoomer Check-In</b>

<b>Current Status:</b>
• Location: ${state.currentLocation ?: 'Unknown'}
• Last Activity: ${lastMotion}
• Mode: ${location.mode}

<b>Today's Activity:</b>
• Room Transitions: ${state.roomTransitions?.size() ?: 0}
• Sitting Time: ${state.totalSittingMinutes ?: 0} min

<b>System Status:</b> ✅ Active
<b>Next Report:</b> ${settings.summaryTime ?: '7:00 AM tomorrow'}

<i>All systems monitoring normally.</i>"""
    
    // SILENT priority for routine check-ins
    def fullMessage = buildPushoverMessage(message, [
        priority: "[S]",
        title: "TellaBoomer Status",
        sound: "none"
    ])
    
    def device = settings.notifyDevice
    if (device) {
        device.deviceNotification(fullMessage)
        log.info "[TellaBoomer] 🔔 Status check-in sent"
    }
}

// ============================================================================
// HELPER FUNCTION: FORMAT TIME SINCE
// ============================================================================

def formatTimeSince(timestamp) {
    def now = now()
    def diff = now - timestamp
    def minutes = Math.round(diff / 60000)
    
    if (minutes < 1) return "Just now"
    if (minutes == 1) return "1 minute ago"
    if (minutes < 60) return "${minutes} minutes ago"
    
    def hours = Math.round(minutes / 60)
    if (hours == 1) return "1 hour ago"
    if (hours < 24) return "${hours} hours ago"
    
    def days = Math.round(hours / 24)
    if (days == 1) return "1 day ago"
    return "${days} days ago"
}

// ============================================================================
// HELPER FUNCTION: GET CRON FOR WEEKLY REPORT
// ============================================================================

def getCronForWeeklyReport() {
    def time = settings.weeklyReportTime
    def dayOfWeek = ["Sunday": "0", "Monday": "1", "Tuesday": "2", 
                     "Wednesday": "3", "Thursday": "4", "Friday": "5", 
                     "Saturday": "6"][settings.weeklyReportDay]
    
    def parts = time.split(":")
    def hour = parts[0]
    def minute = parts[1]
    
    return "0 ${minute} ${hour} ? * ${dayOfWeek}"
}

// ============================================================================
// PUSHOVER DRIVER INTEGRATION HELPERS
// ============================================================================

def getPushoverSounds() {
    return [
        "none": "None (Vibrate Only)",
        "pushover": "Pushover (Default)",
        "bike": "Bike",
        "bugle": "Bugle",
        "cashregister": "Cash Register",
        "classical": "Classical",
        "cosmic": "Cosmic",
        "falling": "Falling",
        "gamelan": "Gamelan",
        "incoming": "Incoming",
        "intermission": "Intermission",
        "magic": "Magic",
        "mechanical": "Mechanical",
        "pianobar": "Piano Bar",
        "siren": "Siren",
        "spacealarm": "Space Alarm",
        "tugboat": "Tug Boat",
        "alien": "Alien Alarm (long)",
        "climb": "Climb (long)",
        "persistent": "Persistent (long)",
        "echo": "Pushover Echo (long)",
        "updown": "Up Down (long)",
        "vibrate": "Vibrate Only",
        "none": "Silent"
    ]
}

def buildPushoverMessage(messageBody, options = [:]) {
    // Build message with Pushover driver tags
    def parts = []
    
    // Always use HTML formatting
    parts << "[HTML]"
    
    // Priority: [S]=silent, [L]=low, [N]=normal, [H]=high, [E]=emergency
    def priority = options.priority ?: "[N]"
    parts << priority
    
    // Custom title with ^title^
    if (options.title) {
        parts << "^${options.title}^"
    }
    
    // Custom sound with #sound#
    if (options.sound && options.sound != "none") {
        parts << "#${options.sound}#"
    }
    
    // Dashboard URL with §url§ and ¤linkText¤
    if (settings.dashboardUrl) {
        parts << "§${settings.dashboardUrl}§"
        def linkText = settings.dashboardLinkText ?: "View Dashboard"
        parts << "¤${linkText}¤"
    }
    
    // Message body
    parts << messageBody
    
    return parts.join("")
}