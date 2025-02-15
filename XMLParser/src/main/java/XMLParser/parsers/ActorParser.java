package XMLParser.src.main.java.XMLParser.parsers;

import XMLParser.src.main.java.XMLParser.dao.StarDAO;
import XMLParser.src.main.java.XMLParser.models.Star;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

public class ActorParser extends DefaultHandler {
    private StarDAO starDAO;
    private StringBuilder currentValue;
    private Star currentStar;
    private Stack<String> elementStack;
    private int processedCount;
    private int errorCount;
    private Logger logger;
    private HashMap<String, String> starNameToIdMap;  // This is for unique stage names only

    // Batch processing fields
    private List<Star> starBatch;
    private static final int BATCH_SIZE = 200;
    private HashMap<String, String> batchNameToIdMap;  // This is for unique stage names only

    public ActorParser(HashMap<String, String> starNameToIdMap, Logger logger, StarDAO starDAO) {
        this.starDAO = starDAO;
        this.currentValue = new StringBuilder();
        this.elementStack = new Stack<>();
        this.processedCount = 0;
        this.errorCount = 0;
        this.logger = logger;
        this.starNameToIdMap = starNameToIdMap;

        // Initialize batch processing structures
        this.starBatch = new ArrayList<>();
        this.batchNameToIdMap = new HashMap<>();
    }

    private boolean isValidStar(Star star) {
        if (star == null) {
            logger.warning("Star object is null");
            return false;
        }

        if (star.getId() == null || star.getId().trim().isEmpty()) {
            logger.warning("Star ID is missing: " + star);
            return false;
        }

        if (star.getName() == null || star.getName().trim().isEmpty()) {
            logger.warning("Name is missing for star ID: " + star);
            return false;
        }

        return true;
    }

    private void processBatch() {
        try {
            boolean success = starDAO.insertStarBatch(starBatch);

            if (success) {
                // Update name map only for new entries. This is maybe handeld in starDAO.java already.
                /*
                for (Star star : starBatch) {
                    if (!starNameToIdMap.containsKey(star.getName())) {
                        starNameToIdMap.put(star.getName(), star.getId());
                    }
                }

                 */
                processedCount += starBatch.size();
            }
        } catch (Exception e) {
            logger.severe("Error processing batch: " + e.getMessage());
            errorCount += starBatch.size();
        } finally {
            // Clear batch data structures
            starBatch.clear();
            batchNameToIdMap.clear();
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        try {
            elementStack.push(qName);
            currentValue.setLength(0);

            if (qName.equals("actor")) {
                currentStar = new Star();
            }
        } catch (Exception e) {
            logger.warning("Error in startElement: " + e.getMessage());
            errorCount++;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        try {
            String currentElement = elementStack.pop();
            String value = currentValue.toString().trim();

            if (qName.equals("actor")) {
                if (currentStar != null && currentStar.getName() != null && !currentStar.getName().trim().isEmpty()) {
                    // Generate/get ID after we have all star info
                    currentStar.setId(starDAO.generateUniqueStarId(currentStar.getName(), currentStar.getBirthYear()));

                    if (isValidStar(currentStar)) {
                        // Only add to batch if we don't already have this name in our maps
                        if (!starNameToIdMap.containsKey(currentStar.getName()) &&
                                !batchNameToIdMap.containsKey(currentStar.getName())) {

                            starBatch.add(currentStar);
                            batchNameToIdMap.put(currentStar.getName(), currentStar.getId());

                            if (starBatch.size() >= BATCH_SIZE) {
                                processBatch();
                            }
                        } else {
                            logger.warning("Already contains " + currentStar.toString());
                            starDAO.num_duplicates++;
                        }
                    } else {
                        logger.warning("Skipping invalid star");
                        errorCount++;
                    }
                } else {
                    logger.warning("Skipping star with missing name");
                    errorCount++;
                }
                currentStar = null;
            } else if (currentStar != null) {
                try {
                    switch (qName) {
                        case "stagename":
                            if (value != null && !value.isEmpty()) {
                                currentStar.setName(value);
                            }
                            break;

                        case "dob":
                            if (value != null && !value.isEmpty() && value.matches("\\d{4}")) {
                                try {
                                    int year = Integer.parseInt(value);
                                    if (year > 1800 && year <= 2024) {
                                        currentStar.setBirthYear(year);
                                    } else {
                                        currentStar.setBirthYear(null);
                                    }
                                } catch (NumberFormatException e) {
                                    currentStar.setBirthYear(null);
                                }
                            }
                            break;
                    }
                } catch (Exception e) {
                    logger.warning("Error processing element " + qName + " with value: " + value);
                    errorCount++;
                }
            }
        } catch (Exception e) {
            logger.warning("Error in endElement: " + e.getMessage());
            errorCount++;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        currentValue.append(ch, start, length);
    }

    @Override
    public void endDocument() {
        // Process any remaining stars in the final batch
        if (!starBatch.isEmpty()) {
            processBatch();
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
            logger.info("\n\n\n\n\n\nParsing completed:");
            logger.info("Total actors inserted: " + processedCount);

            logger.info("Total errors encountered (missing vital information): " + errorCount);
            logger.info("Number of duplicates (Already existing in database or during this session): " + starDAO.num_duplicates);
            return errorCount;

        } catch (Exception e) {
            logger.severe("Error parsing document: " + e.getMessage());
            return ++errorCount;
        }
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public int getErrorCount() {
        return errorCount;
    }
}