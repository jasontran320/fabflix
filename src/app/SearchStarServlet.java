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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


@WebServlet("/api/search-star")
public class SearchStarServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/MySQLReadOnly");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        JsonObject responseJsonObject = new JsonObject();

        String name = request.getParameter("name");
        String birthYearStr = request.getParameter("birthYear");

        if (name == null || name.trim().isEmpty()) {
            response.setStatus(400);
            responseJsonObject.addProperty("message", "Star name is required");
            response.getWriter().write(responseJsonObject.toString());
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String query;
            PreparedStatement stmt;

            if (birthYearStr != null && !birthYearStr.trim().isEmpty()) {
                query = "SELECT id FROM stars WHERE name = ? AND birthYear = ?";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, name);
                stmt.setInt(2, Integer.parseInt(birthYearStr));
            } else {
                query = "SELECT id FROM stars WHERE name = ?";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, name);
            }

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                responseJsonObject.addProperty("found", true);
                responseJsonObject.addProperty("starId", rs.getString("id"));
            } else {
                responseJsonObject.addProperty("found", false);
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            response.setStatus(500);
            responseJsonObject.addProperty("message", "Error searching for star: " + e.getMessage());
        } catch (NumberFormatException e) {
            response.setStatus(400);
            responseJsonObject.addProperty("message", "Invalid birth year format");
        }

        response.getWriter().write(responseJsonObject.toString());
    }
}