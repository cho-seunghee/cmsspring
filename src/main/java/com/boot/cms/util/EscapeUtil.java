package com.boot.cms.util;

import org.springframework.stereotype.Component;

@Component
public class EscapeUtil {

    /**
     * 입력값에 포함된 특수문자를 특정 유니코드 기호로 치환합니다.
     */
    public String escape(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str
                .replace("'", "ˮ")   // 단일 인용부호 → 유니코드
                .replace("\"", "˝")  // 이중 인용부호 → 유니코드
                .replace(";", "⁏")  // 세미콜론 → 유니코드
                .replace("\\", "∖") // 백슬래시 → 유니코드
                .replace("--", "—"); // SQL 주석 기호 "--" → 대체
    }

    /**
     * escape() 메서드로 치환된 유니코드 기호를 원본 특수문자로 복원합니다.
     */
    public static String unescape(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        return str
                .replace("ˮ", "'")   // 유니코드 → 단일 인용부호
                .replace("˝", "\"")  // 유니코드 → 이중 인용부호
                .replace("⁏", ";")  // 유니코드 → 세미콜론
                .replace("∖", "\\") // 유니코드 → 백슬래시
                .replace("—", "--"); // 대체 → SQL 주석 기호 "--"
    }
}
