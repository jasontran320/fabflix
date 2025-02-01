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
import java.sql.*;
import java.util.ArrayList;
import java.util.Map;


@WebServlet("/api/order")
public class PlaceOrderServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
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

        try (Connection conn = dataSource.getConnection()) {
            // Verify credit card
            String ccQuery = "SELECT id FROM creditcards WHERE id = ? AND firstName = ? " +
                    "AND lastName = ? AND expiration = ?";
            PreparedStatement ccStmt = conn.prepareStatement(ccQuery);
            ccStmt.setString(1, creditCard);
            ccStmt.setString(2, firstName);
            ccStmt.setString(3, lastName);
            ccStmt.setDate(4, Date.valueOf(expiration));

            if (ccStmt.executeQuery().next()) {
                // Get cart
                HttpSession session = request.getSession();
                Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute("cart");

                if (cart != null && !cart.isEmpty()) {
                    // Create JsonArray for order details
                    JsonArray orderItems = new JsonArray();
                    String customerId = ((User) session.getAttribute("user")).getId();

                    // Get full movie details for each cart item
                    String movieQuery = "SELECT title, price FROM movies WHERE id = ?";
                    PreparedStatement movieStmt = conn.prepareStatement(movieQuery);

                    // Insert sales and collect data
                    String insertSale = "INSERT INTO sales (customerId, movieId, quantity, saleDate) VALUES (?, ?, ?, NOW())";
                    PreparedStatement saleStmt = conn.prepareStatement(insertSale, Statement.RETURN_GENERATED_KEYS);

                    double totalPrice = 0.0;

                    // Inside the for loop, after getting the sale ID:
                    for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                        String movieId = entry.getKey();
                        Integer quantity = entry.getValue();

                        // Get movie details
                        movieStmt.setString(1, movieId);
                        ResultSet movieRs = movieStmt.executeQuery();

                        if (movieRs.next()) {
                            // Record sale first to get the ID
                            saleStmt.setString(1, customerId);
                            saleStmt.setString(2, movieId);
                            saleStmt.setInt(3, quantity);
                            saleStmt.executeUpdate();

                            // Get sale ID
                            ResultSet saleRs = saleStmt.getGeneratedKeys();
                            int saleId = 0;
                            if (saleRs.next()) {
                                saleId = saleRs.getInt(1);
                            }

                            // Create order item with all details including saleId
                            JsonObject item = new JsonObject();
                            item.addProperty("movieId", movieId);
                            item.addProperty("title", movieRs.getString("title"));
                            item.addProperty("quantity", quantity);
                            item.addProperty("price", movieRs.getDouble("price"));
                            item.addProperty("saleId", saleId);
                            orderItems.add(item);

                            totalPrice += movieRs.getDouble("price") * quantity;
                        }
                    }

                    JsonObject orderData = new JsonObject();
                    orderData.add("items", orderItems);
                    orderData.addProperty("totalPrice", totalPrice);

                    responseJsonObject.addProperty("status", "success");
                    responseJsonObject.add("orderData", orderData);

                    // Clear cart after everything is done
                    session.removeAttribute("cart");
                } else {
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "Cart is empty");
                }
            } else {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Invalid credit card information");
            }
        } catch (Exception e) {
            responseJsonObject.addProperty("status", "fail");
            responseJsonObject.addProperty("message", "Server error: " + e.getMessage());
        }

        response.getWriter().write(responseJsonObject.toString());
    }
}