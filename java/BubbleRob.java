import coppelia.FloatW;
import coppelia.FloatWA;
import coppelia.IntW;
import coppelia.IntWA;
import coppelia.StringWA;
import coppelia.remoteApi;

public class BubbleRob {
	public IntW bubbleRob_graph,bubbleRob_slider, bubbleRob_leftWheel, bubbleRob_rightWheel, bubbleRob_sensingNose, bubbleRob_leftMotor, bubbleRob_rightMotor, Vision_sensor,bubbleRob_handle,bubbleRob_connection;
	int clientID;
	remoteApi vrep;

	public BubbleRob(remoteApi vrep,int clientID,IntW parent){
		this.vrep = vrep;
		this.clientID = clientID;
		IntW handle_child = new IntW(0);
		handle_child = parent;
		int i=0;
		int codeRetour;
		IntW handle_temp = new IntW(0);
		do{
			codeRetour = vrep.simxGetObjectChild(clientID,parent.getValue(),i,handle_temp,remoteApi.simx_opmode_blocking);
			if(codeRetour != remoteApi.simx_return_ok){
				System.out.println("error getting child handle; error : "+codeRetour);//LoadEnvironment.returnCodeDescription(codeRetour));
			}else{
				System.out.println(i);
				switch(i){
					case 0:
						bubbleRob_graph = new IntW(0);
						bubbleRob_graph.setValue(handle_temp.getValue());
						break;
					case 1:
						bubbleRob_connection = new IntW(0);
						bubbleRob_connection.setValue(handle_temp.getValue());
						vrep.simxGetObjectChild(clientID,handle_temp.getValue(),0,handle_temp,remoteApi.simx_opmode_blocking);
						bubbleRob_slider = new IntW(0);
						bubbleRob_slider.setValue(handle_temp.getValue());
						break;
					case 2:
						bubbleRob_leftMotor = new IntW(0);
						bubbleRob_leftMotor.setValue(handle_temp.getValue());
						vrep.simxGetObjectChild(clientID,handle_temp.getValue(),0,handle_temp,remoteApi.simx_opmode_blocking);
						bubbleRob_leftWheel = new IntW(0);
						bubbleRob_leftWheel.setValue(handle_temp.getValue());
						break;
					case 3:
						bubbleRob_rightMotor = new IntW(0);
						bubbleRob_rightMotor.setValue(handle_temp.getValue());
						vrep.simxGetObjectChild(clientID,handle_temp.getValue(),0,handle_temp,remoteApi.simx_opmode_blocking);
						bubbleRob_rightWheel = new IntW(0);
						bubbleRob_rightWheel.setValue(handle_temp.getValue());
						break;
					case 4:
						bubbleRob_sensingNose = new IntW(0);
						bubbleRob_sensingNose.setValue(handle_temp.getValue());
						vrep.simxGetObjectChild(clientID,handle_temp.getValue(),0,handle_temp,remoteApi.simx_opmode_blocking);
						Vision_sensor = new IntW(0);
						Vision_sensor.setValue(handle_temp.getValue());
						break;
					default:
						System.out.println("error too much child");
				}
			}
			i++;
		}while(i<=4);


	}

	public void setVelocity(float velocity){
		vrep.simxSetJointTargetVelocity(clientID,bubbleRob_rightMotor.getValue(),velocity,remoteApi.simx_opmode_blocking);
		vrep.simxSetJointTargetVelocity(clientID,bubbleRob_leftMotor.getValue(),velocity,remoteApi.simx_opmode_blocking);
	}




}
