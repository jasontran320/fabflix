package XMLParser.src.main.java.XMLParser.models;

import java.util.Objects;

public class CastRelation {
    private String starId;
    private String movieId;
    private String starName;  // For tracking purposes
    private String movieFid;  // For tracking purposes

    public CastRelation(String starId, String movieId, String starName, String movieFid) {
        this.starId = starId;
        this.movieId = movieId;
        this.starName = starName;
        this.movieFid = movieFid;
    }

    // Getters
    public String getStarId() { return starId; }
    public String getMovieId() { return movieId; }
    public String getStarName() { return starName; }
    public String getMovieFid() { return movieFid; }

    // For tracking unique combinations
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CastRelation that = (CastRelation) o;
        return Objects.equals(starId, that.starId) &&
                Objects.equals(movieId, that.movieId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(starId, movieId);
    }
}