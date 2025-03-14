package app;

import com.google.gson.JsonArray;
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
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;
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
        String id = request.getParameter("id");
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            // First check if star exists
            String starQuery = "SELECT id as starId, name, birthYear FROM stars WHERE id = ?";
            PreparedStatement starStmt = conn.prepareStatement(starQuery);
            starStmt.setString(1, id);
            ResultSet starRs = starStmt.executeQuery();

            if (!starRs.next()) {
                // Star not found
                response.setStatus(404);
                JsonObject errorJson = new JsonObject();
                errorJson.addProperty("errorMessage", "Star not found");
                out.write(errorJson.toString());
                return;
            }

            // Star exists, get their movies (if any)
            JsonArray jsonArray = new JsonArray();
            JsonObject starObject = new JsonObject();
            starObject.addProperty("star_id", starRs.getString("starId"));
            starObject.addProperty("star_name", starRs.getString("name"));
            starObject.addProperty("star_dob", starRs.getString("birthYear"));

            // Get movies (using LEFT JOIN to include stars with no movies)
            String movieQuery = "SELECT m.id as movieId, m.title, m.year, m.director " +
                    "FROM stars s " +
                    "LEFT JOIN stars_in_movies sim ON s.id = sim.starId " +
                    "LEFT JOIN movies m ON sim.movieId = m.id " +
                    "WHERE s.id = ? " +
                    "ORDER BY m.year DESC, m.title";

            PreparedStatement movieStmt = conn.prepareStatement(movieQuery);
            movieStmt.setString(1, id);
            ResultSet movieRs = movieStmt.executeQuery();

            if (!movieRs.next()) {
                // No movies, but star exists
                jsonArray.add(starObject);
            } else {
                do {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("star_id", starObject.get("star_id").getAsString());
                    jsonObject.addProperty("star_name", starObject.get("star_name").getAsString());
                    String starDob;
                    if (starObject.get("star_dob").isJsonNull()) {
                        starDob = null;  // or you can assign a default value, e.g., "N/A"
                    } else {
                        starDob = starObject.get("star_dob").getAsString();
                    }
                    jsonObject.addProperty("star_dob", starDob);


                    String movieId = movieRs.getString("movieId");
                    if (movieId != null) {  // Check if movie exists
                        jsonObject.addProperty("movie_id", movieId);
                        jsonObject.addProperty("movie_title", movieRs.getString("title"));
                        jsonObject.addProperty("movie_year", movieRs.getString("year"));
                        jsonObject.addProperty("movie_director", movieRs.getString("director"));
                    }
                    jsonArray.add(jsonObject);
                } while (movieRs.next());
            }

            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());
            request.getServletContext().log("Error:", e);
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}