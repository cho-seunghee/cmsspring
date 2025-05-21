package com.boot.cms.service.mapview;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
@RequiredArgsConstructor
public class DynamicQueryService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicQueryService.class);

    private final DataSource dataSource;

    @Setter
    @Getter
    String errorMessage;

    private String createCallString(String procedureName, int paramCount) {
        String placeholders = String.join(",", Collections.nCopies(paramCount, "?"));
        return "{call " + procedureName + "(" + placeholders + ")}";
    }

    public List<Map<String, Object>> executeDynamicQuery(String procedureCall) {
        List<Map<String, Object>> mappedResult = new ArrayList<>();
        Connection connection = null;
        CallableStatement stmt = null;
        ResultSet rs = null;

        try {
            String procedureName = procedureCall.substring(0, procedureCall.indexOf('(')).trim();
            String paramString = procedureTAB("(", ")", procedureCall).trim();
            List<String> params = parseParameters(paramString);
            String callString = createCallString(procedureName, params.size());

            connection = DataSourceUtils.getConnection(dataSource);
            if (connection == null) {
                throw new IllegalStateException("Unable to obtain JDBC Connection from DataSource");
            }

            stmt = connection.prepareCall(callString);

            for (int i = 0; i < params.size(); i++) {
                String param = params.get(i);
                stmt.setString(i + 1, param);
            }

            boolean hasResultSet = stmt.execute();
            if (!hasResultSet) {
                return Collections.emptyList();
            }

            rs = stmt.getResultSet();
            if (rs == null) {
                return Collections.emptyList();
            }

            List<String> columnNames = getColumnNames(rs);
            while (rs.next()) {
                Map<String, Object> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < columnNames.size(); i++) {
                    Object value = rs.getObject(i + 1);
                    rowMap.put(columnNames.get(i), value == null ? "" : value);
                }
                mappedResult.add(rowMap);
            }

        } catch (Exception e) {
            errorMessage = "Error executing stored procedure: {}, Error: {}";
            logger.error(this.getErrorMessage(), procedureCall, e.getMessage(), e);
            throw new IllegalArgumentException("데이터베이스 오류: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                DataSourceUtils.releaseConnection(connection, dataSource);
            } catch (Exception e) {
                errorMessage = "Error closing resources: {}";
                logger.error(this.getErrorMessage(), e.getMessage(), e);
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
                errorMessage = "No columns found in ResultSet";
                logger.warn(this.getErrorMessage());
                columnNames.add("col1");
                return columnNames;
            }
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i) != null ? metaData.getColumnLabel(i) : metaData.getColumnName(i);
                columnNames.add(columnName);
            }
        } catch (Exception e) {
            errorMessage = "Failed to extract column names: {}";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            columnNames.add("col1");
        }
        return columnNames;
    }

    private List<String> parseParameters(String paramString) {
        List<String> params = new ArrayList<>();
        if (paramString.isEmpty()) {
            return params;
        }

        Pattern pattern = Pattern.compile("'([^']*)'");
        Matcher matcher = pattern.matcher(paramString);
        while (matcher.find()) {
            String param = matcher.group(1);
            if (param.contains("(") || param.contains(")")) {
                param = param.replaceAll("[()]", "");
            }
            params.add(param);
        }
        return params;
    }

    private String procedureTAB(String open, String close, String procedureCall) {
        int start = procedureCall.indexOf(open) + 1;
        int end = procedureCall.lastIndexOf(close);
        if (start < 0 || end < 0 || start >= end) {
            return "";
        }
        return procedureCall.substring(start, end);
    }
}