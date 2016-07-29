window_width = 105
window_height = 55
local factor = 9

local scriptName = 'jobsounds'
local jobs = {
	'Dig',
	'DigChannel',
	'ConstructBuilding',
	'FellTree'
}

function round(num)
  return math.floor(num + 0.5)
end

local function initSocket()
	socket = tcp:connect('127.0.0.1', 56730)
end

local function start()
	luasocket = require("plugins.luasocket")
	tcp = luasocket.tcp
	dfhack.println(scriptName.." connecting...")		
	socket = nil;
	pcall(initSocket)
	if (socket ~= nil) then
		dfhack.println(scriptName.." connected")	
		stop = false
		timeLastSend = os.time()
		loop()
	else
		dfhack.println(scriptName.." could not connect")
	end
end

local function send(msg)
	if (pcall(socket.send, socket, msg) == true) then
		timeLastSend = os.time()
	else
		stop = true
	end
end

local function triggerJobSound(unit, jobType)
	if (
	    df.global.window_z == unit.pos.z
	) then
		local ux = (unit.pos.x - window_center_x) / (window_width / 2)
		local uy = (unit.pos.y - window_center_y) / (window_height / 2)
		local d = round(math.sqrt(ux*ux + uy*uy) * factor)
		if (d <= factor) then
			send(unit.id .. " " .. jobType .. " " .. d .. "\n")
		end
	end
end

local function isRegisteredJob(jobType)
	for i, job in ipairs(jobs) do
		if (jobType == df.job_type[job]) then
			return true
		end
	end
	return false
end

local function handleDwarf(unit)
	if (unit.job.current_job ~= nil) then
		if (isRegisteredJob(unit.job.current_job.job_type) == true) then
			local jobPos = unit.job.current_job.pos
			local dx = math.abs(jobPos.x-unit.pos.x)
			local dy = math.abs(jobPos.y-unit.pos.y)
			local dz = math.abs(jobPos.z-unit.pos.z)
			if (dx <= 1 and dy <= 1 and dz <= 1) then
				triggerJobSound(unit, unit.job.current_job.job_type)
			end
		end
	end
end

local function checkDwarves()
	for k,unit in ipairs(df.global.world.units.active) do
		if (dfhack.units.isDwarf(unit)) then
			handleDwarf(unit)
		end
	end
end

function loop()
	if (os.time() - timeLastSend > 2) then
		send("null\n")
	end

	if (stop == true) then
		dfhack.println(scriptName.." stopped")
		return
	end

	if (df.global.pause_state == false) then
		local window_x = df.global.window_x
		local window_y = df.global.window_y
		window_center_x = window_x + round(window_width / 2)
		window_center_y = window_y + round(window_height / 2)
		checkDwarves()
	end

	dfhack.timeout(15, 'ticks', loop)
end

start()