/**
 * ========================================
 * HUBITAT MAKER API CONFIGURATION
 * ========================================
 * 
 * This is the ONLY file you need to edit to connect your dashboards!
 * 
 * HOW TO FIND YOUR MAKER API CREDENTIALS:
 * ========================================
 * 
 * STEP 1: Open Hubitat Web Interface
 * -----------------------------------
 * Go to: http://YOUR-HUB-IP-ADDRESS
 * (Example: http://192.168.1.100 or http://hubitat.local)
 * 
 * 
 * STEP 2: Navigate to Apps
 * -------------------------
 * Click: "Apps" in the left sidebar
 * 
 * 
 * STEP 3: Open Maker API App
 * ---------------------------
 * Find "Maker API" in your installed apps list
 * Click on it to open
 * 
 * If you DON'T see Maker API:
 *   1. Click "Add Built-In App" 
 *   2. Select "Maker API"
 *   3. Select all devices you want to expose
 *   4. Click "Done"
 * 
 * 
 * STEP 4: Get Your Cloud Endpoint URL
 * ------------------------------------
 * Inside Maker API app, scroll down to section:
 * "Get All Devices with Full Details"
 * 
 * You'll see a URL that looks like:
 * https://cloud.hubitat.com/api/abc123-def456-ghi789/apps/12/devices?access_token=your-long-token-here
 *                            ^^^^^^^^^^^^^^^^^^^^^^^^     ^^                       ^^^^^^^^^^^^^^^^^^^^^^
 *                            This is your HUB UID         APP ID                   This is your ACCESS TOKEN
 * 
 * Copy the ENTIRE URL and paste it into MAKER_API_FULL_URL below
 * 
 * 
 * STEP 5: Paste Below & You're Done!
 * -----------------------------------
 */

const HubitatConfig = {
    // ===========================================
    // PASTE YOUR FULL MAKER API URL HERE
    // ===========================================
    // 
    // Example format:
    // 'https://cloud.hubitat.com/api/abc123-def456-ghi789/apps/12/devices?access_token=your-token-here'
    //
    MAKER_API_FULL_URL: 'PASTE_YOUR_URL_HERE',
    
    
    // ===========================================
    // REFRESH SETTINGS (Usually don't need to change these)
    // ===========================================
    
    // How often to refresh dashboard data (in milliseconds)
    // 30000 = 30 seconds (recommended)
    REFRESH_INTERVAL: 30000,
    
    // Timeout for API requests (in milliseconds)
    // 10000 = 10 seconds
    REQUEST_TIMEOUT: 10000,
    
    
    // ===========================================
    // ADVANCED: Manual Override (Optional)
    // ===========================================
    // If the auto-parsing fails, you can manually set these:
    //
    // BASE_URL: 'https://cloud.hubitat.com/api/YOUR-HUB-UID',
    // APP_ID: '12',  // Your Maker API app ID number
    // ACCESS_TOKEN: 'your-access-token-here'
};


/**
 * ========================================
 * AUTO-PARSER (Don't edit below this line)
 * ========================================
 * This automatically extracts the pieces from your full URL
 */

// Parse the full URL into components
function parseHubitatURL(fullUrl) {
    if (!fullUrl || fullUrl === 'PASTE_YOUR_URL_HERE') {
        return {
            isValid: false,
            error: 'Please paste your Maker API URL in hubitat-config.js'
        };
    }
    
    try {
        // Extract base URL (everything before /apps/)
        const baseMatch = fullUrl.match(/(https?:\/\/[^\/]+\/api\/[^\/]+)/);
        if (!baseMatch) {
            return {
                isValid: false,
                error: 'Invalid URL format - cannot find base URL'
            };
        }
        const baseUrl = baseMatch[1];
        
        // Extract app ID (number after /apps/)
        const appIdMatch = fullUrl.match(/\/apps\/(\d+)/);
        if (!appIdMatch) {
            return {
                isValid: false,
                error: 'Invalid URL format - cannot find app ID'
            };
        }
        const appId = appIdMatch[1];
        
        // Extract access token (after access_token=)
        const tokenMatch = fullUrl.match(/access_token=([^&]+)/);
        if (!tokenMatch) {
            return {
                isValid: false,
                error: 'Invalid URL format - cannot find access token'
            };
        }
        const accessToken = tokenMatch[1];
        
        return {
            isValid: true,
            baseUrl: baseUrl,
            appId: appId,
            accessToken: accessToken,
            fullUrl: fullUrl
        };
        
    } catch (error) {
        return {
            isValid: false,
            error: 'Failed to parse URL: ' + error.message
        };
    }
}

// Parse the URL and make it available
const ParsedConfig = parseHubitatURL(HubitatConfig.MAKER_API_FULL_URL);

// Add refresh settings to parsed config
if (ParsedConfig.isValid) {
    ParsedConfig.refreshInterval = HubitatConfig.REFRESH_INTERVAL;
    ParsedConfig.requestTimeout = HubitatConfig.REQUEST_TIMEOUT;
}

// Export for use in other files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { HubitatConfig, ParsedConfig };
}
