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

@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
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
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            String query =
                    "SELECT m.id, m.title, m.year, m.director, r.rating,\n" +
                            " SUBSTRING_INDEX(\n" +
                            " (SELECT GROUP_CONCAT(DISTINCT g.name SEPARATOR ',')\n" +
                            " FROM genres_in_movies gm\n" +
                            " JOIN genres g ON gm.genreId = g.id\n" +
                            " WHERE gm.movieId = m.id) \n" +
                            " , ',', 3) AS genres,\n" +
                            " SUBSTRING_INDEX(\n" +
                            " (SELECT GROUP_CONCAT(DISTINCT CONCAT(s.id, ':', s.name) SEPARATOR ',')\n" +
                            " FROM stars_in_movies sm\n" +
                            " JOIN stars s ON sm.starId = s.id\n" +
                            " WHERE sm.movieId = m.id)\n" +
                            " , ',', 3) AS stars\n" +
                            "FROM movies m\n" +
                            "JOIN ratings r ON m.id = r.movieId\n" +
                            "ORDER BY r.rating DESC\n" + //second tie breaker is either r.numVotes or m.id ASC as so: "ORDER BY r.rating DESC, m.id ASC\n"
                            "LIMIT 20;";




            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();

            while (rs.next()) {
                JsonObject movieObject = new JsonObject();
                movieObject.addProperty("movie_id", rs.getString("id"));
                movieObject.addProperty("movie_title", rs.getString("title"));
                movieObject.addProperty("movie_year", rs.getString("year"));
                movieObject.addProperty("movie_director", rs.getString("director"));
                movieObject.addProperty("movie_rating", rs.getString("rating"));

                // Parse genres
                String genres = rs.getString("genres");
                JsonArray genresArray = new JsonArray();
                if (genres != null) {
                    for (String genre : genres.split(",")) {
                        JsonObject genreObject = new JsonObject();
                        genreObject.addProperty("genre_name", genre);
                        genresArray.add(genreObject);
                    }
                }
                movieObject.add("genres", genresArray);

                // Parse stars (format: "id:name,id:name,...")
                String stars = rs.getString("stars");
                JsonArray starsArray = new JsonArray();
                if (stars != null) {
                    for (String star : stars.split(",")) {
                        String[] starInfo = star.split(":");
                        if (starInfo.length == 2) {
                            JsonObject starObject = new JsonObject();
                            starObject.addProperty("star_id", starInfo[0]);
                            starObject.addProperty("star_name", starInfo[1]);
                            starsArray.add(starObject);
                        }
                    }
                }
                movieObject.add("stars", starsArray);

                jsonArray.add(movieObject);
            }

            rs.close();
            statement.close();

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