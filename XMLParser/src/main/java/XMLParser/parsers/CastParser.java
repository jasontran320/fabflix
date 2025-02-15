package XMLParser.src.main.java.XMLParser.parsers;

import XMLParser.src.main.java.XMLParser.dao.CastDAO;
import XMLParser.src.main.java.XMLParser.dao.StarDAO;
import XMLParser.src.main.java.XMLParser.models.CastRelation;
import XMLParser.src.main.java.XMLParser.models.Star;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class CastParser extends DefaultHandler {
    private CastDAO castDAO;
    private StarDAO starDAO;
    private StringBuilder currentValue;
    private Stack<String> elementStack;
    private int processedCount;
    private int errorCount;
    private Logger logger;

    // Maps for lookups
    private HashMap<String, String> starNameToIdMap;      // Global star name to ID mapping
    private HashMap<String, String> movieFidToIdMap;      // Global movie FID to ID mapping
    private Map<String, String> batchStarNameToIdMap;     // Temporary map for new stars in current batch

    // Current parsing state
    private String currentMovieFid;
    private String currentStarName;

    // Batch processing
    private static final int BATCH_SIZE = 100;
    private List<Star> pendingStars;                      // New stars to be inserted
    private List<CastRelation> pendingRelations;          // Cast relations to be inserted
    private Set<String> processedRelations;               // Track unique star-movie combinations
    int beginning_number_of_stars;

    public CastParser(HashMap<String, String> starNameToIdMap,
                      HashMap<String, String> movieFidToIdMap,
                      Logger logger, StarDAO starDAO) {
        this.castDAO = new CastDAO(logger);
        this.starDAO = starDAO;
        this.beginning_number_of_stars = starDAO.number_processed;
        this.currentValue = new StringBuilder();
        this.elementStack = new Stack<>();
        this.processedCount = 0;
        this.errorCount = 0;
        this.logger = logger;
        this.starNameToIdMap = starNameToIdMap;
        this.movieFidToIdMap = movieFidToIdMap;

        // Initialize batch processing structures
        this.pendingStars = new ArrayList<>();
        this.pendingRelations = new ArrayList<>();
        this.batchStarNameToIdMap = new HashMap<>();
        this.processedRelations = new HashSet<>();
    }

    private void processPendingStars() {
        if (pendingStars.isEmpty()) return;

        try {
            boolean success = starDAO.insertStarBatch(pendingStars);
            if (success) {
                /*
                // Update global map with newly inserted stars. Maybe handeld in star batch already.
                for (Star star : pendingStars) {
                    starNameToIdMap.put(star.getName(), star.getId());
                }
                 */
            } else {
                logger.warning("Failed to insert star batch");
                errorCount += pendingStars.size();
            }
        } catch (SQLException e) {
            logger.severe("Error inserting star batch: " + e.getMessage());
            errorCount += pendingStars.size();
        } finally {
            pendingStars.clear();
            batchStarNameToIdMap.clear();
        }
    }

    private void processPendingRelations() {
        if (pendingRelations.isEmpty()) return;

        try {
            boolean success = castDAO.insertStarInMovieBatch(pendingRelations);
            if (success) {
                processedCount += pendingRelations.size();
            }
        } catch (SQLException e) {
            logger.severe("Error inserting cast relations batch: " + e.getMessage());
            errorCount += pendingRelations.size();
        } finally {
            pendingRelations.clear();
        }
    }

    private String handleMissingActor(String starName) {
        if (starName == null || starName.trim().isEmpty()) {
            return null;
        }

        // Check if we've already created this star in the current batch
        String batchId = batchStarNameToIdMap.get(starName);
        if (batchId != null) {
            return batchId;
        }

        // Create new star
        String newId = starDAO.generateUniqueStarId(starName);
        Star newStar = new Star();
        newStar.setId(newId);
        newStar.setName(starName);

        // Add to pending batch
        pendingStars.add(newStar);
        batchStarNameToIdMap.put(starName, newId);

        // Process star batch if we've reached the size limit
        if (pendingStars.size() >= BATCH_SIZE) {
            processPendingStars();
        }

        return newId;
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        try {
            elementStack.pop();
            String value = currentValue.toString().trim();

            switch (qName) {
                case "f":
                    currentMovieFid = value;
                    break;

                case "a":
                    currentStarName = value;
                    break;

                case "m":
                    if (currentMovieFid != null && currentStarName != null &&
                            !currentMovieFid.trim().isEmpty() && !currentStarName.trim().isEmpty()) {

                        String movieId = movieFidToIdMap.get(currentMovieFid.toUpperCase());
                        if (movieId != null) {
                            // Get or create star ID
                            String starId = starNameToIdMap.get(currentStarName);
                            if (starId == null) {
                                starId = handleMissingActor(currentStarName);
                            }

                            if (starId != null) {
                                // Create unique key for this relation
                                String relationKey = starId + "_" + movieId;
                                if (!processedRelations.contains(relationKey)) {
                                    pendingRelations.add(new CastRelation(starId, movieId,
                                            currentStarName, currentMovieFid));
                                    processedRelations.add(relationKey);

                                    // Process relation batch if we've reached the size limit
                                    if (pendingRelations.size() >= BATCH_SIZE) {
                                        // First ensure all stars are inserted
                                        if (!pendingStars.isEmpty()) {
                                            processPendingStars();
                                        }
                                        // Then insert the relations
                                        processPendingRelations();
                                    }
                                }
                            } else {
                                logger.warning("Failed to handle star: " + currentStarName);
                                errorCount++;
                            }
                        } else {
                            logger.warning("Movie FID not found. (FID: " + currentMovieFid + ". StarName: " + currentStarName + ")");
                            errorCount++;
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            logger.warning("Error processing element " + qName + ": " + e.getMessage());
            errorCount++;
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        elementStack.push(qName);
        currentValue.setLength(0);

        if (qName.equals("m")) {
            currentMovieFid = null;
            currentStarName = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        currentValue.append(ch, start, length);
    }

    @Override
    public void endDocument() {
        // Process any remaining pending items
        if (!pendingStars.isEmpty()) {
            processPendingStars();
        }
        if (!pendingRelations.isEmpty()) {
            processPendingRelations();
        }
    }

    public int parseDocument(String filename) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();

            InputStream input = getClass().getClassLoader().getResourceAsStream(filename);
            if (input == null) {
                throw new RuntimeException("Cannot find file: " + filename);
            }

            parser.parse(input, this);
            logger.info("\n\n\n\n\n\nCast parsing completed:");
            int number_insertions = processedCount - castDAO.num_duplicates;
            logger.info("Note: missing stars were handled by simply adding them into the database with their star name and a null dob");
            logger.info("Stars inserted before cast.xml: " + beginning_number_of_stars);
            logger.info("Missing stars inserted during cast.xml: " + (starDAO.number_processed - beginning_number_of_stars));
            logger.info("Total stars inserted after cast.xml: " + starDAO.number_processed);
            logger.info("\nTotal star-movie relations inserted: " + number_insertions);
            logger.info("Total errors encountered (Invalid Information): " + errorCount);
            logger.info("Number of duplicates (Already existing in database or during this session): " + castDAO.num_duplicates);
            return errorCount;

        } catch (Exception e) {
            logger.severe("Error parsing cast document: " + e.getMessage());
            return ++errorCount;
        } finally {
            if (castDAO != null) castDAO.close();
            if (starDAO != null) starDAO.close();
        }
    }
}