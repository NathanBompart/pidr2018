import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.osm2world.core.ConversionFacade;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.obj.ObjTarget;

/**
 * @author Virginie Galtier
 */
public class ClientOSM {

	public static void main(String[] arg) {
		new ClientOSM();
	}

	ClientOSM() {

		double east = 6.1803100;
		double south = 48.6930200;
		double west = 6.1844500;
		double north = 48.6945600;

		String osmFilePath = "/tmp/map.osm";

		getMap1(east, south, west, north, osmFilePath);

		String objFilePath = "/tmp/map.obj";

		convertOSM2OBJ(osmFilePath, objFilePath);
	}

	private void getMap1(double east, double south, double west, double north, String osmFilePath) {
		HttpsURLConnection httpsConnectionToOSM = null;
		try {
			URL overpassAPIurl = new URL(
					"https://overpass-api.de/api/map?bbox=" + east + "," + south + "," + west + "," + north);
			httpsConnectionToOSM = (HttpsURLConnection) overpassAPIurl.openConnection();
			httpsConnectionToOSM.setRequestMethod("GET");
			httpsConnectionToOSM.setDoOutput(true);
			httpsConnectionToOSM.connect();
		} catch (Exception e) {
			System.err.println("Couldn't connect to overpass service: " + e.getMessage());
			System.exit(1);
			// e.printStackTrace();
		}
		System.out.println("connection to OSM overpass: successful");
		StringBuilder response = null;
		try {
			// read map response
			BufferedReader bufferedReaderFromOSM = new BufferedReader(
					new InputStreamReader(httpsConnectionToOSM.getInputStream()));
			String line = "";
			response = new StringBuilder();
			while ((line = bufferedReaderFromOSM.readLine()) != null) {
				response.append(line + "\n");
			}
			// System.out.print(response + "\n");
			bufferedReaderFromOSM.close();
		} catch (Exception e) {
			System.err.println("Couldn't read response: " + e.getMessage());
			System.exit(1);
			// e.printStackTrace();
		}
		System.out.println("map received");

		httpsConnectionToOSM.disconnect();

		// write to file
		try {
			Files.write(Paths.get(osmFilePath), response.toString().getBytes());
		} catch (IOException e) {
			System.err.println("Couldn't write response to map.osm file: " + e.getMessage());
			System.exit(1);
			// e.printStackTrace();
		}
		System.out.println("map written to " + osmFilePath);
	}

	private void getMap2(double east, double south, double west, double north, String osmFilePath) {
		String request = "<osm-script>\n" + " <bbox-query s=\" " + south + "\" w=\"" + west + "\" n=\"" + north
				+ "\" e=\"" + east + "\"/>\n" + "  <print/>\n" + "</osm-script>";

		HttpURLConnection httpConnectionToOSM = null;

		try {
			// connect to overpass OSM reader service
			URL overpassAPIurl = new URL("http://overpass-api.de/api/interpreter");

			httpConnectionToOSM = (HttpURLConnection) overpassAPIurl.openConnection();
			httpConnectionToOSM.setRequestMethod("POST");
			httpConnectionToOSM.setDoOutput(true);
			httpConnectionToOSM.connect();
		} catch (Exception e) {
			System.err.println("Coudln't connect to overpass service: " + e.getMessage());
			System.exit(1);
			// e.printStackTrace();
		}

		try {
			// send map request
			OutputStreamWriter writerToOSM = new OutputStreamWriter(httpConnectionToOSM.getOutputStream());
			writerToOSM.write(request);
			writerToOSM.flush();
			writerToOSM.close();
		} catch (Exception e) {
			System.err.println("Coudln't send map request: " + e.getMessage());
			System.exit(1);
			// e.printStackTrace();
		}

		StringBuilder response = null;
		try {
			// read map response
			BufferedReader bufferedReaderFromOSM = new BufferedReader(
					new InputStreamReader(httpConnectionToOSM.getInputStream()));
			String line = "";
			response = new StringBuilder();
			while ((line = bufferedReaderFromOSM.readLine()) != null) {
				response.append(line + "\n");
			}
			System.out.print(response + "\n");
			bufferedReaderFromOSM.close();
		} catch (Exception e) {
			System.err.println("Coudln't read response: " + e.getMessage());
			System.exit(1);
			// e.printStackTrace();
		}

		httpConnectionToOSM.disconnect();

		// write to file
		try {
			Files.write(Paths.get(osmFilePath), response.toString().getBytes());
		} catch (IOException e) {
			System.err.println("Couldn't write response to map.osm file: " + e.getMessage());
			System.exit(1);
			// e.printStackTrace();
		}
	}

	private void convertOSM2OBJ(String osmFilePath, String objFilePath) {
		// uses OSM2World
		try {
			ConversionFacade conversionFacade = new ConversionFacade();

			File objFile = new File(objFilePath);
			PrintStream outOBJ = new PrintStream(objFile);
			String mtlFilePath = "/tmp/map.mtl";
			File mtlFile = new File(mtlFilePath);
			PrintStream outMTL = new PrintStream(mtlFile);

			List<Target<?>> targets = new ArrayList<>();
			targets.add(new ObjTarget(outOBJ, outMTL)); // output to OBJ

			File osmFile = new File(osmFilePath);

			conversionFacade.createRepresentations(osmFile, null, null, targets);
		} catch (Exception e) {
			System.err.println("Couldn't convert to OBJ file: " + e.getMessage());
			System.exit(1);
			// e.printStackTrace();
		}
		System.out.println(osmFilePath + " converted to " + objFilePath);
	}
}
