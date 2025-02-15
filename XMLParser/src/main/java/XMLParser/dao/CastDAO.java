package XMLParser.src.main.java.XMLParser.dao;

import XMLParser.src.main.java.XMLParser.models.CastRelation;
import XMLParser.src.main.java.XMLParser.utils.DatabaseConnection;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;


public class CastDAO {
    private Connection conn;
    private Logger logger;
    private Set<String> existingRelations;  // Track starId_movieId combinations

    public int num_duplicates;

    public CastDAO(Logger logger) {
        try {
            this.conn = DatabaseConnection.getConnection();
            this.logger = logger;
            this.existingRelations = new HashSet<>();
            loadExistingRelations();
            num_duplicates = 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing CastDAO", e);
        }
    }

    private void loadExistingRelations() throws SQLException {
        String query = "SELECT starId, movieId FROM stars_in_movies";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String starId = rs.getString("starId");
                String movieId = rs.getString("movieId");
                String relationKey = generateRelationKey(starId, movieId);
                existingRelations.add(relationKey);
            }
        }
    }

    private String generateRelationKey(String starId, String movieId) {
        return starId + "_" + movieId;
    }

    public boolean insertStarInMovieBatch(List<CastRelation> relations) throws SQLException {
        // Filter out existing relations and prepare new ones for insertion
        List<CastRelation> newRelations = new ArrayList<>();
        for (CastRelation relation : relations) {
            String relationKey = generateRelationKey(relation.getStarId(), relation.getMovieId());
            if (!existingRelations.contains(relationKey)) {
                newRelations.add(relation);
            } else {
                //logger.info("Skipping existing relation: " + relationKey);
                num_duplicates++;
            }
        }

        if (newRelations.isEmpty()) {
            return true; // Nothing new to insert
        }

        String sql = "INSERT IGNORE INTO stars_in_movies (starId, movieId) VALUES (?, ?)";
        conn.setAutoCommit(false);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (CastRelation relation : newRelations) {
                stmt.setString(1, relation.getStarId());
                stmt.setString(2, relation.getMovieId());
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            conn.commit();

            // Update tracking for successfully inserted relations
            for (int i = 0; i < results.length; i++) {
                if (results[i] > 0) {
                    CastRelation relation = newRelations.get(i);
                    String relationKey = generateRelationKey(relation.getStarId(), relation.getMovieId());
                    existingRelations.add(relationKey);
                }
            }

            // Return true if any row was inserted
            return Arrays.stream(results).anyMatch(x -> x > 0);
        } catch (SQLException e) {
            conn.rollback();
            logger.severe("Error in batch insert for cast relations: " + e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.warning("Error closing CastDAO resources: " + e.getMessage());
        }
    }
}