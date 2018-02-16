import coppelia.IntW;
import coppelia.IntWA;
import coppelia.remoteApi;

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

public class Cubot {
	public static void main(String[] args) {
		
		System.out.println("Program started");
		
		remoteApi vrep = new remoteApi();
		vrep.simxFinish(-1); // just in case, close all opened connections
		
		int clientID = vrep.simxStart("127.0.0.1", 19999, true, true, 5000, 5);
		
		if (clientID != -1) {
			System.out.println("Connected to remote API server");

			IntWA objectHandles = new IntWA(0);
			int codeRetour = vrep.simxGetObjects(clientID, vrep.sim_handle_all, objectHandles, vrep.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok)
				System.out.format("Number of objects in the scene: %d\n", objectHandles.getArray().length);
			else
				System.out.format("simxGetObjects call returned with error code: " + codeRetourDescription(codeRetour));

			
			IntW handle_MoteurDroit = new IntW(0);
			IntW handle_MoteurGauche = new IntW(0);
			
			codeRetour = vrep.simxGetObjectHandle(clientID, "MoteurDroit", handle_MoteurDroit, remoteApi.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok)
				System.out.println("handle MoteurDroit = " + handle_MoteurDroit.toString());

			codeRetour = vrep.simxGetObjectHandle(clientID, "MoteurGauche", handle_MoteurGauche, remoteApi.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok)
				System.out.println("handle MoteurGauche = " + handle_MoteurGauche.toString());

			try {
				Thread.sleep(2000);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			
			codeRetour = vrep.simxSetJointTargetVelocity(clientID, handle_MoteurDroit.getValue(), 1.0f, remoteApi.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok)
				System.out.println("moteur droit à 1 : OK !");
			else
				System.out.println("moteur droit à 1 : simxSetJointTargetVelocity call returned with error code: "
						+ codeRetourDescription(codeRetour));
			
			codeRetour = vrep.simxSetJointTargetVelocity(clientID, handle_MoteurGauche.getValue(), 1.0f, remoteApi.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok)
				System.out.println("moteur gauche à 1 : OK !");
			else
				System.out.println("moteur gauche à 1 : : simxSetJointTargetVelocity call returned with error code: "
						+ codeRetourDescription(codeRetour));

			try {
				Thread.sleep(3*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			codeRetour = vrep.simxSetJointTargetVelocity(clientID, handle_MoteurDroit.getValue(), 0.0f, remoteApi.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok)
				System.out.println("moteur droit à 0 : OK !");
			else
				System.out.println("moteur droit à 0 : simxSetJointTargetVelocity call returned with error code: "
						+ codeRetourDescription(codeRetour));


			try {
				Thread.sleep(3*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			codeRetour = vrep.simxSetJointTargetVelocity(clientID, handle_MoteurGauche.getValue(), 0.0f, remoteApi.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok)
				System.out.println("moteur gauche à 0 : OK !");
			else
				System.out.println("moteur gauche à 0 : : simxSetJointTargetVelocity call returned with error code: "
						+ codeRetourDescription(codeRetour));

			System.out.println("fin ");
			// Now send some data to V-REP in a non-blocking fashion:
			vrep.simxAddStatusbarMessage(clientID, "Hello V-REP!", remoteApi.simx_opmode_oneshot);

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

	private static String codeRetourDescription(int codeRetour) {
		switch (codeRetour) {
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
			return codeRetour + ": code de retour inconnu";
		}

	}
}
