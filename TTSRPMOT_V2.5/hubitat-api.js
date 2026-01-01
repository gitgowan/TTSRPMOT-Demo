/**
 * ========================================
 * HUBITAT API WRAPPER
 * ========================================
 * This handles all communication with your Hubitat hub
 * You don't need to edit this file!
 */

class HubitatAPI {
    constructor(config) {
        if (!config || !config.isValid) {
            this.error = config ? config.error : 'Configuration not loaded';
            this.isConfigured = false;
            console.error('❌ Hubitat API Error:', this.error);
            return;
        }
        
        this.baseUrl = config.baseUrl;
        this.appId = config.appId;
        this.accessToken = config.accessToken;
        this.requestTimeout = config.requestTimeout || 10000;
        this.isConfigured = true;
        
        console.log('✅ Hubitat API initialized');
        console.log('   Base URL:', this.baseUrl);
        console.log('   App ID:', this.appId);
    }
    
    /**
     * Test connection to Hubitat hub
     * Returns: { success: true/false, message: string, data: object }
     */
    async testConnection() {
        if (!this.isConfigured) {
            return {
                success: false,
                message: this.error,
                data: null
            };
        }
        
        try {
            console.log('🔍 Testing connection to Hubitat...');
            const response = await this._makeRequest('/devices');
            
            if (response.ok) {
                const devices = await response.json();
                return {
                    success: true,
                    message: `Connected! Found ${devices.length} devices`,
                    data: {
                        deviceCount: devices.length,
                        devices: devices
                    }
                };
            } else {
                return {
                    success: false,
                    message: `HTTP ${response.status}: ${response.statusText}`,
                    data: null
                };
            }
            
        } catch (error) {
            return {
                success: false,
                message: `Connection failed: ${error.message}`,
                data: null
            };
        }
    }
    
    /**
     * Get all Hub Variables
     * These are the wellness metrics from your Wellness Activity Tracker app
     */
    async getHubVariables() {
        if (!this.isConfigured) {
            throw new Error(this.error);
        }
        
        try {
            console.log('📊 Fetching hub variables...');
            
            // Hubitat endpoint for hub variables
            const url = `${this.baseUrl}/hubInfo?access_token=${this.accessToken}`;
            
            const response = await this._makeRequest('/hubInfo');
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const data = await response.json();
            
            // Hub variables are in the response - extract them
            // The Wellness Activity Tracker creates variables like:
            // wellness_overnight_score, wellness_fall_detected, etc.
            
            return {
                success: true,
                variables: data.variables || {},
                rawData: data
            };
            
        } catch (error) {
            console.error('❌ Error fetching hub variables:', error);
            return {
                success: false,
                error: error.message,
                variables: {}
            };
        }
    }
    
    /**
     * Get wellness data formatted for dashboards
     * This extracts all the wellness_* variables your app creates
     */
    async getWellnessData() {
        const result = await this.getHubVariables();
        
        if (!result.success) {
            return {
                success: false,
                error: result.error,
                data: {}
            };
        }
        
        // Extract only wellness-related variables
        const wellnessData = {};
        
        if (result.rawData && result.rawData.hsmStatus) {
            // Hub info endpoint returns different structure
            // We need to get variables differently
            console.log('⚠️ Hub info endpoint - trying variable approach...');
        }
        
        // For now, return the full variable set
        // You'll need to adapt this based on your actual Hubitat response structure
        return {
            success: true,
            data: result.variables
        };
    }
    
    /**
     * Get specific device by name or ID
     */
    async getDevice(deviceIdOrName) {
        if (!this.isConfigured) {
            throw new Error(this.error);
        }
        
        try {
            const response = await this._makeRequest(`/devices/${deviceIdOrName}`);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            return await response.json();
            
        } catch (error) {
            console.error('❌ Error fetching device:', error);
            throw error;
        }
    }
    
    /**
     * Get all devices
     */
    async getAllDevices() {
        if (!this.isConfigured) {
            throw new Error(this.error);
        }
        
        try {
            const response = await this._makeRequest('/devices');
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            return await response.json();
            
        } catch (error) {
            console.error('❌ Error fetching devices:', error);
            throw error;
        }
    }
    
    /**
     * Make an HTTP request to Hubitat with timeout
     * @private
     */
    async _makeRequest(endpoint) {
        const url = `${this.baseUrl}/apps/${this.appId}${endpoint}${endpoint.includes('?') ? '&' : '?'}access_token=${this.accessToken}`;
        
        console.log('🌐 Request:', url.replace(this.accessToken, 'TOKEN_HIDDEN'));
        
        return await this._fetchWithTimeout(url, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        }, this.requestTimeout);
    }
    
    /**
     * Fetch with timeout
     * @private
     */
    async _fetchWithTimeout(url, options, timeout) {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), timeout);
        
        try {
            const response = await fetch(url, {
                ...options,
                signal: controller.signal
            });
            clearTimeout(timeoutId);
            return response;
        } catch (error) {
            clearTimeout(timeoutId);
            if (error.name === 'AbortError') {
                throw new Error('Request timeout - check your network connection');
            }
            throw error;
        }
    }
}

/**
 * Helper function to create API instance
 */
function createHubitatAPI() {
    if (typeof ParsedConfig === 'undefined') {
        console.error('❌ ParsedConfig not found - make sure hubitat-config.js is loaded first!');
        return null;
    }
    
    return new HubitatAPI(ParsedConfig);
}

// Export for use in other files
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { HubitatAPI, createHubitatAPI };
}
