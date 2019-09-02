/**
*  Controla a ventilação
*  by Nuno Queirós
* 
*  Versão: 1.0
*  Atualizações:
*  	- 21.02.2018 (0.1): Primeira versão OK
* 	- 26.02.2018 (1.0): Adicionado o controlo de humidade exterior a condicionar a ventilação (apenas ventila se humidade <= 60). 
* 	- 25.01.2019 : Renomeado para Qualidade do Ar. Adicionados os desumidificadores.
**/
 
definition(
    name: "Qualidade do Ar",
    namespace: "nqueiros",
    author: "Nuno Queiros",
    description: "Automatiza a ventilação.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan@2x.png"
)

preferences {
	section("Dispositivos...") {
	    input "fan", "capability.switch", title: "Ventilador", multiple: false, required: true
        input "sensor", "capability.waterSensor", title: "Estação metereológica", required: true, multiple: false
        input "dehumidifierQC", "capability.switch", title: "Desumidificador QC", required: false, multiple: false
	}
	section("Horário...") {
        input "startTimeFAN", "time", title: "Início vetilação", required: true, multiple: false
        input "durationFAN", "number", title: "Duração (min.)", required: true, multiple: false
        input "startTimeDQC", "time", title: "Deshum. QC - Início", required: true, multiple: false
        input "endTimeDQC", "time", title: "Deshum. QC - Fim", required: true, multiple: false        
	}
	section("Opções...") {
        input "maxExtHumidity", "number", title: "Humidade max", required: true, multiple: false
	}    
	section("Testes") {
		input "testMode", "bool", title: "Test mode", multiple: false, required: true, defaultValue: false
        input "testDurationFAN", "number", title: "Duração (seg.)", required: true, multiple: false, default: 30
	}
    
}

def installed() {
	appLog "Ventilation App - Installed. Settings: ${settings}", 1
	initialize()
}

def updated() {
	appLog "Ventilation App - Updated. Settings: ${settings}",1
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	appLog "Ventilation App - Initializing", 1
  
  	//If testmode subscribe the app play/touch event
  	if (settings.testMode) {
  		subscribe(app, testHandler)
  	}
  
	//Schedule Fan
	def UTC_Time = timeToday(settings.startTimeFAN)
	schedule(UTC_Time, "scheduleHandlerFAN")
    //Schedule Dehumidifier QC
	UTC_Time = timeToday(settings.startTimeDQC)
	schedule(UTC_Time, "scheduleHandlerDQCOn")
    UTC_Time = timeToday(settings.endTimeDQC)
	schedule(UTC_Time, "scheduleHandlerDQCOff")  
    
    appLog "Initialization completed!", 1
}

def scheduleHandlerFAN(evt){
	appLog "ScheduleHandler (Start Ventilation Triggered). Event data: ${evt}; State: ${state} ", 1
    runFan(evt)
	appLog "ScheduleHandler (Start Ventilation Finished). State: ${state}", 2
}

def scheduleHandlerDQCOn(evt){
	appLog "ScheduleHandler (Dehumidifier QC - ON). Event data: ${evt}; State: ${state} ", 1
    dehumidifierQC.on()
}

def scheduleHandlerDQCOff(evt){
	appLog "ScheduleHandler (Dehumidifier QC - OFF). Event data: ${evt}; State: ${state} ", 1
    dehumidifierQC.off()
}

def runFan(evt){
	def humidityAttr = sensor.currentValue("humidity")
	    
    if (humidityAttr <= settings.maxExtHumidity) {
    	appLog "Outside humidity level is OK for ventilation: ${humidityAttr}", 1
		sendNotificationEvent("A ventilação foi iniciada às " + (new Date()).format("yyyy/MM/dd HH:mm",location.getTimeZone()) + " e irá funcionar durante " + settings.durationFAN + " minutos. Nível de humidade exterior: " + humidityAttr + ".")
    
    	//Turn off dehumidifiers
        scheduleHandlerDQCOff(evt)
    	//Turn on
    	fan.on()
    
        if (settings.testMode) {
            appLog "TestMode: Will turn off in ${testDurationFAN} seconds...", 2
            runIn(testDurationFAN, turnOffHandler)	
        }    
        else {
            def duration = settings.durationFAN * 60
            appLog "Turned On: Will turn off in ${duration} seconds...", 2
            runIn(duration, turnOffHandler)	
        }
	}
    else {
    	appLog "Outside humidity level is too high for vetilation. Current humidity: ${humidityAttr}%; Max humidity ${settings.maxExtHumidity}%.", 1
		sendNotificationEvent("A ventilação NÃO foi iniciada às " + (new Date()).format("yyyy/MM/dd HH:mm",location.getTimeZone()) + ". A humidade está demasiado elevada " + humidityAttr + " (a humidade máxima é ${settings.maxExtHumidity}%).")
    }
}

def turnOffHandler(evt) {
	appLog "turnOffHandler: Event data: ${evt}", 1

   	//If device is on try repeat the turn off event after 1 minute
   	def fanState = fan.currentValue("switch")
	if (fanState == "on") {
    	appLog "turnOffHandler: Device is ON... the turn off event will run again in 30 seconds [just to make sure :)].", 2
   		runIn(30, turnOffHandler)
   	}
    else {
    	appLog "turnOffHandler: Device is definitively OFF [that's all for now :)]", 2
	}

	//Turn off
	fan.off()   
}

def testHandler(evt) {
   appLog "TestHandler: Event data: ${evt}", 2
   scheduleHandlerFAN(evt)
}

//Application Log
def appLog(String msg, int level){
	//Info -> level=1
	if (level == 1) {
    	msg = "[" + (new Date()).format("yyyy-MM-dd HH:mm:ss",location.getTimeZone()) + "] AppVentilation - " + msg
    	log.info msg
    }
    //Debug -> level>1 and testmode
    else if (level > 1 && settings.testMode) {
    	msg = "[" + (new Date()).format("yyyy-MM-dd HH:mm:ss",location.getTimeZone()) + "] AppVentilation - " + msg
    	log.debug msg
    }
}