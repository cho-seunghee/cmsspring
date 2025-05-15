package com.boot.cms.service.auth;

import com.boot.cms.entity.auth.AuthMenuEntity;
import com.boot.cms.mapper.auth.AuthMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthMenuService {
    private final AuthMenuMapper authMenuMapper;

    public List<Map<String, Object>> getMenuTree(String userId) {
        // 메뉴 데이터 조회
        List<AuthMenuEntity> menuEntities = authMenuMapper.findByMenu(userId);
        if (menuEntities == null || menuEntities.isEmpty()) {
            return Collections.emptyList();
        }

        // 트리 구조로 변환
        return convertToMenuTree(menuEntities);
    }

    private List<Map<String, Object>> convertToMenuTree(List<AuthMenuEntity> menuEntities) {
        // menuId를 키로 하는 맵 생성
        Map<String, AuthMenuEntity> menuMap = menuEntities.stream()
                .collect(Collectors.toMap(AuthMenuEntity::getMenuId, menu -> menu));

        // 노드 맵: menuId -> Map<String, Object>
        Map<String, Map<String, Object>> nodeMap = new HashMap<>();

        // 트리 구조를 저장할 리스트 (최상위 노드)
        List<Map<String, Object>> tree = new ArrayList<>();

        // 각 메뉴를 노드로 변환
        for (AuthMenuEntity menu : menuEntities) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("MENUID", menu.getMenuId());
            node.put("MENUNM", menu.getMenuNm());
            node.put("URL", menu.getUrl() != null ? menu.getUrl() : "");
            node.put("children", new ArrayList<Map<String, Object>>());
            nodeMap.put(menu.getMenuId(), node);

            // 최상위 메뉴 (MENULEVEL=1 또는 upperMenuId 없음)
            if (menu.getUpperMenuId() == null || menu.getUpperMenuId().isEmpty()) {
                tree.add(node);
            } else {
                // 상위 메뉴의 children에 추가
                Map<String, Object> parentNode = nodeMap.get(menu.getUpperMenuId());
                if (parentNode != null) {
                    ((List<Map<String, Object>>) parentNode.get("children")).add(node);
                } else {
                    // 부모가 아직 처리되지 않은 경우, 임시로 트리에 추가 (후에 제거 가능)
                    tree.add(node);
                }
            }
        }

        // menuOrder 기준으로 정렬
        tree.sort(Comparator.comparing(n -> {
            String menuId = menuEntities.stream()
                    .filter(m -> m.getMenuNm().equals(n.get("MENUNM")))
                    .findFirst()
                    .map(AuthMenuEntity::getMenuId)
                    .orElse("");
            return menuMap.get(menuId).getMenuOrder();
        }));

        // 각 노드의 children 정렬
        nodeMap.values().forEach(node -> {
            List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
            children.sort(Comparator.comparing(n -> {
                String menuId = menuEntities.stream()
                        .filter(m -> m.getMenuNm().equals(n.get("MENUNM")))
                        .findFirst()
                        .map(AuthMenuEntity::getMenuId)
                        .orElse("");
                return menuMap.get(menuId).getMenuOrder();
            }));
        });

        // 빈 children 제거 및 유효하지 않은 노드 제거
        tree.removeIf(node -> {
            String menuId = menuEntities.stream()
                    .filter(m -> m.getMenuNm().equals(node.get("MENUNM")))
                    .findFirst()
                    .map(AuthMenuEntity::getMenuId)
                    .orElse("");
            AuthMenuEntity entity = menuMap.get(menuId);
            return entity.getUpperMenuId() != null && !entity.getUpperMenuId().isEmpty();
        });

        nodeMap.values().forEach(node -> {
            List<?> children = (List<?>) node.get("children");
            if (children.isEmpty()) {
                node.remove("children");
            }
        });

        return tree;
    }
}