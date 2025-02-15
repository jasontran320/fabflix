
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/api/check-employee")
public class EmployeeCheckServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject responseJsonObject = new JsonObject();
        HttpSession session = request.getSession();

        Boolean isEmployee = (Boolean) session.getAttribute("isEmployee");
        responseJsonObject.addProperty("isEmployee", isEmployee != null && isEmployee);

        response.getWriter().write(responseJsonObject.toString());
    }
}