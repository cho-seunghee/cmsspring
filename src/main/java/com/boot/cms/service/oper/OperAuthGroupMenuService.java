package com.boot.cms.service.oper;

import com.boot.cms.entity.oper.MenuAuthEntity;
import com.boot.cms.service.mapview.MapViewProcessor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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

    @Setter
    @Getter
    String errorMessage;

    public List<Map<String, Object>> processDynamicView(String rptCd, List<String> params, String empNo, String jobGb) {
        List<Map<String, Object>> unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);
        List<MenuAuthEntity> entities = unescapedResultList.stream()
                .map(this::mapToMenuAuthEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Map<String, Object>> result;
        try {
            result = convertToHierarchicalFormat(entities);
        } catch (Exception e) {
            errorMessage = "Error converting to hierarchical format: ";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return Collections.emptyList();
        }
        return result;
    }

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

            if (entity.getMenuId().isEmpty()) {
                errorMessage = "Invalid entity data (missing MENUID): {}";
                logger.warn(this.getErrorMessage(), map);
                return null;
            }
            return entity;
        } catch (Exception e) {
            errorMessage = "Error mapping to MenuAuthEntity: {}";
            logger.error(this.getErrorMessage(), map, e.getMessage(), e);
            return null;
        }
    }

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
                errorMessage = "Invalid number format: {}";
                logger.warn(this.getErrorMessage(), value, e.getMessage(), e);
                return 0;
            }
        }

        errorMessage = "Unsupported type for parsing: {}";
        logger.warn(this.getErrorMessage(), value.getClass());
        return 0;
    }

    private List<Map<String, Object>> convertToHierarchicalFormat(List<MenuAuthEntity> entities) {
        Map<String, List<MenuAuthEntity>> groupedMenus = entities.stream()
                .collect(Collectors.groupingBy(MenuAuthEntity::getMenuId));

        Map<String, Integer> menuLevelMap = new HashMap<>();
        for (String menuId : groupedMenus.keySet()) {
            calculateMenuLevel(menuId, groupedMenus, menuLevelMap, new HashSet<>());
        }

        Set<String> allAuthNames = entities.stream()
                .filter(row -> row.getAuthNm() != null && !row.getAuthNm().isEmpty())
                .map(MenuAuthEntity::getAuthNm)
                .collect(Collectors.toCollection(TreeSet::new));

        Map<String, Map<String, Object>> nodeMap = new HashMap<>();
        Map<String, List<String>> childrenMap = new HashMap<>();

        for (Map.Entry<String, List<MenuAuthEntity>> entry : groupedMenus.entrySet()) {
            String menuId = entry.getKey();
            List<MenuAuthEntity> menuRows = entry.getValue();
            MenuAuthEntity firstRow = menuRows.get(0);
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("MENUID", menuId);

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
                    errorMessage = "Unexpected MENULEVEL {} for MENUID={}";
                    logger.warn(this.getErrorMessage(), menuLevel, menuId);
            }
            node.put("MENUORDER", firstRow.getMenuOrder());
            node.put("MENULEVEL", menuLevel);

            Map<String, Map<String, Object>> authMap = new HashMap<>();
            for (String authNm : allAuthNames) {
                Map<String, Object> authEntry = new LinkedHashMap<>();
                authEntry.put("AUTHID", "AUTH_" + authNm);
                authEntry.put("AUTHNM", authNm);
                authEntry.put("AUTHYN", "N");
                authEntry.put("children", Collections.emptyList());
                authMap.put(authNm, authEntry);
            }

            for (MenuAuthEntity row : menuRows) {
                if (row.getAuthNm() != null && !row.getAuthNm().isEmpty()) {
                    Map<String, Object> authEntry = authMap.get(row.getAuthNm());
                    if (authEntry != null) {
                        authEntry.put("AUTHID", row.getAuthId() != null && !row.getAuthId().isEmpty() ? row.getAuthId() : authEntry.get("AUTHID"));
                        authEntry.put("AUTHYN", row.getAuthYn());
                    }
                }
            }

            List<Map<String, Object>> authChildren = new ArrayList<>(authMap.values());
            authChildren.sort(Comparator.comparing(m -> (String) m.get("AUTHID"))); // Changed to sort by AUTHID
            node.put("children", authChildren);

            nodeMap.put(menuId, node);

            String upperMenuId = firstRow.getUpperMenuId();
            if (upperMenuId != null && !upperMenuId.isEmpty()) {
                childrenMap.computeIfAbsent(upperMenuId, k -> new ArrayList<>()).add(menuId);
            }
        }

        List<Map<String, Object>> flatList = new ArrayList<>();
        List<String> topLevelMenus = groupedMenus.keySet().stream()
                .filter(menuId -> {
                    MenuAuthEntity firstRow = groupedMenus.get(menuId).get(0);
                    return firstRow.getUpperMenuId() == null || firstRow.getUpperMenuId().isEmpty();
                })
                .sorted(Comparator.comparing((String menuId) -> groupedMenus.get(menuId).get(0).getMenuOrder()))
                .collect(Collectors.toList());

        for (String menuId : topLevelMenus) {
            addMenuNode(menuId, nodeMap, childrenMap, groupedMenus, flatList);
        }

        flatList.forEach(node -> {
            node.remove("MENULEVEL");
            node.remove("MENUORDER");
        });

        return flatList;
    }

    private int calculateMenuLevel(String menuId, Map<String, List<MenuAuthEntity>> groupedMenus,
                                   Map<String, Integer> levelMap, Set<String> visited) {
        if (levelMap.containsKey(menuId)) {
            return levelMap.get(menuId);
        }
        if (visited.contains(menuId)) {
            errorMessage = "Circular reference detected for MENUID={}";
            logger.warn(this.getErrorMessage(), menuId);
            return 1;
        }
        visited.add(menuId);

        List<MenuAuthEntity> menuRows = groupedMenus.get(menuId);
        if (menuRows == null || menuRows.isEmpty()) {
            errorMessage = "No data for MENUID={}";
            logger.warn(this.getErrorMessage(), menuId);
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

    private void addMenuNode(String menuId, Map<String, Map<String, Object>> nodeMap,
                             Map<String, List<String>> childrenMap,
                             Map<String, List<MenuAuthEntity>> groupedMenus,
                             List<Map<String, Object>> flatList) {
        Map<String, Object> node = nodeMap.get(menuId);
        if (node == null) {
            errorMessage = "Node not found for MENUID={}";
            logger.warn(this.getErrorMessage(), menuId);
            return;
        }
        flatList.add(node);

        List<String> children = childrenMap.getOrDefault(menuId, Collections.emptyList());
        children.sort(Comparator.comparing((String childId) -> {
            List<MenuAuthEntity> rows = groupedMenus.get(childId);
            return rows != null ? rows.get(0).getMenuOrder() : Integer.MAX_VALUE;
        }));

        for (String childId : children) {
            addMenuNode(childId, nodeMap, childrenMap, groupedMenus, flatList);
        }
    }
}