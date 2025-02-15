package XMLParser.src.main.java.XMLParser.models;

public class Movie {
    private String id;          // VARCHAR(10)
    private String title;       // VARCHAR(100)
    private int year;          // INT
    private String director;    // VARCHAR(100)
    private double price;      // DECIMAL(10,2)
    private String fid;

    public Movie() {}

    public Movie(String id, String title, int year, String director, double price) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.director = director;
        this.price = price;
        this.fid = null;
    }

    // Getters and Setters
    public String getFid() { return fid; }
    public void setFid(String fid) { this.fid = fid.toUpperCase(); }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    @Override
    public String toString() {
        return "Movie{" +
                "fid='" + fid + '\'' +
                ", title='" + title + '\'' +
                ", year=" + year +
                ", director='" + director + '\'' +
                ", price=" + price +
                '}';
    }
}
