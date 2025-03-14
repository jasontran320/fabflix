package app;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/save-page")
public class SavePageServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            HttpSession session = request.getSession();
            String page = request.getParameter("page");
            if (page == null || page.trim().isEmpty()) {
                response.setStatus(400);
                return;
            }
            session.setAttribute("lastPage", page);
            response.setContentType("text/plain");
            response.setStatus(200);
        } catch (Exception e) {
            response.setStatus(500);
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            response.setContentType("text/plain");
            HttpSession session = request.getSession();
            String lastPage = (String) session.getAttribute("lastPage");
            PrintWriter out = response.getWriter();
            out.write(lastPage != null ? lastPage : "");
            out.close();
        } catch (Exception e) {
            response.setStatus(500);
            e.printStackTrace();
        }
    }
}