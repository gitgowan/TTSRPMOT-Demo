# 🚀 TellaBoomer Dashboard Setup Guide

## Complete Setup in 5 Minutes

This guide will walk you through connecting your dashboards to your live Hubitat hub with real data from the Wellness Activity Tracker app.

---

## 📋 What You'll Need

1. Your Hubitat hub with **Wellness Activity Tracker v2.3.4** installed
2. The **Maker API** app enabled on your hub
3. These files (already included):
   - `hubitat-config.js` ← You'll edit this one
   - `hubitat-api.js` ← Don't edit
   - `hubitat-connection-tester.html` ← Use this to test
   - Your dashboard HTML files

---

## 🔧 Step 1: Find Your Maker API Credentials

### A. Open Hubitat Web Interface

1. Open your web browser
2. Go to: `http://YOUR-HUB-IP-ADDRESS`
   - Example: `http://192.168.1.100`
   - Or: `http://hubitat.local`

### B. Navigate to Maker API

1. Click **"Apps"** in the left sidebar
2. Look for **"Maker API"** in your installed apps list
3. Click on it to open

**Don't see Maker API?**
1. Click **"Add Built-In App"**
2. Select **"Maker API"**
3. Select ALL devices (or at least the ones you want exposed)
4. Click **"Done"**

### C. Get Your Cloud Endpoint URL

Inside the Maker API app:

1. Scroll down to the section labeled:
   ```
   Get All Devices with Full Details
   ```

2. You'll see a URL that looks like this:
   ```
   https://cloud.hubitat.com/api/abc123-def456-ghi789/apps/12/devices?access_token=your-long-token-here
   ```

3. **COPY THE ENTIRE URL** (right-click → Copy)
   - Make sure you get the whole thing including `access_token=...`

---

## ✏️ Step 2: Configure hubitat-config.js

1. Open `hubitat-config.js` in a text editor (Notepad, VS Code, etc.)

2. Find this line:
   ```javascript
   MAKER_API_FULL_URL: 'PASTE_YOUR_URL_HERE',
   ```

3. Replace `PASTE_YOUR_URL_HERE` with your copied URL:
   ```javascript
   MAKER_API_FULL_URL: 'https://cloud.hubitat.com/api/abc123.../access_token=xyz...',
   ```

4. **Save the file**

That's it! You've configured your connection.

---

## 🧪 Step 3: Test Your Connection

1. Open `hubitat-connection-tester.html` in your web browser
   - Just double-click the file
   - Or drag it into your browser

2. The page will automatically check your configuration

3. Click the **"🚀 Test Connection"** button

4. You should see:
   ```
   ✅ Connection Successful!
   Connected! Found X devices
   ```

**If you see an error:**
- Go back to Step 2 and verify you copied the ENTIRE URL
- Make sure your Hubitat hub is online and accessible
- Check that Maker API is enabled

---

## 📊 Step 4: Update Your Dashboards

Now that your connection works, let's connect a dashboard to live data.

### Example: Family Dashboard with Live Data

Open your dashboard HTML file and add these lines in the `<head>` section:

```html
<!-- Load Hubitat API -->
<script src="hubitat-config.js"></script>
<script src="hubitat-api.js"></script>
```

Then replace the demo `updateDashboard()` function with this:

```javascript
// Create API instance
const hubitatAPI = createHubitatAPI();

async function updateDashboard() {
    if (!hubitatAPI || !hubitatAPI.isConfigured) {
        console.error('❌ Hubitat API not configured');
        // Fall back to demo data
        updateWithDemoData();
        return;
    }
    
    try {
        // Get wellness data from hub
        const result = await hubitatAPI.getWellnessData();
        
        if (result.success) {
            const hubData = result.data;
            
            // Update dashboard with real data
            updateSafetyMetrics(hubData);
            updateDailyRoutine(hubData);
            updateActivityMetrics(hubData);
            
            // Update timestamp
            document.getElementById('lastUpdate').textContent = 
                new Date().toLocaleTimeString();
                
            console.log('✅ Dashboard updated with live data');
        } else {
            console.error('❌ Failed to get wellness data:', result.error);
            updateWithDemoData();
        }
        
    } catch (error) {
        console.error('❌ Error updating dashboard:', error);
        updateWithDemoData();
    }
}
```

---

## 🔄 Step 5: Deploy & Enjoy!

