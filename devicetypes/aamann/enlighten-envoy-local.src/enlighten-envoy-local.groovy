/**
 *	Enlighten Solar System (Local)
 *
 *	Copyright 2016 Andreas Amann
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *			http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 */
 
preferences {
	input("confIpAddr", "string", title:"Envoy Local IP Address",
		required: true, displayDuringSetup: true)
	input("confTcpPort", "number", title:"TCP Port",
		defaultValue:"80", required: true, displayDuringSetup: true)
	input("confNumInverters", "number", title:"Number of Inverters/Panels",
		required: true, displayDuringSetup: true)
	input("confInverterSize", "number", title:"Inverter Size (W)",
		required: true, displayDuringSetup: true)
	input("confPanelSize", "number", title:"Panel Size (W)",
		required: true, displayDuringSetup: true)
}
metadata {
	definition (name: "Enlighten Envoy (local)", namespace: "aamann", author: "Andreas Amann") {
		capability "Power Meter"
		capability "Energy Meter"
		capability "Refresh"
		capability "Polling"

		attribute "energy_str", "string"
		attribute "energy_yesterday", "string"
		attribute "energy_last7days", "string"
		attribute "energy_life", "string"
		attribute "power_details", "string"
		attribute "efficiency", "string"
		attribute "efficiency_yesterday", "string"
		attribute "efficiency_last7days", "string"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
		// this tile is used for display in device list (to get correct colorization)
		valueTile("power", "device.power") {
			state("power", label: '${currentValue}W', unit: "W", icon: "st.Weather.weather14", backgroundColors: [
				[value: 0, color: "#bc2323"],
				[value: 1000, color: "#1e9cbb"],
				[value: 2000, color: "#90d2a7"]
			])
		}
		// this tile is used only to provide an icon in the recent events list
		valueTile("energy", "device.energy") {
			state("energy", label: '${currentValue}', unit: "kWh")
		}
		// the following tiles are used for display in the device handler
		multiAttributeTile(name:"SolarMulti", type:"generic", width:6, height:4) {
			tileAttribute("device.power", key: "PRIMARY_CONTROL") {
				attributeState("power", label: '${currentValue}W', unit: "W", backgroundColors: [
					[value: 0, color: "#bc2323"],
					[value: 1000, color: "#1e9cbb"],
					[value: 2000, color: "#90d2a7"]
				])
			}
			tileAttribute("device.power_details", key: "SECONDARY_CONTROL") {
				attributeState("power_details", label: '${currentValue}')
			}
		}
		standardTile("today", "today", width: 2, height: 2) {
			state("default", label: "Today")
		}
		valueTile("energy_str", "device.energy_str", width: 2, height: 2, decoration: "flat", wordWrap: false) {
			state("energy_str", label: '${currentValue}')
		}
		valueTile("efficiency", "device.efficiency", width: 2, height: 2) {
			state("efficiency", label: '${currentValue}', backgroundColors: [
				[value: 0, color: "#bc2323"],
				[value: 2, color: "#d04e00"],
				[value: 4, color: "#f1d801"],
				[value: 5, color: "#90d2a7"],
				[value: 6, color: "#44b621"]
			])
		}
		standardTile("yesterday", "yesterday", width: 2, height: 2) {
			state("default", label: "Yesterday")
		}
		valueTile("energy_yesterday", "device.energy_yesterday", width: 2, height: 2, decoration: "flat", wordWrap: false) {
			state("energy_yesterday", label: '${currentValue}')
		}
		valueTile("efficiency_yesterday", "device.efficiency_yesterday", width: 2, height: 2) {
			state("efficiency_yesterday", label: '${currentValue}', backgroundColors: [
				[value: 0, color: "#bc2323"],
				[value: 2, color: "#d04e00"],
				[value: 4, color: "#f1d801"],
				[value: 5, color: "#90d2a7"],
				[value: 6, color: "#44b621"]
			])
		}
		standardTile("last7days", "last7days", width: 2, height: 2) {
			state("default", label: "Last 7 Days")
		}
		valueTile("energy_last7days", "device.energy_last7days", width: 2, height: 2, decoration: "flat", wordWrap: false) {
			state("energy_last7days", label: '${currentValue}')
		}
		valueTile("efficiency_last7days", "device.efficiency_last7days", width: 2, height: 2) {
			state("efficiency_last7days", label: '${currentValue}', backgroundColors: [
				[value: 0, color: "#bc2323"],
				[value: 2, color: "#d04e00"],
				[value: 4, color: "#f1d801"],
				[value: 5, color: "#90d2a7"],
				[value: 6, color: "#44b621"]
			])
		}
		standardTile("lifetime", "lifetime", width: 2, height: 2) {
			state("default", label: "Lifetime")
		}
		valueTile("energy_life", "device.energy_life", width: 2, height: 2, decoration: "flat", wordWrap: false) {
			state("energy_life", label: '${currentValue}')
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state("default", action:"polling.poll", label: "Refresh", icon:"st.secondary.refresh-icon")
		}
		htmlTile(name:"graphHTML", action: "getGraphHTML", width: 6, height: 4, whitelist: ["www.gstatic.com"])
		main "power"
		details(["SolarMulti", "graphHTML", "today", "energy_str", "efficiency", "yesterday", "energy_yesterday", "efficiency_yesterday", "last7days", "energy_last7days", "efficiency_last7days", "lifetime", "energy_life", "refresh"])
	}
}

