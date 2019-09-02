/**
*  Controla a ventilação
*  by Nuno Queirós
* 
*  Versão: 1.0
*  Atualizações:
*  	- 26.02.2018: Primeira versão OK
**/
 
definition(
    name: "EncherTanque",
    namespace: "nqueiros",
    author: "Nuno Queiros",
    description: "Automatiza o enchimento do tanque.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Pets/App-Does___HaveWater.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Pets/App-Does___HaveWater@2x.png"
)

preferences {
	section("Dispositivos...") {
	    input "electrovalve", "capability.switch", title: "Eletroválvula", multiple: false, required: true
	}
	section("Horário...") {
        input "startTime", "time", title: "Hora de início", required: true, multiple: false
        input "durationMin", "number", title: "Duração (min.)", required: true, multiple: false
	}
	section("Testes") {
		input "testMode", "bool", title: "Test mode", multiple: false, required: true, defaultValue: false
	}
    
}

def runApp(evt){	    
	sendNotificationEvent("O tanque começou a encher às " + (new Date()).format("yyyy/MM/dd HH:mm",location.getTimeZone()) + " e irá funcionar durante " + settings.durationMin + " minutos.")

    //Turn on
    electrovalve.on()
    
    if (settings.testMode) {
        appLog "TestMode: Will turn off in 30 seconds...", 2
        runIn(30, turnOffHandler)	
    }    
    else {
        def duration = settings.durationMin * 60
        appLog "Turned On: Will turn off in ${duration} seconds...", 2
        runIn(duration, turnOffHandler)	
    }
}

def installed() {
	appLog "Installed. Settings: ${settings}", 1
	initialize()
}

def updated() {
	appLog "Updated. Settings: ${settings}",1
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	appLog "Initializing", 1
  
  	//If testmode subscribe the app play/touch event
  	if (settings.testMode) {
  		subscribe(app, testHandler)
  	}
  
	//Schedule
	def UTC_StartTime = timeToday(settings.startTime)
	schedule(UTC_StartTime, "scheduleHandler")
    
    appLog "Initialization completed!", 1
}

def scheduleHandler(evt){
	appLog "ScheduleHandler (Start Filling Triggered). Event data: ${evt}; State: ${state} ", 1
    runApp(evt)
	appLog "ScheduleHandler (Start Filling Finished). State: ${state}", 2
}

def turnOffHandler(evt) {
	appLog "turnOffHandler: Event data: ${evt}", 1

   	//If device is on try repeat the turn off event after 30 seconds
   	def deviceState = electrovalve.currentValue("switch")
	if (deviceState == "on") {
    	appLog "turnOffHandler: Device is ON... the turn off event will run again in 30 seconds [just to make sure :)].", 2
   		runIn(30, turnOffHandler)
   	}
    else {
    	appLog "turnOffHandler: Device is definitively OFF [that's all for now :)]", 2
	}

	//Turn off
	electrovalve.off()   
}

def testHandler(evt) {
   appLog "TestHandler: Event data: ${evt}", 2
   scheduleHandler(evt)
}

//Application Log
def appLog(String msg, int level){
	//Info -> level=1
	if (level == 1) {
    	msg = "[" + (new Date()).format("yyyy-MM-dd HH:mm:ss",location.getTimeZone()) + "] AppFILLTANK - " + msg
    	log.info msg
    }
    //Debug -> level>1 and testmode
    else if (level > 1 && settings.testMode) {
    	msg = "[" + (new Date()).format("yyyy-MM-dd HH:mm:ss",location.getTimeZone()) + "] AppFILLTANK - " + msg
    	log.debug msg
    }
}