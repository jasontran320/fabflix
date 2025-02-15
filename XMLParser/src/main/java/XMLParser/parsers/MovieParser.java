package XMLParser.src.main.java.XMLParser.parsers;

import XMLParser.src.main.java.XMLParser.dao.MovieDAO;
import XMLParser.src.main.java.XMLParser.models.Movie;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;

public class MovieParser extends DefaultHandler {
    private MovieDAO movieDAO;
    private StringBuilder currentValue;
    private Movie currentMovie;
    private Stack<String> elementStack;
    private int processedCount;
    private int errorCount;
    private Logger logger;
    private List<String> currentGenres;
    private String currentFid;
    private HashMap<String, String> movieFidToIdMap;
    private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    private static final int MIN_YEAR = 1800;
    private static final int MAX_YEAR = 2024;
    private String currentDirector;

    // Batch processing fields
    private List<Movie> movieBatch;
    private Map<String, List<String>> movieGenresMap;
    private Map<String, String> batchFidToIdMap;
    private static final int BATCH_SIZE = 100;

    public MovieParser(HashMap<String, String> movieFidToIdMap, Logger logger) {
        this.movieDAO = new MovieDAO(logger);
        this.currentValue = new StringBuilder();
        this.elementStack = new Stack<>();
        this.processedCount = 0;
        this.errorCount = 0;
        this.logger = logger;
        this.currentGenres = new ArrayList<>();
        this.movieFidToIdMap = movieFidToIdMap;
        this.currentDirector = null;

        // Initialize batch processing structures
        this.movieBatch = new ArrayList<>();
        this.movieGenresMap = new HashMap<>();
        this.batchFidToIdMap = new HashMap<>();
    }

    private boolean isValidMovie(Movie movie) {
        if (movie == null) {
            logger.warning("Movie object is null");
            return false;
        }

        if (movie.getId() == null || movie.getId().trim().isEmpty()) {
            logger.warning("Movie ID is missing: " + movie);
            return false;
        }

        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            logger.warning("Title is missing for movie FID: " + movie);
            return false;
        }

        if (movie.getDirector() == null || movie.getDirector().trim().isEmpty()) {
            logger.warning("Director is missing for movie: " + movie);
            return false;
        }

        if (movie.getYear() <= 0) {
            logger.warning("Invalid year for movie: " + movie);
            return false;
        }

        if (movie.getPrice() < 0) {
            logger.warning("Invalid price for movie: " + movie);
            return false;
        }

        return true;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        try {
            elementStack.push(qName);
            currentValue.setLength(0);

            switch(qName) {
                case "directorfilms":
                    currentDirector = null;
                    break;
                case "film":
                    currentMovie = new Movie();
                    currentMovie.setPrice(5 + (Math.random() * 15));
                    if (currentDirector != null) {
                        currentMovie.setDirector(currentDirector);
                    }
                    break;
                case "cats":
                    currentGenres.clear();
                    break;
            }
        } catch (Exception e) {
            logger.warning("Error in startElement: " + e.getMessage());
            errorCount++;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        try {
            elementStack.pop();
            String value = new String(currentValue.toString().trim().getBytes(), ISO_8859_1);

            switch (qName) {
                case "dirname":
                    if (!value.isEmpty()) {
                        currentDirector = value;
                    }
                    break;

                case "film":
                    if (currentFid == null) {
                        logger.warning("Skipping movie: No FID tag: " + currentMovie);
                        errorCount++;
                    }
                    else if (currentMovie == null) {
                        logger.warning("Skipping movie: No opening film tag");
                        errorCount++;
                    } else if (currentFid != null &&
                            (movieFidToIdMap.containsKey(currentFid.toUpperCase()) ||
                                    batchFidToIdMap.containsKey(currentFid.toUpperCase()))) {
                        logger.warning("This movie already exists: " + currentMovie);
                        movieDAO.num_duplicates++;
                    } else {
                        // Generate ID here when we have all movie info
                        String newId = movieDAO.generateUniqueMovieId(
                                currentMovie.getTitle(),
                                currentMovie.getYear(),
                                currentMovie.getDirector()
                        );
                        currentMovie.setId(newId);
                        currentMovie.setFid(currentFid);

                        if (isValidMovie(currentMovie)) {
                            movieBatch.add(currentMovie);
                            if (!currentGenres.isEmpty()) {
                                movieGenresMap.put(newId, new ArrayList<>(currentGenres));
                            }
                            if (currentFid != null) {
                                batchFidToIdMap.put(currentFid.toUpperCase(), newId);
                            }

                            if (movieBatch.size() >= BATCH_SIZE) {
                                processBatch();
                            }
                            processedCount++;
                        } else {
                            logger.info("Skipping invalid movie due to missing required fields");
                            errorCount++;
                        }
                    }
                    currentMovie = null;
                    currentFid = null;
                    currentGenres.clear();
                    break;

                case "fid":
                    if (!value.isEmpty()) {
                        currentFid = value;
                    }
                    break;

                case "t":
                    if (!value.isEmpty()) {
                        currentMovie.setTitle(value);
                    }
                    break;

                case "year":
                    try {
                        if (value.matches("\\d+")) {
                            int year = Integer.parseInt(value);
                            if (year >= MIN_YEAR && year <= MAX_YEAR) {
                                currentMovie.setYear(year);
                            }
                        }
                    } catch (NumberFormatException e) {
                        logger.warning("Invalid year format: " + value);
                    }
                    break;

                case "cat":
                    if (!value.isEmpty()) {
                        currentGenres.add(value.trim().toLowerCase());
                    }
                    break;
            }
        } catch (Exception e) {
            logger.warning("Error processing element <" + qName + ">: " + currentMovie);
            errorCount++;
        }
    }

    private void processBatch() {
        try {
            List<Movie> newMovies = new ArrayList<>();
            boolean success = movieDAO.insertMovieBatch(movieBatch, newMovies, batchFidToIdMap);

            if (success) {
                // Only update the main FID map after successful insertion
                movieFidToIdMap.putAll(batchFidToIdMap);

                // Process genres for successfully inserted movies
                for (Movie movie : newMovies) {
                    List<String> genres = movieGenresMap.get(movie.getId());
                    if (genres != null && !genres.isEmpty()) {
                        movieDAO.insertMovieGenreBatch(movie.getId(), genres);
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Error processing batch: " + e.getMessage());
            errorCount += movieBatch.size();
        } finally {
            // Clear batch data structures
            movieBatch.clear();
            movieGenresMap.clear();
            batchFidToIdMap.clear();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        currentValue.append(ch, start, length);
    }

    @Override
    public void endDocument() {
        // Process any remaining movies in the final batch
        if (!movieBatch.isEmpty()) {
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
            int number_insertions = processedCount - movieDAO.num_redirects;
            logger.info("Total movies inserted: " + number_insertions);
            logger.info("Total errors encountered (Invalid information): " + errorCount);
            logger.info("Number of duplicates (Already existing in database or during this session): " + movieDAO.num_duplicates);
            logger.info("Number of genres inserted: " + movieDAO.num_genres_inserted);
            logger.info("Number of movie genres inserted: " + movieDAO.num_movie_genres_inserted);
            return errorCount;

        } catch (Exception e) {
            logger.severe("Error parsing document: " + e.getMessage());
            return ++errorCount;
        } finally {
            movieDAO.close();
        }
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public int getErrorCount() {
        return errorCount;
    }
}

