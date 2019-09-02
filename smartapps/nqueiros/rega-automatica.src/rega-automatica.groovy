/**
 *  Controla a rega automática
 *
 *	Author: Nuno Queirós
 *
 **/
 
definition(
    name: "Rega Automatica",
    namespace: "nqueiros",
    author: "Nuno Queiros",
    description: "Automatiza a rega nas diversas zonas.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/dry-the-wet-spot.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/dry-the-wet-spot@2x.png"
)

preferences {
	section("Dispositivos...") {
	    input "fan", "capability.switch", title: "Ventilador", multiple: false, required: true
        input "sensor", "capability.waterSensor", title: "Estação metereológica", required: true, multiple: false
	}
	section("Horário...") {
        input "startTime", "time", title: "Hora de início", required: true, multiple: false
        input "durationMin", "number", title: "Duração (min.)", required: true, multiple: false
	}

	section("Dispositivos...") {
		input "sensor", "capability.waterSensor", title: "Estação metereológica", required: true, multiple: false
        input "regaTanque", "capability.switch", title: "Zona 1", multiple: false, required: true
        input "tanque", "capability.switch", title: "Zona 2", multiple: false, required: true
        input "regaMina", "bool", title: "Test mode", multiple: false, required: true, defaultValue: false
	}
	section("Horários...") {
		input "horaInicio", "time", title: "Hora de início", required: true, multiple: false
        input "tempoTanque", "number", title: "Tempo de rega Relay 1 (min.)", required: true, multiple: false
        input "IntensivoRelay2Minutos", "number", title: "Tempo de rega Relay 2 (min.)", required: true, multiple: false
        input "IntensivoPausaDias", "number", title: "Pausa após chuva (dias)", required: true, multiple: false
        input "IntensivoIntervaloDias", "number", title: "Intervalo entre regas (dias)", required: true, multiple: false
	}
    section("Horários...") {
		input "IntensivoHoraInicio", "time", title: "Hora de início", required: true, multiple: false
        input "tempoTanque", "number", title: "Tempo de rega Relay 1 (min.)", required: true, multiple: false
        input "IntensivoRelay2Minutos", "number", title: "Tempo de rega Relay 2 (min.)", required: true, multiple: false
        input "IntensivoPausaDias", "number", title: "Pausa após chuva (dias)", required: true, multiple: false
        input "IntensivoIntervaloDias", "number", title: "Intervalo entre regas (dias)", required: true, multiple: false
	}

    
	section("Periodo de inverno...") {
		input "InvernoHoraInicio", "time", title: "Hora de início", required: true, multiple: false
        input "InvernoRelay1Minutos", "number", title: "Tempo de rega Relay 1 (min.)", required: true, multiple: false
        input "InvernoRelay2Minutos", "number", title: "Tempo de rega Relay 2 (min.)", required: true, multiple: false
        input "InvernoPausaDias", "number", title: "Pausa após chuva (dias)", required: true, multiple: false
        input "InvernoIntervaloDias", "number", title: "Intervalo entre regas (dias)", required: true, multiple: false
	}  
	section("Testes") {
		input "testMode", "bool", title: "Test mode", multiple: false, required: true, defaultValue: false
	}    
}

def runWatering(){
	sendNotificationEvent("A calcular a necessidade de efetuar a rega automática: " + (new Date()).format("yyyy/MM/dd HH:mm",location.getTimeZone()))
	appLog "Watering Started. State: ${state}", 2
    
    updateAppState()
    def currentContext = context //Groovy magic (automatically runs getContext())
    
    //Hora de regar?
    
    
    appLog "Watering finished. State: ${state}", 2
}

/* Update application state data */
def updateAppState(){
    appLog "UpdateAppState (begin). State: ${state}", 2

    def todaysPrecip = sensor.currentPrecipToday

    //Updates the last moisture date if the precipitaion is above 3mm
    if (todaysPrecip.toInteger() >= 3) state.lastMoisture = new Date()

    appLog "UpdateAppState (end). State: ${state}", 2
}

/* Application context */
def getContext(){
    appLog "Updating context.", 2
    appLog "Update context - initial settings: ${settings}", 1
    def retContext=[:]
    
    //Is winter?
    def winterMonths = [1,2,10,11,12]
    def currentMonth = (new Date()).format('MM') as int    
    def isWinter = winterMonths.contains(currentMonth) ? true : false
	
    //set context
    retContext.startTime = isWinter ? settings.InvernoHoraInicio : settings.IntensivoHoraInicio
    retContext.relay1duration = isWinter ? settings.InvernoRelay1Minutos : settings.IntensivoRelay1Minutos
    retContext.relay2duration = isWinter ? settings.InvernoRelay2Minutos : settings.IntensivoRelay2Minutos
    retContext.pauseAfterMoisture = isWinter ? settings.InvernoPausaDias : settings.IntensivoPausaDias
    retContext.pauseBetweenWaterings = isWinter ? settings.InvernoIntervaloDias : settings.IntensivoIntervaloDias

    appLog "Update context - final settings: ${settings}", 1
    appLog "Return context: ${retContext}", 1
    return retContext
}


//****************** INIT and UPDATE *****************//

def installed() {
	appLog "Installed with settings: ${settings}", 2
 	initialize()
}

def updated() {
	appLog "Updated with settings: ${settings}",2
	unsubscribe()
    unschedule()
 	initialize()
}

def initialize() {
  appLog "Initializing Rega Automatica v0.1", 2
  
  if (settings.testMode) {
  	subscribe(app, testHandler)
  }
  
  //Atualiza parâmetros dos relays
  def currentContext = context
  appLog "Set relay1 to ${currentContext.relay1duration * 60} seconds.", 2
  settings.relay1.setWateringConfigurations(currentContext.relay1duration * 60)
  appLog "Set relay2 to ${currentContext.relay2duration * 60} seconds.", 2
  settings.relay2.setWateringConfigurations(currentContext.relay2duration * 60)
  
  //Schedule
  def timeWinter = timeToday(settings.InvernoHoraInicio)
  def timeSummer = timeToday(settings.IntensivoHoraInicio)
  def cronExpressionWinter = "0 ${timeWinter.format("mm")} ${timeWinter.format("HH")} 1/${settings.InvernoIntervaloDias} 11-2 ? *"
  def cronExpressionSummer = "0 ${timeSummer.format("mm")} ${timeSummer.format("HH")} 1/${settings.IntensivoIntervaloDias} 3-10 ? *"
  schedule(cronExpressionWinter, "scheduleHandlerWinter")
  schedule(cronExpressionSummer, "scheduleHandlerSummer")
  
  //colocar aqui timer para atualizar a data de chuva
  runEvery1Hour(updateAppStateHandler)
  
  //Define data da última chuva, caso não exista
  if (!state.lastMoisture) state.lastMoisture = Date.parse("yyyy-MM-dd HH:mm:ss", "2000-01-01 00:00:00")
}

def updateAppStateHandler(){
	updateAppState()
}

def scheduleHandlerWinter(){
    runWatering()
}

def scheduleHandlerSummer(){
	runWatering()
}

def testHandler(evt) {
	appLog "TestHandler: Event data: ${evt}", 2
	runWatering()
    //updateAppState()
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