mappings {
	path("/getGraphHTML") {action: [GET: "getGraphHTML"]}
}

def poll() {
	pullData()
}

def refresh() {
	pullData()
}

def updated() {
	log.trace("$device.displayName updated with settings: ${settings.inspect()}")
	state.maxPower = settings.confNumInverters * settings.confInverterSize
	pullData()
}

private def updateDNI() {
	if (!state.dni || state.dni != device.deviceNetworkId || (state.mac && state.mac != device.deviceNetworkId)) {
		device.setDeviceNetworkId(createNetworkId(settings.confIpAddr, settings.confTcpPort))
		state.dni = device.deviceNetworkId
	}
}

private String createNetworkId(ipaddr, port) {
	if (state.mac) {
		return state.mac
	}
	def hexIp = ipaddr.tokenize('.').collect {
		String.format('%02X', it.toInteger())
	}.join()
	def hexPort = String.format('%04X', port.toInteger())
	return "${hexIp}:${hexPort}"
}

private String getHostAddress() {
	return "${settings.confIpAddr}:${settings.confTcpPort}"
}

def pullData() {
	log.debug "Requesting latest data from Envoy…"
	updateDNI()
	return new physicalgraph.device.HubAction([
		method: "GET",
		path: "/api/v1/production",
		headers: [HOST:getHostAddress()]
	])
}

