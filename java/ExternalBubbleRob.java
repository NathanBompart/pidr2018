import coppelia.BoolW;
import coppelia.FloatWA;
import coppelia.IntW;
import coppelia.remoteApi;

/**
 * @author Virginie Galtier
 * 
 * Example on how to control the V-REP simul externally.
 * Replace the Lua script from the bubbleRob.ttt scene
 * (the original Lua script is at the end of this file).
 * The new script is:
 *		getSimulationTime_function=function(inInts,inFloats,inStrings,inBuffer)
 *		    result=simGetSimulationTime()
 *		    return {},{result},{},''
 *		end
 *		
 *		if (sim_call_type==sim_childscriptcall_initialization) then
 *		    simExtRemoteApiStart(19999)
 *		end
 */
public class ExternalBubbleRob {

	public static void main(String[] arg) {
		new ExternalBubbleRob();
	}

	protected remoteApi vrep;
	protected int clientID;

	public ExternalBubbleRob() {

		// Remote connection
		// ===================================================================================

		// Make sure to have the server side running in V-REP: 
		// in a child script of a V-REP scene, add following command
		// to be executed just once, at simulation start:
		//
		// simExtRemoteApiStart(19999)
		//
		// then start simulation, and run this program.
		//
		// IMPORTANT: for each successful call to simxStart, there
		// should be a corresponding call to simxFinish at the end!
		//
		// -Djava.library.path=/opt/V-REP_PRO_EDU_V3_4_0_Linux/programming/remoteApiBindings/java/lib/64Bit
		vrep = new remoteApi();
		vrep.simxFinish(-1); // just in case, close all opened connections

		clientID = vrep.simxStart("127.0.0.1", 19999, true, true, 5000, 5);
		vrep.simxAddStatusbarMessage(clientID, "Hello V-REP! External Control On", remoteApi.simx_opmode_oneshot);

		if (clientID != -1) {
			System.out.println("Connected to remote API server");


			/*
			IntWA objectHandles = new IntWA(0);
			int codeRetour = vrep.simxGetObjects(clientID, remoteApi.sim_handle_all, objectHandles, remoteApi.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok)
				System.out.format("Number of objects in the scene: %d\n", objectHandles.getArray().length);
			else
				System.out.format("simxGetObjects call returned with error code: " + returnCodeDescription(codeRetour));
			*/

			// Get handles
			/*
			// Handle of BubbleRob
			IntW handle_bubbleRob = new IntW(0);
			codeRetour = vrep.simxGetObjectHandle(clientID, "bubbleRob", handle_bubbleRob, remoteApi.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok)
				System.out.println("get handle_bubbleRob: OK");
			else
				System.out.println("get handle_bubbleRob call returned with error code: " + returnCodeDescription(codeRetour));
			*/
			// Handle of the left motor
			IntW handle_bubbleRob_leftMotor = new IntW(0);
			int codeRetour = vrep.simxGetObjectHandle(clientID, "bubbleRob_leftMotor", handle_bubbleRob_leftMotor, remoteApi.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok)
				System.out.println("get handle_bubbleRob_leftMotor: OK");
			else
				System.out.println("get handle_bubbleRob_leftMotor call returned with error code: " + returnCodeDescription(codeRetour));
			// Handle of the right motor
			IntW handle_bubbleRob_rightMotor = new IntW(0);
			codeRetour = vrep.simxGetObjectHandle(clientID, "bubbleRob_rightMotor", handle_bubbleRob_rightMotor, remoteApi.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok)
				System.out.println("get handle_bubbleRob_rightMotor: OK");
			else
				System.out.println("get handle_bubbleRob_rightMotor call returned with error code: " + returnCodeDescription(codeRetour));
			// Handle of the proximity sensor
			IntW handle_bubbleRob_sensingNose = new IntW(0);
			codeRetour = vrep.simxGetObjectHandle(clientID, "bubbleRob_sensingNose", handle_bubbleRob_sensingNose, remoteApi.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok)
				System.out.println("get handle_bubbleRob_sensingNose: OK");
			else
				System.out.println("get handle_bubbleRob_sensingNose call returned with error code: " + returnCodeDescription(codeRetour));

			// Min and max speeds for each motor
			float minSpeed = (float)(50*Math.PI/180);
			float maxSpeed = (float)(300*Math.PI/180);
			// Tells whether bubbleRob is in forward or backward mode
			float backUntilTime=-1;

			float speed = (minSpeed + maxSpeed)/2;

			while (getSimulationTime()<100) {
				System.out.println("simulation time: " + getSimulationTime());
				// result=simReadProximitySensor(noseSensor)
				BoolW detectionState = new BoolW(false); // detection state (0=no detection). Can be NULL.
				IntW detectedObjectHandle = null; // pointer to a value receiving the handle of the detected object. Can be NULL.
				FloatWA detectedPoint = null; // pointer to 3 values receiving the detected point coordinates (relative to the sensor reference frame). Can be NULL.
				FloatWA detectedSurfaceNormalVector = null; // pointer to 3 values receiving the normal vector (normalized) of the detected surface. Relative to the sensor reference frame. Can be NULL
				int operationMode = remoteApi.simx_opmode_streaming; // a remote API function operation mode. Recommended operation modes for this function are simx_opmode_streaming (the first call) and simx_opmode_buffer (the following calls)
				codeRetour = vrep.simxReadProximitySensor(clientID, handle_bubbleRob_sensingNose.getValue(), detectionState, detectedPoint, detectedObjectHandle, detectedSurfaceNormalVector, operationMode);
				if (codeRetour == remoteApi.simx_return_ok)
					System.out.println("simxReadProximitySensor: OK");
				else
					System.out.println("simxReadProximitySensor call returned with error code: " + returnCodeDescription(codeRetour));

				// -- If we detected something, we set the backward mode:
				//     if (result>0) then backUntilTime=simGetSimulationTime()+4 end
				if (detectionState.getValue()) {
					System.out.println("something deteted...");
					backUntilTime = getSimulationTime()+4;
				}
				//--   if (backUntilTime<simGetSimulationTime()) then
				//--       -- When in forward mode, we simply move forward at the desired speed
				//--        simSetJointTargetVelocity(leftMotor,speed)
				//--        simSetJointTargetVelocity(rightMotor,speed)
				//--    else
				//--        -- When in backward mode, we simply backup in a curve at reduced speed
				//--        simSetJointTargetVelocity(leftMotor,-speed/2)
				//--        simSetJointTargetVelocity(rightMotor,-speed/8)
				//--    end
				if (backUntilTime<getSimulationTime()) {
					// When in forward mode, we simply move forward at the desired speed
					codeRetour = vrep.simxSetJointTargetVelocity(clientID, handle_bubbleRob_leftMotor.getValue(), speed, remoteApi.simx_opmode_blocking);
					if (codeRetour == remoteApi.simx_return_ok)
						System.out.println("simxSetJointTargetVelocity bubbleRob_leftMotor: OK");
					else
						System.out.println("simxSetJointTargetVelocity bubbleRob_leftMotor call returned with error code: " + returnCodeDescription(codeRetour));
					codeRetour = vrep.simxSetJointTargetVelocity(clientID, handle_bubbleRob_rightMotor.getValue(), speed, remoteApi.simx_opmode_blocking);
					if (codeRetour == remoteApi.simx_return_ok)
						System.out.println("simxSetJointTargetVelocity bubbleRob_rightMotor: OK");
					else
						System.out.println("simxSetJointTargetVelocity bubbleRob_rightMotor call returned with error code: " + returnCodeDescription(codeRetour));
				} else {
					// When in backward mode, we simply backup in a curve at reduced speed
					codeRetour = vrep.simxSetJointTargetVelocity(clientID, handle_bubbleRob_leftMotor.getValue(), -speed/2, remoteApi.simx_opmode_blocking);
					if (codeRetour == remoteApi.simx_return_ok)
						System.out.println("simxSetJointTargetVelocity bubbleRob_leftMotor: OK");
					else
						System.out.println("simxSetJointTargetVelocity bubbleRob_leftMotor call returned with error code: " + returnCodeDescription(codeRetour));
					codeRetour = vrep.simxSetJointTargetVelocity(clientID, handle_bubbleRob_rightMotor.getValue(), -speed/8, remoteApi.simx_opmode_blocking);
					if (codeRetour == remoteApi.simx_return_ok)
						System.out.println("simxSetJointTargetVelocity bubbleRob_rightMotor: OK");
					else
						System.out.println("simxSetJointTargetVelocity bubbleRob_rightMotor call returned with error code: " + returnCodeDescription(codeRetour));
				}
			}

			// End of demo
			// ======================================================================================
			try {
				Thread.sleep(2*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			System.out.println("Approching the end...");

			// Now send some data to V-REP in a non-blocking fashion:
			vrep.simxAddStatusbarMessage(clientID, "Bye bye V-REP!", remoteApi.simx_opmode_oneshot);

			// Before closing the connection to V-REP, make sure that the last command sent
			// out had time to arrive. You can guarantee this with (for example):
			IntW pingTime = new IntW(0);
			vrep.simxGetPingTime(clientID, pingTime);

			// Now close the connection to V-REP:
			vrep.simxFinish(clientID);
		} else
			System.out.println("Failed connecting to remote API server");
		System.out.println("Program ended");
	}

	/**
	 * Remote call to simGetSimulationTime function: Retrieves the current simulation time.
	 * 
	 * @return negative value (-1.0) if operation not successful, otherwise the simulation time
	 */
	private float getSimulationTime() {
		/*
		 * ASSOCIATED LUA CODE:

		getSimulationTime_function=function(inInts,inFloats,inStrings,inBuffer)
		    result=simGetSimulationTime()
		    return {},{result},{},''
		end
		 */
		FloatWA outFloats= new FloatWA(1);

		int result=vrep.simxCallScriptFunction(clientID,"bubbleRob",remoteApi.sim_scripttype_childscript, "getSimulationTime_function",null,null,null,null,null,outFloats,null,null,remoteApi.simx_opmode_blocking);
		if (result==remoteApi.simx_return_ok) {
			System.out.println("Remove function call to simGetSimulationTime succeed");
			float returnValue = outFloats.getArray()[0];
			return returnValue;
		} else {
			System.out.println("Remote function call to simGetSimulationTime failed : " + returnCodeDescription(result));
			return -1;
		}	

	}

	/**
	 * Remote API function return codes
	 * 
	 * @param returnCode Remote API function return code
	 * @return textual description of the return code
	 */
	private static String returnCodeDescription(int returnCode) {
		switch (returnCode) {
		case remoteApi.simx_return_ok:
			return "0:\tThe function executed fine.";
		case remoteApi.simx_return_novalue_flag:
			return "1 (i.e. bit 0):\tnovalue_flag \t"
			+ "There is no command reply in the input buffer. This should not always be considered as an error, depending on the selected operation mode.";
		case remoteApi.simx_return_timeout_flag:
			return "2 (i.e. bit 1):\ttimeout_flag \t"
			+ "The function timed out (probably the network is down or too slow).";
		case remoteApi.simx_return_illegal_opmode_flag:
			return "4 (i.e. bit 2):\tillegal_opmode_flag \t"
			+ "The specified operation mode is not supported for the given function.";
		case remoteApi.simx_return_remote_error_flag:
			return "8 (i.e. bit 3):\tremote_error_flag \t"
			+ "The function caused an error on the server side (e.g. an invalid handle was specified).";
		case remoteApi.simx_return_split_progress_flag:
			return "16 (i.e. bit 4):\tsplit_progress_flag \t"
			+ "The communication thread is still processing previous split command of the same type.";
		case remoteApi.simx_return_local_error_flag:
			return "32 (i.e. bit 5):\tlocal_error_flag \t" + "The function caused an error on the client side.";
		case remoteApi.simx_return_initialize_error_flag:
			return "64 (i.e. bit 6):\tinitialize_error_flag \t" + "simxStart was not yet called.";
		default:
			return returnCode + ": unknown return code";
		}

	}


}


/*
--function speedChange_callback(ui,id,newVal)
--    speed=minMaxSpeed[1]+(minMaxSpeed[2]-minMaxSpeed[1])*newVal/100
--end

if (sim_call_type==sim_childscriptcall_initialization) then
    simExtRemoteApiStart(19999)
end

--if (sim_call_type==sim_childscriptcall_initialization) then 
--    -- This is executed exactly once, the first time this script is executed
--    bubbleRobBase=simGetObjectAssociatedWithScript(sim_handle_self) -- this is bubbleRob's handle
--    leftMotor=simGetObjectHandle("bubbleRob_leftMotor") -- Handle of the left motor
--    rightMotor=simGetObjectHandle("bubbleRob_rightMotor") -- Handle of the right motor
--    noseSensor=simGetObjectHandle("bubbleRob_sensingNose") -- Handle of the proximity sensor
--    minMaxSpeed={50*math.pi/180,300*math.pi/180} -- Min and max speeds for each motor
--    backUntilTime=-1 -- Tells whether bubbleRob is in forward or backward mode
--    -- Create the custom UI:
--    xml = '<ui title="'..simGetObjectName(bubbleRobBase)..' speed" closeable="false" resizeable="false" activate="false">'..[[
--                <hslider minimum="0" maximum="100" onchange="speedChange_callback" id="1"/>
--            <label text="" style="* {margin-left: 300px;}"/>
--        </ui>
--        ]]
--    ui=simExtCustomUI_create(xml)
--    speed=(minMaxSpeed[1]+minMaxSpeed[2])*0.5
--    simExtCustomUI_setSliderValue(ui,1,100*(speed-minMaxSpeed[1])/(minMaxSpeed[2]-minMaxSpeed[1]))
--end

--if (sim_call_type==sim_childscriptcall_actuation) then 
--    result=simReadProximitySensor(noseSensor) -- Read the proximity sensor
--    -- If we detected something, we set the backward mode:
--    if (result>0) then backUntilTime=simGetSimulationTime()+4 end 

--   if (backUntilTime<simGetSimulationTime()) then
--       -- When in forward mode, we simply move forward at the desired speed
--        simSetJointTargetVelocity(leftMotor,speed)
--        simSetJointTargetVelocity(rightMotor,speed)
--    else
--        -- When in backward mode, we simply backup in a curve at reduced speed
--        simSetJointTargetVelocity(leftMotor,-speed/2)
--        simSetJointTargetVelocity(rightMotor,-speed/8)
--    end
--end

--if (sim_call_type==sim_childscriptcall_cleanup) then 
--    simExtCustomUI_destroy(ui)
--end 
 */