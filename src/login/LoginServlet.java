package login;

import com.google.gson.JsonObject;
import common.JwtUtil;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jasypt.util.password.StrongPasswordEncryptor;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/MySQLReadOnly");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        JsonObject responseJsonObject = new JsonObject();

        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
        System.out.println("Received reCAPTCHA token length: " +
                (gRecaptchaResponse != null ? gRecaptchaResponse.length() : "null"));

        try {
            // Verify reCAPTCHA first
            System.out.println("Starting reCAPTCHA verification...");
            boolean verificationSuccess = RecaptchaVerifyUtils.verify(gRecaptchaResponse);
            System.out.println("reCAPTCHA verification result: " + verificationSuccess);

            if (!verificationSuccess) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "reCAPTCHA verification failed");
                response.getWriter().write(responseJsonObject.toString());
                return;
            }

            String email = request.getParameter("username");
            String password = request.getParameter("password");
            System.out.println("Checking credentials for email: " + email);

            try (Connection conn = dataSource.getConnection()) {
                String emailQuery = "SELECT id, password FROM customers WHERE email = ?";
                try (PreparedStatement emailStatement = conn.prepareStatement(emailQuery)) {
                    emailStatement.setString(1, email);
                    System.out.println("Executing SQL query...");
                    try (ResultSet emailRs = emailStatement.executeQuery()) {
                        if (!emailRs.next()) {
                            System.out.println("No user found with email: " + email);
                            responseJsonObject.addProperty("status", "fail");
                            responseJsonObject.addProperty("message", "Incorrect email address");
                            responseJsonObject.addProperty("field", "email");
                        } else {
                            String encryptedPassword = emailRs.getString("password");
                            String userId = emailRs.getString("id");
                            System.out.println("Found user with ID: " + userId);

                            // Use StrongPasswordEncryptor to verify the password
                            boolean success = new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);

                            if (!success) {
                                System.out.println("Password mismatch for user: " + email);
                                responseJsonObject.addProperty("status", "fail");
                                responseJsonObject.addProperty("message", "Incorrect password");
                                responseJsonObject.addProperty("field", "password");
                            } else {
                                System.out.println("Login successful for user: " + email);

                                // Create JWT with user information
                                Map<String, Object> claims = new HashMap<>();
                                claims.put("userId", userId);
                                claims.put("isEmployee", false);

                                // Add login timestamp
                                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                                claims.put("loginTime", dateFormat.format(new Date()));

                                // Generate JWT and set as cookie
                                String token = JwtUtil.generateToken(email, claims);
                                JwtUtil.updateJwtCookie(request, response, token);

                                responseJsonObject.addProperty("status", "success");
                                responseJsonObject.addProperty("message", "Login successful");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error during login process: " + e.getMessage());
            e.printStackTrace();

            response.setStatus(500);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Server error: " + e.getMessage());
        }

        response.getWriter().write(responseJsonObject.toString());
    }
}