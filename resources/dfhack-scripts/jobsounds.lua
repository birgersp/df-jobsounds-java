local scriptName = 'jobsounds'
local jobs = {
	'Dig',
	'DigChannel',
	'ConstructBuilding',
	'FellTree'
}

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
	if (socket ~= nil) then
		if (
		    df.global.window_z == unit.pos.z and
			df.global.window_x < unit.pos.x and 
			df.global.window_x+90 > unit.pos.x and
			df.global.window_y < unit.pos.y and 
			df.global.window_y+60 > unit.pos.y
		) then
			send(unit.id .. " " .. jobType .. "\n")
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
		checkDwarves()
	end

	dfhack.timeout(15, 'ticks', loop)
end

start()