def parse(String message) {
	def msg = parseLanMessage(message)
	if (!state.mac || state.mac != msg.mac) {
		state.mac = msg.mac
	}
	if (!msg.body) {
		log.error "No HTTP body found in '${message}'"
		return null
	}
	def data = msg.json
	if (data == state.lastData) {
		log.debug "No new data"
		return null
	}
	state.lastData = data
	log.debug "New data: ${data}"
	def energyToday = (data.wattHoursToday/1000).toFloat()
	def energyLast7Days = (data.wattHoursSevenDays/1000).toFloat()
	def energyLife = (data.wattHoursLifetime/1000000).toFloat()
	def currentPower = data.wattsNow
	def todayDay = new Date().format("dd",location.timeZone)
	if (!state.today || state.today != todayDay) {
		state.peakpower = currentPower
		state.today = todayDay
		state.lastPower = 0
		sendEvent(name: 'energy_yesterday', value: device.currentState("energy_str")?.value, displayed: false)
		sendEvent(name: 'efficiency_yesterday', value: device.currentState("efficiency")?.value, displayed: false)
	}
	def previousPower = state.lastPower != null ? state.lastPower : currentPower
	def powerChange = currentPower - previousPower
	state.lastPower = currentPower
	if (state.peakpower <= currentPower) {
		state.peakpower = currentPower
		state.peakpercentage = (100*state.peakpower/state.maxPower).toFloat()
	}
	def events = []
	events << createEvent(name: 'power_details', value: ("(" + String.format("%+,d", powerChange) + "W) — Today's Peak: " + String.format("%,d", state.peakpower) + "W (" + String.format("%.1f", state.peakpercentage) + "%)"), displayed: false)
	events << createEvent(name: 'energy_last7days', value: String.format("%,#.3f", energyLast7Days) + "kWh", displayed: false)
	events << createEvent(name: 'energy_life', value: String.format("%,#.3f", energyLife) + "MWh", displayed: false)
	def efficiencyToday = (1000*energyToday/(settings.confNumInverters * settings.confPanelSize)).toFloat()
	events << createEvent(name: 'efficiency', value: String.format("%#.3f", efficiencyToday) + "\nkWh/kW", displayed: false)
	def efficiencyLast7Days = (1000/7*energyLast7Days/(settings.confNumInverters * settings.confPanelSize)).toFloat()
	events << createEvent(name: 'efficiency_last7days', value: String.format("%#.3f", efficiencyLast7Days) + "\nkWh/kW", displayed: false)
	events << createEvent(name: 'energy_str', value: String.format("%,#.3f", energyToday) + "kWh", displayed: false)
	events << createEvent(name: 'energy', value: energyToday, unit: "kWh", descriptionText: "Energy is " + String.format("%,#.3f", energyToday) + "kWh\n(Efficiency: " + String.format("%#.3f", efficiencyToday) + "kWh/kW)")
	events << createEvent(name: 'power', value: currentPower, unit: "W", descriptionText: "Power is " + String.format("%,d", currentPower) + "W (" + String.format("%#.1f", 100*currentPower/state.maxPower) + "%)\n(" + String.format("%+,d", powerChange) + "W since last reading)")
	// get power data for yesterday and today so we can create a graph
	def startOfToday = timeToday("00:00", location.timeZone)
	def dataYesterday = device.statesBetween("power", startOfToday - 1, startOfToday, [max: 288]) // 24h in 5min intervals should be more than sufficient even if we manually refreshed throughout the day…
	def dataToday = device.statesSince("power", startOfToday, [max: 288])
	def dataTable = ""
	// we want to start the graph at a full hour
	state.startTime = Math.min(dataYesterday.size() > 0 ? dataYesterday.reverse().first().date.format("HH", location.timeZone).toInteger() : 24, dataToday.size() > 0 ? dataToday.reverse().first().date.format("HH", location.timeZone).toInteger() : 24)
	dataYesterday.reverse().each() {
		dataTable += "[[" + it.date.format("HH,mm", location.timeZone) + ",0], null, null, ${it.integerValue}, null],"
	}
	dataToday.reverse().each() {
		dataTable += "[[" + it.date.format("HH,mm", location.timeZone) + ",0], null, null, null, ${it.integerValue}],"
	}
	// add latest power reading we just received (and haven't generated an event for yet)
	dataTable += "[[" + new Date().format("HH,mm", location.timeZone) + ",0], null, null, null, ${currentPower}],"
	// repeat for energy data
	dataYesterday = device.statesBetween("energy", startOfToday - 1, startOfToday, [max: 288])
	dataToday = device.statesSince("energy", startOfToday, [max: 288])
	// we drop the first point after midnight (0 energy) in order to have the graph scale correctly
	dataYesterday.reverse().drop(1).each() {
		dataTable += "[[" + it.date.format("HH,mm", location.timeZone) + ",0], ${it.floatValue}, null, null, null],"
	}
	dataToday.reverse().drop(1).each() {
		dataTable += "[[" + it.date.format("HH,mm", location.timeZone) + ",0], null, ${it.floatValue}, null, null],"
	}
	dataTable += "[[" + new Date().format("HH,mm", location.timeZone) + ",0], null, ${energyToday}, null, null]"
	state.historyTable = dataTable
	return events
}

def getGraphHTML() {
	renderHTML {
		head {
			"""
				<style type="text/css">body,div {margin:0;padding:0}</style>
				<script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
				<script type="text/javascript">
					google.charts.load('current', {packages: ['corechart']});
					google.charts.setOnLoadCallback(drawGraph);
					function drawGraph() {
						var data = new google.visualization.DataTable();
						data.addColumn('timeofday', 'X');
						data.addColumn('number', 'Power (Yesterday)');
						data.addColumn('number', 'Power (Today)');
						data.addColumn('number', 'Energy (Yesterday)');
						data.addColumn('number', 'Energy (Today)');
						data.addRows([${state.historyTable}]);
						var options = {
							height: 240,
							hAxis: {
								format: 'HH:mm',
								minValue: [${state.startTime},0,0]
							},
							series: {
								0: {targetAxisIndex: 1, color: '#FFC2C2'},
								1: {targetAxisIndex: 1, color: '#FF0000'},
								2: {targetAxisIndex: 0, color: '#D1DFFF'},
								3: {targetAxisIndex: 0, color: '#004CFF'}
							},
							vAxes: {
								0: {
									title: 'Power (W)',
									format: 'decimal',
									textStyle: {color: '#004CFF'},
									titleTextStyle: {color: '#004CFF'}
								},
								1: {
									title: 'Energy (kWh)',
									format: 'decimal',
									textStyle: {color: '#FF0000'},
									titleTextStyle: {color: '#FF0000'}
								}
							},
							legend: {
								position: 'none'
							},
							chartArea: {
								width: '75%',
								height: '85%'
							}
						};
						var chart = new google.visualization.AreaChart(document.getElementById('chart_div'));
						chart.draw(data, options);
					}
				</script>
			"""
		}
		body {
			"""
				<div id="chart_div"></div>
			"""
		}
	}
}