package com.boot.cms.controller.notice;

import com.boot.cms.dto.common.ApiResponse;
import com.boot.cms.util.ResponseEntityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final ResponseEntityUtil responseEntityUtil;

    public static class NoticeData {
        private int id;
        private String title;
        private String content;
        private String date;

        public NoticeData(int id, String title, String content, String date) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.date = date;
        }

        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getDate() { return date; }
    }

    private List<Map<String, Object>> loadNotices() {
        List<NoticeData> noticeDataList = Arrays.asList(
                new NoticeData(1, "공지 1: 시스템 점검 안내", "서버 점검으로 인해 2시간 동안 서비스 이용이 제한됩니다.", "2025-04-02"),
                new NoticeData(2, "공지 2: 신규 기능 업데이트", "새로운 대시보드 기능이 추가되었습니다.", "2025-04-05"),
                new NoticeData(3, "공지 3: 이벤트 안내", "봄맞이 이벤트에 참여하세요!", "2025-04-10"),
                new NoticeData(4, "공지 4: 보안 패치", "최신 보안 패치를 적용했습니다.", "2025-04-15"),
                new NoticeData(5, "공지 5: 서비스 약관 변경", "서비스 약관이 일부 수정되었습니다.", "2025-04-20"),
                new NoticeData(6, "공지 6: 모바일 앱 업데이트", "모바일 앱의 새로운 버전이 출시되었습니다.", "2025-04-25"),
                new NoticeData(7, "공지 7: 고객 지원 시간 변경", "고객 지원 시간이 조정되었습니다.", "2025-04-30"),
                new NoticeData(8, "공지 8: 서버 업그레이드", "더 나은 성능을 위한 서버 업그레이드가 진행됩니다.", "2025-05-02"),
                new NoticeData(9, "공지 9: 신규 이벤트 안내", "여름맞이 특별 이벤트가 시작됩니다.", "2025-05-05"),
                new NoticeData(10, "공지 10: 결제 시스템 점검", "결제 시스템 점검이 예정되어 있습니다.", "2025-05-10"),
                new NoticeData(11, "공지 11: FAQ 업데이트", "자주 묻는 질문이 업데이트되었습니다.", "2025-05-15"),
                new NoticeData(12, "공지 12: 사용자 피드백 반영", "사용자 피드백을 반영한 개선사항을 적용했습니다.", "2025-05-20"),
                new NoticeData(13, "공지 13: 데이터 백업 안내", "정기 데이터 백업이 진행됩니다.", "2025-05-25"),
                new NoticeData(14, "공지 14: 보안 정책 강화", "보안 정책이 강화되었습니다.", "2025-05-30"),
                new NoticeData(15, "공지 15: 서비스 개선 안내", "더 나은 사용자 경험을 위한 서비스 개선이 완료되었습니다.", "2025-06-01")
        );

        return noticeDataList.stream()
                .map(notice -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", notice.getId());
                    map.put("title", notice.getTitle());
                    map.put("content", notice.getContent());
                    map.put("date", notice.getDate());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getNoticeList(
            @RequestBody Map<String, String> request
    ) {
        try {
            List<Map<String, Object>> resultList = loadNotices();

            if (resultList.isEmpty()) {
                return responseEntityUtil.okBodyEntity(null, "01", "조회 결과 없습니다.");
            }
            return responseEntityUtil.okBodyEntity(resultList);
        } catch (Exception e) {
            return responseEntityUtil.okBodyEntity(null, "01", "공지 목록 조회 중 오류 발생: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deleteNotice(
            @RequestBody Map<String, Integer> request
    ) {
        Integer id = request.get("id");
        if (id == null) {
            return responseEntityUtil.okBodyEntity(null, "01", "파라미터가 잘못되어 있습니다.");
        }

        try {
            // Simulate deletion (replace with actual logic if DB is used)
            return responseEntityUtil.okBodyEntity("Notice deleted successfully");
        } catch (Exception e) {
            return responseEntityUtil.okBodyEntity(null, "01", "공지 삭제 중 오류 발생: " + e.getMessage());
        }
    }
}