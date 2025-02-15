package XMLParser.src.main.java.XMLParser.dao;

import XMLParser.src.main.java.XMLParser.models.Movie;
import XMLParser.src.main.java.XMLParser.utils.DatabaseConnection;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class MovieDAO {
    private Connection conn;
    private Set<String> addedMovieIds;           // Track movies added this session
    private HashMap<String, Integer> genreNameToId;  // Track all genre mappings
    private Logger logger;
    private int counter;

    // New fields for tracking all movies (preloaded + session)
    private HashMap<String, String> movieCompositeKeys;  // title_year_director -> id
    private HashMap<String, Movie> movieById;            // Complete movie data by id
    private HashMap<String, String> fidToIdMap;          // Track FIDs to IDs if needed

    public int num_duplicates;
    public int num_redirects;
    public int num_genres_inserted;
    public int num_movie_genres_inserted;


    public MovieDAO(Logger logger) {
        try {
            this.conn = DatabaseConnection.getConnection();
            this.addedMovieIds = new HashSet<>();
            this.genreNameToId = new HashMap<>();
            this.logger = logger;

            // Initialize new tracking structures
            this.movieCompositeKeys = new HashMap<>();
            this.movieById = new HashMap<>();
            this.fidToIdMap = new HashMap<>();
            this.num_duplicates = 0;
            this.num_redirects = 0;

            this.num_genres_inserted = 0;
            this.num_movie_genres_inserted = 0;

            // Load existing data
            loadExistingMovies();
            loadExistingGenres();
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing MovieDAO", e);
        }
    }

    private void loadExistingMovies() throws SQLException {
        String query = "SELECT id, title, year, director, price FROM movies";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Movie movie = new Movie();
                movie.setId(rs.getString("id"));
                movie.setTitle(rs.getString("title"));
                movie.setYear(rs.getInt("year"));
                if (rs.wasNull()) {
                    movie.setYear(0);  // or however you handle null years
                }
                movie.setDirector(rs.getString("director"));
                movie.setPrice(rs.getDouble("price"));

                String compositeKey = generateCompositeKey(
                        movie.getTitle(), movie.getYear(), movie.getDirector());

                movieById.put(movie.getId(), movie);

                movieCompositeKeys.put(compositeKey, movie.getId());
            }
            // Set counter based on existing entries
            counter = movieById.size();
        }
    }

    private void loadExistingGenres() throws SQLException {
        String query = "SELECT id, name FROM genres";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                genreNameToId.put(rs.getString("name"), rs.getInt("id"));
            }
        }
    }

    private String generateCompositeKey(String title, Integer year, String director) {
        return title + "_" + (year != null && year > 0 ? year.toString() : "unknown") +
                "_" + (director != null ? director : "unknown");
    }

    public boolean insertMovieBatch(List<Movie> movies, List<Movie> newMovies, Map<String, String> batchFidToIdMap) throws SQLException {
        String sql = "INSERT IGNORE INTO movies (id, title, year, director, price) VALUES (?, ?, ?, ?, ?)";

        // Filter out movies that already exist

        for (Movie movie : movies) {
            String compositeKey = generateCompositeKey(
                    movie.getTitle(), movie.getYear(), movie.getDirector());

            if (!movieCompositeKeys.containsKey(compositeKey) &&
                    !addedMovieIds.contains(movie.getId())) {
                movieCompositeKeys.put(compositeKey, movie.getId());
                newMovies.add(movie);
            } else {
                if (batchFidToIdMap.containsKey(movie.getFid())) {batchFidToIdMap.put(movie.getFid(), movieCompositeKeys.get(compositeKey)); num_redirects++;}
                logger.info("Movie already exists: " + compositeKey + ". FID: " + movie.getFid());
                num_duplicates++;
            }
        }

        if (newMovies.isEmpty()) {
            return true; // Nothing new to insert
        }

        conn.setAutoCommit(false);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Movie movie : newMovies) {
                stmt.setString(1, movie.getId());
                stmt.setString(2, movie.getTitle());
                if (movie.getYear() == 0) {
                    stmt.setNull(3, Types.INTEGER);
                } else {
                    stmt.setInt(3, movie.getYear());
                }
                stmt.setString(4, movie.getDirector());
                stmt.setDouble(5, movie.getPrice());
                stmt.addBatch();

                String compositeKey = generateCompositeKey(
                        movie.getTitle(), movie.getYear(), movie.getDirector());

                // Update tracking structures
                addedMovieIds.add(movie.getId());
                movieCompositeKeys.put(compositeKey, movie.getId());
                movieById.put(movie.getId(), movie);
            }

            stmt.executeBatch();
            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            logger.severe("Error in batch insert: " + e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public void insertMovieGenreBatch(String movieId, List<String> genres) throws SQLException {
        // First ensure all genres exist and get their IDs
        HashMap<String, Integer> genreIds = new HashMap<>();
        for (String genre : genres) {
            if (genre != null && !genre.trim().isEmpty()) {
                genreIds.put(genre, getOrCreateGenreId(genre));
            }
        }

        String sql = "INSERT IGNORE INTO genres_in_movies (genreId, movieId) VALUES (?, ?)";
        conn.setAutoCommit(false);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String genre : genres) {
                Integer genreId = genreIds.get(genre);
                if (genreId != null && genreId > 0) {
                    stmt.setInt(1, genreId);
                    stmt.setString(2, movieId);
                    stmt.addBatch();
                    num_movie_genres_inserted++;
                }
            }

            stmt.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            logger.severe("Error in batch genre insert: " + e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private int getOrCreateGenreId(String genreName) throws SQLException {
        if (genreName == null || genreName.trim().isEmpty()) {
            return -1;
        }

        genreName = genreName.trim();

        Integer cachedId = genreNameToId.get(genreName);
        if (cachedId != null) {
            return cachedId;
        }

        String insertSql = "INSERT INTO genres (name) VALUES (?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            insertStmt.setString(1, genreName);
            insertStmt.executeUpdate();

            try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    genreNameToId.put(genreName, newId);
                    num_genres_inserted++;
                    return newId;
                }
            }
        }

        return -1;
    }

    public String generateUniqueMovieId(String title, Integer year, String director) {
        // First check if movie exists
        String compositeKey = generateCompositeKey(title, year, director);
        String existingId = movieCompositeKeys.get(compositeKey);
        if (existingId != null) {
            return existingId;
        }

        // Generate new if doesn't exist
        String newId;
        do {
            counter++;
            newId = String.format("tt%07d", counter);
        } while (movieById.containsKey(newId) || addedMovieIds.contains(newId));

        return newId;
    }

    public boolean movieExists(String movieId) {
        return movieById.containsKey(movieId) || addedMovieIds.contains(movieId);
    }

    public String getIdByFid(String fid) {
        return fidToIdMap.get(fid.toUpperCase());
    }

    public void addFidMapping(String fid, String id) {
        fidToIdMap.put(fid.toUpperCase(), id);
    }

    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.warning("Error closing connection: " + e.getMessage());
        }
    }
}