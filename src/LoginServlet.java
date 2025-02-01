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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JsonObject responseJsonObject = new JsonObject();

        try {
            String email = request.getParameter("username");
            String password = request.getParameter("password");
            if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("Email and password are required");
            }

            try (Connection conn = dataSource.getConnection()) {
                String emailQuery = "SELECT id, password FROM customers WHERE email = ?";
                try (PreparedStatement emailStatement = conn.prepareStatement(emailQuery)) {
                    emailStatement.setString(1, email);

                    try (ResultSet emailRs = emailStatement.executeQuery()) {
                        if (!emailRs.next()) {
                            responseJsonObject.addProperty("status", "fail");
                            responseJsonObject.addProperty("message", "Incorrect email address");
                            responseJsonObject.addProperty("field", "email");
                        } else {
                            String storedPassword = emailRs.getString("password");
                            String userId = emailRs.getString("id");

                            if (!password.equals(storedPassword)) {
                                responseJsonObject.addProperty("status", "fail");
                                responseJsonObject.addProperty("message", "Incorrect password");
                                responseJsonObject.addProperty("field", "password");
                            } else {
                                User user = new User(email);
                                user.setId(userId);
                                request.getSession().setAttribute("user", user);
                                responseJsonObject.addProperty("status", "success");
                                responseJsonObject.addProperty("message", "Login successful");
                            }
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", e.getMessage());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Server error occurred");
            e.printStackTrace();
        }

        response.getWriter().write(responseJsonObject.toString());
    }
}