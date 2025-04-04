/**
 * IMPORT URL: https://raw.githubusercontent.com/HubitatCommunity/HoneywellThermo-TCC/master/HoneywellThermo-TCC_C.groovy
 *
 *  Total Comfort API
 *   
 *  Based on Code by Eric Thomas, Edited by Bob Jase, and C Steele
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *
 * csteele: v1.3.21  Added "accumulation" feature to setStatus.
 *                   login returns true/false allowing get/setStatus to retry login
 * csteele: v1.3.20  Added "emergency/auxiliary" heat.
 *                    added fanOperatingState Attribute.
 * csteele: v1.3.19  FollowSchedule typo.
 * csteele: v1.3.18  FollowSchedule enhanced.
 *                    added HoldTime and TemporaryHoldUntilTime into data storage.
 *                    added isScheduleCapable to FollowSchedule check.
 *     lgk: v1.3.17  initialize device.data.unit 
 * tinfoil: v1.3.16  fix to heatLevelUp/Down commands
 *     lgk: v1.3.15  add retries after unauthorized and read failures.
 *                   add html query and parsing to get whole house/steam humidifier info and put in attributes.
 *                   changed tcc line.
 *                   modified again by lgkahn 11/20 first the heating cooling was not working right.
 *                   changed back to using equipment status.. just because the fan is on does not mean it is heating.. 
 *                   fan is also used for humidication or just by itself.
 * 
 *     jvm: v1.3.14  fix to address "componentRefresh()" error in logs.
 * csteele: v1.3.13  added Initialize per ryanmellish suggestion to jumpstart polling after hub power cycle.
 *     jvm: v1.3.12  Enabled Humidity child device.
 * csteele: v1.3.11  refactored support for lastRunningMode as data vs attribute
 * csteele: v1.3.10  add support for lastRunningMode which directly follows thermostatMode
 *                    refactored 'switch/case' code into a Map for fan and operating state
 * nvious1: v1.3.9   adding "fan only" operating mode for when the equipment is off but the fan is running. Added 3 min polling option. 
 * csteele: v1.3.8   made "description logging is" optional and info
 *                    added explicit check for cooling in getStatusHandler
 * csteele: v1.3.7   removed state.displayunits as unused. Everything has already been using the Hub's location.temperatureScale,
 *                    which meant that installed() was redundant too.
 *    jvm : v1.3.6   added range checking for changes to heating and cooling setpoints. 
 *                    Outdoor thermostate creates as a child device. 
 *                    Fixed bugs in use of tccSite variable.
 * csteele:           corrected sendEvent("humidity") 
 *                    corrected operating state to track EquipmentOutputStatus
 *                    refactored cool/heat up/down
 *     jvm:           limit checked temperature set points
 * csteele: v1.3.5   added "%" to humidity and centralized temp scale
 *     jvm: v1.3.4   added "°F" or "°C" unit to temp and setpoint events. Fixed thermostateMode being set to a temperature value.
 * csteele: v1.3.2   centralized Honeywell site url as "tccSite"
 * csteele: v1.3.1   updated to v2 of updateCheck
 * csteele: v1.3.0   converted to asynchttp where possible.
 * csteele: v1.2.3   communications with TCC changed and now Mode and Fan need to be numbers
 *                    Operating State reflects the ENUM values ("Unknown" isn't acceptable)
 * csteele: v1.2.2   replaced F/C selection with value from Location in the hub.
 * csteele: v1.2     option of polling interval, off through 60 min. added descTextEnable for Description logging.
 * csteele: v1.1.5   allow option of permanent or temporary hold.
 * csteele: v1.1     merged Pull Request from rylatorr: Use permanent hold instead of temporary
 * csteele: v1.0     added Cobra's Version Check code, modified debug logging to match Hubitat standards, (on/off and 30 min limit)
 *                      removed Relative Humidity and SmartThings "main" paragraph
 *
 * (Bob) version 10 deals with the fact that Honeywell decided to poison the well with expired cookies
 *    I also changed it so that it polls for an update every 60 seconds
 * lgk version 9 an indicator on the bottom which indicates if following schedule or vacation or temp hold.
 *    it also displays green for following schedule and red for other modes. 
 *    you can also select it to cancel a hold and go back to following schedule.
 *    however, the temp will not update till next refresh even though you have cancelled the hold.
 *    One problem, is that the colors are not working correctly in ios currently and the label is not 
 *    wrapping in  android as it is supposed to.
 * lgk version 8 figured out how to do time without user input of time zone offset.. and this works with and without
 *    daylight saving time.
 * lgk version 7, change the new operating state to be a value vs standard tile
 *    to work around a bug smartthings caused in the latest 2.08 release with text wrapping.
 *    related also added icons to the operating state, and increase the width of the last update
 *    to avoid wrapping.
 * lgk version 6 add support for actually knowing the fan is on or not (added tile),
 *    and also the actual operating state ie heating,cooling or idle via new response variables.
 * lgk version 5, due to intermittant update failures added last update date/time tile so that you can see when it happended
 *    not there is a new input tzoffset which defaults to my time ie -5 which you must set .
 * lgk version 4 supports celsius and fahrenheit with option, and now colors.
 * lgk v 3 added optional outdoor temp sensors and preferences for it, also made api login required.
 *
*/

 public static String version()     {  return "v1.3.21"  }
 public static String tccSite() 	{  return "mytotalconnectcomfort.com"  }

