package com.ernichechelski.remoteconfigmanager;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@SpringBootTest
class RemoteconfigmanagerApplicationTests {

	private final static String PROJECT_ID = "quoteoftheday-7252d";
	private final static String BASE_URL = "https://firebaseremoteconfig.googleapis.com";
	private final static String REMOTE_CONFIG_ENDPOINT = "/v1/projects/" + PROJECT_ID + "/remoteConfig";
	private final static String[] SCOPES = { "https://www.googleapis.com/auth/firebase.remoteconfig" };

	@Test
	void uploadConfig() {
		try {
			String etag = getTemplateETag();
			publishTemplate(etag);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	void contextLoads() {
		try {
			getTemplate();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Retrieve a valid access token that can be use to authorize requests to the Remote Config REST
	 * API.
	 *
	 * @return Access token.
	 * @throws IOException
	 */
	// [START retrieve_access_token]
	private static String getAccessToken() throws IOException {
		GoogleCredential googleCredential = GoogleCredential
				.fromStream(new FileInputStream("quoteoftheday-7252d-firebase-adminsdk-qe717-fe620dddde.json"))
				.createScoped(Arrays.asList(SCOPES));
		googleCredential.refreshToken();
		return googleCredential.getAccessToken();
	}
	// [END retrieve_access_token]

	/**
	 * Get current Firebase Remote Config template from server and store it locally.
	 *
	 * @throws IOException
	 */
	private static void getTemplate() throws IOException {
		HttpURLConnection httpURLConnection = getCommonConnection(BASE_URL + REMOTE_CONFIG_ENDPOINT);
		httpURLConnection.setRequestMethod("GET");
		httpURLConnection.setRequestProperty("Accept-Encoding", "gzip");

		int code = httpURLConnection.getResponseCode();
		if (code == 200) {
			InputStream inputStream = new GZIPInputStream(httpURLConnection.getInputStream());
			String response = inputstreamToString(inputStream);

			JsonParser jsonParser = new JsonParser();
			JsonElement jsonElement = jsonParser.parse(response);

			Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
			String jsonStr = gson.toJson(jsonElement);

			File file = new File("config.json");
			PrintWriter printWriter = new PrintWriter(new FileWriter(file));
			printWriter.print(jsonStr);
			printWriter.flush();
			printWriter.close();

			System.out.println("Template retrieved and has been written to config.json");

			// Print ETag
			String etag = httpURLConnection.getHeaderField("ETag");
			System.out.println("ETag from server: " + etag);
		} else {
			System.out.println(inputstreamToString(httpURLConnection.getErrorStream()));
		}

	}

	/**
	 * Get current Firebase Remote Config template from server and store it locally.
	 *
	 * @throws IOException
	 */
	private static String getTemplateETag() throws IOException {
		HttpURLConnection httpURLConnection = getCommonConnection(BASE_URL + REMOTE_CONFIG_ENDPOINT);
		httpURLConnection.setRequestMethod("GET");
		httpURLConnection.setRequestProperty("Accept-Encoding", "gzip");
		int code = httpURLConnection.getResponseCode();
		if (code == 200) {
			String etag = httpURLConnection.getHeaderField("ETag");
			System.out.println("ETag from server: " + etag);
			return etag;
		} else {
			System.out.println(inputstreamToString(httpURLConnection.getErrorStream()));
			throw new IOException();
		}
	}

	/**
	 * Print the last 5 available Firebase Remote Config template metadata from the server.
	 *
	 * @throws IOException
	 */
	private static void getVersions() throws IOException {
		HttpURLConnection httpURLConnection = getCommonConnection(BASE_URL + REMOTE_CONFIG_ENDPOINT
				+ ":listVersions?pageSize=5");
		httpURLConnection.setRequestMethod("GET");

		int code = httpURLConnection.getResponseCode();
		if (code == 200) {
			String versions = inputstreamToPrettyString(httpURLConnection.getInputStream());

			System.out.println("Versions:");
			System.out.println(versions);
		} else {
			System.out.println(inputstreamToString(httpURLConnection.getErrorStream()));
		}
	}

	/**
	 * Roll back to an available version of Firebase Remote Config template.
	 *
	 * @param version The version to roll back to.
	 *
	 * @throws IOException
	 */
	private static void rollback(int version) throws IOException {
		HttpURLConnection httpURLConnection = getCommonConnection(BASE_URL + REMOTE_CONFIG_ENDPOINT
				+ ":rollback");
		httpURLConnection.setDoOutput(true);
		httpURLConnection.setRequestMethod("POST");
		httpURLConnection.setRequestProperty("Accept-Encoding", "gzip");

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("version_number", version);

		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpURLConnection.getOutputStream());
		outputStreamWriter.write(jsonObject.toString());
		outputStreamWriter.flush();
		outputStreamWriter.close();

		int code = httpURLConnection.getResponseCode();
		if (code == 200) {
			System.out.println("Rolled back to: "  + version);
			InputStream inputStream = new GZIPInputStream(httpURLConnection.getInputStream());
			System.out.println(inputstreamToPrettyString(inputStream));

			// Print ETag
			String etag = httpURLConnection.getHeaderField("ETag");
			System.out.println("ETag from server: " + etag);
		} else {
			System.out.println("Error:");
			InputStream inputStream = new GZIPInputStream(httpURLConnection.getErrorStream());
			System.out.println(inputstreamToString(inputStream));
		}
	}

	/**
	 * Publish local template to Firebase server.
	 *
	 * @throws IOException
	 */
	private static void publishTemplate(String etag) throws IOException {
		if (etag.equals("*")) {
			Scanner scanner = new Scanner(System.in);
			System.out.println("Are you sure you would like to force replace the template? Yes (y), No (n)");
			String answer = scanner.nextLine();
			if (!answer.equalsIgnoreCase("y")) {
				System.out.println("Publish canceled.");
				return;
			}
		}

		System.out.println("Publishing template...");
		HttpURLConnection httpURLConnection = getCommonConnection(BASE_URL + REMOTE_CONFIG_ENDPOINT);
		httpURLConnection.setDoOutput(true);
		httpURLConnection.setRequestMethod("PUT");
		httpURLConnection.setRequestProperty("If-Match", etag);
		httpURLConnection.setRequestProperty("Content-Encoding", "gzip");

		String configStr = readConfig();

		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(httpURLConnection.getOutputStream());
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(gzipOutputStream);
		outputStreamWriter.write(configStr);
		outputStreamWriter.flush();
		outputStreamWriter.close();

		int code = httpURLConnection.getResponseCode();
		if (code == 200) {
			System.out.println("Template has been published.");
		} else {
			System.out.println(inputstreamToString(httpURLConnection.getErrorStream()));
		}
	}

	/**
	 * Read the Firebase Remote Config template from config.json file.
	 *
	 * @return String with contents of config.json file.
	 * @throws FileNotFoundException
	 */
	private static String readConfig() throws FileNotFoundException {
		File file = new File("config.json");
		Scanner scanner = new Scanner(file);

		StringBuilder stringBuilder = new StringBuilder();
		while (scanner.hasNext()) {
			stringBuilder.append(scanner.nextLine());
		}
		return stringBuilder.toString();
	}

	/**
	 * Format content from an InputStream as pretty JSON.
	 *
	 * @param inputStream Content to be formatted.
	 * @return Pretty JSON formatted string.
	 *
	 * @throws IOException
	 */
	private static String inputstreamToPrettyString(InputStream inputStream) throws IOException {
		String response = inputstreamToString(inputStream);

		JsonParser jsonParser = new JsonParser();
		JsonElement jsonElement = jsonParser.parse(response);

		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		String jsonStr = gson.toJson(jsonElement);

		return jsonStr;
	}

	/**
	 * Read contents of InputStream into String.
	 *
	 * @param inputStream InputStream to read.
	 * @return String containing contents of InputStream.
	 * @throws IOException
	 */
	private static String inputstreamToString(InputStream inputStream) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		Scanner scanner = new Scanner(inputStream);
		while (scanner.hasNext()) {
			stringBuilder.append(scanner.nextLine());
		}
		return stringBuilder.toString();
	}

	/**
	 * Create HttpURLConnection that can be used for both retrieving and publishing.
	 *
	 * @return Base HttpURLConnection.
	 * @throws IOException
	 */
	private static HttpURLConnection getCommonConnection(String endpoint) throws IOException {
		URL url = new URL(endpoint);
		HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
		httpURLConnection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
		httpURLConnection.setRequestProperty("Content-Type", "application/json; UTF-8");
		return httpURLConnection;
	}

}
