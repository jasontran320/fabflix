public class User {
    private final String username;
    private String id;

    public User(String username) {
        this.username = username;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}