package src;

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

@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;
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

        // Get star ID from request parameter
        String id = request.getParameter("id");
        request.getServletContext().log("getting id: " + id);

        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT s.id as starId, s.name, s.birthYear, " +
                    "m.id as movieId, m.title, m.year, m.director " +
                    "FROM stars s " +
                    "JOIN stars_in_movies sim ON s.id = sim.starId " +
                    "JOIN movies m ON sim.movieId = m.id " +
                    "WHERE s.id = ? " +
                    "ORDER BY m.year DESC, m.title";

            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, id);

            ResultSet rs = statement.executeQuery();
            JsonArray jsonArray = new JsonArray();

            while (rs.next()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("star_id", rs.getString("starId"));
                jsonObject.addProperty("star_name", rs.getString("name"));
                jsonObject.addProperty("star_dob", rs.getString("birthYear"));
                jsonObject.addProperty("movie_id", rs.getString("movieId"));
                jsonObject.addProperty("movie_title", rs.getString("title"));
                jsonObject.addProperty("movie_year", rs.getString("year"));
                jsonObject.addProperty("movie_director", rs.getString("director"));

                jsonArray.add(jsonObject);
            }

            rs.close();
            statement.close();

            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            request.getServletContext().log("Error:", e);
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}