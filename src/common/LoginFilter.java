package common;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

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
            // Keep default action: pass along the filter chain
            chain.doFilter(request, response);
            return;
        }

        String token = JwtUtil.getCookieValue(httpRequest, "jwtToken");
        Claims claims = JwtUtil.validateToken(token);

        if (claims != null) {
            // Store claims in request attributes for downstream servlets
            httpRequest.setAttribute("claims", claims);

            // Check if request is for dashboard area
            if (httpRequest.getRequestURI().contains("_dashboard")) {
                // Check if user is an employee
                Boolean isEmployee = claims.get("isEmployee", Boolean.class);
                if (isEmployee != null && isEmployee) {
                    // Proceed with the employee request
                    chain.doFilter(request, response);
                } else {
                    // Not an employee, redirect to employee login
                    httpResponse.sendRedirect(httpRequest.getContextPath() + "/_dashboard/login.html");
                }
            } else {
                // Regular user area - allow both regular users and employees
                chain.doFilter(request, response);
            }
        } else {
            // No valid JWT token
            if (httpRequest.getRequestURI().contains("_dashboard")) {
                // Redirect to employee login page
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/_dashboard/login.html");
            } else {
                // Redirect to regular login page
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.html");
            }
        }
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