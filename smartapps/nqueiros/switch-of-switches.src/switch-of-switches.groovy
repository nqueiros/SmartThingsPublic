/**	
 * NQ: Switch of Switches allows controlling multiple on/off switches with energy and power capabilities
 * A master switch can be added to control the group of switches
 */

definition(
   name: "Switch of Switches",
   namespace: "nqueiros",
   author: "Nuno Queiros",
   description: "Control a group of switches from a phisical or virtual switch",
   category: "My Apps",
   iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
   iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Devices") {
    	input "mainSwitch", "capability.switch", title: "Main switch", description: "Select the main switch that will control the group (optional).", multiple: false, required: true 
        input "groupSwitches", "capability.switch", title: "Group switches", description: "The swithes that will be controlled (required).", multiple: true, required: true
    }
    section("App Config") {
    	input "friendlyName", "text", title: "Friendly name", multiple: false, required: true
        input "syncGroupDevices", "bool", title: "Sync group devices", multiple: false, required: true, defaultValue: true
    	input "testMode", "bool", title: "Test mode", multiple: false, required: true, defaultValue: false
    }
}

def installed() {
  logging "Installed with settings: ${settings}"
  initialize()
}

def updated() {
  logging "Updated with settings: ${settings}"
  unsubscribe()
  initialize()
}

def initialize() {
	logging "Initializing.", true
  
  	//Subscreve os eventos do switch principal 
  	subscribe(mainSwitch, "switch", mainHandler)
	
    //Subscreve os eventos dos switches do grupo
    subscribe(groupSwitches, "switch", groupHandler)
    groupSwitches.each{
     	subscribe(it, "power", groupHandler)
        subscribe(it, "energy", groupHandler)
    }
  
  	state.appPowerValues = [:]
    state.appEnergyValues = [:]
}

def mainHandler(evt){
	logging "Main Switch Command: Name - ${evt.name}; Value - ${evt.value}; Type - ${evt.type}", true
    
    switch (evt.value){
    	case "on":
        	logging "ON Command"
            if (evt.type!="partial") groupSwitches.each{it.on()}
            break
        case "off":
        	logging "OFF Command"
            if (evt.type!="partial") groupSwitches.each{it.off()}
            break
		default:
        	logging "WARNING: Unknown command"
            break
	}
}

def groupHandler(evt){
	logging "Group Switch Command: Name - ${evt.name}; Value - ${evt.value}; Device - ${evt.device};  DeviceId - ${evt.deviceId}", true
    
    if (evt.name == "switch") {
    	if (evt.value == "on") {
        	if (syncGroupDevices) mainSwitch.on()
            else mainSwitch.onPartial()
        }
        else {
            if (syncGroupDevices) mainSwitch.off()
            else {
            	//Only sends the off command if all the group switches are off
                boolean allOff = true
                
                groupSwitches.each{
                	allOff = allOff && (it.currentState("switch").getValue() == "off")
                }
                
                if (allOff) mainSwitch.offPartial()
            }
    	}
    }
    else
    	triggerEvent(evt)
}

def triggerEvent(evt){    
    logging "Trigger event. Name: ${evt.name}; Value: ${evt.value}", true
    Float totalValue = 0

    switch (evt.name) {
    	case "power":
            //Update the device value on values map / calculate the totalvalue 
            state.appPowerValues[evt.deviceId] = evt.value
            state.appPowerValues.each{ it -> totalValue+=Float.parseFloat(it.value)}              
            break
        case "energy":
            //Update the device value on values map / calculate the totalvalue 
        	state.appEnergyValues[evt.deviceId] = evt.value
        	state.appEnergyValues.each{ it -> totalValue+=Float.parseFloat(it.value)}
            break
        default:
            logging "WARNING: Unknown command"
            break        
    }

    mainSwitch.physicalSendEvent(evt.name, totalValue)
}

//Application Log
/* type: 0 - info; 1 - trace */
def logging(String msg, boolean trace = false){
    def appName = "SwitchOfSwitches"

	msg = "[" + (new Date()).format("yyyy-MM-dd HH:mm:ss",location.getTimeZone()) + "] ${friendlyName} - " + msg
    
    if (trace && testMode) 
    	log.debug msg
    else if (!trace)    	
    	log.info msg
}