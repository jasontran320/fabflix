package app;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;


@WebServlet("/api/dashboard/metadata")
public class MetadataServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/MySQLReadOnly");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        JsonObject jsonResponse = new JsonObject();
        JsonArray tables = new JsonArray();

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metadata = conn.getMetaData();
            // Get all tables without filtering
            ResultSet tableRs = metadata.getTables("moviedb", null, null, new String[]{"TABLE"});

            while (tableRs.next()) {
                String tableName = tableRs.getString("TABLE_NAME");
                JsonObject tableObj = new JsonObject();
                tableObj.addProperty("name", tableName);

                JsonArray columns = new JsonArray();
                ResultSet columnRs = metadata.getColumns("moviedb", null, tableName, null);

                while (columnRs.next()) {
                    JsonObject column = new JsonObject();
                    column.addProperty("name", columnRs.getString("COLUMN_NAME"));
                    column.addProperty("type", columnRs.getString("TYPE_NAME"));
                    columns.add(column);
                }

                tableObj.add("columns", columns);
                tables.add(tableObj);
            }

            jsonResponse.addProperty("status", "success");
            jsonResponse.add("tables", tables);

        } catch (Exception e) {
            response.setStatus(500);
            jsonResponse.addProperty("status", "error");
            jsonResponse.addProperty("message", e.getMessage());
        }

        response.getWriter().write(jsonResponse.toString());
    }
}