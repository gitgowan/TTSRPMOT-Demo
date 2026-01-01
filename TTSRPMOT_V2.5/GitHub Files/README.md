# 🏗️ TellaBoomer Hubitat Integration System

## Complete Dashboard Connection Framework

This system connects your TellaBoomer dashboards to live data from your Hubitat hub running the **Wellness Activity Tracker v2.3.4** app.

---

## 📦 What's Included

### Core Files (Required)
1. **`hubitat-config.js`** - Configuration file (YOU EDIT THIS)
2. **`hubitat-api.js`** - API wrapper (don't edit)
3. **`hubitat-connection-tester.html`** - Test utility

### Documentation
4. **`SETUP_GUIDE.md`** - Complete setup instructions
5. **`QUICK_REFERENCE.md`** - Quick troubleshooting guide
6. **`README.md`** - This file

### Example
7. **`TellaBoomer_Family_Dashboard_LIVE_EXAMPLE.html`** - Working example

---

## 🎯 Quick Start (3 Steps)

### 1️⃣ Get Your Maker API URL

```
Hubitat → Apps → Maker API → Copy "Get All Devices" URL
```

Example URL:
```
https://cloud.hubitat.com/api/abc-123/apps/12/devices?access_token=xyz789
```

### 2️⃣ Configure

Edit `hubitat-config.js`:
```javascript
MAKER_API_FULL_URL: 'PASTE_YOUR_FULL_URL_HERE',
```

### 3️⃣ Test

Open `hubitat-connection-tester.html` → Click "Test Connection" → See ✅

---

## 🏗️ System Architecture

Think of it like a **water system**:

```
┌─────────────────┐
│  Hubitat Hub    │  ← Water source (your data)
│  with Wellness  │
│  Activity App   │
└────────┬────────┘
         │
         │ Maker API (the pipe)
         │
┌────────▼────────┐
│ hubitat-api.js  │  ← Pump (handles communication)
└────────┬────────┘
         │
         │ Data flow
         │
┌────────▼────────┐
│  Dashboards     │  ← Faucets (your UI)
│  (HTML files)   │
└─────────────────┘
```

### How It Works

1. **Configuration Layer** (`hubitat-config.js`)
   - Stores your Maker API credentials
   - Parses URL into components
   - Sets refresh intervals

2. **API Layer** (`hubitat-api.js`)
   - Creates connection to Hubitat
   - Fetches device data
   - Extracts wellness variables
   - Handles errors gracefully

3. **Dashboard Layer** (Your HTML files)
   - Displays data beautifully
   - Updates automatically
   - Falls back to demo mode if connection fails

---

## 🔌 Using the API in Your Dashboards

### Basic Pattern

```html
<!DOCTYPE html>
<html>
<head>
    <!-- Load API files -->
    <script src="hubitat-config.js"></script>
    <script src="hubitat-api.js"></script>
</head>
<body>
    <div id="wellness">--</div>
    
    <script>
        // Create API instance
        const api = createHubitatAPI();
        
        // Fetch and display data
        async function update() {
            const devices = await api.getAllDevices();
            const data = extractWellnessData(devices);
            
            document.getElementById('wellness').textContent = 
                data.wellness_overnight_score || 'N/A';
        }
        
        // Initial load + auto-refresh
        update();
        setInterval(update, 30000);
    </script>
</body>
</html>
```

### Available API Methods

```javascript
const api = createHubitatAPI();

// Test connection
await api.testConnection()
// Returns: { success: true/false, message: string, data: object }

// Get all devices
await api.getAllDevices()
// Returns: Array of device objects

// Get specific device
await api.getDevice(deviceId)
// Returns: Device object

// Get wellness data (helper)
await api.getWellnessData()
// Returns: { success: bool, data: {...wellness variables} }
```

---

## 📊 Wellness Variable Reference

Your **Wellness Activity Tracker v2.3.4** creates these hub variables:

### Critical Safety
```javascript
wellness_overnight_score         // 0-100
wellness_fall_detected          // true/false
wellness_fall_count_today       // number
wellness_fall_time              // timestamp
wellness_fall_location          // room name
wellness_avg_temperature        // °F
```

### Sleep Quality
```javascript
wellness_sleep_quality_score    // 0-100
wellness_total_bed_time         // minutes
wellness_sleep_restlessness     // percentage
wellness_in_bed                 // true/false
wellness_bedroom_visits         // number
```

### Hygiene & Self-Care
```javascript
wellness_shower_adherence_percent  // 0-100
wellness_shower_days_7day          // 0-7
wellness_daily_showers             // number today
wellness_last_shower_time          // timestamp
wellness_last_shower_duration      // minutes
wellness_morning_showers           // count
wellness_evening_showers           // count
```

### Bathroom Activity
```javascript
wellness_bathroom_visits        // count today
wellness_toilet_flushes         // count today
wellness_last_flush_time        // timestamp
```

### Movement & Activity
```javascript
wellness_total_events           // motion events
wellness_current_location       // room name
wellness_presence_active        // true/false
wellness_current_presence_room  // room name
wellness_last_presence_duration // minutes
wellness_daily_presence_events  // count
```

---

## 🛠️ Customization Options

### Change Refresh Rate

In `hubitat-config.js`:
```javascript
REFRESH_INTERVAL: 30000,  // 30 seconds (default)
// Change to: 60000 for 1 minute
//            10000 for 10 seconds
```

### Change Request Timeout

```javascript
REQUEST_TIMEOUT: 10000,  // 10 seconds (default)
// Increase if you have slow network
```

### Manual Configuration (Advanced)

If auto-parsing fails:
```javascript
const HubitatConfig = {
    BASE_URL: 'https://cloud.hubitat.com/api/YOUR-HUB-ID',
    APP_ID: '12',
    ACCESS_TOKEN: 'your-token-here'
};
```

---

## 🚨 Error Handling

The system gracefully handles errors:

1. **Configuration Errors**
   - Shows clear error message
   - Displays what's wrong
   - Offers retry button

2. **Connection Failures**
   - Falls back to demo data
   - Shows "Demo Mode" badge
   - Logs error to console

3. **Missing Variables**
   - Returns empty object
   - Dashboards show "--" placeholders
   - Warns in console

### Debug Mode

Open browser console (F12):
```javascript
// Check configuration
console.log(ParsedConfig);

// Test connection manually
const api = createHubitatAPI();
api.testConnection().then(console.log);

// View all devices
api.getAllDevices().then(console.log);
```

---

## 📁 Deployment Options

### Option 1: Local Testing
1. Put all files in one folder
2. Open `hubitat-connection-tester.html` in browser
3. Test connection
4. Open any dashboard HTML file

### Option 2: Netlify Deploy
1. Keep all files together:
```
my-dashboard/
├── hubitat-config.js
├── hubitat-api.js
├── index.html
└── TellaBoomer_*.html
```
2. Drag folder to: https://app.netlify.com/drop
3. Done! Live in seconds

### Option 3: Web Server
1. Upload all files to web server
2. Ensure files maintain same directory structure
3. Access via your domain

---

## 🔐 Security Best Practices

### ✅ DO:
- Keep `hubitat-config.js` private
- Use HTTPS cloud endpoint (default)
- Only expose needed devices in Maker API
- Rotate token if accidentally exposed

### ❌ DON'T:
- Commit `hubitat-config.js` to public GitHub
- Share your access token publicly
- Expose all devices in Maker API

### Regenerate Token

If token is compromised:
1. Hubitat → Apps → Maker API
2. Scroll to bottom
3. Click "Generate new token"
4. Update `hubitat-config.js`

---

## 🧪 Testing Checklist

Before deploying:

- [ ] `hubitat-connection-tester.html` shows ✅ green
- [ ] Browser console shows no errors
- [ ] Dashboard updates every 30 seconds
- [ ] Values change when you trigger sensors
- [ ] Falls back gracefully if hub offline
- [ ] All wellness variables present

---

## 📚 File Descriptions

| File | Purpose | Edit? |
|------|---------|-------|
| `hubitat-config.js` | Store your credentials | ✅ YES |
| `hubitat-api.js` | Handle communication | ❌ NO |
| `hubitat-connection-tester.html` | Test connection | ❌ NO |
| `SETUP_GUIDE.md` | Full instructions | ❌ NO |
| `QUICK_REFERENCE.md` | Quick help | ❌ NO |
| Dashboard HTML files | Display data | ⚠️ IF NEEDED |

---

## 🎓 Learning Path

### Beginner
1. Read `QUICK_REFERENCE.md`
2. Configure `hubitat-config.js`
3. Test with `hubitat-connection-tester.html`
4. Use existing dashboards

### Intermediate
1. Read `SETUP_GUIDE.md`
2. Study `TellaBoomer_Family_Dashboard_LIVE_EXAMPLE.html`
3. Modify existing dashboards
4. Add custom metrics

### Advanced
1. Study `hubitat-api.js` source
2. Add custom API methods
3. Create new dashboards from scratch
4. Build custom integrations

---

## 🆘 Troubleshooting

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| "Configuration not loaded" | File path wrong | Ensure same folder |
| "Connection timeout" | Hub offline | Check hub at `http://HUB-IP` |
| "Invalid URL" | Partial URL copied | Copy ENTIRE URL |
| Variables undefined | App not running | Check Wellness Activity Tracker |
| Demo mode stuck | Wrong endpoint | Verify Maker API settings |

---

## 🎉 What's Next?

Now that your system is connected:

1. **Customize** dashboards to your needs
2. **Add alerts** based on wellness thresholds
3. **Share** dashboard link with family
4. **Monitor** trends over time
5. **Expand** with additional sensors

---

## 📞 Support Resources

- **Setup Issues:** See `SETUP_GUIDE.md`
- **Quick Fixes:** See `QUICK_REFERENCE.md`
- **Testing:** Use `hubitat-connection-tester.html`
- **Examples:** Study `TellaBoomer_Family_Dashboard_LIVE_EXAMPLE.html`

---

## 🏆 Success Checklist

- [ ] Maker API URL copied from Hubitat
- [ ] `hubitat-config.js` configured
- [ ] Connection tester shows ✅ green
- [ ] Dashboard shows "Live Data Connected"
- [ ] Browser console shows no errors
- [ ] Data updates automatically
- [ ] Falls back to demo gracefully
- [ ] Ready to deploy!

---

**Congratulations!** 🎊 

Your TellaBoomer dashboards are now connected to live Hubitat data!

---

*This integration framework makes it dead-simple to connect any dashboard to your Hubitat hub. Just configure once, use everywhere.*
