import com.google.gson.JsonArray;
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
import java.sql.*;


@WebServlet("/api/dashboard/add-movie")
public class AddMovieServlet extends HttpServlet {
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
        JsonArray messages = new JsonArray();

        String title = request.getParameter("title");
        String yearStr = request.getParameter("year");
        String director = request.getParameter("director");
        String star = request.getParameter("star");
        String genre = request.getParameter("genre");

        // Input validation
        if (title == null || title.trim().isEmpty() ||
                yearStr == null || yearStr.trim().isEmpty() ||
                director == null || director.trim().isEmpty() ||
                star == null || star.trim().isEmpty() ||
                genre == null || genre.trim().isEmpty()) {

            response.setStatus(400);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "All fields are required");
            response.getWriter().write(responseJsonObject.toString());
            return;
        }

        try {
            int year = Integer.parseInt(yearStr);

            try (Connection conn = dataSource.getConnection();
                 CallableStatement cs = conn.prepareCall("{CALL add_movie(?, ?, ?, ?, ?)}")) {

                cs.setString(1, title);
                cs.setInt(2, year);
                cs.setString(3, director);
                cs.setString(4, star);
                cs.setString(5, genre);

                boolean hasResults = cs.execute();
                boolean success = false;
                boolean movieExists = false;

                // Collect all messages from the stored procedure
                while (hasResults) {
                    try (ResultSet rs = cs.getResultSet()) {
                        while (rs.next()) {
                            String message = rs.getString("message");
                            messages.add(message);
                            if (message.startsWith("Movie already exists")) {
                                movieExists = true;
                            } else if (message.startsWith("Successfully")) {
                                success = true;
                            }
                        }
                    }
                    hasResults = cs.getMoreResults();
                }

                if (movieExists) {
                    response.setStatus(409);
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Movie already exists");
                } else if (success) {
                    responseJsonObject.addProperty("status", "success");
                    responseJsonObject.addProperty("message", "Movie added successfully");
                } else {
                    response.setStatus(500);
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Failed to add movie");
                }
                responseJsonObject.add("messages", messages);
            }
        } catch (NumberFormatException e) {
            response.setStatus(400);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Invalid year format");
        } catch (SQLException e) {
            response.setStatus(500);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Error adding movie: " + e.getMessage());
        }

        response.getWriter().write(responseJsonObject.toString());
    }
}