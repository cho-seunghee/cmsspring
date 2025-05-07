package com.boot.cms.service.oper;

import com.boot.cms.entity.oper.MenuAuthEntity;
import com.boot.cms.service.mapview.MapViewProcessor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OperAuthGroupMenuService {

    private static final Logger logger = LoggerFactory.getLogger(OperAuthGroupMenuService.class);

    private final MapViewProcessor mapViewProcessor;

    /**
     * 동적 프로시저 호출 결과를 계층형 JSON 형식으로 변환하여 반환합니다.
     *
     * @param rptCd  보고서 코드
     * @param params 파라미터 리스트
     * @return 처리된 결과 리스트 (계층형)
     * @throws IllegalArgumentException 파라미터 개수가 맞지 않을 경우
     */
    public List<Map<String, Object>> processDynamicView(String rptCd, List<String> params) {
        logger.debug("Processing dynamic view for rptCd: {}", rptCd);

        // MapViewProcessor를 통해 결과 조회
        List<Map<String, Object>> unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params);
        logger.debug("Unescaped Result List: {}", unescapedResultList);

        // Map을 MenuAuthEntity로 변환
        List<MenuAuthEntity> entities = unescapedResultList.stream()
                .map(this::mapToMenuAuthEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        logger.debug("Mapped Entities: {}", entities);

        // 계층형 JSON으로 변환
        List<Map<String, Object>> result;
        try {
            result = convertToHierarchicalFormat(entities);
        } catch (Exception e) {
            logger.error("Error converting to hierarchical format", e);
            return Collections.emptyList();
        }
        logger.debug("Hierarchical Result: {}", result);
        return result;
    }

    /**
     * Map 데이터를 MenuAuthEntity로 변환합니다.
     *
     * @param map 입력 데이터
     * @return MenuAuthEntity 객체
     */
    private MenuAuthEntity mapToMenuAuthEntity(Map<String, Object> map) {
        try {
            MenuAuthEntity entity = new MenuAuthEntity();
            entity.setMenuId((String) map.getOrDefault("MENUID", ""));
            entity.setMenuNm((String) map.getOrDefault("MENUNM", ""));
            entity.setMenuLevel(parseInt(map.getOrDefault("MENULEVEL", 0)));
            entity.setUpperMenuId((String) map.getOrDefault("UPPERMENUID", ""));
            entity.setMenuOrder(parseInt(map.getOrDefault("MENUORDER", 0)));
            entity.setAuthId((String) map.getOrDefault("AUTHID", ""));
            entity.setAuthNm((String) map.getOrDefault("AUTHNM", ""));
            entity.setAuthYn((String) map.getOrDefault("AUTHYN", "Y"));

            // 필수 필드 검증
            if (entity.getMenuId().isEmpty() || entity.getAuthId().isEmpty() || entity.getAuthNm().isEmpty()) {
                logger.warn("Invalid entity data: {}", map);
                return null;
            }
            return entity;
        } catch (Exception e) {
            logger.error("Error mapping to MenuAuthEntity: {}", map, e);
            return null;
        }
    }

    /**
     * 문자열을 정수로 변환합니다. 실패 시 기본값 반환.
     *
     * @param value 입력 값
     * @return 변환된 정수
     */
    private int parseInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid number format: {}", value);
                return 0;
            }
        }
        logger.warn("Unsupported type for parsing: {}", value.getClass());
        return 0;
    }

    /**
     * MenuAuthEntity 리스트를 평면화된 JSON 형식으로 변환하며, 계층적 순서를 유지합니다.
     *
     * @param entities 원본 데이터 리스트
     * @return 평면화된 JSON 형식의 리스트
     */
    private List<Map<String, Object>> convertToHierarchicalFormat(List<MenuAuthEntity> entities) {
        logger.debug("Converting {} entities to flat format with hierarchical order", entities.size());

        // MENUID로 그룹화
        Map<String, List<MenuAuthEntity>> groupedMenus = entities.stream()
                .collect(Collectors.groupingBy(MenuAuthEntity::getMenuId));
        logger.debug("Grouped Menus: {}", groupedMenus.keySet());

        // 메뉴 노드 맵: MENUID -> Map<String, Object>
        Map<String, Map<String, Object>> nodeMap = new HashMap<>();
        // 자식 메뉴 맵: UPPERMENUID -> List<MENUID>
        Map<String, List<String>> childrenMap = new HashMap<>();
        // MENULEVEL 계산 맵
        Map<String, Integer> menuLevelMap = new HashMap<>();

        // MENULEVEL 계산 및 자식 관계 구성
        for (String menuId : groupedMenus.keySet()) {
            calculateMenuLevel(menuId, groupedMenus, menuLevelMap, new HashSet<>());
        }
        logger.debug("Calculated Menu Levels: {}", menuLevelMap);

        // 각 메뉴를 노드로 변환 및 자식 관계 맵핑
        for (Map.Entry<String, List<MenuAuthEntity>> entry : groupedMenus.entrySet()) {
            String menuId = entry.getKey();
            List<MenuAuthEntity> menuRows = entry.getValue();

            // 첫 번째 행에서 메뉴 정보 가져오기
            MenuAuthEntity firstRow = menuRows.get(0);
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("MENUID", menuId);

            // MENULEVEL에 따라 MENUNM에 접두사 추가
            int menuLevel = menuLevelMap.getOrDefault(menuId, 1);
            String menuNm = firstRow.getMenuNm();
            switch (menuLevel) {
                case 1:
                    node.put("MENUNM", menuNm);
                    break;
                case 2:
                    node.put("MENUNM", "└ " + menuNm);
                    break;
                case 3:
                    node.put("MENUNM", "  └ " + menuNm);
                    break;
                default:
                    node.put("MENUNM", menuNm);
                    logger.warn("Unexpected MENULEVEL {} for MENUID={}", menuLevel, menuId);
            }
            node.put("MENUORDER", firstRow.getMenuOrder()); // 정렬용 임시 저장
            node.put("MENULEVEL", menuLevel); // 정렬용 임시 저장

            // 권한 정보 수집
            List<Map<String, Object>> authChildren = menuRows.stream()
                    .filter(row -> row.getAuthId() != null && !row.getAuthId().isEmpty())
                    .map(row -> {
                        Map<String, Object> authEntry = new LinkedHashMap<>();
                        authEntry.put("AUTHID", row.getAuthId());
                        authEntry.put("AUTHNM", row.getAuthNm());
                        authEntry.put("AUTHYN", row.getAuthYn());
                        authEntry.put("children", Collections.emptyList());
                        return authEntry;
                    })
                    .sorted(Comparator.comparing(m -> (String) m.get("AUTHID")))
                    .collect(Collectors.toList());

            node.put("children", authChildren);
            nodeMap.put(menuId, node);

            // 자식 관계 맵핑
            String upperMenuId = firstRow.getUpperMenuId();
            if (upperMenuId != null && !upperMenuId.isEmpty()) {
                childrenMap.computeIfAbsent(upperMenuId, k -> new ArrayList<>()).add(menuId);
            }
        }

        // 계층적 순서로 flatList 구성
        List<Map<String, Object>> flatList = new ArrayList<>();
        // 최상위 메뉴 (MENULEVEL=1 또는 UPPERMENUID 없음)
        List<String> topLevelMenus = groupedMenus.keySet().stream()
                .filter(menuId -> {
                    MenuAuthEntity firstRow = groupedMenus.get(menuId).get(0);
                    return firstRow.getUpperMenuId() == null || firstRow.getUpperMenuId().isEmpty();
                })
                .sorted(Comparator.comparing((String menuId) -> groupedMenus.get(menuId).get(0).getMenuOrder()))
                .collect(Collectors.toList());

        // 재귀적으로 노드 추가
        for (String menuId : topLevelMenus) {
            addMenuNode(menuId, nodeMap, childrenMap, groupedMenus, flatList);
        }

        // 불필요한 필드 제거
        flatList.forEach(node -> {
            node.remove("MENULEVEL");
            node.remove("MENUORDER");
        });

        logger.debug("Flat List with Hierarchical Order: {}", flatList);
        return flatList;
    }

    /**
     * MENUID를 기반으로 MENULEVEL을 계산합니다.
     *
     * @param menuId       현재 메뉴 ID
     * @param groupedMenus 그룹화된 메뉴 데이터
     * @param levelMap     MENULEVEL 저장 맵
     * @param visited      방문한 메뉴 ID 집합 (순환 방지)
     * @return 계산된 MENULEVEL
     */
    private int calculateMenuLevel(String menuId, Map<String, List<MenuAuthEntity>> groupedMenus,
                                   Map<String, Integer> levelMap, Set<String> visited) {
        if (levelMap.containsKey(menuId)) {
            return levelMap.get(menuId);
        }
        if (visited.contains(menuId)) {
            logger.warn("Circular reference detected for MENUID={}", menuId);
            return 1;
        }
        visited.add(menuId);

        List<MenuAuthEntity> menuRows = groupedMenus.get(menuId);
        if (menuRows == null || menuRows.isEmpty()) {
            logger.warn("No data for MENUID={}", menuId);
            return 1;
        }

        MenuAuthEntity firstRow = menuRows.get(0);
        String upperMenuId = firstRow.getUpperMenuId();
        if (upperMenuId == null || upperMenuId.isEmpty()) {
            levelMap.put(menuId, 1);
            return 1;
        }

        int parentLevel = calculateMenuLevel(upperMenuId, groupedMenus, levelMap, visited);
        int currentLevel = parentLevel + 1;
        levelMap.put(menuId, currentLevel);
        return currentLevel;
    }

    /**
     * 메뉴 노드를 재귀적으로 flatList에 추가합니다.
     *
     * @param menuId       현재 메뉴 ID
     * @param nodeMap      메뉴 노드 맵
     * @param childrenMap  자식 메뉴 맵
     * @param groupedMenus 그룹화된 메뉴 데이터
     * @param flatList     결과 리스트
     */
    private void addMenuNode(String menuId, Map<String, Map<String, Object>> nodeMap,
                             Map<String, List<String>> childrenMap,
                             Map<String, List<MenuAuthEntity>> groupedMenus,
                             List<Map<String, Object>> flatList) {
        // 현재 노드 추가
        Map<String, Object> node = nodeMap.get(menuId);
        if (node == null) {
            logger.warn("Node not found for MENUID={}", menuId);
            return;
        }
        flatList.add(node);
        logger.debug("Added to flatList: MENUID={}", menuId);

        // 자식 메뉴 가져오기 및 정렬
        List<String> children = childrenMap.getOrDefault(menuId, Collections.emptyList());
        children.sort(Comparator.comparing((String childId) -> {
            List<MenuAuthEntity> rows = groupedMenus.get(childId);
            return rows != null ? rows.get(0).getMenuOrder() : Integer.MAX_VALUE;
        }));

        // 자식 메뉴 재귀적으로 추가
        for (String childId : children) {
            addMenuNode(childId, nodeMap, childrenMap, groupedMenus, flatList);
        }
    }
}