metadata {
    definition (name: "Total Comfort API C test", namespace: "csteele", author: "Eric Thomas, lg kahn, C Steele", importUrl: "https://raw.githubusercontent.com/HubitatCommunity/HoneywellThermo-TCC/master/HoneywellThermo-TCC_C.groovy") {
        capability "Polling"
        capability "Thermostat"
        capability "Refresh"
        capability "Temperature Measurement"
        capability "Sensor"
        capability "Relative Humidity Measurement"
        capability "Initialize"   
        command    "heatLevelUp"
        command    "heatLevelDown"
        command    "coolLevelUp"
        command    "coolLevelDown"
        command    "setFollowSchedule"
        attribute  "outdoorHumidity",    "number"
        attribute  "outdoorTemperature", "number"
        attribute  "lastUpdate",         "string"
        attribute  "followSchedule",     "string"
        attribute  "fanOperatingState",  "string"
        
        attribute "humidifierStatus", "string"
        attribute "humidifierSetPoint", "number"
        attribute "humidifierUpperLimit", "number"
        attribute "humidifierLowerLimit", "number"


//	  command "updateCheck"			// **---** delete for Release
    }

    preferences {
       input name: "username", type: "text", title: "Username", description: "Your Total Comfort User Name", required: true
       input name: "password", type: "password", title: "Password", description: "Your Total Comfort password",required: true
       input name: "honeywelldevice", type: "text", title: "Device ID", description: "Your Device ID", required: true
       input name: "haveHumidifier", type: "enum", title: "Do you have an optional whole house steam humidifier and want to enable it?", options: ["Yes", "No"], required: true, defaultValue: "No"
       input name: "enableOutdoorTemps", type: "enum", title: "Do you have the optional outdoor temperature sensor and want to enable it?", options: ["Yes", "No"], required: false, defaultValue: "No"
       input name: "enableHumidity", type: "enum", title: "Do you have the optional Humidity sensor and want to enable it?", options: ["Yes", "No"], required: false, defaultValue: "No"
       input name: "setPermHold", type: "enum", title: "Will Setpoints be temporary or permanent?", options: ["Temporary", "Permanent"], required: false, defaultValue: "Temporary"
       input name: "pollIntervals", type: "enum", title: "Set the Poll Interval.", options: [0:"off", 60:"1 minute", 120:"2 minutes", 180:"3 minutes", 300:"5 minutes",600:"10 minutes",900:"15 minutes",1800:"30 minutes",3600:"60 minutes"], required: true, defaultValue: "600"
       input name: "debugOutput", type: "bool", title: "Enable debug logging?", defaultValue: true
       input name: "descTextEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

// parse events into attributes
def parse(String description) {

}


// handle commands
def coolLevelUp()   {  if (location.temperatureScale == "F")  {  setCoolingSetpoint(device.currentValue("coolingSetpoint") + 1) } else { setCoolingSetpoint( (Double) device.currentValue("coolingSetpoint") + 0.5) }}
def coolLevelDown() {  if (location.temperatureScale == "F")  {  setCoolingSetpoint(device.currentValue("coolingSetpoint") - 1) } else { setCoolingSetpoint( (Double) device.currentValue("coolingSetpoint") - 0.5) }}
def heatLevelUp()   {  if (location.temperatureScale == "F")  {  setHeatingSetpoint(device.currentValue("heatingSetpoint") + 1) } else { setHeatingSetpoint( (Double) device.currentValue("heatingSetpoint") + 0.5) }}
def heatLevelDown() {  if (location.temperatureScale == "F")  {  setHeatingSetpoint(device.currentValue("heatingSetpoint") - 1) } else { setHeatingSetpoint( (Double) device.currentValue("heatingSetpoint") - 0.5) }}


def setCoolingSetpoint(temp) {
	float valIn = temp // for limits check
	temp = ensureRange( temp.toFloat(), state.coolLowerSetptLimit.toFloat(), state.coolUpperSetptLimit.toFloat() )
	if (valIn != vtempal) log.warn "SetPoint limited due to: out of range" 
        deviceDataInit(state.PermHold) 	 // reset all params, then set individually
        state.deviceSetting.CoolSetpoint = temp
        log.info "Setting cool setpoint to: ${temp}"
        setStatus()
        
        if(device.data.SetStatus==1)
        {
            sendEvent(name: 'coolingSetpoint', value: temp as Integer, unit:"°${location.temperatureScale}")
        }
}

def setCoolingSetpoint(double temp) {
	double valIn = temp // for limits check
	temp = ensureRange( temp.toFloat(), state.coolLowerSetptLimit.toFloat(), state.coolUpperSetptLimit.toFloat() )
	if (valIn != temp) log.warn "SetPoint limited due to: out of range" 
        deviceDataInit(state.PermHold) 	 // reset all params, then set individually
        state.deviceSetting.CoolSetpoint = temp
        log.info "Setting cool set point down to: ${temp}"
        setStatus()
        
        if(device.data.SetStatus==1)
        {
            sendEvent(name: 'coolingSetpoint', value: temp as double, unit:"°${location.temperatureScale}")
        }
}


def setHeatingSetpoint(temp) {
	float valIn = temp // for limits check
	temp = ensureRange( temp.toFloat(), state.heatLowerSetptLimit.toFloat(), state.heatUpperSetptLimit.toFloat() )
	if (valIn != temp) log.warn "SetPoint limited due to: out of range" 
        deviceDataInit(state.PermHold) 	 // reset all params, then set individually
        state.deviceSetting.HeatSetpoint = temp
        log.info "Setting heat setpoint to: ${temp}"
        setStatus()
        
        if(device.data.SetStatus==1)
        {
            sendEvent(name: 'heatingSetpoint', value: temp as Integer, unit:"°${location.temperatureScale}")
        }
}

def setHeatingSetpoint(Double temp)
{
	double valIn = temp // for limits check
	temp = ensureRange( temp.toFloat(), state.heatLowerSetptLimit.toFloat(), state.heatUpperSetptLimit.toFloat() )
	if (valIn != temp) log.warn "SetPoint limited due to: out of range" 
        deviceDataInit(state.PermHold) 	 // reset all params, then set individually
        state.deviceSetting.HeatSetpoint = temp
        log.info "Setting heat set point down to: ${temp}"
        setStatus()
        
        if(device.data.SetStatus==1)
        {
        	sendEvent(name: 'heatingSetpoint', value: temp as double, unit:"°${location.temperatureScale}")
        }	
}


def setFollowSchedule() {
	if (debugOutput) log.debug "in set follow schedule"
	deviceDataInit(0) 	 // reset all params, then set individually
//	state.deviceSetting.HeatSetpoint = temp
	setStatus()

	if(device.data.SetStatus==1)
	{
        if (debugOutput) log.debug "Successfully sent follow schedule.!"
//        runIn(60,getStatus)
//        runEvery1Minutes (getStatus)
	}
}

/* ///
def setTargetTemp(temp) {
	if ((temp > state.coolLowerSetptLimit) || (temp < state.coolUpperSetptLimit)) {
		deviceDataInit(state.PermHold) 	 // reset all params, then set individually
		state.deviceSetting.HeatSetpoint = temp
		state.deviceSetting.CoolSetpoint = temp
		setStatus()
	} else { log.warn "Set Point out of range: $temp" }
}

def setTargetTemp(double temp) {
	if ((temp > state.coolLowerSetptLimit) || (temp < state.coolUpperSetptLimit)) {
		deviceDataInit(state.PermHold) 	 // reset all params, then set individually
		state.deviceSetting.HeatSetpoint = temp
		state.deviceSetting.CoolSetpoint = temp
		setStatus()
	} else { log.warn "Set Point out of range: $temp" }
}
*/

def off() {
	setThermostatMode('off')
}

def auto() {
	setThermostatMode('auto')
}

def heat() {
	setThermostatMode('heat')
}

def cool() {
	setThermostatMode('cool')
}

def emergencyHeat() {
	if (isEmergencyHeatAllowed) {
		if (debugOutput) log.debug "Set Emergency/Auxiliary Heat On"
		setThermostatMode('emergency heat')
	}
}

def setThermostatMode(mode) {
	Map modeMap = [auto:5, cool:3, heat:1, off:2, 'emergency heat':4]
	if (debugOutput) log.debug "setThermostatMode: $mode"
//	deviceDataInit(null) 	 // reset all params, then set individually

	state.deviceSetting.SystemSwitch = modeMap.find{ mode == it.key }?.value
	setStatus()
	
	if(device.data.SetStatus==1)
	{
	    sendEvent(name: 'thermostatMode', value: mode)
	    lrM(mode) 
	}
}

def fanOn() {
    setThermostatFanMode('on')
}

def fanAuto() {
    setThermostatFanMode('auto')
}

def fanCirculate() {
    setThermostatFanMode('circulate')
}

def setThermostatFanMode(mode) { 
	Map fanMap = [auto:0, on:1, circulate:2, followSchedule:3]   
	if (debugOutput) log.debug "setThermostatFanMode: $mode"
//	deviceDataInit(null)  	 // reset all params, then set individually
	def fanMode = null
	
	state.deviceSetting.FanMode = fanMap.find{ mode == it.key }?.value
	setStatus()
	
	if(device.data.SetStatus==1)
	{
	    sendEvent(name: 'thermostatFanMode', value: mode)    
	}
}


void setStatus() {
	runInMillis( 1600, settingsAccumWait )
}

void settingsAccumWait() {
	device.data.SetStatus = 0
	if ( !login() ) {
		pauseExecution(6000)
		if ( !login() ) {
			return
		}
	}
    if (debugOutput) log.debug "Honeywell TCC 'setStatus'"
    def today = new Date()

    def params = [
        uri: "https://${tccSite()}/portal/Device/SubmitControlScreenChanges",
        headers: [
            'Accept': 'application/json, text/javascript, */*; q=0.01', // */ comment
            'DNT': '1',
            'Accept-Encoding': 'gzip,deflate,sdch',
            'Cache-Control': 'max-age=0',
            'Accept-Language': 'en-US,en,q=0.8',
            'Connection': 'keep-alive',
            'Host': "${tccSite()}",
            'Referer': "https://${tccSite()}/portal/Device/Control/${settings.honeywelldevice}",
            'X-Requested-With': 'XMLHttpRequest',
            'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36',
            'Cookie': device.data.cookiess
        ],
        body: [
            DeviceID: "${settings.honeywelldevice}",
            SystemSwitch: state.deviceSetting.SystemSwitch,
            HeatSetpoint: state.deviceSetting.HeatSetpoint,
            CoolSetpoint: state.deviceSetting.CoolSetpoint,
            HeatNextPeriod: state.deviceSetting.HeatNextPeriod,
            CoolNextPeriod: state.deviceSetting.CoolNextPeriod,
            StatusHeat: state.deviceSetting.StatusHeat,
            StatusCool: state.deviceSetting.StatusCool,
            fanMode: state.deviceSetting.FanMode,
            DisplayUnits: location.temperatureScale,
            TemporaryHoldUntilTime: state.deviceSetting.TemporaryHoldUntilTime,
            VacationHold: state.deviceSetting.VacationHold
        ],
	  timeout: 10
    ]

    if (debugOutput) log.debug "params = $params"
    try {
    	httpPost(params) {
    	    resp ->
    	        def setStatusResult = resp.data
    	    if (debugOutput) log.debug "Request was successful, $resp.status"
    	    device.data.SetStatus = 1
    	}
    } 
    catch (e) {
    	log.error "Something went wrong: $e"
    }

	// prepare for the next cycle by clearing all the values just sent.
	deviceDataInit(null)

/*
    if (debugOutput) log.debug "params = $params"
    asynchttpPost("setStatusHandler", params) 
*/
}    


def setStatusHandler(resp, data) {
	//log.debug "data was passed successfully"
	//log.debug "status of post call is: ${resp.status}"

	if(resp.getStatus() == 408) {if (debugOutput) log.debug "TCC Request timed out, $resp.status"}
	if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		def setStatusResult = resp.data
		if (debugOutput) log.debug "Request was successful, $resp.status"
		device.data.SetStatus = 1
	} else { if (descTextEnable) log.info "TCC setStatus failed" }
}


def getStatus(Boolean fromUnauth = false) {
    
    if (debugOutput) log.debug "Honeywell TCC getStatus"
    if (debugOutput) log.debug "enable outside temps = $enableOutdoorTemps"
    def today = new Date()
    //if (debugOutput) log.debug "https://${tccSite()}/portal/Device/CheckDataSession/${settings.honeywelldevice}?_=$today.time"

    def params = [
        uri: "https://${tccSite()}/portal/Device/CheckDataSession/${settings.honeywelldevice}",
        headers: [
            'Accept': '*/*', // */ comment
            'DNT': '1',
            'Cache': 'false',
            'dataType': 'json',
            'Accept-Encoding': 'plain',
            'Cache-Control': 'max-age=0',
            'Accept-Language': 'en-US,en,q=0.8',
            'Connection': 'keep-alive',
            'Referer': "https://${tccSite()}/portal",
            'X-Requested-With': 'XMLHttpRequest',
            'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36',
            'Cookie': device.data.cookiess
        ],
	  timeout: 10
    ]

    if (debugOutput) log.debug "sending getStatus request"
    state.fromUnauth = fromUnauth
    
    asynchttpGet("getStatusHandler", params)
}

def getStatusHandler(resp, data) 
{   
	if (resp.getStatus() == 200 || resp.getStatus() == 207)
    {
        if (debugOutput) log.info "status = ${resp.getStatus()}"
        if (debugOutput) log.info "data = ${resp.data}"
        
        // lgk error handling for bad page coming back
        
     try {
              
        if (resp.data)
        {
            
		def setStatusResult = parseJson(resp.data)
	
      if (debugOutput) 
      { 
          log.debug "Request was successful, $resp.status"
          log.debug "data = $setStatusResult"
          log.debug "ld = $setStatusResult.latestData.uiData"
          log.debug "ld = $setStatusResult.latestData.fanData"
      }   
       
    sendEvent(name: 'supportedThermostatFanModes', value: ["\"auto\"", "\"circulate\"", "\"on\""] )
	    
	def curTemp = setStatusResult.latestData.uiData.DispTemperature.toDouble().round(2)
   // def curTemp = curTemp1.toDouble().round(2)
	def switchPos = setStatusResult.latestData.uiData.SystemSwitchPosition
	def coolSetPoint = setStatusResult.latestData.uiData.CoolSetpoint
	def heatSetPoint = setStatusResult.latestData.uiData.HeatSetpoint
	def statusCool = setStatusResult.latestData.uiData.StatusCool
	def statusHeat = setStatusResult.latestData.uiData.StatusHeat
	def Boolean hasIndoorHumid= setStatusResult.latestData.uiData.IndoorHumiditySensorAvailable
	def curHumidity = setStatusResult.latestData.uiData.IndoorHumidity
	def Boolean hasOutdoorHumid = setStatusResult.latestData.uiData.OutdoorHumidityAvailable
	def Boolean hasOutdoorTemp = setStatusResult.latestData.uiData.OutdoorTemperatureAvailable
	def Boolean isScheduleCapable = setStatusResult.latestData.uiData.ScheduleCapable
	def curOutdoorHumidity = setStatusResult.latestData.uiData.OutdoorHumidity.toDouble().round(2)
    def curOutdoorTemp = setStatusResult.latestData.uiData.OutdoorTemperature.toDouble().round(2)
	// EquipmentOutputStatus = 0 off 1 heating 2 cooling
	def equipmentStatus = setStatusResult.latestData.uiData.EquipmentOutputStatus	
	def holdTime = setStatusResult.latestData.uiData.TemporaryHoldUntilTime
	def vacationHoldMode = setStatusResult.latestData.uiData.IsInVacationHoldMode
	def vacationHold = setStatusResult.latestData.uiData.VacationHold
	def Boolean isEmergencyHeatAllowed = setStatusResult.latestData.uiData.SwitchEmergencyHeatAllowed

	state.heatLowerSetptLimit = setStatusResult.latestData.uiData.HeatLowerSetptLimit 
	state.heatUpperSetptLimit = setStatusResult.latestData.uiData.HeatUpperSetptLimit 
	state.coolLowerSetptLimit = setStatusResult.latestData.uiData.CoolLowerSetptLimit 
	state.coolUpperSetptLimit = setStatusResult.latestData.uiData.CoolUpperSetptLimit 
	
    if (isEmergencyHeatAllowed)    
      sendEvent(name: 'supportedThermostatModes', value: ["\"auto\"", "\"cool\"", "\"emergency heat\"", "\"heat\"", "\"off\""] )  
    else
      sendEvent(name: 'supportedThermostatModes', value: ["\"auto\"", "\"cool\"", "\"heat\"", "\"off\""] )  
        
	def fanMode = setStatusResult.latestData.fanData.fanMode
	def fanIsRunning = setStatusResult.latestData.fanData.fanIsRunning
     // reset display units as above doesntg work
             logInfo "got temp = $curTemp"
             logInfo "got humidity = $curHumidity"
          //  log.debug "got outdoor temp = $curOutdoorTemp"
            //log.debug "got outdoor humidity = $curOutdoorHumidity"
           // log.debug "got display units = $displayUnits"
            
	if (debugOutput) log.debug "got holdTime = $holdTime"
	if (debugOutput) log.debug "got Vacation Hold = $vacationHoldMode"
	if (debugOutput) log.debug "got scheduleCapable = $isScheduleCapable"
	if (debugOutput) log.debug "got Emergency Heat = $isEmergencyHeatAllowed"
	
	if (holdTime != 0) {
	    if (debugOutput) log.debug "sending temporary hold"
	    sendEvent(name: 'followSchedule', value: "TemporaryHold")
	}

	if (vacationHoldMode == true) {
	    if (debugOutput) log.debug "sending vacation hold"
	    sendEvent(name: 'followSchedule', value: "VacationHold")
	}

	if (vacationHoldMode == false && holdTime == 0 && isScheduleCapable == true ) {
	    if (debugOutput) log.debug "Sending following schedule"
	    sendEvent(name: 'followSchedule', value: "FollowingSchedule")
	}

	if (hasIndoorHumid == false) { curHumidity = 0 }

	//Operating State Section 
	//Set the operating state to off 
	// thermostatOperatingState - ENUM ["heating", "pending cool", "pending heat", "vent economizer", "idle", "cooling", "fan only"]

	// set fan and operating state
	def fanState = "idle"

	if (fanIsRunning) {
		fanState = "on";
	} 

	def operatingState = [ 0: 'idle', 1: 'heating', 2: 'cooling' ][equipmentStatus] ?: 'idle'

	if ((haveHumidifier != 'Yes') && (fanIsRunning == true) && (equipmentStatus == 0))
	{ 
	    operatingState = "fan only"
	}

	else if ((haveHumidifier == 'Yes')  && (fanIsRunning == true) && (equipmentStatus == 0) && (fanMode == 0))  
	{
	    operatingState = "Humidifying"
	}

	logInfo("Get Operating State: $operatingState - Fan to $fanState, switchPos = $switchPos")
	
	//fan mode 0=auto, 2=circ, 1=on, 3=followSched
	
	n = [ 0: 'auto', 2: 'circulate', 1: 'on', 3: 'followSchedule' ][fanMode]
	sendEvent(name: 'thermostatFanMode', value: n)

    if (isEmergencyHeatAllowed) n = [ 1: 'heat', 2: 'off', 3: 'cool', 4: 'emergency heat', 5: 'auto'][switchPos] ?: 'auto'
    else n = [ 1: 'heat', 2: 'off', 3: 'cool', 4: 'auto'][switchPos] ?: 'auto'
     
	sendEvent(name: 'temperature', value: curTemp, state: n, unit:"°${location.temperatureScale}")
	sendEvent(name: 'thermostatMode', value: n)
	lrM(n)

	//Send events 
	sendEvent(name: 'thermostatOperatingState', value: operatingState)
	sendEvent(name: 'fanOperatingState', value: fanState)
//	sendEvent(name: 'thermostatFanMode', value: fanMode)
//	sendEvent(name: 'thermostatMode', value: switchPos)
	sendEvent(name: 'coolingSetpoint', value: coolSetPoint, unit:"°${location.temperatureScale}")
	sendEvent(name: 'heatingSetpoint', value: heatSetPoint, unit:"°${location.temperatureScale}")
//	sendEvent(name: 'temperature', value: curTemp, state: switchPos, unit:"°${location.temperatureScale}")
	sendEvent(name: 'humidity', value: curHumidity as Integer, unit:"%")

      if (haveHumidifier == 'Yes') 
      {
      	// kludge to figure out if humidifier is on, fan has to be auto, and if fan is on but not heat/cool and we have enabled the humidifyer it should be humidifying"
      	// if (debugOutput)
          log.debug "fanIsRunning = $fanIsRunning, equip status = $equipmentStatus, fanMode = $fanMode, temp = $curTemp, humidity = $curHumidity"
           
       	if ((fanIsRunning == true) && (equipmentStatus == 0) && (fanMode == 0))  
      	{
      		log.debug "Humidifier is On"
         		sendEvent(name: 'humidifierStatus', value: "Humidifying")
      	}
      	else
      	{
      		log.debug "Humidifier is Off"
      		sendEvent(name: 'humidifierStatus', value: "Idle")   
      	}
      }
        
     else sendEvent(name: "humidifierStatus", value: "N/A")
        
	def now = new Date().format('MM/dd/yyyy h:mm a', location.timeZone)
		
	sendEvent(name: "lastUpdate", value: now, descriptionText: "Last Update: $now")
	
	if (enableOutdoorTemps == "Yes") {

	    if (hasOutdoorHumid) {
	        setOutdoorHumidity(curOutdoorHumidity)
	        sendEvent(name: 'outdoorHumidity', value: curOutdoorHumidity as Integer, unit:"%")
	    }
	
	    if (hasOutdoorTemp) {
	        setOutdoorTemperature(curOutdoorTemp)
	        sendEvent(name: 'outdoorTemperature', value: curOutdoorTemp as Integer, unit:"°${location.temperatureScale}")
	    }
	}
            
	} // got resp data
     else
        {
            log.warn "No data returned status = ${resp.getStatus()}"
            log.warn "Scheduling a retry in 5 minutes due to failure!"
            runIn(300,"refreshFromRunin")
        }    
   
     } catch (groovy.json.JsonException e) {
         
       log.warn "in error handler case resp = $resp error = $e"
       if (state.fromUnauth)
            {
              log.debug "2nd failure ... giving up!"
            }
            else
            { 
             log.warn "First failure, Trying again in 60 seconds.!" 
             runIn(60,"refreshFromRunin")
            }     
        }
        
    } // good status code

 else 
    { 
     if (descTextEnable) log.info "TCC getStatus failed" 
    }
    
}

def getHumidifierStatus(Boolean fromUnauth = false)
{
   if (debugOutput)  log.debug "in get humid status enable humidifier = $haveHumidifier"
	if (settings.haveHumidifier == 'No') return
	def params = [
        uri: "https://${tccSite()}/portal/Device/Menu/${settings.honeywelldevice}",
        headers: [
            'Accept': '*/*', // */ comment
            'DNT': '1',
            'dataType': 'json',
            'cache': 'false',
            'Accept-Encoding': 'plain',
            'Cache-Control': 'max-age=0',
            'Accept-Language': 'en-US,en,q=0.8',
            'Connection': 'keep-alive',
            'Host': 'rs.alarmnet.com',
            'Referer': 'https://${tccSite()}/portal/',
            'X-Requested-With': 'XMLHttpRequest',
            'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36',
            'Cookie': device.data.cookiess
        ],
	  timeout: 10
    ]

    if (debugOutput) log.debug "sending gethumidStatus request: $params"

    def CancelLine = [:]
    def Number HumLevel
    def Number HumMin
    def Number HumMax
    try {
     httpGet(params) { response ->
        if (debugOutput) log.debug "GetHumidity Request was successful, $response.status"
        if (debugOutput) log.debug "response = $response.data"

        //  if (debugOutput) log.debug "ld = $response.data.latestData"
        //  if (debugOutput) log.debug "humdata = $response.data.latestData.humData"

       // logInfo("lowerLimit: ${response.data.latestData.humData.lowerLimit}")        
       // logInfo("upperLimit: ${response.data.humData.upperLimit}")        
       // logInfo("SetPoint: ${response.data.humData.Setpoint}")        
       // logInfo("DeviceId: ${response.data.humData.DeviceId}")        
        //logInfo("IndoorHumidity: ${response.data.humData.IndoorHumidity}")        

        def data = response.getData().toString()
          
        data.split("\n").each {
        	//if (debugOutput) log.debug "working on \"${it}\""
        	if (it.contains("CancelMin")) {
            	CancelLine = it.trim()
            	def pair = CancelLine.split(" ");
            	if (debugOutput)   log.debug "got cancel min line: $CancelLine"
            	// log.debug "pair = $pair"
            	def p0 = pair[0]
            	def p1 = pair[1]
            	def p2 = pair[2]

            	// log.debug "p0 = $p0"
            	// log.debug "p1 = $p1"
            	// log.debug "p2 = $p2"

            	def pair2 = p1.split("%")
            	//log.debug "pair2 = $pair2"
            	def p20 = pair2[0]
            	def p21 = pair2[1]
            	def p22 = pair2[2]

            	//log.debug "p20 = $p20"
            	// log.debug "p21 = $p21"
            	//log.debug "p22 = $p22"

            	HumLevel = p21.toInteger()
            	HumMin = p20.toInteger()

            	def pair3 = p2.split("%")
            	//log.debug "pair3 = $pair3"
            	def p30 = pair3[0]
            	// log.debug "p30 = $p30"

            	HumMax = p30.toInteger() 

            	if (debugOutput) log.debug "-----------------------"
            	log.debug "Got current humidifier level = $HumLevel"
            	log.debug "Got Current humidifier Min = $HumMin"
            	log.debug "Got Current humidifier Max= $HumMax"
        	}
        }
        
     	//Send events 
	//	sendEvent(name: 'humidifierStatus', value: HumStatus)
		sendEvent(name: 'humidifierSetPoint', value: HumLevel as Integer, unit:"%")
		sendEvent(name: 'humidifierUpperLimit', value: HumMax as Integer, unit:"%")
		sendEvent(name: 'humidifierLowerLimit', value: HumMin as Integer, unit:"%") 
      }
    } 
    catch (e) {
    	log.error "Something went wrong (getHumidifierStatus): $e"
    	def String eStr = e.toString()
    	def pair = eStr.split(" ")
    	def p1 = pair[0]
    	def p2 = pair[1]
         //  log.debug "p1 = $p1 p2 = $p2"
    	if ((p2 == "Unauthorized") || (p2 == "Read"))
        {
            if (fromUnauth)
            {
              log.debug "2nd Unauthorized failure ... giving up!"
            }
            else
            {
              log.debug "Scheduling a retry in 5 minutes due to Unauthorized!"
              runIn(300,"refreshFromRunin")
            }
        }
    }

}

// Update lastRunningMode based on mode and operatingstate
def lrM(mode) {
	String lrm = getDataValue("lastRunningMode")
	if (mode.contains("auto") || mode.contains("off") && lrm != "heat") { updateDataValue("lastRunningMode", "heat") }
	 else { updateDataValue("lastRunningMode", mode) }
}

def api(method, args = [], success = {}) {}

// initialize the device values. Each method overwrites it's specific value
def deviceDataInit(val) { 	 // reset all params, then set individually
	if ( !state.deviceSetting?.size) {
	    state.deviceSetting = [:]
	    state.deviceSetting << [SystemSwitch: null, StatusHeat: null, StatusCool: null, HeatSetpoint: null, CoolSetpoint: null, HeatNextPeriod: null, CoolNextPeriod: null, FanMode: null, TemporaryHoldUntilTime: null, VacationHold: null] 
	}
//	state.deviceSetting?.size ? log.debug "state.deviceSetting" : log.debug "no state.deviceSetting"
	state.deviceSetting.StatusHeat=val
	state.deviceSetting.StatusCool=val

	// don't clear multiple times
	if ( val == null ) {
		state.deviceSetting.SystemSwitch = null // state.deviceSetting
		state.deviceSetting.HeatSetpoint = null
		state.deviceSetting.CoolSetpoint = null
		state.deviceSetting.HeatNextPeriod = null
		state.deviceSetting.CoolNextPeriod = null
		state.deviceSetting.FanMode = null
		state.deviceSetting.TemporaryHoldUntilTime=null
		state.deviceSetting.VacationHold=null
	}
}

// Need to be logged in before this is called. So don't call this. Call api.
def doRequest(uri, args, type, success) {

}

float ensureRange(float value, float min, float max) {
   return Math.min(Math.max(value, min), max);
}

def refreshFromRunin()
{ 
    log.debug "Calling refresh after Unauthorize failure!"
    refresh(true)
}

void componentRefresh(cd)
{
	if (debugOutput) log.debug "Refresh request from device ${cd.displayName}. This will refresh all component devices."
	getStatus(false)
}

def refresh(Boolean fromUnauth = false) {
    if (debugOutput) log.debug "here Honeywell TCC 'refresh', pollInterval: $pollInterval, units: = °${location.temperatureScale}, fromUnauth = $fromUnauth"
	if ( !login(fromUnauth) ) {
		pauseExecution(6000)
		if ( !login(fromUnauth) ) {
			return
		}
	}
    getHumidifierStatus(fromUnauth)
    getStatus(fromUnauth)
}

def login(Boolean fromUnauth = false) {
	if (debugOutput) log.debug "Honeywell TCC 'login'"
	Boolean ofExit = true 	// default: assume that login works and return a True.

    Map params = [
        uri: "https://${tccSite()}/portal/",
        headers: [
            'Content-Type': 'application/x-www-form-urlencoded',
            'Accept': 'application/json, text/javascript, */*; q=0.01', // */
            'Accept-Encoding': 'sdch',
            'Host': "${tccSite()}",
            'DNT': '1',
            'Origin': "https://${tccSite()}/portal/",
            'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36'
        ],
        body: [timeOffset: '240', UserName: "${settings.username}", Password: "${settings.password}", RememberMe: 'false']
    ]

   // log.debug "Params: $params.headers $params.body"
    device.data.cookiess = ''

    try {
        httpPost(params) { 
            response ->
            if (debugOutput) log.debug "Request was successful, $response.status" // ${response.getHeaders()}"
            String allCookies = ""

       //     response.getHeaders('Set-Cookie').each { if (debugOutput) log.debug "---Set-Cookie: ${it.value}" }

            response.getHeaders('Set-Cookie').each {
                String cookie = it.value.split(';|,')[0]
                Boolean skipCookie = false
                def expireParts = it.value.split('expires=')

                try {
                    def cookieSegments = it.value.split(';')
                    for (int i = 0; i < cookieSegments.length; i++) {
                        def cookieSegment = cookieSegments[i]
                        String cookieSegmentName = cookieSegment.split('=')[0]

                        if (cookieSegmentName.trim() == "expires") {
                            String expiration = cookieSegment.split('=')[1]

                            Date expires = new Date(expiration)
                            Date newDate = new Date() // right now

                            if (expires < newDate) {
                                skipCookie = true
                                //if (debugOutput) log.debug "-skip cookie: $it.value"
                            } else {
                                //if (debugOutput) log.debug "+not skipping cookie: expires=$expires. now=$newDate. cookie: $it.value"
                            }

                        }
                    }
                } catch (e) {
                    if (debugOutput) log.debug "!error when checking expiration date: $e ($expiration) [$expireParts.length] {$it.value}"
                }

                allCookies = allCookies + it.value + ';'

                if (cookie != ".ASPXAUTH_TH_A=") {
                    if (it.value.split('=')[1].trim() != "") {
                        if (!skipCookie) {
                            if (debugOutput) log.debug "Adding cookie to collection: $cookie"
                            device.data.cookiess += cookie + ';'
                        }
                    }
                }
            }
            int cookieCount = device.data.cookiess.split(";", -1).length - 1;
            if (cookieCount < 8) {
			ofExit = false
            }
            //log.debug "cookies: $device.data.cookiess"
        }
    } catch (e) {
        log.warn "Something went wrong during login: $e"
        def String eStr = e.toString()
        def pair = eStr.split(" ")
        def p1 = pair[0]
        def p2 = pair[1]
      
        if ((p2 == "Unauthorized") || (p2 == "Read"))
        {
            if (fromUnauth)
            {
              log.debug "2nd Unauthorized failure ... giving up!"
            }
            else
            {
              log.debug "Scheduling a retry in 5 minutes due to Unauthorized!"
              runIn(300,"refreshFromRunin")
            }
        }
	  ofExit = false
	}
	return ofExit
}

// Initialize after hub power cycle to force a poll cycle
def initialize() {
	logInfo "Initialize Poll"
	state.deviceSetting = [:]
    	state.deviceSetting << [SystemSwitch: null, StatusHeat: null, StatusCool: null, HeatSetpoint: null, CoolSetpoint: null, HeatNextPeriod: null, CoolNextPeriod: null, FanMode: null, TemporaryHoldUntilTime: null, VacationHold: null]
	poll()
}


/* def isLoggedIn() {
    if(!device.data.auth) {
        if (debugOutput) log.debug "No device.data.auth"
        return false
    }

def now = new Date().getTime();
    return device.data.auth.expires_in > now
} */

def poll() {
    pollInterval = pollIntervals.toInteger()
    if (pollInterval) runIn(pollInterval, poll) 
    logInfo "in poll: (every $pollInterval seconds)"
    refresh()
}

def updated() {
    if (debugOutput) log.debug "in updated"
    pollInterval = pollIntervals.toInteger()
    if (debugOutput) log.debug "debug logging is: ${debugOutput == true}"
    if (descTextEnable) log.info "description logging is: ${descTextEnable == true}"
    unschedule()
    dbCleanUp()		// remove antique db entries created in older versions and no longer used.
///    if (debugOutput) runIn(1800,logsOff)   
    if (setPermHold == "Permanent") { state.PermHold = 2 } else { state.PermHold = 1 }
///    schedule("0 0 8 ? * FRI *", updateCheck)  // Cron schedule - How often to perform the update check - (This example is 8am every Friday)
///    runIn(20, updateCheck) 
    if (debugOutput) log.debug "PermHold now = ${state.PermHold}"
    poll()
}

def logsOff(){
    if (descTextEnable) log.warn "debug logging disabled..."
    device.updateSetting("debugOutput",[value:"false",type:"bool"])
}


private logInfo(msg) {
	if (settings?.descTextEnable || settings?.descTextEnable == null) log.info "$msg"
}

private dbCleanUp() {
	// clean up state variables that are obsolete
	state.remove("tempOffset")
	state.remove("version")
	state.remove("Version")
	state.remove("sensorTemp")
	state.remove("author")
	state.remove("Copyright")
	state.remove("verUpdate")
	state.remove("verStatus")
	state.remove("Type")
	state.remove("DisplayUnits")
	device.data.remove("SystemSwitch")
	device.data.remove("VacationHold")
	device.data.remove("HeatNextPeriod")
	device.data.remove("HeatSetpoint")
	device.data.remove("CoolSetpoint")
	device.data.remove("TemporaryHoldUntilTime")
	device.data.remove("CoolNextPeriod")
	device.data.remove("FanMode")
	device.data.remove("StatusHeat")
	device.data.remove("StatusCool")
}


void setOutdoorTemperature(value){
    def cd = getChildDevice("${device.id}-Temperature Sensor")
	if (!cd) 
		{
		cd = addChildDevice("hubitat", "Generic Component Temperature Sensor", "${device.id}-Temperature Sensor", [name: "Outdoor Temperature", isComponent: true])	
		}
    String unit = "°${location.temperatureScale}"
    cd.parse([[name:"temperature", value:value, descriptionText:"${cd.displayName} is ${value}${unit}.", unit: unit]])
}

void setOutdoorHumidity(value){
    def cd = getChildDevice("${device.id}-Humidity Sensor")
	if (!cd) 
		{
		cd = addChildDevice("hubitat", "Generic Component Humidity Sensor", "${device.id}-Humidity Sensor", [name: "Outdoor Humidity", isComponent: true])	
		}
    cd.parse([[name:"humidity", value:value, descriptionText:"${cd.displayName} is ${value}%.", unit:"%"]])
}

///
/*
// Check Version   ***** with great thanks and acknowledgment to Cobra (CobraVmax) for his original code ****
def XupdateCheck()
{    
	def paramsUD = [uri: "https://hubitatcommunity.github.io/HoneywellThermo-TCC/version2.json", timeout: 10  ]
	
 	asynchttpGet("updateCheckHandler", paramsUD) 
}

def updateCheckHandler(resp, data) {

	state.InternalName = "HoneywellThermoTCC_C"

	if (resp.getStatus() == 200 || resp.getStatus() == 207) {
		respUD = parseJson(resp.data)
		// log.warn " Version Checking - Response Data: $respUD"   // Troubleshooting Debug Code - Uncommenting this line should show the JSON response from your webserver 
		state.Copyright = "${thisCopyright} -- ${version()}"
		// uses reformattted 'version2.json' 
		def newVer = padVer(respUD.driver.(state.InternalName).ver)
		def currentVer = padVer(version())               
		state.UpdateInfo = (respUD.driver.(state.InternalName).updated)
            // log.debug "updateCheck: ${respUD.driver.(state.InternalName).ver}, $state.UpdateInfo, ${respUD.author}"

		switch(newVer) {
			case { it == "NLS"}:
			      state.Status = "<b>** This Driver is no longer supported by ${respUD.author}  **</b>"       
			      if (descTextEnable) log.warn "** This Driver is no longer supported by ${respUD.author} **"      
				break
			case { it > currentVer}:
			      state.Status = "<b>New Version Available (Version: ${respUD.driver.(state.InternalName).ver})</b>"
			      if (descTextEnable) log.warn "** There is a newer version of this Driver available  (Version: ${respUD.driver.(state.InternalName).ver}) **"
			      if (descTextEnable) log.warn "** $state.UpdateInfo **"
				break
			case { it < currentVer}:
			      state.Status = "<b>You are using a Test version of this Driver (Expecting: ${respUD.driver.(state.InternalName).ver})</b>"
			      if (descTextEnable) log.warn "You are using a Test version of this Driver (Expecting: ${respUD.driver.(state.InternalName).ver})"
				break
			default:
				state.Status = "Current"
				if (descTextEnable) log.info "You are using the current version of this driver"
				break
		}

		sendEvent(name: "chkUpdate", value: state.UpdateInfo)
		sendEvent(name: "chkStatus", value: state.Status)
      }
      else
      {
           log.error "Something went wrong: CHECK THE JSON FILE AND IT'S URI"
      }
}

// /*
	padVer

	Version progression of 1.4.9 to 1.4.10 would mis-compare unless each column is padded into two-digits first.

// *
def padVer(ver) {
	def pad = ""
	ver.replaceAll( "[vV]", "" ).split( /\./ ).each { pad += it.padLeft( 2, '0' ) }
	return pad
}
*/ ///
def getThisCopyright(){"&copy; 2020 C Steele "}
