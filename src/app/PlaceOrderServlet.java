package app;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.jsonwebtoken.Claims;

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
import java.sql.*;
import java.util.Map;
import java.util.HashMap;


@WebServlet("/api/order")
public class PlaceOrderServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/MySQLReadWrite");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String creditCard = request.getParameter("creditCard");
        String expiration = request.getParameter("expiration");

        JsonObject responseJsonObject = new JsonObject();

        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Authentication required");
            response.getWriter().write(responseJsonObject.toString());
            return;
        }

        String userEmail = claims.getSubject();
        String userId = claims.get("userId", String.class);

        try (Connection conn = dataSource.getConnection()) {
            // Verify credit card
            String ccQuery = "SELECT id FROM creditcards WHERE id = ? AND firstName = ? " +
                    "AND lastName = ? AND expiration = ?";

            try (PreparedStatement ccStmt = conn.prepareStatement(ccQuery)) {
                ccStmt.setString(1, creditCard);
                ccStmt.setString(2, firstName);
                ccStmt.setString(3, lastName);
                ccStmt.setDate(4, Date.valueOf(expiration));

                try (ResultSet ccRs = ccStmt.executeQuery()) {
                    if (!ccRs.next()) {
                        responseJsonObject.addProperty("status", "fail");
                        responseJsonObject.addProperty("message", "Invalid credit card information");
                        response.getWriter().write(responseJsonObject.toString());
                        return;
                    }
                }
            }

            // Get cart
            HttpSession session = request.getSession();
            String cartKey = "cart_" + userEmail;

            Map<String, Integer> cart;
            synchronized (session) {
                cart = (Map<String, Integer>) session.getAttribute(cartKey);
            }

            if (cart == null) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Cart is empty");
                response.getWriter().write(responseJsonObject.toString());
                return;
            }

            // Snapshot cart under lock
            Map<String, Integer> cartSnapshot;
            synchronized (cart) {
                if (cart.isEmpty()) {
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Cart is empty");
                    response.getWriter().write(responseJsonObject.toString());
                    return;
                }
                cartSnapshot = new HashMap<>(cart);
            }

            // Transaction
            conn.setAutoCommit(false);

            try {
                JsonArray orderItems = new JsonArray();
                double totalPrice = 0.0;

                String movieQuery = "SELECT title, price FROM movies WHERE id = ?";
                String insertSale = "INSERT INTO sales (customerId, movieId, quantity, saleDate) VALUES (?, ?, ?, NOW())";

                try (PreparedStatement movieStmt = conn.prepareStatement(movieQuery);
                     PreparedStatement saleStmt = conn.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS)) {

                    for (Map.Entry<String, Integer> entry : cartSnapshot.entrySet()) {
                        String movieId = entry.getKey();
                        int quantity = entry.getValue();
                        String title;
                        double price;
                        movieStmt.setString(1, movieId);
                        try (ResultSet movieRs = movieStmt.executeQuery()) {
                            if (!movieRs.next()) {
                                throw new SQLException("Movie not found: " + movieId);
                            }
                            title = movieRs.getString("title");
                            price = movieRs.getDouble("price");
                        }

                        // Insert sale
                        saleStmt.setString(1, userId);
                        saleStmt.setString(2, movieId);
                        saleStmt.setInt(3, quantity);
                        saleStmt.executeUpdate();

                        int saleId;
                        try (ResultSet saleRs = saleStmt.getGeneratedKeys()) {
                            if (!saleRs.next()) {
                                throw new SQLException("Failed to retrieve saleId for movie: " + movieId);
                            }
                            saleId = saleRs.getInt(1);
                        }

                        JsonObject item = new JsonObject();
                        item.addProperty("movieId", movieId);
                        item.addProperty("title", title);
                        item.addProperty("quantity", quantity);
                        item.addProperty("price", price);
                        item.addProperty("saleId", saleId);
                        orderItems.add(item);

                        totalPrice += price * quantity;
                    }
                }

                conn.commit();

                JsonObject orderData = new JsonObject();
                orderData.add("items", orderItems);
                orderData.addProperty("totalPrice", totalPrice);

                responseJsonObject.addProperty("status", "success");
                responseJsonObject.add("orderData", orderData);

                // Clear cart only after commit
                synchronized (session) {
                    session.removeAttribute(cartKey);
                }


            } catch (Exception e) {
                try { conn.rollback(); } catch (SQLException ignored) {}

                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Server error: " + e.getMessage());

            } finally {
                // Set back to autocommit
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }

        } catch (Exception e) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Server error: " + e.getMessage());
        }

        response.getWriter().write(responseJsonObject.toString());
    }
}
