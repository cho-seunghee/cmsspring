package com.boot.cms.repository.mapview;

import com.boot.cms.entity.mapview.MapViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MapViewRepository extends JpaRepository<MapViewEntity, String> {

    @Query(value = "SELECT RPTCD as rptCd, JOBNM as jobNm, PARAMCNT as paramCnt FROM tb_map_view WHERE RPTCD = :rptCd", nativeQuery = true)
    MapViewEntity findMapViewInfoByRptCd(String rptCd);
}