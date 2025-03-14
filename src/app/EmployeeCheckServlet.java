package app;

import com.google.gson.JsonObject;
import io.jsonwebtoken.Claims;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/check-employee")
public class EmployeeCheckServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        JsonObject responseJsonObject = new JsonObject();

        // Get JWT claims from request attributes (set by LoginFilter)
        Claims claims = (Claims) request.getAttribute("claims");

        // Check if user is an employee based on JWT claims
        Boolean isEmployee = false;
        if (claims != null) {
            isEmployee = claims.get("isEmployee", Boolean.class);
            if (isEmployee == null) {
                isEmployee = false;
            }
        }

        responseJsonObject.addProperty("isEmployee", isEmployee);
        response.getWriter().write(responseJsonObject.toString());
    }
}