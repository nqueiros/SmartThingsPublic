/**
*  Controla o funcionamento do termoacumulador
*  by Nuno Queirós
* 
*  Versão: 0.1
*  Atualizações:
*  	- 12.09.2018 (0.1): Primeira versão
*   - 18.09.2018 - Adicionado o cálculo dinêmico do tempo de execução para atingir a temperatura objetivo
**/
 
definition(
    name: "Termoacumulador",
    namespace: "nqueiros",
    author: "Nuno Queiros",
    description: "Automatiza o funcionamento do termoacumulador.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Allstate/its_too_hot.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Allstate/its_too_hot@2x.png"
)

preferences {
	section("Dispositivos...") {
	    input "termostato", "capability.thermostat", title: "Termostato", multiple: false, required: true
        input "aquecimento30Min", "number", title: "Aquecimento: ºC em 30 min. (default: 15)", defaultValue: 15, required: true, multiple: false
	}
	section("Horário 1...") {
        input "startTimeH1", "time", title: "Início", required: true, multiple: false
        input "temperaturaMinimaH1", "number", title: "Temperatura mínima", required: true, multiple: false
        input "temperaturaObjetivo1", "number", title: "Aquecer até (ºC) (default: 45)", defaultValue: 45, required: true, multiple: false
	}
	section("Horário 2...") {
        input "startTimeH2", "time", title: "Início", required: true, multiple: false
        input "temperaturaMinimaH2", "number", title: "Temperatura mínima", required: true, multiple: false
		input "temperaturaObjetivo2", "number", title: "Aquecer até (ºC) (default: 45)", defaultValue: 45, required: true, multiple: false
        }
	section("Testes") {
		input "testMode", "bool", title: "Test mode", multiple: false, required: true, defaultValue: false
	}
    
}

def executaHorario(horario, evt){
	def temperaturaAtual = termostato.currentValue("temperature")    
    def temperaturaMinima = horario == 1 ? temperaturaMinimaH1:temperaturaMinimaH2
	
    if (temperaturaAtual <= temperaturaMinima) {
    	def duracaoExecucao = daDuracaoExecucao (temperaturaAtual, horario) 
    	
        def msg = "Dado a temperatura atual da água (${temperaturaAtual}) ser inferior à temperatura mínima (${temperaturaMinima}) o aquecimento do termoacumulador está a ser iniciado (" + (new Date()).format("yyyy/MM/dd HH:mm",location.getTimeZone()) + ") e vai ficar ligado durante ${duracaoExecucao} minutos, segundo os cálculos efetuados."
    	appLog msg, 2
		sendNotificationEvent(msg)
    
    	//Turn on
    	termostato.heat()
    
        if (settings.testMode) {
            appLog "TestMode: Will turn off in 30 seconds...", 2
            runIn(30, turnOffHandler)	
        }    
        else {
            def duration = duracaoExecucao * 60
            appLog "Turned On: Will turn off in ${duration} seconds...", 2
            runIn(duration, turnOffHandler)	
        }
	}
    else {
    	def msg = "Dado a temperatura atual da água (${temperaturaAtual}) ser superior à temperatura mínima (${temperaturaMinima}) o aquecimento do termoacumulador não vai ser iniciado (" + (new Date()).format("yyyy/MM/dd HH:mm",location.getTimeZone()) + ")."
    	appLog msg, 2
		sendNotificationEvent(msg)
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
	def UTC_StartTimeH1 = timeToday(settings.startTimeH1)
	schedule(UTC_StartTimeH1, "scheduleHandlerH1")
	def UTC_StartTimeH2 = timeToday(settings.startTimeH2)
	schedule(UTC_StartTimeH2, "scheduleHandlerH2")

    appLog "Initialization completed!", 1
}

def scheduleHandlerH1(evt){
	appLog "Iniciada a validação da necessidade de aquecimento (Horário 1). Event data: ${evt}; State: ${state} ", 1
    executaHorario(1, evt)
	appLog "Terminada a validação da necessidade de aquecimento (Horário 1). State: ${state}", 2
}

def scheduleHandlerH2(evt){
	appLog "Iniciada a validação da necessidade de aquecimento (Horário 2). Event data: ${evt}; State: ${state} ", 1
    executaHorario(2, evt)
	appLog "Terminada a validação da necessidade de aquecimento (Horário 2). State: ${state}", 2
}

def turnOffHandler(evt) {
	def estadoTermostato = termostato.currentValue("thermostatMode")
    
    appLog "turnOffHandler -> Estado atual: ${estadoTermostato}; Event data: ${evt}", 1

   	//If device is on try repeat the turn off event after 1 minute
   	
	if (estadoTermostato == "heat") {
    	appLog "turnOffHandler: Device is ON... the turn off event will run again in 30 seconds [just to make sure :)].", 2
   		runIn(30, turnOffHandler)
   	}
    else {
    	appLog "turnOffHandler: Device is definitively OFF [that's all for now :)]", 2
	}

	//Turn off
	termostato.off()   
}

def daDuracaoExecucao (temperaturaAtual, horario){
	def aquec30Min = aquecimento30Min >0 ? aquecimento30Min : 15
    def tempMax = horario == 1 ? temperaturaObjetivo1 : temperaturaObjetivo2
    
    appLog "daDuracaoExecucao -> temperaturaAtual: ${temperaturaAtual}; temperaturaObjetivo: ${tempMax}", 1

    def aquec1Min = aquec30Min / 30
	def delta = tempMax - temperaturaAtual
    def resultado = 0
    
    if (delta>0)  {
    	resultado = delta/aquec1Min    
    	resultado = resultado.toInteger()		 		//Supostamente o toInteger trunca (que é o objetivo) 
    	resultado = resultado > 120 ? 120 : resultado 	//No máximo trabalha durante 2 horas 
    }

    appLog "daDuracaoExecucao: Tempo de execução calculado: ${resultado}", 2
    return resultado
}


def testHandler(evt) {
   appLog "TestHandler: Event data: ${evt}", 2
   scheduleHandlerH1(evt)
}

//Application Log
def appLog(String msg, int level){
	//Info -> level=1
	if (level == 1) {
    	msg = "[" + (new Date()).format("yyyy-MM-dd HH:mm:ss",location.getTimeZone()) + "] AppTermoacumulador - " + msg
    	log.info msg
    }
    //Debug -> level>1 and testmode
    else if (level > 1 && settings.testMode) {
    	msg = "[" + (new Date()).format("yyyy-MM-dd HH:mm:ss",location.getTimeZone()) + "] AppTermoacumulador - " + msg
    	log.debug msg
    }
}