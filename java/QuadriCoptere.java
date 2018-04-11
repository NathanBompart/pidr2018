import coppelia.IntW;
import coppelia.IntWA;
import coppelia.remoteApi;
import coppelia.FloatWA;

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

public class QuadriCoptere {


    private IntW quadriIntW;
    private int clientID;
    private remoteApi vrep;
    private String nom;

    public QuadriCoptere(remoteApi vrep,int clientID,String SCENE_OBJECT_NAME) {
        this.vrep = vrep;
        this.clientID = clientID;
        this.quadriIntW = new IntW(0);
        this.nom = SCENE_OBJECT_NAME;
        int codeRetour = vrep.simxGetObjectHandle(this.clientID, this.nom, this.quadriIntW, remoteApi.simx_opmode_blocking);
        if(codeRetour != remoteApi.simx_return_ok){
            System.out.println("Erreur chargement du Quadricoptere : "+codeRetourDescription(codeRetour));
        }
    }

    public void forward(){
        FloatWA ford =new FloatWA(3);
        ford.getArray()[0] =(float)0.1;
        ford.getArray()[1]=0;
        ford.getArray()[2]=0;
        forward(ford);
    }

    public void stopForward(){
        FloatWA ford =new FloatWA(3);
        ford.getArray()[0] = 0;
        ford.getArray()[1] = 0;
        ford.getArray()[2] = 0;
        forward(ford);
    }

    private void forward(FloatWA forwardValue){
        int result=vrep.simxCallScriptFunction(clientID,nom,vrep.sim_scripttype_childscript, "modif_sp",null,forwardValue,null,null,null,null,null,null,vrep.simx_opmode_blocking);
        if (result!=vrep.simx_return_ok) {
            System.out.println("Echec de forward : "+codeRetourDescription(result));
        }
    }

    public void rotate(int i){
        assert i==1 || i==-1;
        FloatWA ford =new FloatWA(3);
        ford.getArray()[0] = 0;
        ford.getArray()[1] = 0;
        ford.getArray()[2] = i*(float)3.14/4;
        rotate(ford);
    }

    public void stopRotate(){
        FloatWA ford =new FloatWA(3);
        ford.getArray()[0] = 0;
        ford.getArray()[1] = 0;
        ford.getArray()[2] = 0;
        rotate(ford);
    }

    private void rotate(FloatWA rotateValue) {
        int result = vrep.simxCallScriptFunction(clientID, nom, vrep.sim_scripttype_childscript, "modif_euler", null, rotateValue, null, null, null, null, null, null, vrep.simx_opmode_blocking);
        if (result != vrep.simx_return_ok) {
            System.out.println("Echec de rotate : " + codeRetourDescription(result));
        }
    }

    private static String codeRetourDescription(int codeRetour) {
        return SceneQuadri.codeRetourDescription(codeRetour);
    }
}