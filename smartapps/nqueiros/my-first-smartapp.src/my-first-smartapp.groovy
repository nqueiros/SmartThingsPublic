/**
 *  My First SmartApp
 *
 *  Copyright 2017 Nuno Queir&oacute;s
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
 */
definition(
    name: "My First SmartApp",
    namespace: "nqueiros",
    author: "Nuno Queiros",
    description: "My first test developing a smart app.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Turn on when motion detected:") {
        input "themotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn off when there's been no movement for") {
        input "minutes", "number", required: true, title: "Minutes?"
    }    
    section("Turn on this thing") {
        input "theswitch", "capability.switch", required: true
    }
}


def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

//Helpers
def initialize() {
	subscribe(themotion, "motion.active", motionDetectedHandler)
    subscribe(themotion, "motion.inactive", motionStoppedHandler)
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"

    // get the current state object for the motion sensor
    def motionState = themotion.currentState("motion")

    if (motionState.value == "inactive") {
        // get the time elapsed between now and when the motion reported inactive
        def elapsed = now() - motionState.date.time

        // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * minutes

        if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning switch off"
            theswitch.off()
        } else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
            // Motion active; just log it and do nothing
            log.debug "Motion is active, do nothing and wait for inactive"
    }
}


//Event handlers
def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    theswitch.on()
}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called: $evt"
    runIn(60 * minutes, checkMotion)
}



