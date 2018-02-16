import coppelia.FloatWA;
import coppelia.IntW;
import coppelia.IntWA;
import coppelia.StringWA;
import coppelia.remoteApi;

/**
 * @author Virginie Galtier
 */
public class LoadEnvironment {

	public static void main(String[] arg) {
		new LoadEnvironment();
	}

	protected remoteApi vrep;
	protected int clientID;
	protected final String GROUP_NAME = "environment";
	protected final String SCENE_OBJECT_NAME = "Sphere";
	protected String fileToImportPathAndFileName = "/home/enataf/Documents/VREP/pidr18/m2.obj";

	LoadEnvironment() {
		
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
			int codeRetour = vrep.simxGetObjects(clientID, vrep.sim_handle_all, objectHandles, vrep.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok)
				System.out.format("Number of objects in the scene: %d\n", objectHandles.getArray().length);
			else
				System.out.format("simxGetObjects call returned with error code: " + returnCodeDescription(codeRetour));
			 */

			// Remove previous environment (if any)
			// =================================================================================
			/* ORIGINAL LUA CODE:
				if environment~=nil then
					simAddStatusbarMessage("remove existing environment")
					simRemoveObject(environment)
				else
					simAddStatusbarMessage("no previous environment")
				end
			 */
			IntW handle_groupe = new IntW(0);
			int codeRetour = vrep.simxGetObjectHandle(clientID, GROUP_NAME, handle_groupe, remoteApi.simx_opmode_blocking);
			if (codeRetour == remoteApi.simx_return_ok) {
				System.out.println(GROUP_NAME + " already exists, remove it...");
				vrep.simxAddStatusbarMessage(clientID, "remove existing environment", remoteApi.simx_opmode_oneshot);
				codeRetour = vrep.simxRemoveObject(clientID, handle_groupe.getValue(), remoteApi.simx_opmode_blocking);
				System.out.println(returnCodeDescription(codeRetour));
			}

			// Load environment from .obj file
			// ===================================================================================
			/* ORIGINAL LUA CODE:
			simAddStatusbarMessage("load environment from file...")
			vertices,indices,reserved,names=simImportMesh(0,"/tmp/envSimple.obj",0,0.0001,1)
			
			if (vertices) then
				local tableShapes = {}
				for i=1,#vertices,1 do
					h=simCreateMeshShape(2,20*math.pi/180,vertices[i],indices[i])
					simSetShapeColor(h,"",sim_colorcomponent_ambient,{0.5,0.5,0.5})
					simSetObjectName(h,names[i])
					simSetObjectInt32Parameter(h,sim_shapeintparam_respondable,1)
					tableShapes[i]=h
				end
				environment=simGroupShapes(tableShapes)
			end
			simAddStatusbarMessage("Done!")
			 */
			vrep.simxAddStatusbarMessage(clientID, "load environment from file...", remoteApi.simx_opmode_oneshot);
			ImportMeshResult imr = importMesh(0, fileToImportPathAndFileName, 0, 0.0001f, 1);
			int numberOfShapes = imr.vertices.length;
			int[] handles = new int[numberOfShapes]; // groups all shapes to make it easier to remove them all 
			for (int i=0; i<numberOfShapes; i++) {
				System.out.println("create shape " + i);
				System.out.println("vertice " + imr.vertices[i][0] + " " +  imr.vertices[i][1] + " " +imr.indices[i].length);
				int handle = createMeshShape(2, (float) (20f*Math.PI/180),imr.vertices[i], imr.indices[i]);
				//int handle = createMeshShape(2, (float) (0.25),imr.vertices[i], imr.indices[i]);
				//setObjectName(handle, imr.names[i]);
				vrep.simxSetObjectIntParameter(clientID, handle, vrep.sim_shapeintparam_respondable, 1, remoteApi.simx_opmode_blocking);
				handles[i] = handle;
			}
			int groupHandle = groupShapes(handles);
			setObjectName(groupHandle, GROUP_NAME);
			
			
			// End of demo
			// ======================================================================================
			try {
				Thread.sleep(25*1000);
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
	 * Remote call to simImportMesh function: Imports a mesh from a file
	 * 
	 * @param fileFormat the fileformat to import from. 0 for OBJ format, 1 for DXF format, 2 for 3DS format, 3 for ASCII STL format and 4 for BINARY STL format
	 * @param pathAndFileName the location of the file to import
	 * @param options bit-coded: bit0 set (1): keep identical vertices, bit1 set (2): keep identical triangles, bit2 set (4): don't correct triangle windings
	 * @param identicalVerticeTolerance the distance from which two distinct vertices will be merged. Bit0 of options should be cleared for this to have an effect
	 * @param scalingFactor the scaling factor to apply to the imported vertices
	 * @return a 3-uplet of vertices (a table to vertice tables), indices (a table to indice tables), names (a table to mesh names extracted from the file)	 * 
	 */
	public ImportMeshResult importMesh(int fileFormat, String pathAndFileName, int options, float identicalVerticeTolerance, int scalingFactor) {
		/*
		 * ASSOCIATED LUA CODE:
	
		importMesh_function=function(inInts,inFloats,inStrings,inBuffer)
		    fileFormat = inInts[1]
		    options = inInts[2] 
			scalingFactor = inInts[3]
		    pathAndFilename = inStrings[1]
		    identicalVerticeTolerance = inFloats[1]
		
		    vertices,indices,reserved,names=simImportMesh(fileFormat,pathAndFilename,options,identicalVerticeTolerance,scalingFactor)
		
		    local tabInts = {}
		    tabInts[1] = table.getn(vertices) -- verticesSizes
		    tabInts[2] = table.getn(indices)  -- indicesSizes
		    local indexTabInts = 3
		    for i=1,table.getn(indices),1 do
		        tabInts[indexTabInts] = table.getn(indices[i]) -- size of indices[i]
		        indexTabInts = indexTabInts + 1
		        for j=1, table.getn(indices[i]), 1 do
		            tabInts[indexTabInts] = indices[i][j] -- indices[i][j]
		            indexTabInts = indexTabInts + 1
		        end
		    end
		
		    local tabFloats = {}
		    local indexTabFloats = 1
		    for i=1,table.getn(vertices),1 do
		        tabFloats[indexTabFloats] = table.getn(vertices[i]) -- size of vertices[i]
		        indexTabFloats = indexTabFloats + 1
		        for j=1, table.getn(vertices[i]), 1 do
		            tabFloats[indexTabFloats] = vertices[i][j] -- vertices[i][j]
		            indexTabFloats = indexTabFloats + 1
		        end
		    end
		
		    return tabInts,tabFloats,names,''
		end
		*/
		FloatWA inFloats=new FloatWA(1);
		inFloats.getArray()[0] = identicalVerticeTolerance;

		StringWA inStrings=new StringWA(1);
		inStrings.getArray()[0] = pathAndFileName; 

		IntWA inInts=new IntWA(3);
		inInts.getArray()[0] = fileFormat;
		inInts.getArray()[1] = options; 
		inInts.getArray()[2] = scalingFactor; 

		FloatWA outFloats=new FloatWA(2); // simFloat*** vertices: an array to vertice arrays
										  // simFloat*** reserved: reserved for future extensions. Keep at NULL.
		IntWA outInts= new IntWA(3); // simInt** verticesSizes: an array indicating the individual vertice array sizes
									 // simInt*** indices: an array to indice arrays
									 // simInt** indicesSizes: an array indicating the individual indice array sizes
		StringWA outStrings=new StringWA(1); // simChar*** names: an array to mesh names extracted from the file
			
		int result=vrep.simxCallScriptFunction(clientID,SCENE_OBJECT_NAME,vrep.sim_scripttype_childscript, "importMesh_function",inInts,inFloats,inStrings,null,outInts,outFloats,outStrings,null,vrep.simx_opmode_blocking);
		if (result==vrep.simx_return_ok) {
			int verticesSizes = outInts.getArray()[0];
			int indicesSizes = outInts.getArray()[1];
			int[][] indices = new int[indicesSizes][];
			int index = 2;
			// from this array cell on, the structure is as follow:
			// sizeOfArray_i, value_1_ofArray_i, ..., value_n_ofArray_i, sizeOfArray_i+1, value_1_ofArray_i+1, ...
			for (int i=0; i<indicesSizes; i++) {
				// indice array i
				int[] tab = new int[outInts.getArray()[index]]; // size of this indice array
				index++;
				for (int j=0; j<tab.length; j++) {
					tab[j] = outInts.getArray()[index];
					index++;
					//System.out.println("indices["+i+"]["+j+"] = " + tab[j]);
				}
				indices[i] = tab;
			}

			float[][] vertices = new float[verticesSizes][];
			index = 0;
			for (int i=0; i<verticesSizes; i++) {
				float[] tab = new float[(int) outFloats.getArray()[index]];
				index++;
				for (int j=0; j<tab.length; j++) {
					tab[j] = outFloats.getArray()[index];
					index++;
					//System.out.println("vertices["+i+"]["+j+"] = " + tab[j]);
				}
				vertices[i] = tab;
			}

			String[] names = outStrings.getArray();
			System.out.println("Remove function call to simImportMesh succeed");
			return (new ImportMeshResult(indices, vertices, names));
			
		} else {
			System.out.println("Remote function call to simImportMesh failed : " + returnCodeDescription(result));
			return null;
		}
	}


	/**
	 * Remote call to simCreateMeshShape: Creates a mesh shape
	 * 
	 * @param options Bit-coded: if bit0 is set (1), backfaces are culled. If bit1 is set (2), edges are visible
	 * @param shadingAngle the shading angle
	 * @param vertices an array of vertices
	 * @param indices an array of indices
	 * @return -1 if operation was not successful, otherwise the handle of the newly created shape
	 */
	protected int createMeshShape(int options, float shadingAngle, float[] vertices, int[] indices) {
		/*
		 * ASSOCIATED LUA CODE:
	
		createMeshShape_function=function(inInts,inFloats,inStrings,inBuffer)
		    options = inInts[1]
		    shadingAngle = inFloats[1]
		    vertices = {}
		    for i=2, table.getn(inFloats), 1 do
		        vertices[i-1] = inFloats[i]
		    end
		    indices = {}
		    for i=2, table.getn(inInts), 1 do
		        indices[i-1] = inInts[i]
		    end
		    handle=simCreateMeshShape(options,shadingAngle,vertices,indices)
		    return {handle},{},{},''
		end
		*/
		FloatWA inFloats=new FloatWA(1+vertices.length);
		inFloats.getArray()[0] = shadingAngle;
		for (int i=0; i<vertices.length; i++)
			inFloats.getArray()[i+1]=vertices[i];

		IntWA inInts=new IntWA(1+indices.length);
		inInts.getArray()[0] = options;
		for (int i=0; i<indices.length; i++)
			inInts.getArray()[i+1]=indices[i];

		IntWA outInts= new IntWA(1);
		
		int result=vrep.simxCallScriptFunction(clientID,SCENE_OBJECT_NAME,vrep.sim_scripttype_childscript, "createMeshShape_function",inInts,inFloats,null,null,outInts,null,null,null,vrep.simx_opmode_blocking);
		if (result==vrep.simx_return_ok) {
			System.out.println("Remove function call to simCreatetMeshShape succeed");
			int handle = outInts.getArray()[0];
			return handle;
		} else {
			System.out.println("Remote function call to simCreateMeshShape failed : " + returnCodeDescription(result));
			return -1;
		}
	}

	
	/**
	 * Remote call to simGroupShapes function: Groups (or merges) several shapes into a compound shape (or simple shape)
	 * 
	 * @param shapeHandles the handles of the shapes you wish to group
	 * @return -1 if operation was not successful. Otherwise the handle of the resulting compound shape.
	 */
	private int groupShapes(int[] shapeHandles) {
		/*
		 * ASSOCIATED LUA CODE:
		
		groupShapes_function=function(inInts,inFloats,inStrings,inBuffer)
		    shapeHandles = inInts
		    handle=simGroupShapes(shapeHandles)
		    return {handle},{},{},''
		end	
		*/
		IntWA inInts=new IntWA(shapeHandles.length);
		for (int i=0; i<shapeHandles.length; i++)
			inInts.getArray()[i]=shapeHandles[i];
		
		IntWA outInts= new IntWA(1);

		int result=vrep.simxCallScriptFunction(clientID,SCENE_OBJECT_NAME,vrep.sim_scripttype_childscript, "groupShapes_function",inInts,null,null,null,outInts,null,null,null,vrep.simx_opmode_blocking);
		if (result==vrep.simx_return_ok) {
			System.out.println("Remove function call to simGroupShapes succeed");
			int handle = outInts.getArray()[0];
			return handle;
		} else {
			System.out.println("Remote function call to simGroupShapes failed : " + returnCodeDescription(result));
			return -1;
		}	
	}
	
	
	
	/**
	 * Remote call to simSetObjectName function: Sets the name of an object based on its handle.
	 * 
	 * @param objectHandle handle of the object
	 * @param objectName name (or alternative name) of the object
	 * @return -1 if operation was not successful
	 */
	private int setObjectName(int objectHandle, String objectName) {
		/*
		 * ASSOCIATED LUA CODE:
		
		setObjectName_function=function(inInts,inFloats,inStrings,inBuffer)
		    objectHandle = inInts[1]
		    objectName = inStrings[1]
		    result=simSetObjectName(objectHandle,objectName)
		    return {result},{},{},''
		end
		 */
		IntWA inInts = new IntWA(1);
		inInts.getArray()[0] = objectHandle;
		
		StringWA inStrings = new StringWA(1);
		inStrings.getArray()[0] = objectName;
		
		IntWA outInts= new IntWA(1);
		
		int result=vrep.simxCallScriptFunction(clientID,SCENE_OBJECT_NAME,vrep.sim_scripttype_childscript, "setObjectName_function",inInts,null,inStrings,null,outInts,null,null,null,vrep.simx_opmode_blocking);
		if (result==vrep.simx_return_ok) {
			System.out.println("Remove function call to simSetObjectName succeed");
			int returnValue = outInts.getArray()[0];
			return returnValue;
		} else {
			System.out.println("Remote function call to simSetObjectName failed : " + returnCodeDescription(result));
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
	
	
	/**
	 * result of the call to simImportMeshResult
	 */
	protected class ImportMeshResult {
		protected int[][] indices;
		protected float[][] vertices;
		protected String[] names;
		public ImportMeshResult(int[][] indices, float[][] vertices, String[] names) {
			assert (indices.length == vertices.length);
			assert (indices.length == names.length);
			this.indices = indices;
			this.vertices = vertices;
			this.names = names;
		}		
	}

}
