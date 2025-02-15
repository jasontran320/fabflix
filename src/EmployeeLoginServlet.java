import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jasypt.util.password.StrongPasswordEncryptor;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "EmployeeLoginServlet", urlPatterns = "/api/dashboard-login")
public class EmployeeLoginServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
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

            String email = request.getParameter("email");
            String password = request.getParameter("password");
            System.out.println("Checking employee credentials for email: " + email);

            try (Connection conn = dataSource.getConnection()) {
                String query = "SELECT password, fullname FROM employees WHERE email = ?";
                try (PreparedStatement statement = conn.prepareStatement(query)) {
                    statement.setString(1, email);
                    System.out.println("Executing SQL query...");
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println("No employee found with email: " + email);
                            responseJsonObject.addProperty("status", "fail");
                            responseJsonObject.addProperty("message", "Invalid email");
                        } else {
                            String encryptedPassword = rs.getString("password");
                            String fullName = rs.getString("fullname");

                            // Use StrongPasswordEncryptor to verify the password
                            boolean success = new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);

                            if (!success) {
                                System.out.println("Password mismatch for employee: " + email);
                                responseJsonObject.addProperty("status", "fail");
                                responseJsonObject.addProperty("message", "Invalid password");
                            } else {
                                System.out.println("Login successful for employee: " + email);
                                Employee employee = new Employee(email);
                                employee.setFullName(fullName);

                                // Set both employee and isEmployee attributes
                                HttpSession session = request.getSession();
                                session.setAttribute("employee", employee);
                                session.setAttribute("isEmployee", true);

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