import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;

// file sourced from:
// Repository: https://github.com/UCI-Chenli-teaching/cs122b-project2-login-cart-example/tree/main
// File: src/LoginFilter.java

/**
 * Servlet Filter implementation class LoginFilter
 */
@WebFilter(filterName = "LoginFilter", urlPatterns = "/*")
public class LoginFilter implements Filter {
    private final ArrayList<String> allowedURIs = new ArrayList<>();

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        System.out.println("LoginFilter: " + httpRequest.getRequestURI());

        // Check if this URL is allowed to access without logging in
        if (this.isUrlAllowedWithoutLogin(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession();

        if (httpRequest.getRequestURI().contains("_dashboard")) {
            // For dashboard pages, require employee login
            if (session.getAttribute("employee") == null) {
                String contextPath = httpRequest.getContextPath();
                httpResponse.sendRedirect(contextPath + "/_dashboard/login.html");
                return;
            }
        } else {
            // For regular pages, require either user or employee login
            if (session.getAttribute("user") == null && session.getAttribute("employee") == null) {
                String contextPath = httpRequest.getContextPath();
                httpResponse.sendRedirect(contextPath + "/login.html");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isUrlAllowedWithoutLogin(String requestURI) {
        /*
         Setup your own rules here to allow accessing some resources without logging in
         Always allow your own login related requests(html, js, servlet, etc..)
         You might also want to allow some CSS files, etc..
         */
        return allowedURIs.stream().anyMatch(requestURI.toLowerCase()::endsWith);
    }

    public void init(FilterConfig fConfig) {
        allowedURIs.add("login.html");
        allowedURIs.add("login.js");
        allowedURIs.add("api/login");
        // Add employee dashboard URLs
        allowedURIs.add("_dashboard/login.html");
        allowedURIs.add("_dashboard/login.js");
        allowedURIs.add("api/dashboard-login");
    }

    public void destroy() {
        // ignored.
    }

}