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
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;


@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
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
            String selectMovies =
                    "SELECT m.id, m.title, m.year, m.director, COALESCE(r.rating, -1) as rating";

            String selectCount =
                    "SELECT COUNT(DISTINCT m.id) as total";

            StringBuilder fromWhere = new StringBuilder(
                    " FROM movies m " +
                    " LEFT JOIN ratings r ON m.id = r.movieId " +
                    " WHERE 1=1"
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
                fromWhere.append(" AND MATCH(m.title) AGAINST(? IN BOOLEAN MODE)");
                params.add(searchPattern.toString());
            }

            String year = request.getParameter("year");
            if (year != null && !year.isEmpty()) {
                fromWhere.append(" AND m.year = ?");
                params.add(Integer.parseInt(year));
            }

            String director = request.getParameter("director");
            if (director != null && !director.isEmpty()) {
                fromWhere.append(" AND m.director LIKE ?");
                params.add("%" + director + "%");
            }

            String star = request.getParameter("star");
            if (star != null && !star.isEmpty()) {
                fromWhere.append(
                    " AND EXISTS (" +
                    "   SELECT 1 " +
                    "   FROM stars_in_movies sm " +
                    "   JOIN stars s ON s.id = sm.starId " +
                    "   WHERE sm.movieId = m.id " +
                    "     AND s.name LIKE ?" +
                    " )"
                );
                params.add("%" + star + "%");
            }

            String genre = request.getParameter("genre");
            if (genre != null && !genre.isEmpty()) {
                fromWhere.append(" AND m.id IN (SELECT movieId FROM genres_in_movies WHERE genreId = ?)");
                params.add(genre);
            }

            String startsWith = request.getParameter("startsWith");
            if (startsWith != null && !startsWith.isEmpty()) {
                if (startsWith.equals("*")) {
                    fromWhere.append(" AND m.title REGEXP '^[^a-zA-Z0-9]'");
                } else if (startsWith.matches("[0-9]")) {
                    fromWhere.append(" AND m.title LIKE ?");
                    params.add(startsWith + "%");
                } else {
                    // make letters case insensitive
                    fromWhere.append(" AND LOWER(m.title) LIKE LOWER(?)");
                    params.add(startsWith + "%");
                }
            }

            // --- SORT (whitelist) ---

            // Defaults
            String defaultPrimaryField = "m.title";
            String defaultPrimaryDir = "ASC";
            String defaultSecondaryField = "r.rating";
            String defaultSecondaryDir = "DESC";

            // Parse sort
            String sortParam = request.getParameter("sort");
            String primary = defaultPrimaryField;
            String primaryDir = defaultPrimaryDir;
            String secondary = defaultSecondaryField;
            String secondaryDir = defaultSecondaryDir;

            if (sortParam != null && !sortParam.isBlank()) {
                String[] sortParts = sortParam.split(",");
                if (sortParts.length == 4) {
                    String primaryFieldRaw = sortParts[0].trim().toLowerCase();
                    String primaryDirRaw = sortParts[1].trim().toUpperCase();
                    String secondaryFieldRaw = sortParts[2].trim().toLowerCase();
                    String secondaryDirRaw = sortParts[3].trim().toUpperCase();

                    // Field whitelist
                    String primaryField =
                            primaryFieldRaw.equals("rating") ? "r.rating" :
                            primaryFieldRaw.equals("title")  ? "m.title"  :
                            null;

                    String secondaryField =
                            secondaryFieldRaw.equals("rating") ? "r.rating" :
                            secondaryFieldRaw.equals("title")  ? "m.title"  :
                            null;

                    // Dir whitelist
                    String pDir =
                            (primaryDirRaw.equals("ASC") || primaryDirRaw.equals("DESC")) ? primaryDirRaw : null;

                    String sDir =
                            (secondaryDirRaw.equals("ASC") || secondaryDirRaw.equals("DESC")) ? secondaryDirRaw : null;

                    // Only accept if all 4 tokens are valid
                    if (primaryField != null && secondaryField != null && pDir != null && sDir != null) {
                        primary = primaryField;
                        primaryDir = pDir;
                        secondary = secondaryField;
                        secondaryDir = sDir;
                    }
                }
            }

            // Build ORDER BY using safe tokens
            String orderByClause =
                    " ORDER BY " + primary + " " + primaryDir +
                    ", " + secondary + " " + secondaryDir;

            // Build LIMIT and OFFSET
            int page = Math.max(1, Integer.parseInt(request.getParameter("page")));
            int limit = Integer.parseInt(request.getParameter("limit"));
            limit = Math.min(100, Math.max(1, limit));
            int offset = (page - 1) * limit;

            String limitOffsetClause = " LIMIT ? OFFSET ?";
            
            // Finalize 2 queries
            String dataQuery = selectMovies + fromWhere + orderByClause + limitOffsetClause;
            String countQuery = selectCount + fromWhere;

            // --- Construct Subqueries ---

            try (PreparedStatement statement = conn.prepareStatement(dataQuery)) {
                int paramIndex = 1;
                for (Object p : params) {
                    statement.setObject(paramIndex++, p);
                }
                statement.setInt(paramIndex++, limit);
                statement.setInt(paramIndex, offset);

                JsonArray movies = new JsonArray();
                ArrayList<String> movieIds = new ArrayList<>();
                Map<String, JsonObject> movieById = new HashMap<>();

                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        String movieId = rs.getString("id");
                        JsonObject movie = createMovieJson(rs);
                        movie.add("genres", new JsonArray());
                        movie.add("stars", new JsonArray());

                        movies.add(movie);
                        movieIds.add(movieId);
                        movieById.put(movieId, movie);
                    }
                }


                // --- Build Genres ---

                if (!movieIds.isEmpty()) {
                    String genreBatchQuery =
                        "SELECT movieId, id, name FROM (" +
                        "  SELECT gm.movieId AS movieId, g.id AS id, g.name AS name, " +
                        "         ROW_NUMBER() OVER (PARTITION BY gm.movieId ORDER BY g.name) AS rn " +
                        "  FROM genres_in_movies gm " +
                        "  JOIN genres g ON g.id = gm.genreId " +
                        "  WHERE gm.movieId IN (" + makePlaceholders(movieIds.size()) + ") " +
                        ") AS temp " +
                        "WHERE rn <= 3 " +
                        "ORDER BY movieId, name";

                    try (PreparedStatement genreBatchStmt = conn.prepareStatement(genreBatchQuery)) {
                        int idx = 1;
                        for (String id : movieIds) {
                            genreBatchStmt.setString(idx++, id);
                        }

                        Map<String, JsonArray> genresByMovie = new HashMap<>();
                        try (ResultSet grs = genreBatchStmt.executeQuery()) {
                            while (grs.next()) {
                                String movieId = grs.getString("movieId");

                                JsonObject genreObj = new JsonObject();
                                genreObj.addProperty("genre_id", grs.getString("id"));
                                genreObj.addProperty("genre_name", grs.getString("name"));

                                genresByMovie.computeIfAbsent(movieId, k -> new JsonArray());
                                genresByMovie.get(movieId).add(genreObj);
                            }
                        }

                        // attach to each movie
                        for (String id : movieIds) {
                            JsonObject movie = movieById.get(id);
                            movie.add("genres", genresByMovie.getOrDefault(id, new JsonArray()));
                        }
                    }
                }

                // --- Build Stars ---

                if (!movieIds.isEmpty()) {
                    String starBatchQuery =
                            "SELECT movieId, id, name FROM (" +
                            "  SELECT sm.movieId AS movieId, s.id AS id, s.name AS name, " +
                            "         COUNT(sm2.movieId) AS movie_count, " +
                            "         ROW_NUMBER() OVER (" +
                            "           PARTITION BY sm.movieId " +
                            "           ORDER BY COUNT(sm2.movieId) DESC, s.name " +
                            "         ) AS rn " +
                            "  FROM stars_in_movies sm " +
                            "  JOIN stars s ON s.id = sm.starId " +
                            "  JOIN stars_in_movies sm2 ON sm2.starId = s.id " +
                            "  WHERE sm.movieId IN (" + makePlaceholders(movieIds.size()) + ") " +
                            "  GROUP BY sm.movieId, s.id, s.name " +
                            ") AS temp " +
                            "WHERE rn <= 3 " +
                            "ORDER BY movieId, movie_count DESC, name";

                    try (PreparedStatement starBatchStmt = conn.prepareStatement(starBatchQuery)) {
                        int idx = 1;
                        for (String id : movieIds) {
                            starBatchStmt.setString(idx++, id);
                        }

                        Map<String, JsonArray> starsByMovie = new HashMap<>();
                        try (ResultSet srs = starBatchStmt.executeQuery()) {
                            while (srs.next()) {
                                String movieId = srs.getString("movieId");

                                JsonObject starObj = new JsonObject();
                                starObj.addProperty("star_id", srs.getString("id"));
                                starObj.addProperty("star_name", srs.getString("name"));

                                starsByMovie.computeIfAbsent(movieId, k -> new JsonArray());
                                starsByMovie.get(movieId).add(starObj);

                            }
                        }

                        // attach to each movie
                        for (String id : movieIds) {
                            JsonObject movie = movieById.get(id);
                            movie.add("stars", starsByMovie.getOrDefault(id, new JsonArray()));
                        }
                    }
                }

                // --- Execute Count Query ---
                    
                try (PreparedStatement countStmt = conn.prepareStatement(countQuery)) {
                    int countIndex = 1;
                    for (Object p : params) {
                        countStmt.setObject(countIndex++, p);
                    }
                    int totalRecords = 0;
                    try (ResultSet countRs = countStmt.executeQuery()) {
                        if (countRs.next()) {
                            totalRecords = countRs.getInt("total");
                        }
                    }
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

    private String makePlaceholders(int n) {
        return String.join(",", Collections.nCopies(n, "?"));
    }

}