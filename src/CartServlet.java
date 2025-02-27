import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;


@WebServlet("/api/cart")
public class CartServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/MySQLReadOnly");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        JsonObject responseJsonObject = new JsonObject();

        // Get cart from session
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
        }

        // Convert cart to JSON array with movie details
        JsonArray cartItems = new JsonArray();
        double totalPrice = 0;

        try (Connection conn = dataSource.getConnection()) {
            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                String movieId = entry.getKey();
                Integer quantity = entry.getValue();

                String query = "SELECT title, price FROM movies WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, movieId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    JsonObject item = new JsonObject();
                    item.addProperty("movieId", movieId);
                    item.addProperty("title", rs.getString("title"));
                    item.addProperty("quantity", quantity);
                    item.addProperty("price", rs.getDouble("price"));
                    totalPrice += rs.getDouble("price") * quantity;
                    cartItems.add(item);
                }
            }
            responseJsonObject.add("items", cartItems);
            responseJsonObject.addProperty("totalPrice", totalPrice);
        } catch (Exception e) {
            response.setStatus(500);
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Server error: " + e.getMessage());
        }

        response.getWriter().write(responseJsonObject.toString());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        String movieId = request.getParameter("movieId");
        String action = request.getParameter("action");
        Integer quantity = request.getParameter("quantity") != null ?
                Integer.parseInt(request.getParameter("quantity")) : 1;

        System.out.println("POST request received:");
        System.out.println("movieId: " + movieId);
        System.out.println("action: " + action);
        System.out.println("quantity: " + quantity);

        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute("cart", cart);
            System.out.println("Created new cart");
        }

        System.out.println("Cart before: " + cart);
        JsonObject responseJsonObject = new JsonObject();

        synchronized (cart) {
            try {
                switch (action) {
                    case "add":
                        cart.merge(movieId, quantity, Integer::sum);
                        break;
                    case "update":
                        if (quantity > 0) cart.put(movieId, quantity);
                        break;
                    case "remove":
                        cart.remove(movieId);
                        break;
                }
                System.out.println("Cart after: " + cart);
                responseJsonObject.addProperty("status", "success");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", e.getMessage());
            }
        }

        response.getWriter().write(responseJsonObject.toString());
    }
}