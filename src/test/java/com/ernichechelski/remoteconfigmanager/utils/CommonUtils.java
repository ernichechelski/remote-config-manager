package com.ernichechelski.remoteconfigmanager.utils;

import com.ernichechelski.remoteconfigmanager.Config;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gson.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * This class contains all utility methods.
 * All they are static because running tests contains all triggering logic.
 */
public class CommonUtils {

    // By this property entire config is read.
    private final static Config config = new Config();

    private final static String BASE_URL = "https://firebaseremoteconfig.googleapis.com";

    private final static String[] SCOPES = { "https://www.googleapis.com/auth/firebase.remoteconfig" };

    private static String getEndpoint() throws IOException {
        String projectId;
        try {
            projectId = getProjectId();
        } catch (IOException e) {
            projectId = getProjectIdFromEnvironment();
        }
        return "/v1/projects/" + projectId + "/remoteConfig";
    }

    private static String getAccessToken() throws IOException {
        GoogleCredential googleCredential = GoogleCredential
                .fromStream(new FileInputStream("secrets.properties"))
                .createScoped(Arrays.asList(SCOPES));
        googleCredential.refreshToken();
        return googleCredential.getAccessToken();
    }

    private static String getAccessTokenFromEnvironment() throws IOException {
        GoogleCredential googleCredential = GoogleCredential
                .fromStream(new FileInputStream("secrets.json"))
                .createScoped(Arrays.asList(SCOPES));
        googleCredential.refreshToken();
        return googleCredential.getAccessToken();
    }

    private static String getProjectId() throws IOException {
        File file = new File("projectId.properties");
        Scanner scanner = new Scanner(file);

        StringBuilder stringBuilder = new StringBuilder();
        while (scanner.hasNext()) {
            stringBuilder.append(scanner.nextLine());
        }
        return stringBuilder.toString();
    }

    private static String getProjectIdFromEnvironment() throws IOException {
        File file = new File("projectId.json");
        Scanner scanner = new Scanner(file);

        StringBuilder stringBuilder = new StringBuilder();
        while (scanner.hasNext()) {
            stringBuilder.append(scanner.nextLine());
        }
        return stringBuilder.toString();
    }

    /**
     * Get current Firebase Remote Config template from server and store it locally.
     *
     * @throws IOException
     */
    public static void getTemplate(String filename) throws IOException {
        HttpURLConnection httpURLConnection = getCommonConnection(BASE_URL + getEndpoint());
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

            File file = new File(filename);
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
    public static String getTemplateETag() throws IOException {
        HttpURLConnection httpURLConnection = getCommonConnection(BASE_URL + getEndpoint());
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
        HttpURLConnection httpURLConnection = getCommonConnection(BASE_URL + getEndpoint()
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
        HttpURLConnection httpURLConnection = getCommonConnection(BASE_URL + getEndpoint()
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
    public static void publishTemplate(String etag) throws IOException {
        publishTemplate(etag, null);
    }

    private static void publishTemplate(String etag, Config config) throws IOException {
        System.out.println("Publishing template...");
        HttpURLConnection httpURLConnection = getCommonConnection(BASE_URL + getEndpoint());
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestMethod("PUT");
        httpURLConnection.setRequestProperty("If-Match", etag);
        httpURLConnection.setRequestProperty("Content-Encoding", "gzip");

        String configStr = createConfig();

        if (!isJSONValid(configStr)) {
            throw new IOException("Config is not valid JSON!");
        }

        performRequest(httpURLConnection, configStr);

        int code = httpURLConnection.getResponseCode();
        if (code == 200) {
            System.out.println("Template has been published.");
        } else {
            System.out.println(inputstreamToString(httpURLConnection.getErrorStream()));
        }
    }

    private static void performRequest(HttpURLConnection httpURLConnection, String body) throws IOException {
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(httpURLConnection.getOutputStream());
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(gzipOutputStream);
        outputStreamWriter.write(body);
        outputStreamWriter.flush();
        outputStreamWriter.close();
    }

    /**
     * Read the Firebase Remote Config template from config.json file.
     *
     * @return String with contents of config.json file.
     * @throws FileNotFoundException
     */
    public static String readConfig() throws FileNotFoundException {
        File file = new File("config.json");
        Scanner scanner = new Scanner(file);

        StringBuilder stringBuilder = new StringBuilder();
        while (scanner.hasNext()) {
            stringBuilder.append(scanner.nextLine());
        }
        return stringBuilder.toString();
    }

    /**
     * Creates the Firebase Remote Config template from Config class.
     */
    public static String createConfig()  {
       return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(new ConfigWrapper(config));
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
        try {
            httpURLConnection.setRequestProperty("Authorization", "Bearer " + getAccessTokenFromEnvironment());
        } catch (IOException e) {
            httpURLConnection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        }
        httpURLConnection.setRequestProperty("Content-Type", "application/json; UTF-8");
        return httpURLConnection;
    }

    public static boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }
}
