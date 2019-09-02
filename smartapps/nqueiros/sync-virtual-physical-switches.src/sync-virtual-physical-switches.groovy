/**
 *  Virtual / Physical Switch Sync (i.e. Enerwave ZWN-RSM2 Adapter, Monoprice Dual Relay, Philio PAN04, Aeon SmartStrip)
 *
 *  Copyright 2016 Eric Maycock (erocm123)
 * 
 *  Note: Use a "Simulated Switch" from the IDE for best results
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *	
 *	Based on Eric Maycock's Smart App (Virtual / Physical Switch Sync) 
 */
definition(
   name: "Sync Virtual / Physical Switches",
   namespace: "nqueiros",
   author: "Nuno Queiros",
   description: "Keeps multi switch devices sync with their virtual switches",
   category: "My Apps",
   iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
   iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "numberPage", nextPage: "setupPage")
    page(name: "setupPage")
}

def numberPage() {
    dynamicPage(name: "numberPage", install: false, uninstall: true) {
        section {
            input "vNumber", "number", title:"Number of virtual switches", description: 2, defaultValue: 2 , required: true
        }
        section([title:"Available Options", mobileOnly:true]) {
			label title:"Assign a name for your app (optional)", required:false
		}
    }

}

def setupPage() {
    dynamicPage(name: "setupPage", install: true, uninstall: true) {
    section {
        input "physical", "capability.switch", title: "Which Physical Switch?", multiple: false, required: true
        for (int i = 1; i <= vNumber; i++){
            input "virtual${i}", "capability.switch", title: "Virtual Switch to link to Switch ${i}?", multiple: false, required: true        
        }
    }
    }
}

def installed() {
  appLog "Installed with settings: ${settings}", 2
  initialize()
}

def updated() {
  appLog "Updated with settings: ${settings}",2
  unsubscribe()
  initialize()
}

def initialize() {
  appLog "Initializing Virtual / Physical Switch Sync v 1.0", 2
  for (int i = 1; i <= vNumber; i++){
     subscribe(physical, "switch${i}", physicalHandler)
     subscribeToCommand(settings["virtual${i}"], "on", virtualHandler)
     subscribeToCommand(settings["virtual${i}"], "off", virtualHandler)
     subscribeToCommand(settings["virtual${i}"], "wateringSecs", virtualHandler)
     subscribe(physical, "power${i}", powerHandler)
     subscribe(physical, "energy${i}", energyHandler)
  }
}

def virtualHandler(evt) {
  	appLog "virtualHandler called with event: deviceId ${evt.deviceId} name:${evt.name} source:${evt.source} value:${evt.value} isStateChange: ${evt.isStateChange()} isPhysical: ${evt.isPhysical()} isDigital: ${evt.isDigital()} data: ${evt.data} device: ${evt.device}", 2
    for (int i = 1; i <= vNumber; i++){
    	
        def virtualDevice = settings["virtual${i}"]
        appLog "IF inside FOR: ${evt.deviceId}==${virtualDevice.id} ", 1
        
        if (evt.deviceId == virtualDevice.id) {

			switch (evt.name) {
            	
                /*REGA - Define a configuração dos segundos no dispositivo físico*/ 
                case "wateringSecs": 
                	int wSeconds = evt.value.toInteger()
        			appLog "A definir segundos de rega: ${wSeconds}", 1
					physical.configureOffSeconds(i, wSeconds)
                    appLog "Segundos de rega definidos",1
                    //physical.setOffSeconds(i, wSeconds)
					break

				/*Executa a ação física*/
				default: 
                    appLog "COMMAND CALL: physical.${evt.value}${i}()", 1
                    physical."${evt.value}${i}"()
                    break
            }   
       	}
    }
}

def physicalHandler(evt) {
  appLog "physicalHandler called with event:  name:${evt.name} source:${evt.source} value:${evt.value} isStateChange: ${evt.isStateChange()} isPhysical: ${evt.isPhysical()} isDigital: ${evt.isDigital()} data: ${evt.data} device: ${evt.device}", 2
  for (int i = 1; i <= vNumber; i++){
       if (evt.name == "switch${i}") {
            try {
            	def virtualDevice = settings["virtual${i}"]
            	appLog "Command to VIRTUAL: ${virtualDevice}.${evt.value}Physical()", 1
                virtualDevice."${evt.value}Physical"()
			} catch (e) {
                log.error "Error bypassing a PHYSICAL command to VIRTUAL.", e
			}
       }
    }
}

def powerHandler(evt) {
   appLog "powerHandler called with event:  name:${evt.name} source:${evt.source} value:${evt.value} isStateChange: ${evt.isStateChange()} isPhysical: ${evt.isPhysical()} isDigital: ${evt.isDigital()} data: ${evt.data} device: ${evt.device}", 2
   for (int i = 1; i <= vNumber; i++){
       if (evt.name == "power${i}") {
       		def virtualDevice = settings["virtual${i}"]
           	appLog "Power to VIRTUAL: sendEvent(${virtualDevice.name}, [name:power, value:${evt.value}])", 1
            virtualDevice.physicalSendEvent(name:"power", value:"${evt.value}")
       }
   }
}

def energyHandler(evt) {
   appLog "energyHandler called with event:  name:${evt.name} source:${evt.source} value:${evt.value} isStateChange: ${evt.isStateChange()} isPhysical: ${evt.isPhysical()} isDigital: ${evt.isDigital()} data: ${evt.data} device: ${evt.device}", 2
   for (int i = 1; i <= vNumber; i++){
       	if (evt.name == "energy${i}") {
       		def virtualDevice = settings["virtual${i}"]
           	appLog "Power to VIRTUAL: sendEvent(${virtualDevice.name}, [name:energy, value:${evt.value}])", 1
            virtualDevice.physicalSendEvent(name:"energy", value:"${evt.value}")          
       	}
   }
}

//Application Log
def appLog(String msg, int level){
	def debugLevel = 1 //1-all details, 2-simple
	if (level>= debugLevel) log.debug msg
}