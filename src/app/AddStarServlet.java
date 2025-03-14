package app;

import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;

@WebServlet("/api/dashboard/add-star")
public class AddStarServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/MySQLReadWrite");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        JsonObject responseJsonObject = new JsonObject();

        String starName = request.getParameter("starName");
        String birthYearStr = request.getParameter("birthYear");

        if (starName == null || starName.trim().isEmpty()) {
            response.setStatus(400);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Star name is required");
            response.getWriter().write(responseJsonObject.toString());
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            // Check if star already exists
            String checkQuery = "SELECT id FROM stars WHERE name = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, starName);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    response.setStatus(409); // Conflict status code
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Star already exists");
                    response.getWriter().write(responseJsonObject.toString());
                    return;
                }
            }

            // Get the largest ID currently in use
            String maxIdQuery = "SELECT MAX(id) as maxId FROM stars";
            String newId;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(maxIdQuery)) {
                rs.next();
                String currentMaxId = rs.getString("maxId");
                int currentNum = Integer.parseInt(currentMaxId.substring(2));
                newId = String.format("nm%07d", currentNum + 1);
            }

            // Insert the new star
            String insertQuery = birthYearStr != null && !birthYearStr.trim().isEmpty() ?
                    "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)" :
                    "INSERT INTO stars (id, name) VALUES (?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setString(1, newId);
                pstmt.setString(2, starName);
                if (birthYearStr != null && !birthYearStr.trim().isEmpty()) {
                    pstmt.setInt(3, Integer.parseInt(birthYearStr));
                }
                pstmt.executeUpdate();

                responseJsonObject.addProperty("status", "success");
                responseJsonObject.addProperty("message", "Star added successfully");
                responseJsonObject.addProperty("starId", newId);
                responseJsonObject.addProperty("starName", starName);
            }

        } catch (SQLException e) {
            response.setStatus(500);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Error adding star: " + e.getMessage());
        } catch (NumberFormatException e) {
            response.setStatus(400);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Invalid birth year format");
        }

        response.getWriter().write(responseJsonObject.toString());
    }
}