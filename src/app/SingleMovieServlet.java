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

@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 3L;
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
        request.getServletContext().log("getting movie with id: " + id);
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            JsonObject jsonObject = new JsonObject();

            // Movie and Rating info
            String movieQuery = "SELECT m.id, m.title, m.year, m.director, r.rating, r.numVotes " +
                    "FROM movies m " +
                    "LEFT JOIN ratings r ON m.id = r.movieId " +
                    "WHERE m.id = ?";

            try (PreparedStatement movieStmt = conn.prepareStatement(movieQuery)) {
                movieStmt.setString(1, id);
                ResultSet movieRs = movieStmt.executeQuery();

                if (movieRs.next()) {
                    jsonObject.addProperty("movie_id", movieRs.getString("id"));
                    jsonObject.addProperty("movie_title", movieRs.getString("title"));
                    jsonObject.addProperty("movie_year", movieRs.getString("year"));
                    jsonObject.addProperty("movie_director", movieRs.getString("director"));
                    jsonObject.addProperty("movie_rating", movieRs.getString("rating"));
                    jsonObject.addProperty("movie_votes", movieRs.getString("numVotes"));
                }
            }

            // Genres (alphabetically)
            String genreQuery = "SELECT g.id, g.name " +
                    "FROM genres g " +
                    "JOIN genres_in_movies gm ON g.id = gm.genreId " +
                    "WHERE gm.movieId = ? " +
                    "ORDER BY g.name ASC";

            JsonArray genresArray = new JsonArray();
            try (PreparedStatement genreStmt = conn.prepareStatement(genreQuery)) {
                genreStmt.setString(1, id);
                ResultSet genreRs = genreStmt.executeQuery();

                while (genreRs.next()) {
                    JsonObject genreObject = new JsonObject();
                    genreObject.addProperty("genre_id", genreRs.getString("id"));
                    genreObject.addProperty("genre_name", genreRs.getString("name"));
                    genresArray.add(genreObject);
                }
            }
            jsonObject.add("genres", genresArray);

            // Stars (by movie count then name)
            String starQuery = "SELECT s.id, s.name, COUNT(*) as movie_count " +
                    "FROM stars s " +
                    "JOIN stars_in_movies sm ON s.id = sm.starId " +
                    "WHERE s.id IN (SELECT starId FROM stars_in_movies WHERE movieId = ?) " +
                    "GROUP BY s.id, s.name " +
                    "ORDER BY movie_count DESC, s.name ASC";

            JsonArray starsArray = new JsonArray();
            try (PreparedStatement starStmt = conn.prepareStatement(starQuery)) {
                starStmt.setString(1, id);
                ResultSet starRs = starStmt.executeQuery();

                while (starRs.next()) {
                    JsonObject starObject = new JsonObject();
                    starObject.addProperty("star_id", starRs.getString("id"));
                    starObject.addProperty("star_name", starRs.getString("name"));
                    starsArray.add(starObject);
                }
            }
            jsonObject.add("stars", starsArray);

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