### Option A: Test Locally
1. Open your dashboard HTML file in a browser
2. Check the browser console (F12) for connection status
3. You should see live data flowing in!

### Option B: Deploy to Netlify
1. Keep all files together in one folder:
   ```
   your-folder/
   ├── hubitat-config.js
   ├── hubitat-api.js
   ├── index.html (your gallery)
   ├── TellaBoomer_Family_Dashboard_v2.5.html
   ├── TellaBoomer_Clinical_Assessment_Dashboard_v2.5.html
   └── ... (other dashboards)
   ```

2. Drag the folder to Netlify Drop: https://app.netlify.com/drop

3. Your site goes live instantly with real data!

---

## 🎯 Variable Names Reference

Your **Wellness Activity Tracker v2.3.4** creates these hub variables:

### Safety & Critical
- `wellness_overnight_score` - Overall wellness score (0-100)
- `wellness_fall_detected` - Boolean: fall detected
- `wellness_fall_time` - Timestamp of last fall
- `wellness_fall_location` - Room where fall occurred
- `wellness_fall_count_today` - Number of falls today
- `wellness_avg_temperature` - Average home temperature

### Sleep Quality
- `wellness_sleep_quality_score` - Sleep score (0-100)
- `wellness_total_bed_time` - Minutes in bed
- `wellness_sleep_restlessness` - Restlessness percentage
- `wellness_in_bed` - Boolean: currently in bed

### Daily Routine
- `wellness_toilet_flushes` - Flush count today
- `wellness_bathroom_visits` - Bathroom visit count
- `wellness_last_flush_time` - Last flush timestamp
- `wellness_daily_showers` - Shower count today
- `wellness_last_shower_time` - Last shower timestamp
- `wellness_shower_adherence_percent` - 7-day shower %
- `wellness_shower_days_7day` - Shower days in last week

### Activity
- `wellness_total_events` - Total motion events
- `wellness_current_location` - Current room
- `wellness_presence_active` - Boolean: presence detected
- `wellness_current_presence_room` - Room with presence
- `wellness_last_presence_duration` - Minutes in current room

---

## ❓ Troubleshooting

### "Configuration not loaded" error
- Make sure `hubitat-config.js` is in the same folder as your HTML file
- Check that the script tag loads BEFORE `hubitat-api.js`

### "Connection timeout" error
- Verify your Hubitat hub is online
- Check your network connection
- Try accessing the Maker API URL directly in your browser

### Variables showing as undefined
- Confirm Wellness Activity Tracker app is running
- Check that hub variables are created (Hubitat Settings → Hub Variables)
- Variable names must match exactly (case-sensitive)

### Still having issues?
1. Open browser console (F12)
2. Look for error messages
3. Check the Network tab to see if API calls are being made
4. Verify the URL in the Network tab matches your Maker API URL

---

## 🎉 Success Checklist

- [ ] Copied Maker API URL from Hubitat
- [ ] Pasted URL into `hubitat-config.js`
- [ ] Tested connection (green checkmark)
- [ ] Updated dashboard to use `createHubitatAPI()`
- [ ] Seeing live data in dashboard
- [ ] Deployed to Netlify (optional)

---

## 🔐 Security Notes

**Is this secure?**

- ✅ Your access token is in your config file (not visible to others)
- ✅ Hubitat's cloud endpoint uses HTTPS encryption
- ✅ Only devices you select in Maker API are exposed
- ⚠️ Don't share your `hubitat-config.js` publicly (contains token)
- ⚠️ If deploying publicly, consider IP restrictions in Maker API

**Best practices:**
1. Only expose necessary devices in Maker API
2. Don't commit `hubitat-config.js` to public GitHub repos
3. Regenerate token if accidentally exposed
4. Use Hubitat's cloud endpoint (already secured)

---

## 📚 Next Steps

Now that your dashboards are connected:

1. **Customize** - Adjust refresh intervals in `hubitat-config.js`
2. **Expand** - Add more dashboards using the same API
3. **Share** - Deploy to Netlify and share with family/care team
4. **Monitor** - Set up alerts based on wellness metrics

---

**Need Help?**

- Check browser console (F12) for detailed error messages
- Verify Wellness Activity Tracker v2.3.4 is running
- Confirm hub variables exist in Hubitat Settings
- Test connection using `hubitat-connection-tester.html`

**You're all set! 🎊**
