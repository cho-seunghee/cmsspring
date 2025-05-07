package com.boot.cms.service.mapview;

import com.boot.cms.util.EscapeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DynamicQueryService {

    private final DataSource dataSource;


    private String createCallString(String procedureName, int paramCount) {
        String placeholders = String.join(",", Collections.nCopies(paramCount, "?")); // "?, ?, ..."
        return "{CALL " + procedureName + "(" + placeholders + ")}";
    }

    public List<Map<String, Object>> executeDynamicQuery(String procedureCall) {
        List<Map<String, Object>> mappedResult = new ArrayList<>();
        Connection connection = null;
        CallableStatement stmt = null;
        ResultSet rs = null;

        try {
            // Log the procedure call for debugging
            System.out.println("Executing procedure call: " + procedureCall);

            // Parse procedure name and parameters (e.g., "UP_BOARDCATEGORYINFO_SELECT('F')" -> name: "UP_BOARDCATEGORYINFO_SELECT", params: ["F"])
            String procedureName = procedureCall.substring(0, procedureCall.indexOf('(')).trim();
            String paramString = procedureCall.substring(procedureCall.indexOf('(') + 1, procedureCall.lastIndexOf(')')).trim();
            System.out.println("Procedure Name: " + paramString);

            List<String> params = parseParameters(paramString);

            System.out.println("Procedure Name: " + params);


            // Create placeholder string for CallableStatement (e.g., "{CALL UP_BOARDCATEGORYINFO_SELECT(?)}")
            //String callString = "{CALL " + procedureName + "(" + String.join(",", Collections.nCopies(params.size(), "?")) + ")}";
            String callString = createCallString(procedureName, params.size());

            // Get JDBC connection from DataSource
            connection = DataSourceUtils.getConnection(dataSource);
            if (connection == null) {
                throw new IllegalStateException("Unable to obtain JDBC Connection from DataSource");
            }

            // Prepare stored procedure call
            stmt = connection.prepareCall(callString);

            // Bind parameters
            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }

            // Execute query
            boolean hasResultSet = stmt.execute();
            if (!hasResultSet) {
                System.out.println("No ResultSet returned by procedure: " + procedureName);
                return Collections.emptyList();
            }

            rs = stmt.getResultSet();
            if (rs == null) {
                System.out.println("ResultSet is null for procedure: " + procedureName);
                return Collections.emptyList();
            }

            // Get column names from ResultSetMetaData
            List<String> columnNames = getColumnNames(rs);

            // Process result set
            while (rs.next()) {
                Map<String, Object> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < columnNames.size(); i++) {
                    Object value = rs.getObject(i + 1);
                    rowMap.put(columnNames.get(i), value == null ? "" : value);
                }
                mappedResult.add(rowMap);
            }

            // Log result size
            System.out.println("Retrieved " + mappedResult.size() + " rows with columns: " + columnNames);

        } catch (Exception e) {
            System.err.println("Error executing stored procedure: " + procedureCall + ", Error: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            // Clean up resources
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                DataSourceUtils.releaseConnection(connection, dataSource);
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }

        return mappedResult;
    }

    private List<String> getColumnNames(ResultSet rs) {
        List<String> columnNames = new ArrayList<>();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            if (columnCount == 0) {
                System.out.println("No columns found in ResultSet");
                columnNames.add("col1"); // Minimal fallback
                return columnNames;
            }
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i) != null ? metaData.getColumnLabel(i) : metaData.getColumnName(i);
                columnNames.add(columnName);
            }
        } catch (Exception e) {
            System.err.println("Failed to extract column names: " + e.getMessage());
            // Fallback: Generic names
            columnNames.add("col1"); // Minimal fallback
        }
        return columnNames;
    }

    private List<String> parseParameters(String paramString) {
        List<String> params = new ArrayList<>();
        if (paramString.isEmpty()) {
            return params;
        }

        // Match quoted strings, capturing content inside quotes
        Pattern pattern = Pattern.compile("'([^']*)'");
        Matcher matcher = pattern.matcher(paramString);
        while (matcher.find()) {
            String param = matcher.group(1); // Capture group 1 (content inside quotes)
            params.add(param);
        }
        return params;
    }
}