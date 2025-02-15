package XMLParser.src.main.java.XMLParser.dao;

import XMLParser.src.main.java.XMLParser.models.Star;
import XMLParser.src.main.java.XMLParser.utils.DatabaseConnection;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class StarDAO {
    private Connection conn;
    private Set<String> addedStarIds;              // Tracks stars added in this session
    private Set<String> addedCompositeKeys;        // Tracks composite keys added in this session
    private Logger logger;
    private int counter;

    // New fields for tracking all stars (preloaded + session)
    private Map<String, String> compositeKeyToId;  // name_birthyear -> id for ALL stars
    private Map<String, Star> starById;            // Complete star data by id
    private HashMap<String, String> starNameToIdMap;
    public int num_duplicates;
    public int number_processed;

    public StarDAO(Logger logger, HashMap<String, String> starNameToIdMap) {
        try {
            this.conn = DatabaseConnection.getConnection();
            this.addedStarIds = new HashSet<>();
            this.addedCompositeKeys = new HashSet<>();
            this.logger = logger;
            this.starNameToIdMap = starNameToIdMap;

            // Initialize tracking structures
            this.compositeKeyToId = new HashMap<>();
            this.starById = new HashMap<>();
            this.num_duplicates = 0;

            // Load existing data
            loadExistingStars();
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing StarDAO", e);
        }
    }

    private void loadExistingStars() throws SQLException {
        String query = "SELECT id, name, birthYear FROM stars";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                Integer birthYear = rs.getInt("birthYear");
                if (rs.wasNull()) {
                    birthYear = null;
                }

                Star star = new Star();
                star.setId(id);
                star.setName(name);
                star.setBirthYear(birthYear);

                String compositeKey = generateCompositeKey(name, birthYear);
                compositeKeyToId.put(compositeKey, id);
                starNameToIdMap.put(name, id);
                starById.put(id, star);
            }

            // Initialize counter based on existing entries
            this.counter = starById.size();
        }
    }

    private String generateCompositeKey(String name, Integer birthYear) {
        return name + "_" + (birthYear != null ? birthYear.toString() : "unknown");
    }

    public boolean insertStarBatch(List<Star> stars) throws SQLException {
        // Filter out stars that already exist in database
        List<Star> newStars = new ArrayList<>();
        for (Star star : stars) {
            String compositeKey = generateCompositeKey(star.getName(), star.getBirthYear());

            // Check if star already exists in database or current session
            if (!compositeKeyToId.containsKey(compositeKey) && !addedCompositeKeys.contains(compositeKey)) {
                newStars.add(star);
                addedStarIds.add(star.getId());
                addedCompositeKeys.add(compositeKey);
            } else {
                logger.warning("Star already exists: " + compositeKey);
                num_duplicates++;
            }
        }

        if (newStars.isEmpty()) {
            return true; // Nothing new to insert
        }

        String sql = "INSERT IGNORE INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
        conn.setAutoCommit(false);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Star star : newStars) {
                stmt.setString(1, star.getId());
                stmt.setString(2, star.getName());
                if (star.getBirthYear() != null) {
                    stmt.setInt(3, star.getBirthYear());
                } else {
                    stmt.setNull(3, Types.INTEGER);
                }
                stmt.addBatch();

                // Update our tracking maps
                starNameToIdMap.put(star.getName(), star.getId());
                String compositeKey = generateCompositeKey(star.getName(), star.getBirthYear());
                compositeKeyToId.put(compositeKey, star.getId());
                starById.put(star.getId(), star);
            }

            stmt.executeBatch();
            conn.commit();
            number_processed += newStars.size();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            logger.severe("Error in batch insert: " + e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public String generateUniqueStarId(String name, Integer birthYear) {
        // First check if star already exists
        String compositeKey = generateCompositeKey(name, birthYear);
        String existingId = compositeKeyToId.get(compositeKey);
        if (existingId != null) {
            return existingId;  // Return existing ID if found
        }

        // Generate new ID if star doesn't exist
        String newId;
        do {
            counter++;
            newId = String.format("nm%07d", counter);
        } while (starById.containsKey(newId) || addedStarIds.contains(newId));

        return newId;
    }

    public String generateUniqueStarId(String name) {
        // First check if star already exists
        String existingId = starNameToIdMap.get(name);
        if (existingId != null) {
            return existingId;  // Return existing ID if found
        }

        // Generate new ID if star doesn't exist
        String newId;
        do {
            counter++;
            newId = String.format("nm%07d", counter);
        } while (starById.containsKey(newId) || addedStarIds.contains(newId));

        return newId;
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