package XMLParser.src.main.java.XMLParser.models;

public class Star {
    private String id;          // VARCHAR(10)
    private String name;        // VARCHAR(100)
    private Integer birthYear;  // INT (nullable)

    public Star() {}

    public Star(String id, String name, Integer birthYear) {
        this.id = id;
        this.name = name;
        this.birthYear = birthYear;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }

    @Override
    public String toString() {
        return "Star{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", birthYear=" + birthYear +
                '}';
    }
}
