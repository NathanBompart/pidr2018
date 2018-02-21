import coppelia.FloatW;
import coppelia.FloatWA;
import coppelia.IntW;
import coppelia.IntWA;
import coppelia.StringWA;
import coppelia.remoteApi;
//import BubbleRob;

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
	protected final String SCENE_OBJECT_NAME = "Dummy";
	protected final String BUBBLEROB = "bubbleRob";
	protected String fileToImportPathAndFileName = "/tmp/placeStan.obj";
	protected BubbleRob bubble;
	//protected String fileToImportPathAndFileName = "/home/galtier/VREP/Environments/envSimple.obj";
	//protected String fileToImportPathAndFileName = "/home/galtier/VREP/Environments/placeStan.obj";
	
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
			int returnCode = vrep.simxGetObjectHandle(clientID, GROUP_NAME, handle_groupe, remoteApi.simx_opmode_blocking);
			if (returnCode == remoteApi.simx_return_ok) {
				System.out.println(GROUP_NAME + " already exists, remove it...");
				vrep.simxAddStatusbarMessage(clientID, "remove existing environment", remoteApi.simx_opmode_oneshot);
				returnCode = vrep.simxRemoveObject(clientID, handle_groupe.getValue(), remoteApi.simx_opmode_blocking);
				System.out.println(returnCodeDescription(returnCode));
			}

			IntW handle_bubbleRob = new IntW(0);
			returnCode = vrep.simxGetObjectHandle(clientID, BUBBLEROB, handle_bubbleRob, remoteApi.simx_opmode_blocking);
			if (returnCode != remoteApi.simx_return_ok) {
				System.out.println(BUBBLEROB + " doesn't exists, add it...");
				System.out.println(returnCodeDescription(returnCode));
			}

			bubble = new BubbleRob(vrep,clientID,handle_bubbleRob);


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
			ImportMeshResult imr = importMesh(0, fileToImportPathAndFileName, 0, 0.0001f, 1f);
			int numberOfShapes = imr.vertices.length;
			int[] handles = new int[numberOfShapes]; // groups all shapes to make it easier to remove them all 
			for (int i=0; i<numberOfShapes; i++) {
				System.out.println("create shape " + i);
				int handle = createMeshShape(2, (float) (20f*Math.PI/180),imr.vertices[i], imr.indices[i]);
				//setObjectName(handle, imr.names[i]);
				vrep.simxSetObjectIntParameter(clientID, handle, remoteApi.sim_shapeintparam_respondable, 1, remoteApi.simx_opmode_blocking);
				handles[i] = handle;
			}
			int groupHandle = groupShapes(handles);
			setObjectName(groupHandle, GROUP_NAME);


			// Rotate mesh
			// =====================================================================================
			FloatWA eulerAngles = new FloatWA(3);
			eulerAngles.getArray()[0] = (float)Math.PI/2;
			eulerAngles.getArray()[1] = 0f;
			eulerAngles.getArray()[2] = -1*(float)Math.PI/2;
			vrep.simxSetObjectOrientation(clientID, groupHandle, -1, eulerAngles, remoteApi.simx_opmode_blocking);


			// align bottom
			// /!\ after rotation z<->x
			FloatW objbbox_min_x = new FloatW(0f);
			vrep.simxGetObjectFloatParameter(clientID, groupHandle, remoteApi.sim_objfloatparam_objbbox_min_x, objbbox_min_x, remoteApi.simx_opmode_blocking);

			FloatWA position = new FloatWA(3);
			position.getArray()[0] = 0;
			position.getArray()[1] = 0;
			position.getArray()[2] = -1*objbbox_min_x.getValue();
			vrep.simxSetObjectPosition(clientID, groupHandle, -1, position, remoteApi.simx_opmode_blocking);

			vrep.simxGetObjectPosition(clientID,groupHandle,-1,position,remoteApi.simx_opmode_blocking);
			position.getArray()[0] = (float)3.2535e2;
			position.getArray()[1] = (float)-1.4142e3;
			returnCode = vrep.simxSetObjectPosition(clientID,groupHandle,-1,position,remoteApi.simx_opmode_blocking);

			if(returnCode != remoteApi.simx_return_ok){
				System.out.println("erreur translation");
			}

			// End of demo
			// ======================================================================================
			try {
				Thread.sleep(2*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			bubble.setVelocity(50f);
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
	public ImportMeshResult importMesh(int fileFormat, String pathAndFileName, int options, float identicalVerticeTolerance, float scalingFactor) {
		/*
		 * ASSOCIATED LUA CODE:

		importMesh_function=function(inInts,inFloats,inStrings,inBuffer)
		    fileFormat = inInts[1]
		    pathAndFilename = inStrings[1]
		    options = inInts[2] 
		    identicalVerticeTolerance = inFloats[1]
		    scalingFactor = inFloats[2]

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
		FloatWA inFloats=new FloatWA(2);
		inFloats.getArray()[0] = identicalVerticeTolerance;
		inFloats.getArray()[1] = scalingFactor;

		StringWA inStrings=new StringWA(1);
		inStrings.getArray()[0] = pathAndFileName; 

		IntWA inInts=new IntWA(2);
		inInts.getArray()[0] = fileFormat;
		inInts.getArray()[1] = options; 

		FloatWA outFloats=new FloatWA(2); // simFloat*** vertices: an array to vertice arrays
		// simFloat*** reserved: reserved for future extensions. Keep at NULL.
		IntWA outInts= new IntWA(3); // simInt** verticesSizes: an array indicating the individual vertice array sizes
		// simInt*** indices: an array to indice arrays
		// simInt** indicesSizes: an array indicating the individual indice array sizes
		StringWA outStrings=new StringWA(1); // simChar*** names: an array to mesh names extracted from the file

		int result=vrep.simxCallScriptFunction(clientID,SCENE_OBJECT_NAME,remoteApi.sim_scripttype_childscript, "importMesh_function",inInts,inFloats,inStrings,null,outInts,outFloats,outStrings,null,remoteApi.simx_opmode_blocking);
		if (result==remoteApi.simx_return_ok) {
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

		int result=vrep.simxCallScriptFunction(clientID,SCENE_OBJECT_NAME,remoteApi.sim_scripttype_childscript, "createMeshShape_function",inInts,inFloats,null,null,outInts,null,null,null,remoteApi.simx_opmode_blocking);
		if (result==remoteApi.simx_return_ok) {
			System.out.println("Remote function call to simCreatetMeshShape succeed");
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

		int result=vrep.simxCallScriptFunction(clientID,SCENE_OBJECT_NAME,remoteApi.sim_scripttype_childscript, "groupShapes_function",inInts,null,null,null,outInts,null,null,null,remoteApi.simx_opmode_blocking);
		if (result==remoteApi.simx_return_ok) {
			System.out.println("Remote function call to simGroupShapes succeed");
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

		int result=vrep.simxCallScriptFunction(clientID,SCENE_OBJECT_NAME,remoteApi.sim_scripttype_childscript, "setObjectName_function",inInts,null,inStrings,null,outInts,null,null,null,remoteApi.simx_opmode_blocking);
		if (result==remoteApi.simx_return_ok) {
			System.out.println("Remote function call to simSetObjectName succeed");
			int returnValue = outInts.getArray()[0];
			return returnValue;
		} else {
			System.out.println("Remote function call to simSetObjectName failed : " + returnCodeDescription(result));
			return -1;
		}	


	}



	/**
	 * Remote call to simGetObjectMatrix function: Retrieves the transformation matrix of an object.
	 * 
	 * @param objectHandle handle of the object
	 * @param relativeToObjectHandle indicates relative to which reference frame we want the matrix. Specify -1 to retrieve the absolute transformation matrix, sim.handle_parent to retrieve the transformation matrix relative to the object's parent, or an object handle relative to whose reference frame we want the transformation matrix.
	 * @return table of 12 numbers (the last row of the 4x4 matrix (0,0,0,1) is not returned), or nil in case of an error.
	 */
	private float[] getObjectMatrix(int objectHandle, int relativeToObjectHandle) {
		/*
		 * ASSOCIATED LUA CODE:

		getObjectMatrix_function=function(inInts,inFloats,inStrings,inBuffer)
		    objectHandle = inInts[1]
		    relativeToObjectHandle = inInts[2]
		    matrix=simgetObjectMatrix(objectHandle,relativeToObjectHandle)
		    return {},matrix,{},''
		end
		 */
		IntWA inInts = new IntWA(2);
		inInts.getArray()[0] = objectHandle;
		inInts.getArray()[1] = relativeToObjectHandle;

		FloatWA outFloats = new FloatWA(12);

		int result=vrep.simxCallScriptFunction(clientID, SCENE_OBJECT_NAME, remoteApi.sim_scripttype_childscript, "getObjectMatrix_function", inInts, null, null, null, null, outFloats, null, null, remoteApi.simx_opmode_blocking);
		if (result==remoteApi.simx_return_ok) {
			System.out.println("Remote function call to simGetObjectMatrix succeed");
			return outFloats.getArray();
		} else {
			System.out.println("Remote function call to simGetObjectMatrix failed : " + returnCodeDescription(result));
			return null;
		}	
	}

	/**
	 * Remote call to simSetObjectMatrix function: Sets the transformation matrix of an object. Dynamically simulated objects will implicitely be reset before the command is applied (i.e. similar to calling sim.resetDynamicObject just before).
	 * 
	 * @param objectHandle handle of the object
	 * @param relativeToObjectHandle indicates relative to which reference frame the matrix is specified. Specify -1 to set the absolute transformation matrix, sim_handle_parent to set the transformation matrix relative to the object's parent, or an object handle relative to whose reference frame the transformation matrix is specified.
	 * @param matrix pointer to 12 simFloat values (the last row of the 4x4 matrix (0,0,0,1) is not needed)
The x-axis of the orientation component is (matrix[0],matrix[4],matrix[8])
The y-axis of the orientation component is (matrix[1],matrix[5],matrix[9])
The z-axis of the orientation component is (matrix[2],matrix[6],matrix[10])
The translation component is (matrix[3],matrix[7],matrix[11])
	 * @return -1 if operation was not successful. In a future release, a more differentiated return value might be available
	 */
	private int setObjectMatrix(int objectHandle, int relativeToObjectHandle, float[] matrix) {
		IntWA inInts = new IntWA(2);
		inInts.getArray()[0] = objectHandle;
		inInts.getArray()[1] = relativeToObjectHandle;
		FloatWA inFloats = new FloatWA(12);
		for (int i=0; i<12; i++) {
			inFloats.getArray()[i] = matrix[i];
		}

		IntWA outInts= new IntWA(1);

		int result=vrep.simxCallScriptFunction(clientID, SCENE_OBJECT_NAME, remoteApi.sim_scripttype_childscript, "setObjectMatrix_function", inInts, inFloats, null, null, outInts, null, null, null, remoteApi.simx_opmode_blocking);
		if (result==remoteApi.simx_return_ok) {
			System.out.println("Remove function call to simSetObjectMatrix succeed");
			return outInts.getArray()[0];
		} else {
			System.out.println("Remote function call to simSetObjectMatrix failed : " + returnCodeDescription(result));
			return -1;
		}	

	}


	/**
	 * Remote call to simMultiplyVector function: Multiplies a vector with a transformation matrix (v=m*v).
	 * 
	 * @param matrix the transformation matrix (a table containing 12 values (the last row (0,0,0,1) is not required))
	 * @param vector the original vector (a table containing 3 values (the last element (1) of the homogeneous coordinates is not required)
	 * @return the result vector (a table containing 3 values (the last element (1) of the homogeneous coordinates is omitted))
	 */
	private float[] multiplyVector(float[] matrix, float[] vector) {
		/*
		 * ASSOCIATED LUA CODE:

		multiplyVector_function=function(inInts,inFloats,inStrings,inBuffer)
		    matrix = {inFloats[1], inFloats[2], inFloats[3], inFloats[4], inFloats[5], inFloats[6], inFloats[7], inFloats[8], inFloats[9], inFloats[10], inFloats[11], inFloats[12]}
		    vector = {inFloats[13], inFloats[14], inFloats[15]}
		    resultVector = simMultiplyVector(matrix, vector)
		    return {},resultVector,{},''
		end
		 */
		FloatWA inFloats = new FloatWA(12+3);
		for (int i=0; i<12; i++) {
			inFloats.getArray()[i] = matrix[i];
		}
		for (int i=0; i<3; i++) {
			inFloats.getArray()[12+i] = vector[i];
		}

		FloatWA outFloats = new FloatWA(3);

		int result=vrep.simxCallScriptFunction(clientID, SCENE_OBJECT_NAME, remoteApi.sim_scripttype_childscript, "multiplyVector_function", null, inFloats, null, null, null, outFloats, null, null, remoteApi.simx_opmode_blocking);
		if (result==remoteApi.simx_return_ok) {
			System.out.println("Remote function call to multiplyVector succeed");
			return outFloats.getArray();
		} else {
			System.out.println("Remote function call to multiplyVector failed : " + returnCodeDescription(result));
			return null;
		}	
	}

	/**
	 * Remote call to simMultiplyMatrices function: Multiplies two transformation matrices.
	 * 
	 * @param matrix1 the first input matrix
	 * @param matrix2 the second input matrix
	 * @return the output matrix (the result of the multiplication: matrixIn1*matrixIn2).
A transformation matrix contains 12 values (the last row (0,0,0,1) is omitted):
The x-axis of the orientation component is (matrix[0],matrix[4],matrix[8])
The y-axis of the orientation component is (matrix[1],matrix[5],matrix[9])
The z-axis of the orientation component is (matrix[2],matrix[6],matrix[10])
The position component is (matrix[3],matrix[7],matrix[11])
	 */
	private float[] multiplyMatrices(float[] matrix1, float[] matrix2) {
		/*
		 * ASSOCIATED LUA CODE:

		multiplyMatrices_function=function(inInts,inFloats,inStrings,inBuffer)
		    matrix1 = {inFloats[1], inFloats[2], inFloats[3], inFloats[4], inFloats[5], inFloats[6], inFloats[7], inFloats[8], inFloats[9], inFloats[10], inFloats[11], inFloats[12]}
		    matrix2 = {inFloats[13], inFloats[14], inFloats[15], inFloats[16], inFloats[17], inFloats[18], inFloats[19], inFloats[20], inFloats[21], inFloats[22], inFloats[23], inFloats[24]}
		    resultMatrix = simMultiplyMatrices(matrix1, matrix2)
		    return {},resultMatrix,{},''
		end
		 */
		FloatWA inFloats = new FloatWA(12+12);
		for (int i=0; i<12; i++) {
			inFloats.getArray()[i] = matrix1[i];
		}
		for (int i=0; i<12; i++) {
			inFloats.getArray()[12+i] = matrix2[i];
		}

		FloatWA outFloats = new FloatWA(12);

		int result=vrep.simxCallScriptFunction(clientID, SCENE_OBJECT_NAME, remoteApi.sim_scripttype_childscript, "multiplyMatrices_function", null, inFloats, null, null, null, outFloats, null, null, remoteApi.simx_opmode_blocking);
		if (result==remoteApi.simx_return_ok) {
			System.out.println("Remote function call to multiplyMatrices succeed");
			return outFloats.getArray();
		} else {
			System.out.println("Remote function call to multiplyMatrices failed : " + returnCodeDescription(result));
			return null;
		}	
	}



	/**
	 * Remote call to simBuildMatrix function: Builds a transformation matrix based on a position vector and Euler angles.
	 * 
	 * @param position pointer to 3 simFloat values representing the position component
	 * @param eulerAngles pointer to 3 simFloat values representing the angular component
	 * @return table containing the transformation matrix (except for the last row), or nil in case of an error. Table values in Lua are indexed from 1, not 0!
	 */
	private float[] buildMatrix(float[] position, float[] eulerAngles) {
		/*
		 * ASSOCIATED LUA CODE:

		buildMatrix_function==function(inInts,inFloats,inStrings,inBuffer)
		    position = {intFloats[1], inFloats[2], inFloats[3]}
		    eulerAngles = {intFloats[4], inFloats[5], inFloats[6]}
		    resultMatrix = simBuildMatrix(position, eulerAngles)
		    return {},resultMatrix,{},''
		end
		 */
		FloatWA inFloats = new FloatWA(3+3);
		for (int i=0; i<3; i++) {
			inFloats.getArray()[i] = position[i];
			System.out.println("position["+i+"] = " + inFloats.getArray()[i]);
		}
		for (int i=0; i<3; i++) {
			inFloats.getArray()[3+i] = eulerAngles[i];
			System.out.println("eulerAngles["+i+"] = " + inFloats.getArray()[3+i]);
		}


		FloatWA outFloats = new FloatWA(12);

		int result=vrep.simxCallScriptFunction(clientID, SCENE_OBJECT_NAME, remoteApi.sim_scripttype_childscript, "buildMatrix_function", null, inFloats, null, null, null, outFloats, null, null, remoteApi.simx_opmode_blocking);
		if (result==remoteApi.simx_return_ok) {
			System.out.println("Remote function call to buildMatrix succeed");
			return outFloats.getArray();
		} else {
			System.out.println("Remote function call to buildMatrix failed : " + returnCodeDescription(result));
			return null;
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
