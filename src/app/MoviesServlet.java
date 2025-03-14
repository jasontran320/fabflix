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
import java.sql.SQLException;
import java.util.ArrayList;

@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
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
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            StringBuilder baseQuery = new StringBuilder(
                    "SELECT m.id, m.title, m.year, m.director, " +
                            "COALESCE(r.rating, -1) as rating " +
                            "FROM movies m " +
                            "LEFT JOIN ratings r ON m.id = r.movieId " +
                            "WHERE 1=1"
            );
            ArrayList<Object> params = new ArrayList<>();

            String title = request.getParameter("title");
            if (title != null && !title.isEmpty()) {
                // Convert search terms for boolean mode full-text search
                String[] words = title.trim().split("\\s+");
                StringBuilder searchPattern = new StringBuilder();
                for (String word : words) {
                    searchPattern.append(" +").append(word).append("*");
                }
                baseQuery.append(" AND MATCH(m.title) AGAINST(? IN BOOLEAN MODE)");
                params.add(searchPattern.toString());
            }

            String year = request.getParameter("year");
            if (year != null && !year.isEmpty()) {
                baseQuery.append(" AND m.year = ?");
                params.add(Integer.parseInt(year));
            }

            String director = request.getParameter("director");
            if (director != null && !director.isEmpty()) {
                baseQuery.append(" AND m.director LIKE ?");
                params.add("%" + director + "%");
            }

            String star = request.getParameter("star");
            if (star != null && !star.isEmpty()) {
                baseQuery.append(" AND m.id IN (SELECT movieId FROM stars_in_movies WHERE starId IN " +
                        "(SELECT id FROM stars WHERE name LIKE ?))");
                params.add("%" + star + "%");
            }

            String genre = request.getParameter("genre");
            if (genre != null && !genre.isEmpty()) {
                baseQuery.append(" AND m.id IN (SELECT movieId FROM genres_in_movies WHERE genreId = ?)");
                params.add(genre);
            }

            String startsWith = request.getParameter("startsWith");
            if (startsWith != null && !startsWith.isEmpty()) {
                if (startsWith.equals("*")) {
                    baseQuery.append(" AND m.title REGEXP '^[^a-zA-Z0-9]'");
                } else if (startsWith.matches("[0-9]")) {
                    baseQuery.append(" AND m.title LIKE ?");
                    params.add(startsWith + "%");
                } else {
                    // make letters case insensitive
                    baseQuery.append(" AND LOWER(m.title) LIKE LOWER(?)");
                    params.add(startsWith + "%");
                }
            }

            String[] sortParts = request.getParameter("sort").split(",");
            String primaryField = sortParts[0];
            String primaryDir = sortParts[1];
            String secondaryField = sortParts[2];
            String secondaryDir = sortParts[3];

            String primary = primaryField.equals("rating") ? "r.rating" : "m.title";
            String secondary = secondaryField.equals("rating") ? "r.rating" : "m.title";

            baseQuery.append(" ORDER BY ")
                    .append(primary).append(" ").append(primaryDir.toUpperCase())
                    .append(", ")
                    .append(secondary).append(" ").append(secondaryDir.toUpperCase());

            int page = Math.max(1, Integer.parseInt(request.getParameter("page")));
            int limit = Integer.parseInt(request.getParameter("limit"));
            int offset = (page - 1) * limit;
            baseQuery.append(" LIMIT ? OFFSET ?");
            params.add(limit);
            params.add(offset);



            try (PreparedStatement statement = conn.prepareStatement(baseQuery.toString());
                 PreparedStatement genreStmt = conn.prepareStatement(
                         "SELECT g.name, g.id FROM genres g " +
                                 "JOIN genres_in_movies gm ON g.id = gm.genreId " +
                                 "WHERE gm.movieId = ? ORDER BY g.name LIMIT 3"
                 );
                 PreparedStatement starStmt = conn.prepareStatement(
                         "SELECT s.id, s.name, COUNT(*) as movie_count " +
                                 "FROM stars s " +
                                 "JOIN stars_in_movies sm ON s.id = sm.starId " +
                                 "WHERE s.id IN (SELECT starId FROM stars_in_movies WHERE movieId = ?) " +
                                 "GROUP BY s.id, s.name " +
                                 "ORDER BY movie_count DESC, s.name " +
                                 "LIMIT 3"
                 )) {

                for (int i = 0; i < params.size(); i++) {
                    statement.setObject(i + 1, params.get(i));
                }
                ResultSet rs = statement.executeQuery();

                JsonArray movies = new JsonArray();
                while (rs.next()) {
                    String movieId = rs.getString("id");
                    JsonObject movie = createMovieJson(rs);

                    genreStmt.setString(1, movieId);
                    try (ResultSet genreRs = genreStmt.executeQuery()) {
                        movie.add("genres", createGenreArray(genreRs));
                    }

                    starStmt.setString(1, movieId);
                    try (ResultSet starRs = starStmt.executeQuery()) {
                        movie.add("stars", createStarArray(starRs));
                    }

                    movies.add(movie);
                }

                String countQuery = baseQuery.toString()
                        .replaceFirst("SELECT.*?FROM", "SELECT COUNT(DISTINCT m.id) as total FROM")
                        .replaceFirst("ORDER BY.*$", "");

                try (PreparedStatement countStmt = conn.prepareStatement(countQuery)) {
                    for (int i = 0; i < params.size() - 2; i++) {
                        countStmt.setObject(i + 1, params.get(i));
                    }
                    ResultSet countRs = countStmt.executeQuery();
                    int totalRecords = countRs.next() ? countRs.getInt("total") : 0;

                    JsonObject result = new JsonObject();
                    result.add("movies", movies);
                    result.addProperty("totalRecords", totalRecords);

                    out.write(result.toString());
                    response.setStatus(200);
                }
            }
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

    private JsonObject createMovieJson(ResultSet rs) throws SQLException {
        JsonObject movie = new JsonObject();
        movie.addProperty("movie_id", rs.getString("id"));
        movie.addProperty("movie_title", rs.getString("title"));
        movie.addProperty("movie_year", rs.getString("year"));
        movie.addProperty("movie_director", rs.getString("director"));
        double rating = rs.getDouble("rating");
        movie.addProperty("movie_rating", rating == -1 ? "N/A" : String.valueOf(rating));
        return movie;
    }

    private JsonArray createGenreArray(ResultSet rs) throws SQLException {
        JsonArray genres = new JsonArray();
        while (rs.next()) {
            JsonObject genre = new JsonObject();
            genre.addProperty("genre_name", rs.getString("name"));
            genre.addProperty("genre_id", rs.getString("id"));
            genres.add(genre);
        }
        return genres;
    }

    private JsonArray createStarArray(ResultSet rs) throws SQLException {
        JsonArray stars = new JsonArray();
        while (rs.next()) {
            JsonObject star = new JsonObject();
            star.addProperty("star_id", rs.getString("id"));
            star.addProperty("star_name", rs.getString("name"));
            stars.add(star);
        }
        return stars;
    }
}