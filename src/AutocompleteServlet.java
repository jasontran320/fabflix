import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;



@WebServlet("/api/autocomplete")
public class AutocompleteServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            String query = request.getParameter("query");

            if (query == null || query.trim().length() < 3) {
                out.write("[]");
                return;
            }

            String[] words = query.trim().split("\\s+");
            StringBuilder searchPattern = new StringBuilder();
            for (String word : words) {
                searchPattern.append(" +").append(word).append("*");
            }

            String sql = "SELECT DISTINCT id, title FROM movies " +
                    "WHERE MATCH(title) AGAINST(? IN BOOLEAN MODE) " +
                    "ORDER BY title LIMIT 10";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, searchPattern.toString());
                ResultSet rs = stmt.executeQuery();

                JsonArray jsonArray = new JsonArray();
                while (rs.next()) {
                    JsonObject suggestion = new JsonObject();
                    suggestion.addProperty("value", rs.getString("title"));

                    JsonObject data = new JsonObject();
                    data.addProperty("movieId", rs.getString("id"));
                    suggestion.add("data", data);

                    jsonArray.add(suggestion);
                }
                out.write(jsonArray.toString());
            }
        } catch (Exception e) {
            response.sendError(500, e.getMessage());
        }
        out.close();
    }
}