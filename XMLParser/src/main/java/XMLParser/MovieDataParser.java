package XMLParser.src.main.java.XMLParser;

import XMLParser.src.main.java.XMLParser.parsers.ActorParser;
import XMLParser.src.main.java.XMLParser.parsers.CastParser;
import XMLParser.src.main.java.XMLParser.parsers.MovieParser;
import XMLParser.src.main.java.XMLParser.dao.StarDAO;
// Will import other parsers later
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.*;

public class MovieDataParser {

    private HashMap<String, String> starNameToIdMap;

    private HashMap<String, String> movieFidToIdMap;
    private StarDAO starDAO;
    private static Logger logger;

    private void setupLogging() {
        try {
            this.logger = Logger.getLogger("MovieDataParser");
            FileHandler fh = new FileHandler("parser_log.txt");

            // Custom formatter with different formats based on level
            fh.setFormatter(new SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    if (record.getLevel() == Level.INFO) {
                        return record.getMessage() + "\n";
                    } else {
                        // Keep default format for warnings
                        return super.format(record);
                    }
                }
            });

            logger.removeHandler(Arrays.stream(logger.getHandlers())
                    .findFirst()
                    .orElse(null));
            logger.addHandler(fh);
            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            System.err.println("Could not setup logging: " + e.getMessage());
        }
    }

    public MovieDataParser() {
        setupLogging();
        this.starNameToIdMap = new HashMap<>();
        this.movieFidToIdMap = new HashMap<>();
        this.starDAO = new StarDAO(logger, starNameToIdMap);
    }

    public void parseAll() {
        System.out.println("Starting XML parsing process...");

        // Track overall statistics
        long startTime = System.currentTimeMillis();
        int totalErrors = 0;

        try {

            // 1. Parse Actors first
            System.out.println("\nParsing actors63.xml...");
            logger.info("\nParsing actors63.xml...\n\n\n\n\n");
            ActorParser actorParser = new ActorParser(starNameToIdMap, logger, starDAO);
            int actorErrors = actorParser.parseDocument("actors63.xml");
            totalErrors += actorErrors;



            // 2. Will add Movies parsing here
            // movies need to exist before we can link them to stars
            System.out.println("\nParsing mains243.xml...");
            logger.info("\n\n\n\n\n\n\n___________________________________________________________________________________________");
            logger.info("\nParsing mains243.xml...\n\n\n\n\n");
            MovieParser movieParser = new MovieParser(movieFidToIdMap, logger);
            int movieErrors = movieParser.parseDocument("mains243.xml");
            totalErrors += movieErrors;




            // 3. Will add Casts parsing here
            // needs both movies and actors to be parsed first




            System.out.println("\nParsing casts124.xml...");
            logger.info("\n\n\n\n\n\n\n___________________________________________________________________________________________");
            logger.info("\nParsing casts124.xml...\n\n\n\n\n");
            CastParser castParser = new CastParser(starNameToIdMap, movieFidToIdMap, logger, starDAO);
            int castErrors = castParser.parseDocument("casts124.xml");
            totalErrors += castErrors;



            long endTime = System.currentTimeMillis();
            System.out.println("\nParsing of every xml file complete:");
            System.out.println("Total time: " + (endTime - startTime) / 1000.0 + " seconds");
            System.out.println("Total errors: " + totalErrors);
            logger.info("\n\n\n\n\n\n\n___________________________________________________________________________________________");
            logger.info("\n\n\n\n\n\nParsing of every xml file complete:");
            logger.info("Total time: " + (endTime - startTime) / 1000.0 + " seconds");
            logger.info("Total errors: " + totalErrors);
            //logger.info(String.valueOf(movieFidToIdMap.size()));
            //logger.info(String.valueOf(starNameToIdMap.size()));


        } catch (Exception e) {
            System.err.println("Critical error during parsing:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        MovieDataParser parser = new MovieDataParser();
        parser.parseAll();
    }
}