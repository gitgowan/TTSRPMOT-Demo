# 📋 Quick Reference Card

## 🎯 5-Minute Setup Checklist

### ✅ Step 1: Get Maker API URL
1. Open: `http://YOUR-HUB-IP`
2. Click: **Apps** → **Maker API**
3. Copy the full URL from "Get All Devices" section
4. Should look like: `https://cloud.hubitat.com/api/.../access_token=...`

### ✅ Step 2: Configure
1. Open: `hubitat-config.js`
2. Paste URL in: `MAKER_API_FULL_URL: 'YOUR_URL'`
3. Save file

### ✅ Step 3: Test
1. Open: `hubitat-connection-tester.html` in browser
2. Click: **Test Connection**
3. See: ✅ "Connection Successful!"

### ✅ Step 4: Deploy
1. Upload all files to Netlify or open locally
2. Check browser console (F12) for "✅ Updated with live data"
3. Done!

---

## 📂 File Structure

```
your-dashboard-folder/
├── hubitat-config.js          ← EDIT THIS (paste your URL)
├── hubitat-api.js              ← Don't edit
├── hubitat-connection-tester.html  ← Use to test
├── SETUP_GUIDE.md             ← Full instructions
├── index.html                  ← Dashboard gallery
└── TellaBoomer_*_Dashboard.html    ← Your dashboards
```

---

## 🔧 Common Issues

| Problem | Solution |
|---------|----------|
| "Configuration not loaded" | Make sure `hubitat-config.js` is in same folder as HTML |
| "Connection timeout" | Check hub is online at `http://HUB-IP` |
| "Invalid URL format" | Copy ENTIRE URL including `access_token=...` |
| "No wellness variables" | Verify Wellness Activity Tracker app is running |
| Variables undefined | Check Hubitat Settings → Hub Variables |

---

## 📊 Hub Variable Names

Your Wellness Activity Tracker creates these:

**Safety:**
- `wellness_fall_detected` - Boolean
- `wellness_fall_count_today` - Number
- `wellness_avg_temperature` - Number (°F)

**Sleep:**
- `wellness_sleep_quality_score` - 0-100
- `wellness_total_bed_time` - Minutes
- `wellness_sleep_restlessness` - Percentage

**Hygiene:**
- `wellness_shower_adherence_percent` - 0-100
- `wellness_shower_days_7day` - 0-7
- `wellness_last_shower_time` - String

**Activity:**
- `wellness_bathroom_visits` - Number
- `wellness_toilet_flushes` - Number
- `wellness_total_events` - Number

---

## 🆘 Troubleshooting Commands

**Check configuration in browser console:**
```javascript
console.log(ParsedConfig);
// Should show: { isValid: true, baseUrl: "...", ... }
```

**Test API directly:**
```javascript
const api = createHubitatAPI();
api.testConnection().then(result => console.log(result));
// Should show: { success: true, ... }
```

**Get wellness data:**
```javascript
const api = createHubitatAPI();
api.getWellnessData().then(data => console.log(data));
// Should show your hub variables
```

---

## 🔐 Security Checklist

- [ ] Access token is private (don't share `hubitat-config.js`)
- [ ] Using HTTPS cloud endpoint (secure by default)
- [ ] Only necessary devices exposed in Maker API
- [ ] Not committing config to public repos

---

## 📞 Getting Help

1. **Check connection:** Open `hubitat-connection-tester.html`
2. **Check console:** Press F12, look for errors
3. **Verify hub:** Go to `http://HUB-IP` in browser
4. **Check variables:** Hubitat Settings → Hub Variables

---

## 🎉 Success Indicators

You'll know it's working when you see:

✅ Green "Live Data Connected" badge
✅ Browser console shows: "✅ Updated with live data"
✅ Metrics update every 30 seconds
✅ Values change when you test sensors

---

## 🚀 Next Steps

1. **Customize refresh rate** in `hubitat-config.js`
2. **Add more dashboards** using same API
3. **Deploy to Netlify** for remote access
4. **Share link** with family/caregivers

---

**Remember:** All files must be in the same folder for the API to work!
