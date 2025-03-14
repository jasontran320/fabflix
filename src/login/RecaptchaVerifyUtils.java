package login;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

public class RecaptchaVerifyUtils {
    private static final String SITE_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final String SECRET_KEY = "6LdzLc4qAAAAANJepFOi--y2l3ApoXUHOz6-NRLW";

    public static boolean verify(String gRecaptchaResponse) throws Exception {
        if (gRecaptchaResponse == null || gRecaptchaResponse.isEmpty()) {
            System.out.println("reCAPTCHA token is null or empty");
            return false;
        }

        System.out.println("Making verification request to Google...");
        URL verifyUrl = new URL(SITE_VERIFY_URL);
        HttpsURLConnection conn = (HttpsURLConnection) verifyUrl.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        String postParams = "secret=" + SECRET_KEY + "&response=" + gRecaptchaResponse;
        System.out.println("Sending verification request to Google's servers...");

        conn.setDoOutput(true);
        try (OutputStream outStream = conn.getOutputStream()) {
            outStream.write(postParams.getBytes());
            outStream.flush();
        }

        System.out.println("Reading Google's response...");
        try (InputStream inputStream = conn.getInputStream();
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {

            JsonObject jsonObject = new Gson().fromJson(inputStreamReader, JsonObject.class);
            boolean success = jsonObject.get("success").getAsBoolean();
            System.out.println("Google verification response: " + jsonObject);
            return success;
        } catch (Exception e) {
            System.out.println("Error during reCAPTCHA verification: " + e.getMessage());
            throw e;
        }
    }
}