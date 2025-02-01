import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json"); // Add this line
        String email = request.getParameter("username");
        String password = request.getParameter("password");
        JsonObject responseJsonObject = new JsonObject();

        try (Connection conn = dataSource.getConnection()) {
            // First, check if the email exists
            String emailQuery = "SELECT id, password FROM customers WHERE email = ?";
            PreparedStatement emailStatement = conn.prepareStatement(emailQuery);
            emailStatement.setString(1, email);
            ResultSet emailRs = emailStatement.executeQuery();

            if (!emailRs.next()) {
                // Email doesn't exist
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Incorrect email address");
                responseJsonObject.addProperty("field", "email");
            } else {
                // Email exists, now check password
                String storedPassword = emailRs.getString("password");
                String userId = emailRs.getString("id");

                if (!password.equals(storedPassword)) {
                    // Password is wrong
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Incorrect password");
                    responseJsonObject.addProperty("field", "password");
                } else {
                    // Both are correct
                    User user = new User(email);
                    user.setId(userId);
                    request.getSession().setAttribute("user", user);
                    responseJsonObject.addProperty("status", "success");
                    responseJsonObject.addProperty("message", "Login successful");
                }
            }

            emailRs.close();
            emailStatement.close();

        } catch (Exception e) {
            response.setStatus(500);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Server error: " + e.getMessage());
        }

        response.getWriter().write(responseJsonObject.toString());
    }
}