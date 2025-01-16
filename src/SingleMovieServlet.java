package src;

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
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 3L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        String id = request.getParameter("id");
        request.getServletContext().log("getting movie with id: " + id);
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            // Get movie details, genres, stars, and rating in one query
            String query = "SELECT m.id, m.title, m.year, m.director, " +
                    "g.name as genre_name, " +
                    "s.id as star_id, s.name as star_name, " +
                    "r.rating, r.numVotes " +
                    "FROM movies m " +
                    "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                    "LEFT JOIN genres g ON gm.genreId = g.id " +
                    "LEFT JOIN stars_in_movies sm ON m.id = sm.movieId " +
                    "LEFT JOIN stars s ON sm.starId = s.id " +
                    "LEFT JOIN ratings r ON m.id = r.movieId " +
                    "WHERE m.id = ? " +
                    "ORDER BY r.rating DESC";

            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, id);
            ResultSet rs = statement.executeQuery();

            JsonObject jsonObject = new JsonObject();
            JsonArray genresArray = new JsonArray();
            JsonArray starsArray = new JsonArray();

            boolean firstRow = true;
            while (rs.next()) {
                if (firstRow) {
                    // Set movie details from first row
                    jsonObject.addProperty("movie_id", rs.getString("id"));
                    jsonObject.addProperty("movie_title", rs.getString("title"));
                    jsonObject.addProperty("movie_year", rs.getString("year"));
                    jsonObject.addProperty("movie_director", rs.getString("director"));
                    jsonObject.addProperty("movie_rating", rs.getString("rating"));
                    jsonObject.addProperty("movie_votes", rs.getString("numVotes"));
                    firstRow = false;
                }

                // Add unique genres
                String genreName = rs.getString("genre_name");
                if (genreName != null && !genresArray.toString().contains(genreName)) {
                    JsonObject genreObject = new JsonObject();
                    genreObject.addProperty("genre_name", genreName);
                    genresArray.add(genreObject);
                }

                // Add unique stars
                String starId = rs.getString("star_id");
                String starName = rs.getString("star_name");
                if (starId != null && !starsArray.toString().contains(starId)) {
                    JsonObject starObject = new JsonObject();
                    starObject.addProperty("star_id", starId);
                    starObject.addProperty("star_name", starName);
                    starsArray.add(starObject);
                }
            }

            jsonObject.add("genres", genresArray);
            jsonObject.add("stars", starsArray);

            rs.close();
            statement.close();

            out.write(jsonObject.toString());
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