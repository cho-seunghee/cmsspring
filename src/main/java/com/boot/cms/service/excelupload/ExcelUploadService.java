package com.boot.cms.service.excelupload;

import com.boot.cms.config.AppConfig;
import com.boot.cms.entity.excelupload.ExcelUploadTableInfoEntity;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import com.boot.cms.util.EscapeUtil;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 엑셀 업로드 서비스 클래스
 * tb_exceluploadtableinfo 테이블에서 업로드 기준 정보를 가져와 엑셀 파일을 처리하고,
 * 동적으로 INSERT 쿼리를 생성하여 대상 테이블에 데이터를 삽입
 */
@Service
@RequiredArgsConstructor
public class ExcelUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelUploadService.class);
    private static final int BATCH_SIZE = 1000; // 서버 부하 방지를 위한 배치 크기

    // 단일 날짜/시간 상수 및 포맷터
    private final LocalDateTime CURRENT_DATE_TIME = LocalDateTime.now();
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private final DateTimeFormatter DATE_FORMATTER_DB = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final DateTimeFormatter UPLOAD_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final DataSource dataSource;
    private final AppConfig.FileConfig fileConfig;
    @Autowired
    private EscapeUtil escapeUtil;

    private String errorMessage;
    private String query;

    /**
     * 비동기 엑셀 파일 업로드 처리
     * @param rptCd 업로드 키코드
     * @param workbook 엑셀 워크북
     * @param empNo 사원 번호
     * @param empNm 사원 이름
     */
    @Async
    public void asyncExcelUpload(String rptCd, Workbook workbook, String empNo, String empNm) {
        try {
            excelUpload(rptCd, workbook, empNo, empNm);
        } catch (Exception e) {
            errorMessage = "비동기 엑셀 업로드 처리 중 오류: " + rptCd;
            logger.error(errorMessage, e);
            insertExcelUploadHist(rptCd, "N", errorMessage + ": " + e.getMessage());
            insertExcelUploadResult(rptCd, generateUploadKey(), "0", "N", "0", errorMessage + ": " + e.getMessage(), empNo, empNm);
        } finally {
            // Workbook 자원 해제
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    logger.error("Workbook 닫기 실패", e);
                }
            }
        }
    }

    /**
     * 엑셀 파일 업로드 처리
     * @param rptCd 업로드 키코드
     * @param workbook 엑셀 워크북
     * @param empNo 사원 번호
     * @param empNm 사원 이름
     */
    public void excelUpload(String rptCd, Workbook workbook, String empNo, String empNm) {
        // 입력 유효성 검사
        if (workbook == null) {
            errorMessage = "엑셀 워크북이 null입니다.";
            logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        // 업로드 기준 정보 조회
        ExcelUploadTableInfoEntity tableInfo = getExcelUploadTableInfo(rptCd);
        if (tableInfo == null || !"Y".equals(tableInfo.getUseYn())) {
            errorMessage = "유효한 업로드 설정이 없습니다: " + rptCd;
            logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        // 대상 테이블 TRUNCATE
        Connection conn = null;
        Statement stmt = null;

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            query = "TRUNCATE TABLE " + tableInfo.getTargetTable();
            stmt.executeUpdate(query);
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("트랜잭션 롤백 실패", ex);
                }
            }
            errorMessage = "테이블 TRUNCATE 실패: " + tableInfo.getTargetTable();
            logger.error(errorMessage + ", query={}", query, e);
            throw new IllegalArgumentException(errorMessage, e);
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error("데이터베이스 자원 해제 오류", e);
            }
        }

        int currentRowNum = 0; // 오류 추적용 행 번호
        int currentColNum = 0; // 오류 추적용 열 번호

        try {
            Sheet sheet = workbook.getSheetAt(0);
            int endRowNum = sheet.getPhysicalNumberOfRows();

            // 실제 데이터가 있는 컬럼 수 계산 (헤더 행 기준)
            int endColNum = 0;
            Row headerRow = sheet.getRow(tableInfo.getStartRow() - 1);
            if (headerRow != null) {
                for (int colNum = 0; colNum < headerRow.getLastCellNum(); colNum++) {
                    Cell cell = headerRow.getCell(colNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String cellValue = formatCellValue(cell, DATE_FORMATTER, TIME_FORMATTER);
                    if (!cellValue.isEmpty()) {
                        endColNum++; // 비어있지 않은 셀만 카운트
                    }
                }
            }

            // 헤더 행 컬럼 수 유효성 검사
            if (endColNum != tableInfo.getColCnt()) {
                errorMessage = "[DB: " + tableInfo.getColCnt() + ", 엑셀: " + endColNum + "] " + "헤더 컬럼 수가 일치하지 않습니다";
                logger.error(errorMessage);
                insertExcelUploadHist(rptCd, "N", errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }

            List<String> insertValues = new ArrayList<>();
            int rowCount = 0;

            // 엑셀 데이터 처리
            for (int rowNum = tableInfo.getStartRow(); rowNum <= endRowNum; rowNum++) {
                currentRowNum = rowNum; // 현재 행 번호 저장
                Row row = sheet.getRow(rowNum - 1);
                if (row == null) continue;

                // 데이터 행의 컬럼 수 계산
                int dataColNum = 0;
                for (int colNum = 0; colNum < row.getLastCellNum(); colNum++) {
                    Cell cell = row.getCell(colNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String cellValue = formatCellValue(cell, DATE_FORMATTER, TIME_FORMATTER);
                    if (!cellValue.isEmpty()) {
                        dataColNum++;
                    }
                }

                // 데이터 행 컬럼 수 유효성 검사
                if (dataColNum > tableInfo.getColCnt()) {
                    errorMessage = "[DB: " + tableInfo.getColCnt() + ", 엑셀: " + dataColNum + "] " + "데이터 행 컬럼 수가 초과되었습니다";
                    logger.error(errorMessage);
                    insertExcelUploadHist(rptCd, "N", errorMessage);
                    throw new IllegalArgumentException(errorMessage);
                }

                StringBuilder rowValue = new StringBuilder("(NULL,");
                boolean hasData = false;

                for (int colNum = 0; colNum < tableInfo.getColCnt(); colNum++) {
                    currentColNum = colNum + 1; // 열 번호 (1부터 시작, 사용자 친화적)
                    Cell cell = row.getCell(colNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String cellValue = formatCellValue(cell, DATE_FORMATTER, TIME_FORMATTER);
                    String escapedValue = escapeUtil.escape(cellValue != null ? cellValue : "");
                    rowValue.append("'").append(escapedValue).append("',");
                    if (!cellValue.isEmpty()) hasData = true;
                }

                if (!hasData) continue;

                rowValue.setLength(rowValue.length() - 1);
                rowValue.append(")");
                insertValues.add(rowValue.toString());
                rowCount++;

                if (insertValues.size() >= BATCH_SIZE || rowNum == endRowNum) {
                    executeBatchInsert(tableInfo, insertValues);
                    insertValues.clear();
                }
            }

            // 성공 로그 기록
            insertExcelUploadHist(rptCd, "Y", "업로드 성공");
            insertExcelUploadResult(rptCd, generateUploadKey(), String.valueOf(rowCount), "Y", String.valueOf(rowCount), "업로드 성공", empNo, empNm);

        } catch (Exception e) {
            errorMessage = "엑셀 오류: 열(" + currentColNum + "), 행(" + currentRowNum + "): " + e.getMessage();
            logger.error(errorMessage, e);
            insertExcelUploadHist(tableInfo.getUploadName(), "N", errorMessage);
            throw new IllegalArgumentException(errorMessage, e);
        }
    }

    /**
     * tb_exceluploadtableinfo 테이블에서 업로드 기준 정보 조회
     * @param rptCd 업로드 키코드
     * @return 업로드 기준 정보 엔티티
     */
    private ExcelUploadTableInfoEntity getExcelUploadTableInfo(String rptCd) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ExcelUploadTableInfoEntity tableInfo = null;

        try {
            conn = dataSource.getConnection();
            query = "SELECT UPLOADNAME, TARGETTABLE, STARTROW, COLCNT, USEYN, DELYN " +
                    "FROM tb_exceluploadtableinfo WHERE RPTCD = ? AND USEYN = 'Y'";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, rptCd);
            rs = stmt.executeQuery();

            if (rs.next()) {
                tableInfo = new ExcelUploadTableInfoEntity();
                tableInfo.setUploadName(rs.getString("UPLOADNAME"));
                tableInfo.setTargetTable(rs.getString("TARGETTABLE"));
                tableInfo.setStartRow(rs.getInt("STARTROW"));
                tableInfo.setColCnt(rs.getInt("COLCNT"));
                tableInfo.setUseYn(rs.getString("USEYN"));
                tableInfo.setDelYn(rs.getString("DELYN"));
            }
        } catch (SQLException e) {
            errorMessage = "업로드 기준 정보 조회 실패: " + rptCd;
            logger.error(errorMessage + ", query={}", query, e);
            throw new IllegalArgumentException(errorMessage, e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                logger.error("데이터베이스 자원 해제 오류", e);
            }
        }
        return tableInfo;
    }

    /**
     * 배치 INSERT 실행
     * @param tableInfo 업로드 기준 정보
     * @param insertValues 삽입할 값 목록
     */
    private void executeBatchInsert(ExcelUploadTableInfoEntity tableInfo, List<String> insertValues) {
        if (insertValues.isEmpty()) return;

        Connection conn = null;
        Statement stmt = null;
        PreparedStatement deleteStmt = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();

            // INSERT 쿼리 동적 생성 및 실행
            query = "INSERT INTO " + tableInfo.getTargetTable() + " VALUES " + String.join(",", insertValues);
            stmt.executeUpdate(query);

            // 조건부 삭제 (DELYN = 'Y')
            if ("Y".equals(tableInfo.getDelYn())) {
                query = "TRUNCATE TABLE " + tableInfo.getTargetTable();
                deleteStmt = conn.prepareStatement(query);
                deleteStmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("트랜잭션 롤백 실패, query={}", query, ex);
                }
            }
            errorMessage = "배치 삽입 실패: " + tableInfo.getUploadName();
            logger.error(errorMessage + ", query={}", query, e);
            throw new IllegalArgumentException(errorMessage, e);
        } finally {
            try {
                if (deleteStmt != null) deleteStmt.close();
                if (stmt != null) stmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error("데이터베이스 자원 해제 오류", e);
            }
        }
    }

    /**
     * 셀 값 포맷팅
     * @param cell 엑셀 셀
     * @param dateFormatter 날짜 포맷
     * @param timeFormatter 시간 포맷
     * @return 포맷된 셀 값
     */
    private String formatCellValue(Cell cell, DateTimeFormatter dateFormatter, DateTimeFormatter timeFormatter) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }

        DataFormatter dataFormatter = new DataFormatter();
        String formattedValue = dataFormatter.formatCellValue(cell).trim();

        switch (cell.getCellType()) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    try {
                        String formatString = cell.getCellStyle().getDataFormatString().toLowerCase();
                        // 시간 형식(h:mm, h:mm:ss, h:mm:ss AM/PM 등) 감지
                        if (formatString.matches(".*(h:mm|hh:mm|h:m|hh:m|AM/PM|am/pm).*")) {
                            // 엑셀의 소수점 시간 값 직접 처리
                            double numericValue = cell.getNumericCellValue();
                            long totalSeconds = Math.round(numericValue * 24 * 60 * 60); // 소수점 → 초
                            int hours = (int) (totalSeconds / 3600);
                            int minutes = (int) ((totalSeconds % 3600) / 60);
                            int seconds = (int) (totalSeconds % 60);
                            LocalTime time = LocalTime.of(hours % 24, minutes, seconds);
                            return time.format(timeFormatter); // HH:mm:ss (e.g., 11:00:02, 12:05:00)
                        }
                        // 날짜 형식
                        java.util.Date utilDate = cell.getDateCellValue();
                        LocalDateTime dateTime = utilDate.toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime();
                        return dateTime.toLocalDate().format(dateFormatter); // yyyy-MM-dd
                    } catch (Exception e) {
                        logger.error("날짜/시간 형식 변환 실패: 셀={}, 값={}, 형식={}",
                                cell, formattedValue, cell.getCellStyle().getDataFormatString(), e);
                        return formattedValue;
                    }
                } else {
                    // NUMERIC 셀이지만 날짜 형식이 아닌 경우 (e.g., 20250603, 123456)
                    if (formattedValue.matches("\\d{8}")) {
                        try {
                            LocalDate date = LocalDate.parse(formattedValue, DateTimeFormatter.ofPattern("yyyyMMdd"));
                            return formattedValue; // 20250603
                            // Optionally convert to yyyy-MM-dd:
                            // return date.format(dateFormatter); // 2025-06-03
                        } catch (DateTimeParseException e) {
                            return formattedValue; // Non-date number (e.g., 123456)
                        }
                    }
                    return formattedValue; // 123456 등 그대로 반환
                }
            case STRING:
                String strVal = cell.getStringCellValue().trim();
                if (strVal.isEmpty()) {
                    return "";
                }
                // 맨 앞 single quote 처리
                String processedVal = strVal.startsWith("'") && strVal.length() > 1 ? strVal.substring(1) : strVal;

                // 숫자형 (yyyyMMdd)
                if (processedVal.matches("\\d{8}")) {
                    try {
                        LocalDate date = LocalDate.parse(processedVal, DateTimeFormatter.ofPattern("yyyyMMdd"));
                        return processedVal; // 20250603
                        // Optionally convert to yyyy-MM-dd:
                        // return date.format(dateFormatter); // 2025-06-03
                    } catch (DateTimeParseException e) {
                        return processedVal; // Non-date number
                    }
                }
                // 날짜 형식 (yyyy/MM/dd)
                if (processedVal.matches("\\d{4}/\\d{2}/\\d{2}")) {
                    try {
                        LocalDate date = LocalDate.parse(processedVal, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                        return date.format(dateFormatter); // yyyy-MM-dd
                    } catch (DateTimeParseException e) {
                        return processedVal;
                    }
                }
                // 시간 형식 (H:mm, H:mm:ss, H:mm:ss AM/PM)
                if (processedVal.matches("\\d{1,2}:\\d{2}(:\\d{2})?(\\s*(AM|PM|am|pm))?")) {
                    String cleanedTime = processedVal.replaceAll("\\s*(AM|PM|am|pm)", "").trim();
                    try {
                        DateTimeFormatter timeParseFormatter = cleanedTime.matches("\\d{1,2}:\\d{2}:\\d{2}")
                                ? DateTimeFormatter.ofPattern("H:mm:ss")
                                : DateTimeFormatter.ofPattern("H:mm");
                        LocalTime time = LocalTime.parse(cleanedTime, timeParseFormatter);
                        return time.format(timeFormatter); // HH:mm:ss
                    } catch (DateTimeParseException e) {
                        return cleanedTime;
                    }
                }
                // 비숫자형 (e.g., 이철수2)
                return processedVal.equals("-") ? "0" : processedVal;
            default:
                return formattedValue.isEmpty() ? "" : formattedValue;
        }
    }

    /**
     * 업로드 이력 삽입
     * @param rptCd 업로드 키코드
     * @param resultYn 결과 여부 (Y/N)
     * @param message 메시지
     */
    private void insertExcelUploadHist(String rptCd, String resultYn, String message) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = dataSource.getConnection();
            String sql = "INSERT INTO tb_exceluploadhist (RPTCD, UPLOADDT, RESULTYN, MESSAGE) VALUES (?, NOW(), ?, ?)"; // 로컬 변수명 query와 충돌 방지를 위해 sql로 변경
            query = sql
                    .replaceFirst("\\?", "'" + escapeUtil.escape(rptCd) + "'")
                    .replaceFirst("\\?", "'" + escapeUtil.escape(resultYn) + "'")
                    .replaceFirst("\\?", "'" + escapeUtil.escape(message) + "'");
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, rptCd);
            stmt.setString(2, resultYn);
            stmt.setString(3, message);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("tb_exceluploadhist 삽입 실패: rptCd={}, query={}, error={}", rptCd, query, e.getMessage(), e);
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                logger.error("데이터베이스 자원 해제 오류", e);
            }
        }
    }

    /**
     * 업로드 결과 삽입
     * @param rptCd 업로드 키코드
     * @param uploadKey 업로드 키
     * @param uploadRow 업로드 행
     * @param resultYn 결과 여부 (Y/N)
     * @param totCnt 총 개수
     * @param message 메시지
     * @param empNo 사원 번호
     * @param empNm 사원 이름
     */
    private void insertExcelUploadResult(String rptCd, String uploadKey, String uploadRow, String resultYn, String totCnt, String message, String empNo, String empNm) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            String sql = "INSERT INTO tb_exceluploadresult (UPLOAD_KEY, UPLOAD_ROW, UPLOAD_MON, UPLOADDT, EMPNO, EMPNM, RPTCD, RESULTYN, TOT_CNT, MESSAGE) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // 로컬 변수명 query와 충돌 방지를 위해 sql로 변경
            query = sql
                    .replaceFirst("\\?", "'" + escapeUtil.escape(uploadKey) + "'")
                    .replaceFirst("\\?", "'" + escapeUtil.escape(uploadRow) + "'")
                    .replaceFirst("\\?", "'" + escapeUtil.escape(CURRENT_DATE_TIME.format(YEAR_MONTH_FORMATTER)) + "'")
                    .replaceFirst("\\?", "'" + escapeUtil.escape(CURRENT_DATE_TIME.format(DATE_FORMATTER_DB)) + "'")
                    .replaceFirst("\\?", "'" + escapeUtil.escape(empNo) + "'")
                    .replaceFirst("\\?", "'" + escapeUtil.escape(empNm != null ? empNm : "") + "'")
                    .replaceFirst("\\?", "'" + escapeUtil.escape(rptCd) + "'")
                    .replaceFirst("\\?", "'" + escapeUtil.escape(resultYn) + "'")
                    .replaceFirst("\\?", "'" + escapeUtil.escape(totCnt) + "'")
                    .replaceFirst("\\?", "'" + escapeUtil.escape(message) + "'");
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, uploadKey);
            stmt.setString(2, uploadRow);
            stmt.setString(3, CURRENT_DATE_TIME.format(YEAR_MONTH_FORMATTER));
            stmt.setString(4, CURRENT_DATE_TIME.format(DATE_FORMATTER_DB));
            stmt.setString(5, empNo);
            stmt.setString(6, empNm != null ? empNm : "");
            stmt.setString(7, rptCd);
            stmt.setString(8, resultYn);
            stmt.setString(9, totCnt);
            stmt.setString(10, message);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("tb_exceluploadresult 삽입 실패: rptCd={}, query={}, error={}", rptCd, query, e.getMessage(), e);
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                logger.error("데이터베이스 자원 해제 오류", e);
            }
        }
    }

    /**
     * 업로드 키 생성
     * @return 생성된 업로드 키
     */
    private String generateUploadKey() {
        return CURRENT_DATE_TIME.format(UPLOAD_KEY_FORMATTER);
    }
}