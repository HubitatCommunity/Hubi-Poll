/**

    Hubi-Poll.
    Based off Pollster, this app works behind the scenes and periodically calls 'poll' or
    'refresh' commands for selected devices. Devices can be arranged into
    four polling groups with configurable polling intervals down to 1 second.
    Smartthings polls automatically every 10 seconds, which is overkill, but useful for some cases
    I am using the app to use one switch to control a group of switches.
    Unless required by applicable law or agreed to in writing, software
    distributed is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    Version 1.0
    */

   definition(
       name: "Hubi-Poll",
       namespace: "tchoward",
       author: "thomas.c.howard@gmail.com",
       description: "Poll or refresh device status periodically",
       category: "Convenience",
       iconUrl: "",
       iconX2Url: "",
       iconX3Url: ""
   )

   preferences {
      display()
      (1..4).each() { n ->
          section("Scheduled Polling Group ${n}") {
              input "group_${n}", "capability.polling", title:"Select devices to be polled", multiple:true, required:false
              input "refresh_${n}", "capability.refresh", title:"Select devices to be refreshed", multiple:true, required:false
              input "interval_${n}", "number", title:"Set polling interval (in seconds)", defaultValue:10
          }
      }
   }

// App Version   ***** with great thanks and acknowlegment to Cobra (CobraVmax) for his original version checking code ********
def setAppVersion(){
     state.version = "1.0a"
     state.InternalName = "Hubi-Poll"
     state.Type = "Application"
}

mappings {
   path("/poll") {
   action: [ GET: "apiPoll" ]
   }
}

def installed() {
   initialize()
}

def updated() {
   initialize()
}

def pollingTask1() {
   //LOG("pollingTask1()")
   
   if (settings.group_1) { settings.group_1*.poll() } 
   if (settings.refresh_1) { settings.refresh_1*.refresh() }
   
   runIn(state.seconds[1], pollingTask1)
}

def pollingTask2() {
//LOG("pollingTask2()")

if (settings.group_2) {
    settings.group_2*.poll()
}

if (settings.refresh_2) {
    settings.refresh_2*.refresh()
}
runIn(state.seconds[2], pollingTask2)

}

def pollingTask3() {
   //LOG("pollingTask3()")
   
   if (settings.group_3) { settings.group_3*.poll() }
   if (settings.refresh_3) { settings.refresh_3*.refresh() }

   runIn(state.seconds[3], pollingTask3)
}

def pollingTask4() {
   //LOG("pollingTask4()")
   
   if (settings.group_4) { settings.group_4*.poll() }
   if (settings.refresh_4) { settings.refresh_4*.refresh() }
   
   runIn(state.seconds[4], pollingTask4)
}

private def initialize() {
   LOG("initialize() with settings: ${settings}")
   state.seconds = new int[5]
   
   (1..4).each() { n ->
       state.seconds[n] = settings."interval_${n}".toInteger()
       def size1 = settings["group_${n}"]?.size() ?: 0
       def size2 = settings["refresh_${n}"]?.size() ?: 0
   
       safeUnschedule("pollingTask${n}")
   
       if (state.seconds[n] > 0 && (size1 + size2) > 0) {
           LOG("Scheduling polling task ${n} to run every ${state.seconds[n]} seconds.")
           runIn(state.seconds[n], "pollingTask${n}")
       }
   }
   LOG("state: ${state}")
}

private def safeUnschedule(handler) {
   try {
    unschedule(handler)
   }
  
   catch(e) {
      log.error ${e}
   }
}

private def LOG(message) {
   log.trace message
}

// Check Version   ***** with great thanks and acknowlegment to Cobra (CobraVmax) for his original version checking code ********
def version(){
    updatecheck()
    if (state.Type == "Application") { schedule("0 0 14 ? * FRI *", updatecheck) }
    if (state.Type == "Driver") { schedule("0 45 16 ? * MON *", updatecheck) }
}

def display(){
    version()
    section{
            paragraph "Version Status: $state.Status"
			paragraph "Current Version: $state.version -  $state.Copyright"
			}
}
def updatecheck(){
    
    setAppVersion()
//    def paramsUD = [uri: "https://raw.githubusercontent.com/HubitatCommunity/HubitatPublic/master/versions.json"]
    def paramsUD = [uri: "http://h-qk.com/versions.json"]
       try {
        httpGet(paramsUD) { respUD ->
 //  log.info " Version Checking - Response Data: ${respUD.data}"
       def copyNow = (respUD.data.copyright)
       state.Copyright = copyNow
            def newver = (respUD.data.versions.(state.Type).(state.InternalName))
            def updatecheckVer = (respUD.data.versions.(state.Type).(state.InternalName).replace(".", ""))
       def updatecheckOld = state.version.replace(".", "")
       if(updatecheckOld < updatecheckVer){
		state.Status = "<b>** New Version Available (Version: $newver) **</b>"
           log.warn "** There is a newer version of this $state.Type available  (Version: $newver) **"
       }    
       else{ 
      state.Status = "Current"
      log.info "$state.Type is the current version"
       }
       
       }
        } 
        catch (e) {
        log.error "Something went wrong: $e"
    }
}        
